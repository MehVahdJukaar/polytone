package net.mehvahdjukaar.polytone.utils;

import java.util.*;

// Moonlight class
public class FrequencyOrderedCollection<T> implements Collection<T> {
    private final Map<T, Integer> frequencies = new HashMap<>();
    private List<Map.Entry<T, Integer>> sortedEntries = new ArrayList<>();

    @Override
    public boolean add(T obj) {
        return add(obj, 1);
    }

    public boolean add(T obj, int count) {
        if (count <= 0) {
            return false;
        }
        boolean wasAdded = frequencies.containsKey(obj);
        frequencies.merge(obj, count, Integer::sum);

        if (wasAdded) {
            updateSortedEntries();
        } else {
            // New entry, need to re-sort
            sortedEntries = new ArrayList<>(frequencies.entrySet());
            sortedEntries.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
        }
        return true;
    }

    public boolean remove(T obj, int count) {
        if (count <= 0 || !frequencies.containsKey(obj)) {
            return false;
        }
        frequencies.merge(obj, -count, (oldCount, delta) -> {
            int newCount = oldCount + delta;
            return (newCount > 0) ? newCount : null;
        });

        updateSortedEntries();
        return true;
    }

    @Override
    public boolean remove(Object obj) {
        if (frequencies.remove(obj) != null) {
            updateSortedEntries();
            return true;
        }
        return false;
    }

    public boolean removeAllOccurrences(T obj) {
        return remove(obj);
    }

    private void updateSortedEntries() {
        sortedEntries = new ArrayList<>(frequencies.entrySet());
        sortedEntries.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
    }

    public T getFirst() {
        if (!sortedEntries.isEmpty()) {
            return sortedEntries.get(0).getKey();
        }
        return null;
    }

    public T getLast() {
        if (!sortedEntries.isEmpty()) {
            return sortedEntries.get(sortedEntries.size() - 1).getKey();
        }
        return null;
    }

    @Override
    public Iterator<T> iterator() {
        return sortedEntries.stream().map(Map.Entry::getKey).iterator();
    }

    @Override
    public int size() {
        return frequencies.size();
    }

    @Override
    public boolean isEmpty() {
        return frequencies.isEmpty();
    }

    @Override
    public boolean contains(Object obj) {
        return frequencies.containsKey(obj);
    }

    @Override
    public Object[] toArray() {
        return sortedEntries.stream().map(Map.Entry::getKey).toArray();
    }

    @Override
    public <U> U[] toArray(U[] a) {
        return sortedEntries.stream().map(Map.Entry::getKey).toArray(size -> a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return frequencies.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean changed = false;
        for (T item : c) {
            changed |= add(item);
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object item : c) {
            changed |= remove(item);
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean changed = false;

        // First, identify which elements should be removed
        Iterator<T> iterator = iterator();
        Set<T> toRemove = new HashSet<>();

        while (iterator.hasNext()) {
            T item = iterator.next();
            if (!c.contains(item)) {
                toRemove.add(item);
            }
        }

        // Remove identified elements
        for (T item : toRemove) {
            frequencies.remove(item);
            changed = true;
        }

        // Update sortedEntries list if any elements were removed
        if (changed) {
            updateSortedEntries();
        }

        return changed;
    }

    @Override
    public void clear() {
        frequencies.clear();
        sortedEntries.clear();
    }

}
