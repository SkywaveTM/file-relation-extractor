package collect;

import model.FileName;
import model.Revision;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import utils.Logger;

import java.net.URL;
import java.util.*;

public class SvnRevisionCollector extends RevisionCollector<SvnRevisionCollector> {
    private SVNURL url;
    private long latestRevisionId = 0;
    private long latestRevisionTime = 0;

    public SvnRevisionCollector(URL url) throws SVNException {
        this.url = SVNURL.parseURIEncoded(url.toExternalForm());
        SVNRepositoryFactory.create(this.url).getLatestRevision();
    }

    @Override
    public SvnRevisionCollector getThis() {
        return this;
    }

    @Override
    public Set<Revision> collect() throws SVNException {
        // get svnkit instances
        SVNClientManager svnClientManager = SVNClientManager.newInstance();
        SVNLogClient logClient = svnClientManager.getLogClient();

        // prepare revision collector
        RevisionLogHandler revisionLogHandler = new RevisionLogHandler();

        // fetch data from given url with collector

        SVNRevision from = getFrom() >= 0 ? SVNRevision.create(new Date(getFrom())) : null;
        SVNRevision to = getTo() >= 0 ? SVNRevision.create(new Date(getTo())) : null;

        logClient.doLog(url, new String[] {}, null, from, to, true, true, getLimit(), revisionLogHandler);

        // retrieve revision data from collector
        return revisionLogHandler.getRevisions();
    }

    @Override
    public Revision getLatestRevision() throws SVNException {

        // fetch the latest revision's data
        SVNRevision latestSvnRevision = SVNRevision.create(latestRevisionId);
        DirEntryHandler handler = new DirEntryHandler(latestRevisionId, latestRevisionTime);

        SVNLogClient logClient = SVNClientManager.newInstance().getLogClient();
        logClient.doList(url, null, latestSvnRevision, false, true, handler); // TODO: 2016-08-04 TOO SLOW!
        return handler.getLatestRevision();
    }

    private class RevisionLogHandler implements ISVNLogEntryHandler {
        private Set<Revision> revisions;

        public RevisionLogHandler() {
            revisions = new TreeSet<>();
        }

        public Set<Revision> getRevisions() {
            return revisions;
        }

        // this method is called by SVNLogClient for each SVNLogEntry.
        @Override
        public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
            Logger.INSTANCE.info(String.format("Revision %s (%s)", logEntry.getRevision(), logEntry.getDate()));

            Map<String, SVNLogEntryPath> changedPaths = logEntry.getChangedPaths();

            long revision = logEntry.getRevision();
            long time = logEntry.getDate().getTime();
            Revision newRevision = new Revision(String.valueOf(revision), time, logEntry.getAuthor(), logEntry.getMessage());

            // extract files
            changedPaths.keySet().stream()
                    .map(FileName::new)
                    .filter(x -> getAllowedExtensions().contains(x.getExtension()))
                    .filter(x -> getIgnoreStrings().stream().allMatch(y -> !x.toString().toLowerCase().contains(y)))
                    .forEach(newRevision::addFile);

            if (newRevision.getChangedFiles().size() > 0) {
                if (newRevision.getTime() > latestRevisionTime) {
                    latestRevisionTime = newRevision.getTime();
                    latestRevisionId = Long.parseLong(newRevision.getId());
                }

                revisions.add(newRevision);
            }

        }
    }

    private class DirEntryHandler implements ISVNDirEntryHandler {
        private Revision latestRevision;

        public DirEntryHandler(long id, long time) {
            latestRevision = new Revision(String.valueOf(id), time, "", "");
        }

        @Override
        public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
            System.out.println(dirEntry.getRelativePath());

            if (dirEntry.getKind().equals(SVNNodeKind.FILE)) {
                // TODO: 2016-08-12 extension filter
                latestRevision.addFile(new FileName(dirEntry.getRelativePath()));
            }
        }

        public Revision getLatestRevision() {
            return latestRevision;
        }
    }
}
