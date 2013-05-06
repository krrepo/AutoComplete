package com.glg.trie;

/*
 * Copyright 2011-2013 Nicolai Diethelm
 *
 * This software is free software. You can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * A data structure for rank-sensitive autocomplete. It provides O(log <i>n</i>)
 * time for the basic operations such as searching for the top <i>k</i> highest
 * weighted autocomplete suggestions for a given prefix, modifying the weight of
 * a suggestion, inserting a new suggestion, or removing a suggestion. The
 * structure is based on a compressed ternary search tree of the suggestions,
 * where nodes (prefixes) with the same completions are merged into one node,
 * and where each node that corresponds to a suggestion stores the weight of the
 * suggestion and a reference to the suggestion. In addition, each node in the
 * tree holds a rank-ordered array of references to the nodes of the top
 * <i>k</i> suggestions that start with the corresponding prefix.
 * <p>
 * The space consumption of the tree is determined by the number of nodes and
 * the average length of the array held by each node (the length of the
 * character sequence of a node does not affect space consumption because only
 * the first character is stored explicitly; the other characters are read from
 * the corresponding suggestion or a suggestion that is referenced instead). For
 * each suggestion inserted into the tree, at most one new node is added and at
 * most one existing node is split into two nodes. A tree with <i>n</i>
 * suggestions has thus less than 2<i>n</i> nodes. In the worst case, when the
 * tree has 2<i>n</i> - 1 nodes, each internal node of the corresponding trie
 * (prefix tree) has exactly two child nodes. If all leaf nodes of the trie are
 * at the same depth, i.e. if the height of the trie is log<sub>2</sub><i>n</i>,
 * the tree has <i>n</i> nodes with an array of length 1, <i>n</i>/2 nodes with
 * an array of length 2, <i>n</i>/4 nodes with an array of length 4, and so on
 * until the maximum length of <i>k</i> is reached. Assuming <i>k</i> is a power
 * of two, this gives a total array length of
 * <i>n</i> + 2(<i>n</i>/2) + 4(<i>n</i>/4) + ... + <i>k</i>(<i>n</i>/<i>k</i>)
 * + <i>k</i>(<i>n</i>/<i>k</i> - 1), which is approximately
 * <i>n</i>(log<sub>2</sub><i>k</i> + 2).
 * <p>
 * Ternary search trees are robust. Even in the worst case, when the suggestions
 * are inserted into the tree in lexicographic order, performance is only
 * slightly degraded. The reason for this is that not the entire tree
 * degenerates into a linked list, only each of the small binary search trees
 * within the ternary search tree does. However, for best performance, the
 * suggestions should be inserted into the tree in a random order. In practice,
 * this produces a reasonably well balanced tree where the search space is cut
 * more or less in half each time the search goes left or right.
 * <p>
 * This implementation is not synchronized. If multiple threads access a tree
 * concurrently, and at least one of the threads modifies the tree, it must be
 * synchronized externally. This is typically accomplished by synchronizing on
 * some object that naturally encapsulates the tree.
 * 
 * @version 19 February 2013
 */
public class SuggestTree {
    private final int k;
    private Node root;
    private int size;
    private boolean replaceWithSuccessor;

    /**
     * Creates a tree that returns the top {@code k} highest weighted
     * autocomplete suggestions for a given prefix.
     * @throws IllegalArgumentException if the specified {@code k} value is less
     * than 1
     */
    public SuggestTree(int k) {
        if(k < 1)
            throw new IllegalArgumentException();
        this.k = k;
        root = null;
        size = 0;
        replaceWithSuccessor = false;
    }
    
    /**
     * Returns the number of suggestions in this tree.
     */
    public int size() {
        return size;
    }
    
    /**
     * Removes all of the suggestions from this tree.
     */
    public void clear() {
        root = null;
        size = 0;
    }

