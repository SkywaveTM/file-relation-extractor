package merge;

import model.Revision;
import model.RevisionGroup;

import java.util.*;

public class SlidingWindowMerger implements RevisionMerger {
    private final TreeSet<Revision> revisions;
    private long windowSizeInSecond = 60;
    private boolean allowDuplicated = false;

    public SlidingWindowMerger() {
        this.revisions = new TreeSet<>((Comparator<Revision>) (o1, o2) -> Long.compare(o1.getTime(), o2.getTime()));
    }

    public SlidingWindowMerger setWindowSizeInSecond(long size) {
        if (size < 0) {
            throw new IllegalArgumentException();
        }

        this.windowSizeInSecond = size;

        return this;
    }

    public SlidingWindowMerger setAllowDuplicated(boolean allow) {
        this.allowDuplicated = allow;

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
            Revision head = revisionArray[headIndex];

            long maxTime = head.getTime() + milliSecondWindowSize;
            int nextIndex = headIndex;

            Set<Revision> grouped = new TreeSet<>();

            while (nextIndex < revisionArray.length
                    && revisionArray[nextIndex].getTime() <= maxTime
                    && revisionArray[nextIndex].getAuthor().equals(revisionArray[headIndex].getAuthor())) {
                grouped.add(revisionArray[nextIndex]);
                nextIndex++;
            }

            groupId++;
            result.add(new RevisionGroup(groupId, grouped));

            if (allowDuplicated) {
                headIndex++;
            } else {
                headIndex = nextIndex;
            }
        }

        return result;
    }

    @Override
    public void setRevisions(Collection<Revision> revisions) {
        this.revisions.addAll(revisions);
    }
}
