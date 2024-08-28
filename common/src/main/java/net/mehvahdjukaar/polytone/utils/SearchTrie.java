package net.mehvahdjukaar.polytone.utils;

import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.item.ItemModelOverride;
import net.minecraft.core.component.TypedDataComponent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// Moonlight class
public class SearchTrie<K, O> {
    protected final TrieNode<K, O> root;

    public SearchTrie() {
        root = new TrieNode<>();
    }

    public void insert(List<K> paths, O object) {
        TrieNode<K, O> current = root;

        // Traverse the trie to insert the path
        for (int i = 0; i <= paths.size() - 1; i++) {
            K folder = paths.get(i);
            current.children.putIfAbsent(folder, new TrieNode<>());
            current = current.children.get(folder);
        }

        // Add the object to the final node
        current.objects.add(object);
    }

    public Collection<O> search(List<K> paths) {
        TrieNode<K, O> current = getNode(paths);
        if (current == null) return Collections.emptyList();
        // Once at the target node, collect all objects from this node and its children
        return current.collectObjects();
    }

    public boolean remove(List<K> path) {
        TrieNode<K, O> current = getNode(path);
        if (current == null) return false;
        current.children.clear();
        current.objects.clear();
        return true;
    }

    @Nullable
    protected TrieNode<K, O> getNode(List<K> path) {
        TrieNode<K, O> current = root;
        for (K key : path) {
            current = current.children.getOrDefault(key,  current.children.get(null));
            if (current == null) {
                return null; // Path doesn't exist
            }
        }
        return current;
    }

    public void clear() {
        root.children.clear();
        root.objects.clear();
    }

    public Collection<K> listKeys(List<K> path) {
        TrieNode<K, O> startNode = getNode(path);
        if (startNode != null) {
            return startNode.children.keySet();
        }
        return Collections.emptyList();
    }

    protected static class TrieNode<K, O> {
        Map<K, TrieNode<K, O>> children = new HashMap<>();
        List<O> objects = new ArrayList<>();

        public TrieNode() {
        }

        public List<O> collectObjects() {
            List<O> result = new ArrayList<>(objects);
            for (TrieNode<K, O> child : children.values()) {
                result.addAll(child.collectObjects());
            }
            return result;
        }
    }

    public void printTrie() {
        printNode(root, "", "root", true);
    }

    private void printNode(TrieNode<K, O> node, String prefix, Object nodeName, boolean isTail) {
        if (!node.objects.isEmpty()) {
            System.out.println(prefix + (isTail ? "\\--- " : "|--- ") + nodeName + " " + node.objects);
        } else {
            System.out.println(prefix + (isTail ? "\\--- " : "|--- ") + nodeName );
        }
        List<K> childrenKeys = new ArrayList<>(node.children.keySet());
        for (int i = 0; i < childrenKeys.size(); i++) {
            K key = childrenKeys.get(i);
            TrieNode<K, O> childNode = node.children.get(key);
            boolean isLastChild = i == childrenKeys.size() - 1;

            // Calculate the new prefix for child nodes
            String newPrefix = prefix + (isTail ? "    " : "|   ");

            // Print the child node
            printNode(childNode, newPrefix, key, isLastChild);
        }
    }


}