    /**
     * Returns a list of the highest weighted suggestions in this tree that
     * start with the specified prefix. If the tree contains no suggestion with
     * the prefix, then {@code null} is returned.
     * @throws IllegalArgumentException if the specified prefix is an empty
     * string
     * @throws NullPointerException if the specified prefix is {@code null}
     */
    public Node getSuggestions(String prefix) {
        if(prefix.isEmpty())
        	 throw new IllegalArgumentException();
        int i = 0;
        Node n = root;
        while(n != null) {
            if(prefix.charAt(i) < n.firstChar)
                n = n.left;
            else if(prefix.charAt(i) > n.firstChar)
                n = n.right;
            else {
                for(i++; i < n.charEnd && i < prefix.length(); i++) {
                    if(prefix.charAt(i) != n.suggestion.charAt(i))
                        return null;
                }
                if(i < prefix.length())
                    n = n.mid;
                else
                    return n;
            }
        }
        return null;
    }

    /**
     * Returns the weight of the specified suggestion, or -1 if this tree does
     * not contain the suggestion.
     * @throws NullPointerException if the specified suggestion is {@code null}
     */
    public int getWeight(String suggestion) {
        if(suggestion.isEmpty())
            return -1;
        Node n = getNode(suggestion);
        return (n != null) ? n.weight : -1;
    }
    
    private Node getNode(String suggestion) {
        int i = 0;
        Node n = root;
        while(n != null) {
            if(suggestion.charAt(i) < n.firstChar)
                n = n.left;
            else if(suggestion.charAt(i) > n.firstChar)
                n = n.right;
            else {
                for(i++; i < n.charEnd; i++) {
                    if(i == suggestion.length()
                            || suggestion.charAt(i) != n.suggestion.charAt(i))
                        return null;
                }
                if(i < suggestion.length())
                    n = n.mid;
                else
                    return (n.weight != -1) ? n : null;
            }
        }
        return null;
    }
    
    /**
     * Inserts the specified suggestion with the specified weight into this
     * tree, or assigns the specified new weight to the suggestion if it is
     * already present.
     * @throws IllegalArgumentException if the specified suggestion is an empty
     * string or the specified weight is negative
     * @throws NullPointerException if the specified suggestion is {@code null}
     */
    public void put(String suggestion, String value, int weight) {
        if(suggestion.isEmpty() || weight < 0)
            throw new IllegalArgumentException();
        if(root == null) {
            root = new Node(suggestion, value, weight, 0, null);
            size++;
            return;
        }
        int i = 0;
        Node n = root;
        while(true) {
            if(suggestion.charAt(i) < n.firstChar) {
                if(n.left != null)
                    n = n.left;
                else {
                    n.left = new Node(suggestion, value, weight, i, n);
                    insertIntoLists(n.left);
                    size++;
                    return;
                }
            }else if(suggestion.charAt(i) > n.firstChar) {
                if(n.right != null)
                    n = n.right;
                else {
                    n.right = new Node(suggestion, value, weight, i, n);
                    insertIntoLists(n.right);
                    size++;
                    return;
                }
            }else{
                for(i++; i < n.charEnd; i++) {
                    if(i == suggestion.length()
                            || suggestion.charAt(i) != n.suggestion.charAt(i)) {
                        n = splitNode(n, i);
                        break;
                    }
                }
                if(i < suggestion.length()) {
                    if(n.mid != null)
                        n = n.mid;
                    else {
                        n.mid = new Node(suggestion,value, weight, i, n);
                        insertIntoLists(n.mid);
                        size++;
                        return;
                    }
                }else if(n.weight == -1) {
                    n.suggestion = suggestion;
                    n.value = value;
                    n.weight = weight;
                    insertIntoLists(n);
                    size++;
                    return;
                }else if(n.weight != weight) {
                    int oldWeight = n.weight;
                    n.weight = weight;
                    updateListPosition(n, oldWeight);
                    return;
                }else
                    return;
            }
        }
    }
    
