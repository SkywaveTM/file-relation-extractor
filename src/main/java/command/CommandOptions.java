package command;

import collect.GitRevisionCollector;
import collect.RevisionCollector;
import collect.SvnRevisionCollector;
import com.beust.jcommander.Parameter;
import export.CsvFileRelationExporter;
import export.FileRelationExporter;
import export.SqliteFileRelationExporter;
import merge.DistanceMerger;
import merge.NoMerger;
import merge.RevisionMerger;
import merge.SlidingWindowMerger;
import model.FileName;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.tmatesoft.svn.core.SVNException;
import utils.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

// http://jcommander.org/
public class CommandOptions {
    public static final String PARAMETER_FULL_BRANCH = "--branch";
    public static final String PARAMETER_SHORT_BRANCH = "-b";
    public static final String PARAMETER_FULL_GIT_TEMP_DIR = "--git-temp-dir";
    public static final String PARAMETER_SHORT_GIT_TEMP_DIR = "-gtd";
    public static final String PARAMETER_FULL_PRESERVE_GIT_TEMP = "--preserve-git-temp-dir";
    public static final String PARAMETER_SHORT_PRESERVE_GIT_TEMP = "-pgtd";
    public static final String PARAMETER_FULL_FROM = "--from";
    public static final String PARAMETER_SHORT_FROM = "-f";
    public static final String PARAMETER_FULL_TO = "--to";
    public static final String PARAMETER_SHORT_TO = "-t";
    public static final String PARAMETER_FULL_SLIDING_WINDOW_SIZE = "--sliding-window-size";
    public static final String PARAMETER_SHORT_SLIDING_WINDOW_SIZE = "-sws";
    public static final String PARAMETER_FULL_MERGE_METHOD = "--merge-method";
    public static final String PARAMETER_SHORT_MERGE_METHOD = "-mm";
    public static final String PARAMETER_FULL_EXPORT_FILE_NAME = "--export-file-name";
    public static final String PARAMETER_SHORT_EXPORT_FILE_NAME = "-efn";
    public static final String PARAMETER_FULL_EXPORT_TYPE = "--export-type";
    public static final String PARAMETER_SHORT_EXPORT_TYPE = "-et";
    public static final String PARAMETER_FULL_LIMIT = "--limit";
    public static final String PARAMETER_SHORT_LIMIT = "-l";
    public static final String PARAMETER_FULL_ALLOWED_EXTENSIONS = "--allowed-extensions";
    public static final String PARAMETER_SHORT_ALLOWED_EXTENSIONS = "-ae";
    public static final String PARAMETER_FULL_DUPLICATED_FILE_HANDLING = "--duplicated-file-handling";
    public static final String PARAMETER_SHORT_DUPLICATED_FILE_HANDLING = "-dfh";
    public static final String PARAMETER_FULL_IGNORE_SINGLE_FILE_GROUP = "--ignore-single-file-group";
    public static final String PARAMETER_SHORT_IGNORE_SINGLE_FILE_GROUP = "-isfg";
    public static final String PARAMETER_FULL_IGNORE_STRINGS = "--ignore-strings";
    public static final String PARAMETER_SHORT_IGNORE_STRINGS = "-is";
    public static final String PARAMETER_FULL_HELP = "--help";
    public static final String PARAMETER_SHORT_HELP = "-h";
    public static final String PARAMETER_FULL_MAX_DISTINCT_PACKAGES = "--max-distinct-packages";
    public static final String PARAMETER_SHORT_MAX_DISTINCT_PACKAGES = "-mdp";
    public static final String PARAMETER_FULL_DEBUG = "--debug";

