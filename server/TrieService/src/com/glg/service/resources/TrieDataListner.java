package com.glg.service.resources;

import java.io.File;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import com.glg.service.SocketIOTrieServer;
import com.glg.service.TrieObject;
import com.glg.trie.Entry;
import com.glg.trie.Trie;

public class TrieDataListner<T> implements DataListener<T> {
	static final Pattern quot = Pattern.compile("\"");
	static final Pattern punct= Pattern.compile("\\p{Punct}");
	static final Pattern diacritics = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	static final Pattern dynamic = Pattern.compile("_dynamic", Pattern.CASE_INSENSITIVE);
	String PATH = "data/";
	static Map<String, Trie> tries;

	public TrieDataListner(Map<String, Trie>triesArg){
		tries = triesArg;
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
			if ( entity.contains("reload") ){
				 asyncServiceMethod(entity);
				 continue;
			}
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

	public void asyncServiceMethod(final String entity){ 
        Runnable task = new Runnable() {
        SocketIOTrieServer s = SocketIOTrieServer.getInstance();
        //get entity name - assumes entity is of the form "reload_entityname"
        Pattern p = Pattern.compile(".*reload_(.*)");
        Matcher m = p.matcher(entity);

            @Override 
            public void run() { 
                try { 
                	if (m.find())
                	  s.loadFile(new File(PATH+m.group(1)+".csv.gz")); 
                } catch (Exception ex) { 
                    //handle error which cannot be thrown back 
                } 
            } 
        }; 
        new Thread(task, "ServiceThread").start(); 
    }
	
	private String clean(String str) {
		String cleaned = punct.matcher(str).replaceAll("").toLowerCase();
	    cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFD); 
	    return diacritics.matcher(cleaned).replaceAll("");
	}
	
}
