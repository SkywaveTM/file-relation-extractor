package model;

public class CountEntity {
    private int sameCount = 0, otherCount = 0;

    public CountEntity(int same, int other) {
        addSameCount(same);
        addOtherCount(other);
    }

    public void addSameCount(int toAdd) {
        if (toAdd < 0) {
            throw new IllegalArgumentException();
        }

        this.sameCount += toAdd;
    }


    public void addOtherCount(int toAdd) {
        if (toAdd < 0) {
            throw new IllegalArgumentException();
        }

        this.otherCount += toAdd;
    }

    public int getSameCount() {
        return sameCount;
    }

    public int getOtherCount() {
        return otherCount;
    }

    public int getTotalCount() {
        return sameCount + otherCount;
    }
}
