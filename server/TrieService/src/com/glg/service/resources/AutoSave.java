package com.glg.service.resources;

import java.util.TimerTask;

public class AutoSave extends TimerTask{
	TrieResource trie;
	
	public AutoSave(TrieResource resource){
		trie = resource;
	}
	
	public void run(){
		trie.saveMaps();
	}
}
