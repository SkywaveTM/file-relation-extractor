package model;

import java.util.*;

public class AccumulatedEntity {
    private TreeMap<Long, CountEntity> counts = new TreeMap<>(); // time-count pairs.

    // add counts on given time
    public void addCount(long time, int same, int other) {
        // if there's no count for the given time, then copy the count right before the given time.
        if (!counts.containsKey(time)) {
            Map.Entry<Long, CountEntity> entry = counts.lowerEntry(time);
            int lastSame = 0;
            int lastOther = 0;

            if (entry != null) {
                lastSame = entry.getValue().getSameCount();
                lastOther = entry.getValue().getOtherCount();
            }

            counts.put(time, new CountEntity(lastSame, lastOther));
        }

        // update counts after the given time.
        NavigableMap<Long, CountEntity> tailMap = counts.tailMap(time, true);

        for (CountEntity countEntity : tailMap.values()) {
            countEntity.addSameCount(same);
            countEntity.addOtherCount(other);
        }
    }

    public NavigableMap<Long, CountEntity> getCounts() {
        return Collections.unmodifiableNavigableMap(counts);
    }
}
