package net.mehvahdjukaar.polytone.utils;

import java.util.*;

// Moonlight class
public class FrequencyOrderedCollection<T> implements Collection<T> {
    private final Map<T, Integer> frequencies = new HashMap<>();
    private List<Map.Entry<T, Integer>> sortedEntries = new ArrayList<>();

    // Add an element with a default count of 1
    @Override
    public boolean add(T obj) {
        return add(obj, 1);
    }

    // Add an element with a specified count
    public boolean add(T obj, int count) {
        if (count <= 0) {
            return false; // Do not add if count is non-positive
        }
        boolean wasAdded = frequencies.containsKey(obj);
        frequencies.merge(obj, count, Integer::sum);

        // Update sortedEntries list if the element was already in the map
        if (wasAdded) {
            updateSortedEntries();
        } else {
            // New entry, need to re-sort
            sortedEntries = new ArrayList<>(frequencies.entrySet());
            sortedEntries.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
        }
        return true;
    }

    // Remove a specific number of occurrences of an element
    public boolean remove(T obj, int count) {
        if (count <= 0 || !frequencies.containsKey(obj)) {
            return false; // Do nothing if count is non-positive or object doesn't exist
        }
        frequencies.merge(obj, -count, (oldCount, delta) -> {
            int newCount = oldCount + delta;
            return (newCount > 0) ? newCount : null; // If count goes to 0 or below, remove entry
        });

        // Update sortedEntries list
        updateSortedEntries();
        return true;
    }

    // Remove all occurrences of an element
    @Override
    public boolean remove(Object obj) {
        if (frequencies.remove(obj) != null) {
            updateSortedEntries();
            return true;
        }
        return false;
    }

    // Remove all occurrences of an element
    public boolean removeAllOccurrences(T obj) {
        return remove(obj);
    }

    // Update sortedEntries list based on current frequencies
    private void updateSortedEntries() {
        sortedEntries = new ArrayList<>(frequencies.entrySet());
        sortedEntries.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
    }

    // Get the element with the highest frequency
    public T getFirst() {
        if (!sortedEntries.isEmpty()) {
            return sortedEntries.get(0).getKey();
        }
        return null; // Return null if the collection is empty
    }

    // Get the element with the lowest frequency
    public T getLast() {
        if (!sortedEntries.isEmpty()) {
            return sortedEntries.get(sortedEntries.size() - 1).getKey();
        }
        return null; // Return null if the collection is empty
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
