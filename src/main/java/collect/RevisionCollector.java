package collect;

import model.FileName;
import model.Revision;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class RevisionCollector<T extends RevisionCollector<T>> {
    private int limit = 0;
    private long from = -1;
    private long to = -1;

    // only lower cases are allowed
    private Set<String> extensions;
    private Set<String> ignoreStrings;

    public T setLimit(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException();
        }

        this.limit = limit;

        return getThis();
    }

    public T setAllowedExtensions(String[] extensions) {
        this.extensions = Arrays.asList(extensions).stream().map(String::toLowerCase).collect(Collectors.toSet());
        this.extensions = Collections.unmodifiableSet(this.extensions);

        return getThis();
    }

    public T setIgnoreStrings(String[] ignoreStrings) {
        this.ignoreStrings = Arrays.asList(ignoreStrings).stream().map(String::toLowerCase).collect(Collectors.toSet());
        this.ignoreStrings = Collections.unmodifiableSet(this.ignoreStrings);

        return getThis();
    }

    public T setFrom(long from) {
        this.from = from;

        return getThis();
    }

    public T setTo(long to) {
        this.to = to;

        return getThis();
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    public int getLimit() {
        return limit;
    }

    public Set<String> getAllowedExtensions() {
        return extensions;
    }
    public Set<String> getIgnoreStrings() {
        return ignoreStrings;
    }

    public abstract T getThis();
    public abstract Set<Revision> collect() throws Exception;
    public abstract Revision getLatestRevision() throws Exception;
}
