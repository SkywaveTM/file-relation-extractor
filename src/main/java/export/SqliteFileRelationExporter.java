package export;

import command.CommandOptions;
import model.*;
import utils.DateUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SqliteFileRelationExporter implements FileRelationExporter {

    private File dbFile;

    public SqliteFileRelationExporter(File dbFile) throws SQLException, ClassNotFoundException {
        this.dbFile = dbFile;
        dbFile.getAbsoluteFile().getParentFile().mkdirs();

        // test connection
        Connection c = getConnection(dbFile);
        c.close();
    }

    @Override
    public void exportOptions(CommandOptions options) throws SQLException {
        try {
            Connection c = getConnection(dbFile);


            createOptionTable(c);
            putOptionRows(options, c);

            c.commit(); // as auto commit is disabled, it should be manually called.
            c.close();
        } catch (ClassNotFoundException ignored) {

        }

    }

    @Override
    public void exportAccumulatedEntities(TreeMap<FileName, AccumulatedEntity> accumulated) throws SQLException {
        try {
            Connection c = getConnection(dbFile);

            createAccumulatedTable(c);
            putAccumulatedRows(accumulated, c);

            c.commit(); // as auto commit is disabled, it should be manually called.
            c.close();
        } catch (ClassNotFoundException ignored) {

        }
    }


    @Override
    public void exportPairEntities(TreeMap<FileName, TreeMap<FileName, Integer>> pairs) throws SQLException {
        try {
            Connection c = getConnection(dbFile);

            createPairTable(c);
            putPairRows(pairs, c);

            c.commit(); // as auto commit is disabled, it should be manually called.
            c.close();
        } catch (ClassNotFoundException ignored) {

        }
    }

    @Override
    public void exportGroups(Collection<RevisionGroup> groups) throws SQLException {
        try {
            Connection c = getConnection(dbFile);


            // todo: add author/message
            createGroupTable(c);
            putGroupRows(groups, c);

            c.commit(); // as auto commit is disabled, it should be manually called.
            c.close();
        } catch (ClassNotFoundException ignored) {

        }
    }

    @Override
    public void exportLatestRevision(Revision revision) throws Exception {
        // TODO: 2016-08-04  
    }

    private Connection getConnection(File file) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        c.setAutoCommit(false); // if not, performance is SERIOUSLY degraded.

        return c;
    }


    private void createOptionTable(Connection c) throws SQLException {
        Statement statement = c.createStatement();

        // create option table
        String createOption = String.format("CREATE TABLE %s (%S STRING PRIMARY KEY NOT NULL, %S STRING)",
                TABLE_OPTION, COLUMN_OPTION, COLUMN_VALUE);
        statement.executeUpdate(createOption);

        statement.close();
    }

    private void putOptionRows(CommandOptions options, Connection c) throws SQLException {
        String insertOption = String.format("INSERT INTO %S (%S, %S) VALUES (?, ?)",
                TABLE_OPTION, COLUMN_OPTION, COLUMN_VALUE);
        PreparedStatement insertOptionStatement = c.prepareStatement(insertOption);

        for (Field field : options.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object value = field.get(options);

                if (value == null) {
                    continue;
                }

                insertOptionStatement.setString(1, field.getName());
                insertOptionStatement.setString(2, value.toString());
            } catch (IllegalAccessException e) {
                continue;
            } finally {
                field.setAccessible(false);
            }

            insertOptionStatement.executeUpdate();
        }

        insertOptionStatement.close();
    }

    private void putAccumulatedRows(TreeMap<FileName, AccumulatedEntity> accumulated, Connection c) throws SQLException {
        String insertAccumulated = String.format("INSERT INTO %S (%S, %S, %S, %S, %S) VALUES (?, ?, ?, ?, ?)",
                TABLE_ACCUMULATED,
                COLUMN_DATE, COLUMN_FILE,
                COLUMN_SAME_COUNT, COLUMN_OTHER_COUNT, COLUMN_TOTAL_COUNT);
        PreparedStatement insertAccumulatedStatement = c.prepareStatement(insertAccumulated);

        for (Map.Entry<FileName, AccumulatedEntity> fileEntry : accumulated.entrySet()) {
            for (Map.Entry<Long, CountEntity> timeEntry : fileEntry.getValue().getCounts().entrySet()) {
                insertAccumulatedStatement.setString(1, DateUtil.getDate(timeEntry.getKey()));
                insertAccumulatedStatement.setString(2, fileEntry.getKey().toString());
                insertAccumulatedStatement.setInt(3, timeEntry.getValue().getSameCount());
                insertAccumulatedStatement.setInt(4, timeEntry.getValue().getOtherCount());
                insertAccumulatedStatement.setInt(5, timeEntry.getValue().getTotalCount());

                insertAccumulatedStatement.executeUpdate();
            }
        }
        insertAccumulatedStatement.close();
    }

    private void createAccumulatedTable(Connection c) throws SQLException {
        // create file table
        String createFile = String.format("CREATE TABLE %s (" +
                        "%s STRING NOT NULL, %s STRING NOT NULL," +
                        "%s INT, %s INT, %s INT, " +
                        "PRIMARY KEY(%s, %s))",
                TABLE_ACCUMULATED,
                COLUMN_DATE, COLUMN_FILE,
                COLUMN_SAME_COUNT, COLUMN_OTHER_COUNT, COLUMN_TOTAL_COUNT,
                COLUMN_DATE, COLUMN_FILE
        );
        PreparedStatement createFileStatement = c.prepareStatement(createFile);
        createFileStatement.execute();
        createFileStatement.close();
    }


    private void createPairTable(Connection c) throws SQLException {
        String createPair = String.format("CREATE TABLE %s (" +
                        "%s STRING NOT NULL, %s STRING NOT NULL, %s INT, " +
                        "PRIMARY KEY(%s, %s))",
                TABLE_PAIR,
                COLUMN_FROM, COLUMN_TO, COLUMN_COUNT,
                COLUMN_FROM, COLUMN_TO
        );

        PreparedStatement createPairStatement = c.prepareStatement(createPair);
        createPairStatement.execute();
        createPairStatement.close();
    }

    private void putPairRows(TreeMap<FileName, TreeMap<FileName, Integer>> pairs, Connection c) throws SQLException {
        String insertPair = String.format("INSERT INTO %S (%S, %S, %S) VALUES (?, ?, ?)",
                TABLE_PAIR,
                COLUMN_FROM, COLUMN_TO, COLUMN_COUNT);
        PreparedStatement insertPairStatement = c.prepareStatement(insertPair);

        for (Map.Entry<FileName, TreeMap<FileName, Integer>> from : pairs.entrySet()) {
            for (Map.Entry<FileName, Integer> to : from.getValue().entrySet()) {
                insertPairStatement.setString(1, from.getKey().toString());
                insertPairStatement.setString(2, to.getKey().toString());
                insertPairStatement.setInt(3, to.getValue());

                insertPairStatement.executeUpdate();
            }
        }

        insertPairStatement.close();
    }

    private void putGroupRows(Collection<RevisionGroup> groups, Connection c) throws SQLException {
        // group put statement
        String insertGroup = String.format("INSERT INTO %S (%S, %S) VALUES (?, ?)",
                TABLE_GROUP,
                COLUMN_GROUP_ID, COLUMN_DATE);
        PreparedStatement groupPairStatement = c.prepareStatement(insertGroup);

        // revision put statement
        String revisionInsert = String.format("INSERT INTO %S (%S, %S, %S) VALUES (?, ?, ?)",
                TABLE_REVISION, COLUMN_REVISION_ID, COLUMN_GROUP_ID, COLUMN_DATE);
        PreparedStatement revisionStatement = c.prepareStatement(revisionInsert);

        // file put statement
        String fileInsert = String.format("INSERT INTO %S (%S, %S) VALUES (?, ?)",
                TABLE_FILE, COLUMN_GROUP_ID, COLUMN_FILE);
        PreparedStatement fileStatement = c.prepareStatement(fileInsert);

        for (RevisionGroup group : groups) {
            groupPairStatement.setLong(1, group.getGroupId());
            groupPairStatement.setString(2, DateUtil.getDate(group.getHeadTime()));
            groupPairStatement.executeUpdate();

            for (Revision revision : group.getRevisions()) {
                // put revision data
                revisionStatement.setString(1, revision.getId());
                revisionStatement.setLong(2, group.getGroupId());
                revisionStatement.setString(3, DateUtil.getDate(revision.getTime()));
                revisionStatement.executeUpdate();
            }

            // put file data
            for (FileName file : group.getFiles()) {
                fileStatement.setLong(1, group.getGroupId());
                fileStatement.setString(2, file.toString());
                fileStatement.executeUpdate();
            }
        }

        revisionStatement.close();
        fileStatement.close();
        groupPairStatement.close();
    }

    private void createGroupTable(Connection c) throws SQLException {
        Statement statement = c.createStatement();

        // create group table
        String createGroup = String.format("CREATE TABLE %s (" +
                        "%s INT NOT NULL, %s STRING, " +
                        "PRIMARY KEY(%s))",
                TABLE_GROUP,
                COLUMN_GROUP_ID, COLUMN_DATE,
                COLUMN_GROUP_ID
        );
        statement.executeUpdate(createGroup);

        // create revision table
        String createRevision = String.format("CREATE TABLE %s (%s STRING PRIMARY KEY NOT NULL, " +
                        "%s STRING NOT NULL, %s STRING NOT NULL)",
                TABLE_REVISION, COLUMN_REVISION_ID,
                COLUMN_GROUP_ID, COLUMN_DATE);
        statement.executeUpdate(createRevision);

        // create file table
        String createFile = String.format("CREATE TABLE %s (" +
                        "%s STRING NOT NULL, %s STRING NOT NULL, " +
                        "PRIMARY KEY(%s, %s)," +
                        "FOREIGN KEY(%s) REFERENCES %s(%s))",
                TABLE_FILE,
                COLUMN_GROUP_ID, COLUMN_FILE,
                COLUMN_GROUP_ID, COLUMN_FILE,
                COLUMN_GROUP_ID, TABLE_GROUP, COLUMN_GROUP_ID
        );
        statement.executeUpdate(createFile);

        statement.close();
    }

}
