package export;

import command.CommandOptions;
import model.*;
import utils.DateUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;


// TODO: 2016-08-12 needs string normalization
public class CsvFileRelationExporter implements FileRelationExporter {
    public static final String FILE_POSTFIX = ".csv";

    private Path rootDirectory;

    public CsvFileRelationExporter(File root) throws IOException {
        rootDirectory = Paths.get(root.getAbsolutePath());
        Files.createDirectories(rootDirectory);
    }

    @Override
    public void exportOptions(CommandOptions options) throws Exception {
        Path path = rootDirectory.resolve(TABLE_OPTION + FILE_POSTFIX);
        Files.createFile(path);
        BufferedWriter writer = Files.newBufferedWriter(path);
        writer.write(String.format("%S, %S\n", COLUMN_OPTION, COLUMN_VALUE));

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

                writer.write(String.format("%s, %s\n", field.getName(), value.toString()));
            } catch (IllegalAccessException e) {

            } finally {
                field.setAccessible(false);
            }
        }

        writer.close();
    }

    @Override
    public void exportAccumulatedEntities(TreeMap<FileName, AccumulatedEntity> accumulated) throws Exception {
        Path path = rootDirectory.resolve(TABLE_ACCUMULATED + FILE_POSTFIX);
        Files.createFile(path);
        BufferedWriter writer = Files.newBufferedWriter(path);

        writer.write(String.format("%S, %S, %S, %S, %S\n",
                COLUMN_DATE, COLUMN_FILE,
                COLUMN_SAME_COUNT, COLUMN_OTHER_COUNT, COLUMN_TOTAL_COUNT));
        for (Map.Entry<FileName, AccumulatedEntity> fileEntry : accumulated.entrySet()) {
            for (Map.Entry<Long, CountEntity> timeEntry : fileEntry.getValue().getCounts().entrySet()) {
                writer.write(String.format("%S, %S, %S, %S, %S\n",
                        DateUtil.getDate(timeEntry.getKey()),
                        fileEntry.getKey().toString(),
                        timeEntry.getValue().getSameCount(),
                        timeEntry.getValue().getOtherCount(),
                        timeEntry.getValue().getTotalCount()
                ));
            }
        }

        writer.close();
    }

    @Override
    public void exportPairEntities(TreeMap<FileName, TreeMap<FileName, Integer>> pairs) throws Exception {
        Path path = rootDirectory.resolve(TABLE_PAIR + FILE_POSTFIX);
        Files.createFile(path);
        BufferedWriter writer = Files.newBufferedWriter(path);

        writer.write(String.format("%S, %S, %S\n",
                COLUMN_FROM, COLUMN_TO, COLUMN_COUNT));
        for (Map.Entry<FileName, TreeMap<FileName, Integer>> from : pairs.entrySet()) {
            for (Map.Entry<FileName, Integer> to : from.getValue().entrySet()) {

                writer.write(String.format("%S, %S, %S\n",
                        from.getKey().toString(), to.getKey().toString(), to.getValue()));
            }
        }
        writer.close();
    }

    @Override
    public void exportGroups(Collection<RevisionGroup> groups) throws Exception {
        Path groupPath = rootDirectory.resolve(TABLE_GROUP + FILE_POSTFIX);
        Files.createFile(groupPath);
        BufferedWriter groupWriter = Files.newBufferedWriter(groupPath);

        Path revisionPath = rootDirectory.resolve(TABLE_REVISION + FILE_POSTFIX);
        Files.createFile(revisionPath);
        BufferedWriter revisionWriter = Files.newBufferedWriter(revisionPath);

        Path filePath = rootDirectory.resolve(TABLE_FILE + FILE_POSTFIX);
        Files.createFile(filePath);
        BufferedWriter fileWriter = Files.newBufferedWriter(filePath);


        // group put statement
        String groupHeader = String.format("%S, %S\n", COLUMN_GROUP_ID, COLUMN_DATE);
        groupWriter.write(groupHeader);

        // revision put statement
        String revisionHeader = String.format("%S, %S, %S %S, %S\n",
                COLUMN_REVISION_ID, COLUMN_GROUP_ID, COLUMN_DATE, COLUMN_AUTHOR, COLUMN_MESSAGE);
        revisionWriter.write(revisionHeader);

        // file put statement
        String fileHeader = String.format("%S, %S\n", COLUMN_GROUP_ID, COLUMN_FILE);
        fileWriter.write(fileHeader);

        for (RevisionGroup group : groups) {
            // put group data
            groupWriter.write(String.format("%S, %S\n", group.getGroupId(), DateUtil.getDate(group.getHeadTime())));

            for (Revision revision : group.getRevisions()) {
                // put revision data
                revisionWriter.write(String.format("%S, %S, %S, %S, %S\n",
                        revision.getId(), group.getGroupId(), DateUtil.getDate(revision.getTime()),
                        revision.getAuthor(), revision.getMessage().replaceAll(",", "，").replaceAll("\n", " ")));
            }

            // put file data
            for (FileName file : group.getFiles()) {
                fileWriter.write(String.format("%S, %S\n", group.getGroupId(), file.toString()));
            }
        }

        groupWriter.close();
        revisionWriter.close();
        fileWriter.close();
    }

    @Override
    public void exportLatestRevision(Revision revision) throws Exception {
        Path path = rootDirectory.resolve(TABLE_LATEST_REVISION + FILE_POSTFIX);
        Files.createFile(path);
        BufferedWriter writer = Files.newBufferedWriter(path);

        writer.write(String.format("%S\n", COLUMN_FILE));
        for (FileName fileName: revision.getChangedFiles()) {
            writer.write(String.format("%S\n", fileName.toString()));
        }

        writer.close();
    }
}