    public static final String DATE_STRING_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_STRING_FORMAT);
    public static final String DEFAULT_GIT_TEMP_DIR = "git_temp";
    public static final String DEFAULT_BRANCH = "";
    public static final String DEFAULT_CSV_POSTFIX = "_csv";
    public static final String DEFAULT_SQLITE_POSTFIX = ".sqlite";

    public enum MergeMethod {Window, DuplicatedWindow, Distance, NoMerge}
    public enum DuplicatedHandlingMethod {Error, Override, Numbering}
    public enum UrlType {INVALID, GIT, SVN}

    public enum ExportType {CSV, SQLITE}

    /* COLLECT */
    @Parameter(description = "target url of SVN/GIT. ex: https://mygit.com/test/test.git, file:///c:/mygit/.git",
            arity = 1, required = true, validateValueWith = UrlValidator.class)
    private List<URL> targetUrl = new LinkedList<>();

    @Parameter(names = {PARAMETER_FULL_BRANCH, PARAMETER_SHORT_BRANCH}, description = "(GIT ONLY) target branch.")
    private String branch = DEFAULT_BRANCH;
    @Parameter(names = {PARAMETER_FULL_GIT_TEMP_DIR, PARAMETER_SHORT_GIT_TEMP_DIR}, description = "(REMOTE GIT ONLY) name of the temp directory.")
    private String gitTempDirectoryName = DEFAULT_GIT_TEMP_DIR;

    private File gitTempDirectory = null;

    @Parameter(names = {PARAMETER_FULL_PRESERVE_GIT_TEMP, PARAMETER_SHORT_PRESERVE_GIT_TEMP}, description = "(REMOTE GIT ONLY) preserves the temp. git directory.")
    private boolean preserveGitTempDir = false;

    @Parameter(names = {PARAMETER_FULL_FROM, PARAMETER_SHORT_FROM},
            converter = EpochConverter.class,
            description = "Starting time of the revisions. Must be '" + DATE_STRING_FORMAT + "' format.")
    private long revisionFrom = -1;

    @Parameter(names = {PARAMETER_FULL_TO, PARAMETER_SHORT_TO},
            converter = EpochConverter.class,
            description = "Ending time of the revisions. Must be '" + DATE_STRING_FORMAT + "' format.")
    private long revisionTo = -1;

    @Parameter(names = {PARAMETER_FULL_LIMIT, PARAMETER_SHORT_LIMIT},
            description = "Maximum number of commits. Latest commits will be used. 0 will be treated as no limit.")
    private int limit = 0;

    @Parameter(names = {PARAMETER_FULL_ALLOWED_EXTENSIONS, PARAMETER_SHORT_ALLOWED_EXTENSIONS}, variableArity = true,
            description = "Allowed Extensions to extract. Case insensitive, separated by space.")
    private List<String> extensions = Collections.singletonList("java");

    @Parameter(names = {PARAMETER_FULL_IGNORE_STRINGS, PARAMETER_SHORT_IGNORE_STRINGS}, variableArity = true,
            description = "Ignore files which are containing any these strings. Separated by space.")
    private List<String> ignoreStrings = Arrays.asList("test", "example", "docs");

    private UrlType urlType = null;

    /* MERGE */
    @Parameter(names = {PARAMETER_FULL_SLIDING_WINDOW_SIZE, PARAMETER_SHORT_SLIDING_WINDOW_SIZE},
            description = "Sliding window size in second(s) used in the merging phase.")
    private int slidingWindowSize = 60;

    @Parameter(names = {PARAMETER_FULL_MERGE_METHOD, PARAMETER_SHORT_MERGE_METHOD},
            description = "The method to merge commits into groups.")
    private MergeMethod mergeMethodType = MergeMethod.Window;

    @Parameter(names = {PARAMETER_FULL_IGNORE_SINGLE_FILE_GROUP, PARAMETER_SHORT_IGNORE_SINGLE_FILE_GROUP},
            description = "Ignore groups with single file.")
    private boolean ignoreSingleFileGroup = false;

    @Parameter(names = {PARAMETER_FULL_MAX_DISTINCT_PACKAGES, PARAMETER_SHORT_MAX_DISTINCT_PACKAGES}, description = "the maximum number of distinct packages in a single group.")
    private int maxDistinctPackages = 5;

    /* EXPORT */
    @Parameter(names = {PARAMETER_FULL_EXPORT_FILE_NAME, PARAMETER_SHORT_EXPORT_FILE_NAME},
            description = "File name of the export result.")
    private String exportFileName = "";

    private File exportFile = null;

    @Parameter(names = {PARAMETER_FULL_EXPORT_TYPE, PARAMETER_SHORT_EXPORT_TYPE}, description = "Type of a export file.")
    private ExportType exportType = ExportType.CSV;

    @Parameter(names = {PARAMETER_FULL_DUPLICATED_FILE_HANDLING, PARAMETER_SHORT_DUPLICATED_FILE_HANDLING},
            description = "The method to be used when the export file name already exists.")
    private DuplicatedHandlingMethod handlingType = DuplicatedHandlingMethod.Numbering;

    /* ETC */
    @Parameter(names = {PARAMETER_FULL_HELP, PARAMETER_SHORT_HELP}, help = true, description = "Prints options.")
    private boolean help = false;

    @Parameter(names = {PARAMETER_FULL_DEBUG}, hidden = true)
    private boolean debug = false;

    public URL getTargetUrl() {
        return targetUrl.get(0);
    }

    public void setTargetUrl(URL targetUrl) {
        this.targetUrl.clear();
        this.targetUrl.add(targetUrl);
        urlType = null;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    private String getGitTempDirectoryName() {
        return gitTempDirectoryName;
    }

    public File getGitTempDirectory() throws IOException {
        if (gitTempDirectory == null) {
            gitTempDirectory = generateFile(getGitTempDirectoryName());
        }

        return gitTempDirectory;
    }

    public void setGitTempDirectoryName(String gitTempDirectoryName) {
        this.gitTempDirectoryName = gitTempDirectoryName;
        gitTempDirectory = null;
    }

    public boolean isPreserveGitTempDir() {
        return preserveGitTempDir;
    }

    public void setPreserveGitTempDir(boolean preserveGitTempDir) {
        this.preserveGitTempDir = preserveGitTempDir;
    }

    public long getRevisionFrom() {
        return revisionFrom;
    }

    public void setRevisionFrom(long revisionFrom) {
        this.revisionFrom = revisionFrom;
    }

    public long getRevisionTo() {
        return revisionTo;
    }

    public void setRevisionTo(long revisionTo) {
        this.revisionTo = revisionTo;
    }

    public int getSlidingWindowSize() {
        return slidingWindowSize;
    }

    public void setSlidingWindowSize(int slidingWindowSize) {
        this.slidingWindowSize = slidingWindowSize;
    }

    public MergeMethod getMergeMethodType() {
        return mergeMethodType;
    }

    public void setMergeMethodType(MergeMethod mergeMethodType) {
        this.mergeMethodType = mergeMethodType;
    }

    public ExportType getExportType() {
        return exportType;
    }

    public void setExportType(ExportType exportType) {
        this.exportType = exportType;
    }

    private String getExportFileName() {
        if (exportFileName.isEmpty()) {
            String fileName = generateExportFileName(getTargetUrl().toExternalForm());

            switch (getExportType()) {
                case CSV:
                    fileName += DEFAULT_CSV_POSTFIX;
                    break;
                case SQLITE:
                    fileName += DEFAULT_SQLITE_POSTFIX;
                    break;
            }

            return fileName;
        }
        else {
            return exportFileName;
        }
    }

    public void setExportFileName(String exportFileName) {
        this.exportFileName = exportFileName;
        exportFile = null;
    }

    public File getExportFile() throws IOException {
        if (exportFile == null) {
            exportFile = generateFile(getExportFileName());
        }

        return exportFile;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<String> extensions) {
        this.extensions = extensions;
    }

    public DuplicatedHandlingMethod getHandlingType() {
        return handlingType;
    }

    public void setHandlingType(DuplicatedHandlingMethod handlingType) {
        this.handlingType = handlingType;
    }

    public boolean isIgnoreSingleFileGroup() {
        return ignoreSingleFileGroup;
    }

    public void setIgnoreSingleFileGroup(boolean ignoreSingleFileGroup) {
        this.ignoreSingleFileGroup = ignoreSingleFileGroup;
    }

    public int getMaxDistinctPackages() {
        return maxDistinctPackages;
    }

    public void setMaxDistinctPackages(int maxDistinctPackages) {
        this.maxDistinctPackages = maxDistinctPackages;
    }

    public List<String> getIgnoreStrings() {
        return ignoreStrings;
    }

    public void setIgnoreStrings(List<String> ignoreStrings) {
        this.ignoreStrings = ignoreStrings;
    }

    public boolean isHelp() {
        return help;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public UrlType getUrlType() {
        if (urlType == null) {
            generateRevisionCollector();
        }

        return urlType;
    }

    public RevisionCollector generateRevisionCollector() {
        try {
            GitRevisionCollector collector = new GitRevisionCollector(getTargetUrl());
            if (!getGitTempDirectoryName().isEmpty()) {
                collector.setTempDirectory(new File(getGitTempDirectoryName()));
            }
            collector.setPreserveTemp(isPreserveGitTempDir());

            if (!branch.isEmpty()) {
                collector.setBranch(branch);
            }

            urlType = UrlType.GIT;
            Logger.INSTANCE.info(String.format("%s GIT repository found.", collector.isLocalFile() ? "Local" : "Remote"));

            return collector;
        } catch (GitAPIException e) {
            try {
                SvnRevisionCollector collector = new SvnRevisionCollector(getTargetUrl());

                urlType = UrlType.SVN;
                Logger.INSTANCE.info("SVN repository found.");

                return collector;
            } catch (SVNException e1) {
                urlType = UrlType.INVALID;
                throw new IllegalArgumentException("Given URL is neither SVN nor GIT.");
            }
        }
    }

    private static String generateExportFileName(String svnUrl) {
        return svnUrl.replaceAll("[^a-zA-Z0-9]+$", "").replaceAll("[^a-zA-Z0-9]+", "_");
    }

    // generate File instance based on the given file name and the given handling type.
    private File generateFile(String fileName) throws IOException {
        File file = new File(fileName);

        if (file.exists()) {
            switch (getHandlingType()) {
                case Override:
                    if (!file.delete()) {
                        throw new IOException("Can't delete file.");
                    }
                    break;
                case Error:
                    throw new IOException("File already exists.");
                case Numbering:
                    int duplicatedCount = 0;
                    FileName newName = new FileName(fileName);

                    while (file.exists()) {
                        duplicatedCount++;
                        String newFile = String.format("%s_%d", newName.getName(),
                                duplicatedCount);
                        if (!newName.getExtension().isEmpty()) {
                            newFile += "." + newName.getExtension();
                        }

                        file = new File(newFile);
                    }
                    break;
            }
        }

        return file;
    }

    // generate RevisionMerger instance based on given options.
    public RevisionMerger generateMerger() {
        switch (getMergeMethodType()) {
            case DuplicatedWindow:
                return new SlidingWindowMerger()
                        .setWindowSizeInSecond(getSlidingWindowSize())
                        .setAllowDuplicated(true);
            case Window:
                return new SlidingWindowMerger()
                        .setWindowSizeInSecond(getSlidingWindowSize())
                        .setAllowDuplicated(false);
            case Distance:
                return new DistanceMerger()
                        .setWindowSizeInSecond(getSlidingWindowSize());
            default:
                return new NoMerger();
        }
    }

    public FileRelationExporter generateExporter() throws IOException, SQLException, ClassNotFoundException {
        switch (getExportType()) {
            case CSV:
                return new CsvFileRelationExporter(getExportFile());
            case SQLITE:
                return new SqliteFileRelationExporter(getExportFile());
            default:
                return null; // can't reach
        }
    }
}
