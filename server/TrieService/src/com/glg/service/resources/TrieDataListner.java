package com.glg.service.resources;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
		System.out.println("Entity:" + to.getEntity() + " prefix:" + to.getPrefix());
		String entity = to.getEntity();
		if (entity!=null && tries.containsKey(entity)){
			String key = clean(to.getPrefix());
			Trie trie = tries.get(entity);
			List<Entry<String, String, String>> values = trie.getSuggestions(key);
			if (values.size() > 0){
				//create output object and send data back
				TrieObject out = new TrieObject();
				Map<String, List<Entry<String, String, String>>> map = new HashMap<String, List<Entry<String, String, String>>>();
				map.put(entity, values);
				out.setSuggestions(map);
				System.out.println("Got valid suggestions");
				client.sendJsonObject(out, new AckCallback<String>(String.class) {
                    @Override
                    public void onSuccess(String result) {
                        System.out.println("ack from client: data: " + result);
                    }
				});
                
			}
		}else{
			//send back empty data message
			System.out.println("Entity is null or not contained in trie");
		}
		
	}
	
	private String clean(String str) {
		String cleaned = punct.matcher(str).replaceAll("").toLowerCase();
	    cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFD); 
	    return diacritics.matcher(cleaned).replaceAll("");
	}
	
}
