package com.glg.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.glg.service.resources.TrieDataListner;
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
	
    // Private constructor prevents instantiation from other classes
    private SocketIOTrieServer() { 
    	tries = new HashMap<String, Trie>();
    }

    /**
    * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
    * or the first access to SingletonHolder.INSTANCE, not before.
    */
    private static class SingletonHolder { 
            public static final SocketIOTrieServer INSTANCE = new SocketIOTrieServer();
    }

    public static SocketIOTrieServer getInstance() {
            return SingletonHolder.INSTANCE;
    }
	
	

	
	public void loadMaps(){
		String filename = "";
		try{
			File dir = new File(PATH);
			for (File f : dir.listFiles()) {
				filename = f.getName();
				if (f.isFile() && (f.getName().endsWith("csv.gz") || f.getName().endsWith("csv"))){
					loadFile(f);
				}
		    }
		}catch(Exception e){
			logger.error("Error loading maps:" + e + " filename:"+ filename);
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args)  throws InterruptedException {
		SocketIOTrieServer tries = SocketIOTrieServer.getInstance();
		tries.loadMaps();
		TrieDataListner listner = new TrieDataListner(tries.tries);
		logger.info("Loaded tries");
		
		Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(8100);

        final SocketIOServer server = new SocketIOServer(config);
        logger.info("created server");
        server.addJsonObjectListener(TrieObject.class, listner);

        server.start();
        logger.info("started server");
        
        Thread.sleep(Integer.MAX_VALUE);

        server.stop();
	}
	
	public void loadFile(File f){
		String[] row = null;
		try{
			
			CSVReader reader = null;
			if (f.getName().endsWith("csv.gz")){
				reader = new CSVReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f)), "UTF-8"));
			}else if (f.getName().endsWith("csv")){
				reader = new CSVReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
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
	
	private synchronized static String utftoasci(String s){
		  final StringBuffer sb = new StringBuffer( s.length() * 2 );

		  final StringCharacterIterator iterator = new StringCharacterIterator( s );
		  
		  char ch = iterator.current();
		  		  
		  while( ch != StringCharacterIterator.DONE ){
		    boolean f=false;
		    String hex = (Integer.toHexString((int)ch));
//		    "Ê" ==> "E"
		    if(  hex.equalsIgnoreCase("ca") ){sb.append("E");f=true;}
//		    "È" ==> "E"
		    if(  hex.equalsIgnoreCase("c8") ){sb.append("E");f=true;}
//		    "ë" ==> "e"
		    if(  hex.equalsIgnoreCase("eb") ){sb.append("e");f=true;}
//		    "é" ==> "e"
		    if(  hex.equalsIgnoreCase("e9") ){sb.append("e");f=true;}
//		    "è" ==> "e"
		    if(  hex.equalsIgnoreCase("e8") ){sb.append("e");f=true;}
//		    "Â" ==> "A"
		    if(  hex.equalsIgnoreCase("c2") ){sb.append("A");f=true;}
//		    "ß" ==> "ss"
		    if(  hex.equalsIgnoreCase("df") ){sb.append("ss");f=true;}
//		    "Ç" ==> "C"
		    if(  hex.equalsIgnoreCase("c7") ){sb.append("C");f=true;}
//		    "ª" ==> ""
		    if(  hex.equalsIgnoreCase("aa") ){sb.append("");f=true;}
//		    "º" ==> ""
		    if(  hex.equalsIgnoreCase("ba") ){sb.append("");f=true;}
//		    "Ñ" ==> "N"
		    if(  hex.equalsIgnoreCase("d1") ){sb.append("N");f=true;}
//		    "É" ==> "E"
		    if(  hex.equalsIgnoreCase("c9") ){sb.append("E");f=true;}
//		    "Ä" ==> "A"
		    if(  hex.equalsIgnoreCase("c4") ){sb.append("A");f=true;}
//		    "Å" ==> "A"
		    if(  hex.equalsIgnoreCase("c5") ){sb.append("A");f=true;}
//		    "ä" ==> "a"
		    if(  hex.equalsIgnoreCase("e4") ){sb.append("a");f=true;}
//		    "Ü" ==> "U"
		    if(  hex.equalsIgnoreCase("dc") ){sb.append("U");f=true;}
//		    "ö" ==> "o"
		    if(  hex.equalsIgnoreCase("f6") ){sb.append("o");f=true;}
//		    "ü" ==> "u"
		    if(  hex.equalsIgnoreCase("fc") ){sb.append("u");f=true;}
//		    "á" ==> "a"
		    if(  hex.equalsIgnoreCase("e1") ){sb.append("a");f=true;}
//		    "É" ==> "E"
		    if(  hex.equalsIgnoreCase("c9") ){sb.append("E");f=true;}
//		    "ó" ==> "o"
		    if(  hex.equalsIgnoreCase("f3") ){sb.append("o");f=true;}
//		    "Ó" ==> "O"
		    if(  hex.equalsIgnoreCase("d3") ){sb.append("O");f=true;}
//		    "ò" ==> "o"
		    if(  hex.equalsIgnoreCase("f2") ){sb.append("o");f=true;}
//		    "Ò" ==> "O"
		    if(  hex.equalsIgnoreCase("d2") ){sb.append("O");f=true;}
//		    "ô" ==> "o"
		    if(  hex.equalsIgnoreCase("f4") ){sb.append("o");f=true;}
//		    "Ô" ==> "O"
		    if(  hex.equalsIgnoreCase("d4") ){sb.append("O");f=true;}
//		    "ő" ==> "o"
		    if(  hex.equalsIgnoreCase("151") ){sb.append("o");f=true;}
//		    "Ő" ==> "O"
		    if(  hex.equalsIgnoreCase("150") ){sb.append("O");f=true;}
//		    "õ" ==> "o"
		    if(  hex.equalsIgnoreCase("f5") ){sb.append("o");f=true;}
//		    "Õ" ==> "O"
		    if(  hex.equalsIgnoreCase("d5") ){sb.append("O");f=true;}
//		    "ø" ==> "oe"
		    if(  hex.equalsIgnoreCase("f8") ){sb.append("oe");f=true;}
//		    "Ø" ==> "OE"
		    if(  hex.equalsIgnoreCase("d8") ){sb.append("OE");f=true;}		    
//		    "ō" ==> "o"
		    if(  hex.equalsIgnoreCase("14d") ){sb.append("o");f=true;}
//		    "Ō" ==> "O"
		    if(  hex.equalsIgnoreCase("14c") ){sb.append("O");f=true;}
//		    "ơ" ==> "o"
		    if(  hex.equalsIgnoreCase("1a1") ){sb.append("o");f=true;}
//		    "Ơ" ==> "O"
		    if(  hex.equalsIgnoreCase("1a0") ){sb.append("O");f=true;}
//		    "ö" ==> "oe"
		    if(  hex.equalsIgnoreCase("f6") ){sb.append("oe");f=true;}
//		    "Ö" ==> "OE"
		    if(  hex.equalsIgnoreCase("d6") ){sb.append("OE");f=true;}
		    		    
		    if(!f){
		     sb.append(ch);
		    } 
		   ch = iterator.next();
		  }
		  return sb.toString();
		 }
	
	private String clean(String str) {
		try{

			String cleaned = punct.matcher(str).replaceAll("").toLowerCase();		
		    cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFD);
	        cleaned = diacritics.matcher(cleaned).replaceAll("");
	        //http://stackoverflow.com/questions/285228/how-to-convert-utf-8-to-us-ascii-in-java
			cleaned = utftoasci(cleaned);

	        return cleaned;
	        
	    }catch(Exception e){	
		  logger.error("Error writing csv:" + e);
		  return "";
	    }
	}
}