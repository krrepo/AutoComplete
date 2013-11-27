package com.glg.service.resources;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.ArrayList;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import com.glg.service.TrieObject;
import com.glg.trie.Entry;
import com.glg.trie.Trie;

public class TrieDataListner<T> implements DataListener<T> {
	static final Pattern quot = Pattern.compile("\"");
	static final Pattern punct= Pattern.compile("\\p{Punct}");
	static final Pattern diacritics = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	static final Pattern dynamic = Pattern.compile("_dynamic", Pattern.CASE_INSENSITIVE);
	
	static Map<String, Trie> tries;

	public TrieDataListner(Map<String, Trie>tries){
		this.tries = tries;
	}
	
	@Override
	public void onData(SocketIOClient client, T arg1, AckRequest ackRequest) {
		TrieObject to = (TrieObject) arg1;
		System.out.println("in on data");
		String key = clean(to.getPrefix());
		String entityString = to.getEntity();
		String[] entities = entityString.split(",");
		//create output object
		Map<String, List<Entry<String, String, String>>> output = new HashMap<String, List<Entry<String, String, String>>>();
		for (String entity : entities){
			if (key!=null && tries.containsKey(entity)){			
				Trie trie = tries.get(entity);
				List<Entry<String, String, String>> values = trie.getSuggestions(key);
				output.put(entity, values);
			}
		}
		System.out.println("Got valid suggestions");
		client.sendJsonObject(output, new AckCallback<String>(String.class) {
            @Override
            public void onSuccess(String result) {
                System.out.println("ack from client: data: " + result);
            }
		});
	}	
	
	private String clean(String str) {
		String cleaned = punct.matcher(str).replaceAll("").toLowerCase();
	    cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFD); 
	    return diacritics.matcher(cleaned).replaceAll("");
	}
	
}
