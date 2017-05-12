package merge;

import model.Revision;
import model.RevisionGroup;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class DistanceMerger implements RevisionMerger {
    private final TreeSet<Revision> revisions;
    private long windowSizeInSecond = 60;
    public DistanceMerger() {
        this.revisions = new TreeSet<>((Comparator<Revision>) (o1, o2) -> Long.compare(o1.getTime(), o2.getTime()));
    }

    public DistanceMerger setWindowSizeInSecond(long size) {
        if (size < 0) {
            throw new IllegalArgumentException();
        }

        this.windowSizeInSecond = size;

        return this;
    }
    @Override
    public Set<RevisionGroup> merge() {
        Revision[] revisionArray = revisions.toArray(new Revision[revisions.size()]);
        Set<RevisionGroup> result = new TreeSet<>();
        long milliSecondWindowSize = windowSizeInSecond * 1000;

        int headIndex = 0;
        long groupId = 0;
        while (headIndex < revisionArray.length) {
            Set<Revision> grouped = new TreeSet<>();

            long previousTime = Long.MAX_VALUE - milliSecondWindowSize;
            int nextIndex = headIndex;

            while (nextIndex < revisionArray.length
                    && revisionArray[nextIndex].getTime() <= previousTime + milliSecondWindowSize
                    && revisionArray[nextIndex].getAuthor().equals(revisionArray[headIndex].getAuthor())) {


                grouped.add(revisionArray[nextIndex]);
                nextIndex++;
            }

            groupId++;
            result.add(new RevisionGroup(groupId, grouped));

            headIndex = nextIndex;
        }

        return result;
    }

    @Override
    public void setRevisions(Collection<Revision> revisions) {
        this.revisions.addAll(revisions);
    }
}