    private Node splitNode(Node n, int position) {
        Node[] list = (n.list.length < k) ? n.list : Arrays.copyOf(n.list, k);
        Node m = new Node(list, n, position);
        n.firstChar = n.suggestion.charAt(position);
        if(n.left != null)
            n.left.parent = m;
        n.left = null;
        if(n.right != null)
            n.right.parent = m;
        n.right = null;
        if(n == root)
            root = m;
        else if(n == n.parent.left)
            n.parent.left = m;
        else if(n == n.parent.mid)
            n.parent.mid = m;
        else
            n.parent.right = m;
        n.parent = m;
        return m;
    }
    
    private void insertIntoLists(Node suggestion) {
        for(Node n = suggestion, m = n.mid; n != null; m = n, n = n.parent) {
            if(n.mid == m && m != null) {
                Node[] list = n.list;
                if(list.length < k) {
                    Node[] a = new Node[list.length + 1];
                    int i = list.length;
                    while(i > 0 && suggestion.weight > list[i - 1].weight) {
                        a[i] = list[i - 1];
                        i--;
                    }
                    a[i] = suggestion;
                    System.arraycopy(list, 0, a, 0, i);
                    n.list = a;
                }else if(suggestion.weight > list[k - 1].weight) {
                    int i = k - 1;
                    while(i > 0 && suggestion.weight > list[i - 1].weight) {
                        list[i] = list[i - 1];
                        i--;
                    }
                    list[i] = suggestion;
                }else
                    return;
            }
        }
    }
    
    private void updateListPosition(Node suggestion, int oldWeight) {
        int i = 0;
        for(Node n = suggestion, m = n.mid; n != null; m = n, n = n.parent) {
            if(n.mid == m && m != null) {
                Node[] list = n.list;
                while(i < k && suggestion != list[i])
                    i++;
                if(i == k && suggestion.weight <= list[k - 1].weight)
                    return;
                else if(suggestion.weight > oldWeight) {
                    int j = (i < k) ? i : i - 1;
                    while(j > 0 && suggestion.weight > list[j - 1].weight) {
                        list[j] = list[j - 1];
                        j--;
                    }
                    list[j] = suggestion;
                }else {
                    int j = i;
                    while(j < list.length - 1
                            && suggestion.weight < list[j + 1].weight) {
                        list[j] = list[j + 1];
                        j++;
                    }
                    Node c;
                    if(j == k - 1 && (c = listCandidate(n)) != null
                            && c.weight > suggestion.weight)
                        list[j] = c;
                    else
                        list[j] = suggestion;
                }
            }
        }
    }
    
    private Node listCandidate(Node n) {
        Node[] list = n.list;
        Node candidate = null;
        if(n.weight != -1) {
            int i = 0;
            while(i < k && n != list[i])
                i++;
            if(i == k)
                candidate = n;
        }
        for(Node m = firstBSTNode(n.mid); m != null; m = nextBSTNode(m)) {
            secondForLoop:
            for(int i = 0, j = 0; i < m.list.length; i++, j++) {
                Node suggestion = m.list[i];
                for(; j < k; j++) {
                    if(suggestion == list[j])
                        continue secondForLoop;
                }
                if(candidate == null || candidate.weight < suggestion.weight)
                    candidate = suggestion;
                break secondForLoop;
            }
        }
        return candidate;
    }
    
    private Node firstBSTNode(Node bstRoot) {
        Node n = bstRoot;
        if(n != null) {
            while(n.left != null)
                n = n.left;
        }
        return n;
    }
    
    private Node nextBSTNode(Node n) {
        if(n.right != null) {
            n = n.right;
            while(n.left != null)
                n = n.left;
            return n;
        }else{
            Node m = n;
            n = n.parent;
            while(m == n.right) {
                m = n;
                n = n.parent;
            }
            return (m == n.left) ? n : null;
        }
    }
    
