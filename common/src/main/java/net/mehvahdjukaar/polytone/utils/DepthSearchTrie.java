package net.mehvahdjukaar.polytone.utils;

import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @param <K>  Key Value element (actual value) i.e. "dog"
 * @param <KT> Type of Key Value element i.e. String.class
 * @param <O>  Object to store in the tree
 * @param <I>  Object that stores key value pairs
 */
public abstract class DepthSearchTrie<K, KT, O, I> {
    protected final TrieNode<K, KT, O> root;

    public DepthSearchTrie() {
        root = new TrieNode<>();
    }

    public void insert(List<K> paths, O object) {
        TrieNode<K, KT, O> current = root;

        // Traverse the trie to insert the path
        for (int i = 0; i <= paths.size() - 1; i++) {
            K folder = paths.get(i);
            Object folderType = getKeyOfType(folder);

            if (current.type == null) {
                current.type = getKeyOfType(paths.get(i));
            }

            current = current.children.computeIfAbsent(folder, a -> new TrieNode<>());
        }

        // Add the object to the final node
        if(current.object == null) current.object = new ArrayList<>();
        current.object.add(object);
    }

    protected abstract KT getKeyOfType(K folder);

    protected abstract K getKeyFromType(Object type, I stack);


    public boolean remove(List<K> path) {
        TrieNode<K, KT, O> current = getNode(path);
        if (current == null) return false;
        current.children.clear();
        current.object = null;
        return true;
    }

    public List<O> search(List<K> paths) {
        TrieNode<K, KT, O> current = getNode(paths);
        if (current == null) return null;
        // Once at the target node, collect all objects from this node and its children
        return current.object;
    }

    @Nullable
    public List<O> search(I valueHolder) {
        TrieNode<K, KT, O> current = getNode(valueHolder);
        if (current == null) return null;
        // Once at the target node, collect all objects from this node and its children
        return current.object;
    }

    @Nullable
    protected TrieNode<K, KT, O> getNode(List<K> path) {
        TrieNode<K, KT, O> current = root;
        for (K key : path) {
            current = current.children.getOrDefault(key, current.children.get(null));
            if (current == null) {
                return null; // Path doesn't exist
            }
        }
        return current;
    }

    @Nullable
    protected TrieNode<K, KT, O> getNode(I keyProvider) {
        TrieNode<K, KT, O> current = root;
        while (true) {
            Object type = current.type;
            Object folder = getKeyFromType(type, keyProvider);
            if (folder == null) return current;
            var child = current.children.getOrDefault(folder, current.children.get(null));
            if (child == null) {
                return current;
            }
            current = child;
        }
    }


    public void clear() {
        root.children.clear();
        root.object = null;
    }

    public Collection<K> listKeys(List<K> path) {
        TrieNode<K, KT, O> startNode = getNode(path);
        if (startNode != null) {
            return startNode.children.keySet();
        }
        return Collections.emptyList();
    }

    protected static class TrieNode<K, KT, O> {
        Map<K, TrieNode<K, KT, O>> children = new HashMap<>();
        List<O> object;
        KT type = null;
    }

    public void printTrie() {
        printNode(root, "", "root", true);
    }

    private void printNode(TrieNode<K, KT, O> node, String prefix, Object nodeName, boolean isTail) {
        if (nodeName == null) nodeName = "*";
        nodeName = nodeName + " (child type " + node.type + ")";
        System.out.println(prefix + (isTail ? "\\--- " : "|--- ") + nodeName);
        if (node.object != null) System.out.println(prefix + (isTail ? " " : "| ") + "[" + node.object + "]");
        List<K> childrenKeys = new ArrayList<>(node.children.keySet());
        for (int i = 0; i < childrenKeys.size(); i++) {
            K key = childrenKeys.get(i);
            TrieNode<K, KT, O> childNode = node.children.get(key);
            boolean isLastChild = i == childrenKeys.size() - 1;

            // Calculate the new prefix for child nodes
            String newPrefix = prefix + (isTail ? "    " : "|   ");

            // Print the child node
            printNode(childNode, newPrefix, key, isLastChild);
        }
    }

    public void optimizeTree() {
        optimizeNode(root);
    }

    private static <K, KT, O> void optimizeNode(TrieNode<K, KT, O> node) {
        if (node == null) return;

        node.children.values().forEach(DepthSearchTrie::optimizeNode);

        Map<K, TrieNode<K, KT, O>> toAdd = new HashMap<>();
        var iterator = node.children.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var key = entry.getKey();
            TrieNode<K, KT, O> child = entry.getValue();
            if (child.type == null && key == null) {
                int childSize = child.children.size();
                if (childSize == 1) {
                    // get only child
                    TrieNode<K, KT, O> onlyChild = child.children.values().iterator().next();
                    iterator.remove();
                    toAdd.put(entry.getKey(), onlyChild);
                } else if (childSize == 0 && node.object == null) {
                    iterator.remove();
                    node.object = child.object;
                }
            }
        }
        node.children.putAll(toAdd);
    }
}
