package model;

public class FileName implements Comparable<FileName> {
    private String fileName;

    public FileName(String fileName) {
        this.fileName = fileName.toLowerCase(); // normalize the file name.
    }

    public String getExtension() {
        int splitIndex = fileName.lastIndexOf(".");

        if (splitIndex < 0) {
            return "";
        }

        return fileName.substring(splitIndex + 1);
    }

    public String getParent() {
        int splitIndex = fileName.lastIndexOf("/");

        if (splitIndex < 0) {
            return "/";
        }

        return fileName.substring(0, splitIndex + 1);
    }

    public String getName() {
        int splitIndex = fileName.lastIndexOf("/");

        String subName;
        if (splitIndex < 0) {
            subName = fileName;
        } else {
            subName = fileName.substring(splitIndex + 1);
        }

        splitIndex = subName.lastIndexOf(".");

        if (splitIndex < 0) {
            return subName;
        }

        return subName.substring(0, splitIndex);
    }

    @Override
    public String toString() {
        return fileName;
    }

    @Override
    public int compareTo(FileName o) {
        return fileName.compareTo(o.fileName);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FileName && fileName.equals(((FileName) obj).fileName);
    }
}
