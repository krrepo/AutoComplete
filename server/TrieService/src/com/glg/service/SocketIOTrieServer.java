package com.glg.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.Map;
import java.util.Timer;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.VoidAckCallback;
import com.corundumstudio.socketio.listener.DataListener;
import com.glg.service.resources.TrieResource;
import com.glg.trie.Trie;

public class SocketIOTrieServer {
	private final static Logger logger = LoggerFactory.getLogger(SocketIOTrieServer.class);
	static final Pattern quot = Pattern.compile("\"");
	static final Pattern punct= Pattern.compile("\\p{Punct}");
	static final Pattern diacritics = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	static final Pattern dynamic = Pattern.compile("_dynamic", Pattern.CASE_INSENSITIVE);
	//static final long SAVE_FREQUENCY = 1000 * 60 * 60 * 2;
	//test save every 3 minutes
	long SAVE_FREQUENCY = 1000 * 60 * 60;
	String PATH = "data/";
	int NUM_RESULTS = 5;
	int MAX_LIST_SIZE = 1000;
	
	//@Context HttpHeaders requestHeaders;
	
	Timer timer;
	Map<String, Trie> tries;
	
	public void loadMaps(){
		try{
			File dir = new File(PATH);
			for (File f : dir.listFiles()) {
				if (f.isFile() && (f.getName().endsWith("csv.gz") || f.getName().endsWith("csv"))){
					loadFile(f);
				}
		    }
		}catch(Exception e){
			logger.error("Error loading maps:" + e);
		}
	}
	
	
	public static void main(String[] args)  throws InterruptedException {
		SocketIOTrieServer tries = new SocketIOTrieServer();
		tries.loadMaps();
		logger.info("Loaded tries");
		
		Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(9092);

        final SocketIOServer server = new SocketIOServer(config);
        logger.info("created server");
        server.addJsonObjectListener(TrieObject.class, new DataListener<TrieObject>() {
            @Override
            public void onData(final SocketIOClient client, TrieObject data, final AckRequest ackRequest) {

                // check is ack requested by client,
                // but it's not required check
                if (ackRequest.isAckRequested()) {
                    // send ack response with data to client
                    ackRequest.sendAckData("client message was delivered to server!", "yeah!");
                }
                
                // send message back to client with ack callback WITH data
                TrieObject trie = new TrieObject();
                String entity = data.getEntity();
                if (entity!=null && tries.tries.containsKey(entity)){
                	String prefix = data.getPrefix();
                }
                client.sendJsonObject(trie, new AckCallback<String>(String.class) {
                    @Override
                    public void onSuccess(String result) {
                        System.out.println("ack from client: " + client.getSessionId() + " data: " + result);
                    }
                });

                // send message back to client with ack callback WITHOUT data
                TrieObject ackTrie = new TrieObject();
                client.sendJsonObject(ackTrie, new VoidAckCallback() {
                    @Override
                    public void onSuccess() {
                        System.out.println("ack from client: " + client.getSessionId());
                    }
                });

            }
        });

        server.start();
        logger.info("started server");
        
        Thread.sleep(Integer.MAX_VALUE);

        server.stop();
	}
	
	private void loadFile(File f){
		String[] row = null;
		try{
			
			CSVReader reader = null;
			if (f.getName().endsWith("csv.gz")){
				reader = new CSVReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f))));
			}else if (f.getName().endsWith("csv")){
				reader = new CSVReader(new InputStreamReader(new FileInputStream(f)));
			}
			
			String name = f.getName();
			boolean isDynamic = false;
			if (name.contains(".")){
				name = name.substring(0, name.indexOf('.'));
			}
			if (dynamic.matcher(name).find()){
				name = name.substring(0, name.indexOf('_'));
				isDynamic = true;
			}
			
			Trie vals = new Trie(NUM_RESULTS);
			while ((row = reader.readNext())!=null){	
				if (row.length == 1){
					String value = row[0].trim();
					String key = clean(value);
					vals.insert(key, value);
				}else if (row.length == 2){
					String value = row[1].trim();
					String key = clean(row[0].trim());
					vals.insert(key, value);
				}else if (row.length == 3){
					String value = row[1].trim();
					String key = clean(row[0].trim());
					int weight = Integer.valueOf(row[2].trim());
					vals.insert(key, value, weight);
				}else if (row.length == 4){
					String value = row[1].trim();
					String key = clean(row[0].trim());
					int weight = Integer.valueOf(row[2].trim());
					String display = row[3].trim();
					vals.insert(key, value, display, weight);
				}
			}
			logger.info(name + " size:" + vals.getSize());
			if (vals.getSize() > 0){
				//check to see if cache object exists
				String path = f.getAbsolutePath();
				path = path.substring(0, path.indexOf(f.getName()));
				
				String cachename = f.getName();
				cachename = cachename.substring(0, f.getName().indexOf('.'));
				cachename += ".obj";
				path+=cachename;
				
				File obj = new File(path);
				if (obj.exists()){
					//load cache file
					vals.loadCache(obj);
				}
				if (!isDynamic){
					vals.setNonMutable();
				}
				tries.put(name, vals);
			}
		}catch(Exception e){
			logger.error("Error loading file: " + f.getName() + " row length: " + row.length + " error:" + e);
			if (row!=null){
				for (String r : row){
					logger.error(r);
				}
			}
			//e.printStackTrace();
		}
	}
	
	private String clean(String str) {
		String cleaned = punct.matcher(str).replaceAll("").toLowerCase();
	    cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFD); 
	    return diacritics.matcher(cleaned).replaceAll("");
	}
}
