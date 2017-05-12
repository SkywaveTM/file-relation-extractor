package merge;

import model.Revision;
import model.RevisionGroup;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class NoMerger implements RevisionMerger {

    private TreeSet<Revision> revisions;

    public NoMerger() {
        this.revisions = new TreeSet<>((Comparator<Revision>) (o1, o2) -> Long.compare(o1.getTime(), o2.getTime()));
    }

    @Override
    public Set<RevisionGroup> merge() {
        final long[] groupId = {0};

        return revisions.stream().map(x -> {
            TreeSet<Revision> newSet = new TreeSet<>();
            newSet.add(x);
            groupId[0]++;
            return new RevisionGroup(groupId[0], newSet);
        }).collect(Collectors.toSet());
    }

    @Override
    public void setRevisions(Collection<Revision> revisions) {
        this.revisions.addAll(revisions);
    }
}
