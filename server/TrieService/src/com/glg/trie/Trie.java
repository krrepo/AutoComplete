package com.glg.trie;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;

import au.com.bytecode.opencsv.CSVWriter;


public class Trie {
	boolean nonMutable = false;
	int maxSize = -1;
	//LRUMap<String, Integer>
	LRUMap cache;
	SuggestTree tree;
	
	public Trie(int maxSize, int numSuggestions, LRUMap cache){
		this.maxSize = maxSize;
		this.cache = cache;
		tree = new SuggestTree(numSuggestions);
	}
	
	public Trie(int maxSize, int numSuggestions){
		this.maxSize = maxSize;
		cache = new LRUMap(maxSize);
		tree = new SuggestTree(numSuggestions);
	}
	
	//assumes not a cache
	public Trie(int numSuggestions){
		//cache = new LRUMap(maxSize);
		tree = new SuggestTree(numSuggestions);
	}
	
	public void insert(String key, String value, int rank){
		if (!nonMutable){
			if (key.length() > 0 && value.length() > 0){
				if (maxSize > -1){
					//see if there's enough space
					if (cache.isFull()){
						//remove element
						String remove = (String) cache.lastKey();
						cache.remove(remove);
						tree.remove(remove);
					}
					cache.put(key, 1);
				}
				tree.remove(key);
				tree.put(key, value, rank);
			}
		}
	}
	
	public void insert(String key, String value){
		if (!nonMutable){
			if (key.length() > 0 && value.length() > 0){
				if (maxSize > -1){
					//see if there's enough space
					if (cache.isFull()){
						//remove element
						String remove = (String) cache.lastKey();
						cache.remove(remove);
						tree.remove(remove);
					}
					cache.put(key, 1);
				}
				tree.put(key, value, 1);
			}
		}
	}

	public void updateRank(String key, String value, int rank){
		if (!nonMutable){
			if (maxSize > -1){
				if (cache.containsKey(key)){
					cache.put(key, 1);
				}
			}
			tree.put(key, value, rank);
		}
	}
	
	public void incrementRank(String key){
		if (!nonMutable){
			try{
				int currWeight = 0;
				String value = "";
				currWeight = tree.getWeight(key);
				value = tree.getSuggestions(key).getValue();
				currWeight++;
				tree.put(key, value, currWeight);
				
				if (maxSize > -1){
					if (cache.containsKey(key)){
						cache.put(key, 1);
					}
				}
			}catch (Exception e){}
		}
	}
	
	public void remove(String suggestion){
		if (!nonMutable){
			if (maxSize > -1){
				cache.remove(suggestion);
			}
			tree.remove(suggestion);
		}
	}
	
	public int getSize(){
		return tree.size();
	}
	
	public List<Entry<String, String>> getSuggestions(String key){
		List<Entry<String, String>>out = new ArrayList<Entry<String, String>>();
		if (key != null && key.length() > 0){
			Node n = tree.getSuggestions(key);
			if (n!=null){
				for (int i = 0; i < n.size(); i++){
					out.add(new Entry<String, String>(n.get(i), n.getValue(i)));
				}
			}
		}
		return out;
	}
	
	public SuggestTree getTrie(){
		return tree;
	}
	
	public LRUMap getQueue(){
		return cache;
	}
	
	public void printTrie(){
		com.glg.trie.SuggestTree.Iterator i = tree.iterator();
		Node n;
		while ((n = i.next())!=null){
			System.out.println(
				n.getSuggestion() +"\t"+ 
				n.getValue() +"\t"+ 
				n.getWeight());
		}
	}
	
	public boolean writeCache(File f){
		try{
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream( new RandomAccessFile(f, "rw").getFD()));
			objectOutputStream.writeObject(maxSize);
			objectOutputStream.writeObject(cache);
			objectOutputStream.close();
		}catch(Exception e){
			return false;
		}
		return true;
	}
	
	public boolean loadCache(File f){
		try{
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
			maxSize = (Integer) in.readObject();
			cache = (LRUMap) in.readObject();
			in.close();
		}catch(Exception e){
			return false;
		}
		return true;
	}
	
	public void setNonMutable(){
		nonMutable = true;
	}
	
	public boolean isNonMutable(){
		return nonMutable;
	}

	public boolean isCache(){
		return (maxSize>-1) ? true : false;
	}
}
