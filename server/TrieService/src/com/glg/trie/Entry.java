package com.glg.trie;

public class Entry<K, V, P> {
	K key;
	V value;
	P display;
	
	public Entry(K key, V value){
		this.key = key;
		this.value = value;
	}
	
	public Entry(K key, V value, P display){
		this.key = key;
		this.value = value;
		this.display = display;
	}
	
	public K getKey(){return key;}
	public V getValue(){return value;}
	public P getDisplay(){return display;}
}
