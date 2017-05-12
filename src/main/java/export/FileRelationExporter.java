package export;

import command.CommandOptions;
import model.AccumulatedEntity;
import model.FileName;
import model.Revision;
import model.RevisionGroup;

import java.sql.SQLException;
import java.util.Collection;
import java.util.TreeMap;

public interface FileRelationExporter {
    String TABLE_OPTION = "Options";
    String TABLE_REVISION = "Revisions";
    String TABLE_FILE = "Files";
    String TABLE_GROUP = "Groups";
    String TABLE_ACCUMULATED = "AccumulatedCounts";
    String TABLE_PAIR = "Pairs";
    String TABLE_LATEST_REVISION = "LatestRevision";

    String COLUMN_OPTION = "Option";
    String COLUMN_VALUE = "Value";
    String COLUMN_REVISION_ID = "RevisionID";
    String COLUMN_GROUP_ID = "GroupID";
    String COLUMN_FILE = "File";
    String COLUMN_DATE = "Date";
    String COLUMN_SAME_COUNT = "SameCount";
    String COLUMN_OTHER_COUNT = "OtherCount";
    String COLUMN_TOTAL_COUNT = "TotalCount";
    String COLUMN_FROM = "FromFile";
    String COLUMN_TO = "ToFile";
    String COLUMN_COUNT = "Count";
    String COLUMN_AUTHOR = "Author";
    String COLUMN_MESSAGE = "Message";

    void exportOptions(CommandOptions options) throws Exception;

    void exportAccumulatedEntities(TreeMap<FileName, AccumulatedEntity> accumulated) throws Exception;

    void exportPairEntities(TreeMap<FileName, TreeMap<FileName, Integer>> pairs) throws Exception;

    void exportGroups(Collection<RevisionGroup> groups) throws Exception;

    void exportLatestRevision(Revision revision) throws Exception;
}
