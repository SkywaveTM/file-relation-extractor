package model;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

public class Revision implements Comparable<Revision> {
    private String id;
    private String author;
    private String message;

    private long time; // epoch time
    private TreeSet<FileName> changedFiles;

    public Revision(String id, long time, String author, String message) {
        this.id = id;
        this.time = time;
        this.author = author;
        this.message = message;
        changedFiles = new TreeSet<>();
    }

    public void addFile (FileName file) {
        changedFiles.add(file);
    }

    public String getId() {
        return id;
    }

    public long getTime() {
        return time;
    }

    public SortedSet<FileName> getChangedFiles() {
        return Collections.unmodifiableSortedSet(changedFiles);
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public int compareTo(Revision o) {
        return id.compareTo(o.id);
    }

}
