package extract;

import model.AccumulatedEntity;
import model.FileName;
import model.RevisionGroup;

import java.util.*;

public class FileRelationExtractor {
    private final Collection<RevisionGroup> revisions;

    private TreeSet<FileName> files = null;
    private TreeMap<FileName, AccumulatedEntity> accumulatedEntities = null;
    private TreeMap<FileName, TreeMap<FileName, Integer>> pairEntities = null;

    public FileRelationExtractor(Collection<RevisionGroup> revisions) {
        this.revisions = revisions;
    }

    public TreeSet<FileName> getFiles() {
        if (files == null) {
            buildFiles();
        }

        return files;
    }

    public TreeMap<FileName, AccumulatedEntity> getAccumulatedEntities() {
        if (accumulatedEntities == null) {
            buildRelations();
        }

        return accumulatedEntities;
    }

    public TreeMap<FileName, TreeMap<FileName, Integer>> getPairEntities() {
        if (pairEntities == null) {
            buildRelations();
        }

        return pairEntities;
    }

    private void buildFiles() {
        files = new TreeSet<>();

        for (RevisionGroup revision : revisions) {
            files.addAll(revision.getFiles());
        }
    }

    private void buildRelations() {
        // initialize fields
        if (files == null) {
            buildFiles();
        }

        accumulatedEntities = new TreeMap<>();
        pairEntities = new TreeMap<>();

        for (FileName file : files) {
            accumulatedEntities.put(file, new AccumulatedEntity());
            pairEntities.put(file, new TreeMap<>());
        }

        // extract file relations
        for (RevisionGroup revision : revisions) {
            SortedSet<FileName> revisionFiles = revision.getFiles();
            Map<String, Integer> packageCount = countFilesInPackage(revisionFiles);

            for (FileName file : revisionFiles) {
                // build accumulated data
                AccumulatedEntity accumulatedEntity = accumulatedEntities.get(file);

                int same = packageCount.get(file.getParent()) - 1;
                int other = revisionFiles.size() - (same + 1);

                accumulatedEntity.addCount(revision.getHeadTime(), same, other);

                // update pair count
                TreeMap<FileName, Integer> filePairs = pairEntities.get(file);
                for (FileName otherFile : revisionFiles) {
                    if (file.compareTo(otherFile) <= 0) {
                        continue;
                    }

                    if (!filePairs.containsKey(otherFile)) {
                        filePairs.put(otherFile, 0);
                    }

                    filePairs.put(otherFile, filePairs.get(otherFile) + 1);
                }
            }
        }
    }

    // count the number of files in the packages
    // K: name of package
    // V: number of files in K
    private Map<String, Integer> countFilesInPackage(Collection<FileName> files) {
        Map<String, Integer> packageCount = new TreeMap<>();

        for (FileName file : files) {
            String packageName = file.getParent();

            if (!packageCount.containsKey(packageName)) {
                packageCount.put(packageName, 0);
            }

            packageCount.put(packageName, packageCount.get(packageName) + 1);
        }

        return packageCount;
    }
}
