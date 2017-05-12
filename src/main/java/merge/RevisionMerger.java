package merge;

import model.Revision;
import model.RevisionGroup;

import java.util.Collection;
import java.util.Set;

public interface RevisionMerger {
    Set<RevisionGroup> merge();
    void setRevisions(Collection<Revision> revisions);
}
