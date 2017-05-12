package collect;

import model.FileName;
import model.Revision;
import org.apache.commons.io.FileDeleteStrategy;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import utils.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class GitRevisionCollector extends RevisionCollector<GitRevisionCollector> {
    public static final String DEFAULT_BRANCH = "master";
    public static final String DEFAULT_TEMP = "git_temp/";

    private URL uri;
    private String branch;
    private List<String> branchList;
    private File tempDirectory = new File(DEFAULT_TEMP);
    private boolean localFile;
    private boolean preserveTemp = false;

    private Revision latestRevision = null;

    public GitRevisionCollector(URL uri) throws GitAPIException {
        this.uri = uri;
        this.localFile = uri.getProtocol().equals("file");

        branchList = Git.lsRemoteRepository()
                .setHeads(true)
                .setRemote(uri.toExternalForm())
                .call().stream()
                .map(Ref::getName)
                .map(x -> x.substring(x.lastIndexOf('/') + 1))
                .collect(Collectors.toList());

        if (branchList.contains(DEFAULT_BRANCH)) {
            setBranch(DEFAULT_BRANCH);
        } else {
            setBranch(branchList.get(0));
        }
    }

    public boolean isLocalFile() {
        return localFile;
    }

    public String[] getBranchList() {
        return branchList.toArray(new String[branchList.size()]);
    }

    public void setTempDirectory(File file) {
        tempDirectory = file;
    }

    public void setPreserveTemp(boolean value) {
        preserveTemp = value;
    }

    public void setBranch(String branch) {
        if (!branchList.contains(branch)) {
            throw new IllegalArgumentException(branch + " branch not exists");
        }

        this.branch = branch;
    }

    @Override
    public GitRevisionCollector getThis() {
        return this;
    }

    @Override
    public Set<Revision> collect() throws Exception {
        FileRepository repository = null;

        repository = getFileRepository();

        ObjectReader reader = repository.newObjectReader();
        Git git = new Git(repository);

        Iterable<RevCommit> commits = git.log().add(repository.resolve("refs/heads/" + branch)).setRevFilter(RevFilter.NO_MERGES).call();
        TreeSet<Revision> revisions = new TreeSet<>();

        long limit = getLimit() > 0 ? getLimit() : Long.MAX_VALUE;

        // commit info are sorted by time desc.
        RevCommit latestCommit = null;
        for (RevCommit commit : commits) {
            // check limit
            if (limit < 0) {
                break;
            }

            // check time range
            long commitTime = commit.getCommitTime() * 1000L;
            if (getFrom() > 0 && commitTime < getFrom()) {
                break;
            }

            if (getTo() > 0 && commitTime > getTo()) {
                continue;
            }

            Logger.INSTANCE.info(String.format("Commit %s (%s)", commit.getId().getName(), new Date(commitTime)));

            // maybe needs some changes? revfilter on log().filter()?
            // fixme: can't get initial commit files
            if (commit.getParentCount() != 1) {
                continue;
            }

            // get changed files
            RevCommit parent = commit.getParent(0);

            CanonicalTreeParser oldTree = new CanonicalTreeParser();
            oldTree.reset(reader, parent.getTree().getId());

            CanonicalTreeParser newTree = new CanonicalTreeParser();
            newTree.reset(reader, commit.getTree().getId());

            Revision newRevision = new Revision(commit.getId().getName(), commitTime, commit.getName(), commit.getFullMessage());
            List<DiffEntry> diffs = git.diff()
                    .setOldTree(oldTree)
                    .setNewTree(newTree)
                    .call();

            diffs.stream()
//                    .filter(x -> x.getChangeType() != DiffEntry.ChangeType.DELETE)
                    .map(DiffEntry::getNewPath)
                    .map(FileName::new)
                    .filter(x -> getAllowedExtensions().contains(x.getExtension().toLowerCase()))
                    .filter(x -> getIgnoreStrings().stream().allMatch(y -> !x.toString().toLowerCase().contains(y)))
                    .forEach(newRevision::addFile);

            // only use commits with changes files
            if (newRevision.getChangedFiles().size() > 0) {
                // check latest Revision

                if (latestCommit == null || commit.getCommitTime() > latestCommit.getCommitTime()) {
                    latestCommit = commit;
                }

                revisions.add(newRevision);
            }

            limit--;
        }

        latestRevision = getAllFiles(repository, latestCommit);

        git.close();
        reader.close();
        repository.close();

        if (!localFile && !preserveTemp) {
            deleteTempDirectory();
        }

        return revisions;
    }

    private FileRepository getFileRepository() throws GitAPIException, IOException {
        if (localFile) {
            return new FileRepository(uri.getPath());
        }

        if (tempDirectory.exists()) {
            try {
                FileDeleteStrategy.FORCE.delete(tempDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Logger.INSTANCE.info("Getting all commits...");
        Git.cloneRepository()
                .setBare(true)
                .setURI(uri.toExternalForm())
                .setProgressMonitor(new TextProgressMonitor(new Writer() {
                    @Override
                    public void write(char[] cbuf, int off, int len) throws IOException {
                        Logger.INSTANCE.info(String.copyValueOf(cbuf));
                    }

                    @Override
                    public void flush() throws IOException {

                    }

                    @Override
                    public void close() throws IOException {

                    }
                }))
                .setGitDir(tempDirectory)
                .setBranch(branch)
                .call();

        return new FileRepository(tempDirectory);
    }

    @Override
    public Revision getLatestRevision() {
        return latestRevision;
    }

    private Revision getAllFiles(FileRepository repository, RevCommit commit) throws IOException {
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(commit.getTree());
        treeWalk.setRecursive(true);

        Revision result = new Revision(commit.getId().toString(), commit.getCommitTime() * 1000, "", "");

        while (treeWalk.next()) {
            if (!treeWalk.isSubtree()) {
                // TODO: 2016-08-04 extension filter?
                result.addFile(new FileName(treeWalk.getPathString()));
            }
        }

        treeWalk.close();

        return result;
    }

    private void deleteTempDirectory() {
        if (tempDirectory.exists()) {
            try {
                // TODO: 2016-08-04 can't remove 
                FileDeleteStrategy.FORCE.delete(tempDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