    /**
     * Removes the specified suggestion from this tree, if present.
     * The algorithm is symmetric, preserving the balance of the tree.
     * @throws NullPointerException if the specified suggestion is {@code null}
     */
    public void remove(String suggestion) {
        if(suggestion.isEmpty())
            return;
        Node n = getNode(suggestion);
        if(n == null)
            return;
        n.weight = -1;
        size--;
        Node m = n;
        if(n.mid == null) {
            Node replacement = removeNode(n);
            if(replacement != null)
                replacement.parent = n.parent;
            if(n == root)
                root = replacement;
            else if(n == n.parent.mid)
                n.parent.mid = replacement;
            else {
                if(n == n.parent.left)
                    n.parent.left = replacement;
                else
                    n.parent.right = replacement;
                while(n != root && n != n.parent.mid)
                    n = n.parent;
            }
            n = n.parent;
            if(n == null)
                return;
        }
        if(n.weight == -1 && n.mid.left == null && n.mid.right == null) {
            n = mergeWithChild(n);
            while(n != root && n != n.parent.mid)
                n = n.parent;
            n = n.parent;
            if(n == null)
                return;
        }
        removeFromLists(m, n);
    }
        
    private Node removeNode(Node n) {
        Node replacement;
        if(n.left == null)
            replacement = n.right;
        else if(n.right == null)
            replacement = n.left;
        else if(replaceWithSuccessor = !replaceWithSuccessor) {
            replacement = n.right;
            if(replacement.left != null) {
                while(replacement.left != null)
                    replacement = replacement.left;
                replacement.parent.left = replacement.right;
                if(replacement.right != null)
                    replacement.right.parent = replacement.parent;
                replacement.right = n.right;
                n.right.parent = replacement;
            }
            replacement.left = n.left;
            n.left.parent = replacement;
        }else {
            replacement = n.left;
            if(replacement.right != null) {
                while(replacement.right != null)
                    replacement = replacement.right;
                replacement.parent.right = replacement.left;
                if(replacement.left != null)
                    replacement.left.parent = replacement.parent;
                replacement.left = n.left;
                n.left.parent = replacement;
            }
            replacement.right = n.right;
            n.right.parent = replacement;
        }
        return replacement;
    }
    
    private Node mergeWithChild(Node n) {
        Node child = n.mid;
        child.firstChar = n.firstChar;
        child.left = n.left;
        if(child.left != null)
            child.left.parent = child;
        child.right = n.right;
        if(child.right != null)
            child.right.parent = child;
        child.parent = n.parent;
        if(n == root)
            root = child;
        else if(n == n.parent.left)
            n.parent.left = child;
        else if(n == n.parent.mid)
            n.parent.mid = child;
        else
            n.parent.right = child;
        return child;
    }
    
    private void removeFromLists(Node suggestion, Node firstList) {
        int i = 0;
        for(Node n = firstList, m = n.mid; n != null; m = n, n = n.parent) {
            if(n.mid == m) {
                if(n.weight == -1)
                    n.suggestion = n.mid.suggestion;
                Node[] list = n.list;
                while(i < k && suggestion != list[i])
                    i++;
                if(i < k) {
                    Node c;
                    if(list.length == k && (c = listCandidate(n)) != null) {
                        for(int j = i; j < k - 1; j++)
                            list[j] = list[j + 1];
                        list[k - 1] = c;
                    }else {
                        int len = list.length;
                        Node[] a = new Node[len - 1];
                        System.arraycopy(list, 0, a, 0, i);
                        System.arraycopy(list, i + 1, a, i, len - i - 1);
                        n.list = a;
                    }
                }
            }
        }
    }
    /**
     * Returns an iterator over the suggestions in this tree.
     */
    public SuggestionIterator suggestionIterator() {
        return new SuggestionIterator();
    }
    
    public Iterator iterator(){
    	return new Iterator();
    }
    
    /**
     * An iterator over the suggestions in the tree. The iterator returns the
     * suggestions in lexicographic order.
     */
    public final class SuggestionIterator{
        
