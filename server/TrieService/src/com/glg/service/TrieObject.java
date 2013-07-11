package com.glg.service;

import java.util.List;
import java.util.Map;

import com.glg.trie.Entry;

public class TrieObject {
	String entity;
	String prefix;
	Map<String, List<Entry<String, String, String>>> suggestions;
	
	public void setSuggestions(Map<String, List<Entry<String, String, String>>> suggestions){
		this.suggestions = suggestions;
	}
	
	public Map<String, List<Entry<String, String, String>>> getSuggestions(){
		return suggestions;
	}
	
	public void setEntity(String entity){
		this.entity = entity;
	}
	
	public String getEntity(){
		return entity;
	}
	
	public void setPrefix(String prefix){
		this.prefix = prefix;
	}
	
	public String getPrefix(){
		return prefix;
	}
}
