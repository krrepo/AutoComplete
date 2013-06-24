package com.glg.trie;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.list.TreeList;

/**
 * A rank-ordered list of autocomplete suggestions. The highest weighted
 * suggestion is at index 0, the second highest weighted at index 1, and so
 * on.
 */
public final class Node implements Comparable{
    Node[] list;
	String suggestion;
    String value;
    String display;
    int weight;
    char firstChar;
    final short charEnd;
    Node left, mid, right, parent;
    
    Node(String suggestion, String value, String display, int weight, int index, Node parent) {
        list = new Node[] {this};
        this.suggestion = suggestion;
        this.weight = weight;
        firstChar = suggestion.charAt(index);
        charEnd = (short) suggestion.length();
        left = mid = right = null;
        this.parent = parent;
        this.value = value;
        this.display = display;
    }
    
    Node(String suggestion, String value, int weight, int index, Node parent) {
        list = new Node[] {this};
        this.suggestion = suggestion;
        this.weight = weight;
        firstChar = suggestion.charAt(index);
        charEnd = (short) suggestion.length();
        left = mid = right = null;
        this.parent = parent;
        this.value = value;
        this.display = "";
    }
    
    
    //Node(Node[] list, Node n, int charEnd) {
    Node(Node[] list, Node n, int charEnd){
        this.list = list;
        suggestion = n.suggestion;
        weight = -1;
        firstChar = n.firstChar;
        this.charEnd = (short) charEnd;
        left = n.left;
        mid = n;
        right = n.right;
        parent = n.parent;
    }
    
    /**
     * Returns the suggestion at the specified position in this list.
     * @throws IndexOutOfBoundsException if the {@code index} argument is
     * less than 0 or not less than the number of suggestions in the list
     */
    public String get(int index) {
        return list[index].suggestion;
    }
    
    /**
     * Returns the suggestion at the specified position in this list.
     * @throws IndexOutOfBoundsException if the {@code index} argument is
     * less than 0 or not less than the number of suggestions in the list
     */
    public String getValue(int index) {
        return list[index].value;
    }
    
    public String getDisplay(int index) {
        return list[index].display;
    }
    
    public String getSuggestion(){
    	return suggestion;
    }
    
    public String getValue(){
    	return value;
    }
    
    public String getDisplay(){
    	return display;
    }
    
    public int getWeight(){
    	return weight;
    }
    
    /**
     * Returns the weight of the suggestion at the specified position in
     * this list.
     * @throws IndexOutOfBoundsException if the {@code index} argument is
     * less than 0 or not less than the number of suggestions in the list
     */
    public int getWeight(int index) {
        return list[index].weight;
    }
    
    /**
     * Returns the number of suggestions in this list.
     */
    public int size() {
        return list.length;
    }


	@Override
	public int compareTo(Object arg0) {
		Node that = (Node)arg0;
		if (that.weight > this.weight){
			return -1;
		}else if (that.weight == this.weight){
			return 0;
		}
		return 1;
	}
}