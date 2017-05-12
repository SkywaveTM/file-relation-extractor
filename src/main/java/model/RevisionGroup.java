package model;

import java.util.*;

public class RevisionGroup implements Comparable<RevisionGroup> {
    private TreeSet<Revision> revisions;
    private long groupId;

    public RevisionGroup(long groupId, Set<Revision> revisions) {
        if (revisions.isEmpty()) {
            throw new IllegalArgumentException("A revision group must contains at least 1 revision.");
        }

        this.revisions = new TreeSet<>((o1, o2) -> {
            return Long.compare(o1.getTime(), o2.getTime());
        });

        this.revisions.addAll(revisions);

        this.groupId = groupId;
    }

    public long getHeadTime() {
        return revisions.first().getTime();
    }

    public String getHeadId() {
        return revisions.first().getId();
    }

    public long getGroupId() {
        return groupId;
    }

    public SortedSet<Revision> getRevisions() {
        return Collections.unmodifiableSortedSet(revisions);
    }

    public SortedSet<FileName> getFiles() {
        SortedSet<FileName> files = new TreeSet<>();

        for (Revision revision : revisions) {
            files.addAll(revision.getChangedFiles());
        }

        return files;
    }

    @Override
    public int compareTo(RevisionGroup o) {
        return Long.compare(getHeadTime(), o.getHeadTime());
    }

}