        private Node current;
        private boolean initialState;
        
        private SuggestionIterator() {
            current = null;
            initialState = true;
        }
        
        /**
         * Returns the next suggestion in the iteration, or {@code null} if the
         * iteration has no more suggestions.
         * @throws IllegalStateException if the last suggestion returned by the
         * iterator has been removed from the tree
         */
        public String next() {
            if(current == null) {
                if(initialState) {
                    if(root != null)
                        current = firstSuggestion(root);
                    initialState = false;
                }
            }else if(current.weight == -1)
                throw new IllegalStateException();
            else
                current = nextSuggestion();
            return (current != null) ? current.suggestion : null;
        }
        
        private Node firstSuggestion(Node subtree) {
            Node n = subtree;
            while(true) {
                while(n.left != null)
                    n = n.left;
                if(n.weight == -1)
                    n = n.mid;
                else
                    return n;
            }
        }

        private Node nextSuggestion() {
            Node n = current;
            if(n.mid != null)
                return firstSuggestion(n.mid);
            else if(n.right != null)
                return firstSuggestion(n.right);
            else if(n.parent == null)
                return null;
            Node m = n;
            n = n.parent;
            while(m == n.right || m == n.mid && n.right == null) {
                m = n;
                n = n.parent;
                if(n == null)
                    return null;
            }
            if(m == n.left)
                return (n.weight != -1) ? n : firstSuggestion(n.mid);
            else
                return firstSuggestion(n.right);
        }
        
        /**
         * Returns the weight of the last suggestion returned by the iterator.
         * @throws IllegalStateException if the {@code next} method has not yet
         * been called or the last call to {@code next} returned {@code null},
         * or if the last suggestion returned by the iterator has been removed
         * from the tree
         */
        public int getWeight() {
            if(current == null || current.weight == -1)
                throw new IllegalStateException();
            return current.weight;
        }
    }
    
    /**
     * An iterator over the suggestions in the tree. The iterator returns the
     * suggestions in lexicographic order.
     */
    public final class Iterator {
        
        private Node current;
        private boolean initialState;
        
        private Iterator() {
            current = null;
            initialState = true;
        }
        
        /**
         * Returns the next suggestion in the iteration, or {@code null} if the
         * iteration has no more suggestions.
         * @throws IllegalStateException if the last suggestion returned by the
         * iterator has been removed from the tree
         */
        public Node next() {
            if(current == null) {
                if(initialState) {
                    if(root != null)
                        current = firstSuggestion(root);
                    initialState = false;
                }
            }else if(current.weight == -1)
                throw new IllegalStateException();
            else
                current = nextSuggestion();
            return current;
        }
        
        private Node firstSuggestion(Node subtree) {
            Node n = subtree;
            while(true) {
                while(n.left != null)
                    n = n.left;
                if(n.weight == -1)
                    n = n.mid;
                else
                    return n;
            }
        }

        private Node nextSuggestion() {
            Node n = current;
            if(n.mid != null)
                return firstSuggestion(n.mid);
            else if(n.right != null)
                return firstSuggestion(n.right);
            else if(n.parent == null)
                return null;
            Node m = n;
            n = n.parent;
            while(m == n.right || m == n.mid && n.right == null) {
                m = n;
                n = n.parent;
                if(n == null)
                    return null;
            }
            if(m == n.left)
                return (n.weight != -1) ? n : firstSuggestion(n.mid);
            else
                return firstSuggestion(n.right);
        }
        
        /**
         * Returns the weight of the last suggestion returned by the iterator.
         * @throws IllegalStateException if the {@code next} method has not yet
         * been called or the last call to {@code next} returned {@code null},
         * or if the last suggestion returned by the iterator has been removed
         * from the tree
         */
        public int getWeight() {
            if(current == null || current.weight == -1)
                throw new IllegalStateException();
            return current.weight;
        }
    }
    

}