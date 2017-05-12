package command;

import collect.RevisionCollector;
import com.beust.jcommander.JCommander;
import export.FileRelationExporter;
import extract.FileRelationExtractor;
import merge.RevisionMerger;
import model.AccumulatedEntity;
import model.FileName;
import model.Revision;
import model.RevisionGroup;
import utils.Logger;

import java.io.File;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CommandExecutor {
    public void run(String... args) throws Exception {
        // prepare arguments
        CommandOptions options = new CommandOptions();
        JCommander commander = new JCommander(options);

        // if arguments are not given, print help.
        if (args.length == 0 || (args.length == 1 && args[0].isEmpty())) {
            commander.usage();
            return;
        }

        // parse arguments
        try {
            commander.parse(args);
        } catch (Exception e) {
            Logger.INSTANCE.severe(e.getMessage());
            e.printStackTrace();
            return;
        }

        // if help option is used, print the usage and exit.
        if (options.isHelp()) {
            commander.usage();
            return;
        }

        Logger.INSTANCE.info("Parsing arguments...");

        run(options);
    }

    public void run(CommandOptions options) throws Exception {
        // set logger level
        Logger.INSTANCE.setLevel(options.isDebug() ? Level.CONFIG : Level.INFO);

        // initialize starting time
        long startTime = System.currentTimeMillis();

        // set usable file name
        File file = options.getExportFile();

        // collect revision data
        Logger.INSTANCE.info("Collecting revision data...");
        RevisionCollector collector = options.generateRevisionCollector()
                .setFrom(options.getRevisionFrom())
                .setTo(options.getRevisionTo())
                .setLimit(options.getLimit())
                .setAllowedExtensions(options.getExtensions().toArray(new String[options.getExtensions().size()]))
                .setIgnoreStrings(options.getIgnoreStrings().toArray(new String[options.getIgnoreStrings().size()]));
        Set<Revision> revisions = collector.collect();
        Revision latestRevision = collector.getLatestRevision(); // todo: maybe a result should use a separated class.

        // merge revision data
        Logger.INSTANCE.info("Merging revision data into groups...");
        RevisionMerger merger = options.generateMerger();
        merger.setRevisions(revisions);
        Set<RevisionGroup> groups = merger.merge();

        // remove groups containing single file if the option is set.
        if (options.isIgnoreSingleFileGroup()) {
            Logger.INSTANCE.info("Removing single file groups...");
            removeSingleFileGroup(groups);
        }

        // remove groups with many packages
        Logger.INSTANCE.info("Removing groups with many packages...");
        Iterator<RevisionGroup> groupIterator = groups.iterator();

        while (groupIterator.hasNext()) {
            RevisionGroup group = groupIterator.next();
            int packageCount = group.getFiles().stream().map(FileName::getParent).collect(Collectors.toSet()).size();

            if (packageCount > options.getMaxDistinctPackages()) {
                groupIterator.remove();
            }

        }

        // extract relation information
        Logger.INSTANCE.info("Extracting relation information...");
        FileRelationExtractor extractor = new FileRelationExtractor(groups);
        TreeMap<FileName, AccumulatedEntity> accumulatedEntities = extractor.getAccumulatedEntities();
        TreeMap<FileName, TreeMap<FileName, Integer>> pairEntities = extractor.getPairEntities();

        // save relation information as sqlite db
        Logger.INSTANCE.info("Writing data...");
        FileRelationExporter exporter = options.generateExporter();
        exporter.exportGroups(groups);
        exporter.exportAccumulatedEntities(accumulatedEntities);
        exporter.exportPairEntities(pairEntities);
        exporter.exportOptions(options);
        exporter.exportLatestRevision(latestRevision);

        // print information
        long endTime = System.currentTimeMillis();

        Logger.INSTANCE.info(String.format("Done. (%dms elapsed)", endTime - startTime));
        Logger.INSTANCE.info(String.format("%s created.", file.getAbsolutePath()));
    }

    private void removeSingleFileGroup(Set<RevisionGroup> groups) {
        Iterator<RevisionGroup> groupIterator = groups.iterator();

        while (groupIterator.hasNext()) {
            RevisionGroup group = groupIterator.next();

            if (group.getFiles().size() < 2) {
                groupIterator.remove();
            }
        }
    }

}
