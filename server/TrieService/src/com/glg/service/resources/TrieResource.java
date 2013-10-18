package com.glg.service.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.Normalizer;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.glg.trie.Entry;
import com.glg.trie.Node;
import com.glg.trie.SuggestTree;
import com.glg.trie.Trie;
import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.spi.resource.Singleton;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Singleton
@Path("/typeahead")
public class TrieResource {
	private final static Logger logger = LoggerFactory.getLogger(TrieResource.class);
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

	
	public TrieResource(){
				
		tries = new HashMap<String, Trie>();
		loadProperties();
		loadMaps();

		timer = new Timer(); 
		//run every two hours
		timer.schedule( new AutoSave(this), SAVE_FREQUENCY, SAVE_FREQUENCY ); 
	}

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
	
	public void saveMaps(){
		try{
			for (Map.Entry<String, Trie> entry : tries.entrySet()){
				Trie t = entry.getValue();
				String filename = PATH+entry.getKey();
				if (!t.isNonMutable()){
					filename+="_dynamic";
				}
				
				//delete .csv files and keep around .csv.gz files
				File f = new File(filename+".csv");
				if (f.exists()){
					f.delete();
				}
				
				CSVWriter writer = new CSVWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(new File(filename+".csv.gz")))));
				writeCSV(t.getTrie(), writer);
				writer.close();
				
				if (t.isCache()){
					t.writeCache(new File(filename+".obj"));
				}
			}
			logger.info("Saved tries");
		}catch(Exception e){
			logger.error("Error saving map files:" +e);
		}
	}
	
	private void loadFile(File f){
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
	
	@GET
	@Path("/trie")
	@Produces({"application/x-javascript"})
	public JSONWithPadding TagText(@QueryParam("callback") String callback, @QueryParam("entity")List<String>entities, @QueryParam("value") String val){
		Map<String, List<Entry<String, String, String>>> output = new HashMap<String, List<Entry<String, String, String>>>();
		if (entities != null){
			for (String entity : entities){
				if (val!=null && tries.containsKey(entity)){
					String key = clean(val);
					Trie trie = tries.get(entity);
					List<Entry<String, String, String>> values = trie.getSuggestions(key);
					logger.debug("val is " + val + "   entity is " + entity + "   key is " + key + "   size of values is " + values.size());
					if (values.size() > 0){
						output.put(entity, values);
					}
				}
			}
		}
		return new JSONWithPadding(new GenericEntity<Map<String, List<Entry<String, String, String>>>>(output) {
        }, callback);
    }
	
	@GET
	@Path("/listTrie")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listTrie(@QueryParam("entity") String entity){
		//logger.info("Entity:" + entity + " val:" + val);
		List<String[]> output = new ArrayList<String[]>();
		if (entity!=null && entity.length() > 0  && tries.containsKey(entity)){
			Trie trie = tries.get(entity);
			if (trie.getSize() <= MAX_LIST_SIZE){
				com.glg.trie.SuggestTree.Iterator i = trie.getTrie().iterator();
				Node n;
				while ((n = i.next())!=null){
					String[] vals = new String[4];
					//key, value, weight
					vals[0] = n.getSuggestion();
					vals[1] = n.getValue();
					vals[2] = n.getWeight() + "";
					vals[3] = n.getDisplay();
					output.add(vals);
				}
				return Response.status(200).entity(output).build();
			}
			return Response.status(Status.ACCEPTED).build();
		}
		return Response.noContent().build();
    }
	
	@GET
	@Path("/listEntities")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEntites() {
		Map<String, String>entities = new HashMap<String, String>();
		for (Map.Entry<String, Trie> entry: tries.entrySet()){
			entities.put(entry.getKey(), entry.getValue().isNonMutable() ? "nonMutable":"Mutable");
		}

		return Response.status(200).entity(entities).build();
	}
	
	public List<String> getResults(SuggestTree trie, String query){
		List<String>out = new ArrayList<String>(NUM_RESULTS);
		Node n = trie.getSuggestions(query);
		if (n!=null){
			for (int i = 0; i < n.size(); i++){
				out.add(n.getValue(i));
			}
		}
		return out;
	}
	
	//test with curl -F "file=@{filename}" {server:port}/upload
	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(
		@FormDataParam("file") InputStream uploadedInputStream,
		@FormDataParam("file") FormDataContentDisposition fileDetail) {
		
		if (fileDetail.getFileName().length() > 0){
			logger.info("file detail length: " + fileDetail.getFileName() + " " + fileDetail.getType() +  " " + fileDetail.getSize());
			
			String uploadedFileLocation = PATH+fileDetail.getFileName().toLowerCase();
			// save it
			writeToFile(uploadedInputStream, uploadedFileLocation);
			
			loadFile(new File(uploadedFileLocation));
	
			return Response.status(200).entity("File " + fileDetail.getFileName() + " uploaded").build();
		}
		return Response.noContent().build();
	}

	@GET
	@Path("/download")
	@Produces("application/x-gzip")
	public Response downloadItem(@QueryParam("entity") final String entity){
		if (entity!=null && entity.length() > 0 && tries.containsKey(entity)){
			
		    StreamingOutput streamingOutput = new StreamingOutput(){
		        public void write(OutputStream output) throws IOException, WebApplicationException {
		            Trie trie = tries.get(entity);
		            try{
		            	//CSVWriter writer = new CSVWriter(new OutputStreamWriter(output));
		            	CSVWriter writer = new CSVWriter(new OutputStreamWriter(new GZIPOutputStream(output)));
		            	writeCSV(trie.getTrie(), writer);
		            	writer.close();
		            }catch(Exception e){
		            	logger.error("Error writing stream: " + e);
		            }
		        }        
		    };
		     
		    return Response.ok(streamingOutput, "application/x-gzip").header("content-disposition","attachment;").build();
		}
		
	    return Response.noContent().build();
	}
	
	@GET
	@Path("/createCache")
	public Response createCache(@QueryParam("entity") String entity, @QueryParam("maxSize") int size){
		logger.info("In create cache: " + entity);
		if (entity!=null && entity.length() > 0  && !tries.containsKey(entity) && size > 0){
			Trie t = new Trie(size, NUM_RESULTS);
			tries.put(entity, t);
			logger.info("Created cache: " + entity);
			return Response.ok().build();
		}
		return Response.notModified().build();
	}
	
	@GET
	@Path("/update")
	public Response updateEntry(@QueryParam("entity") String entity, @QueryParam("key") String key, 
			@QueryParam("value") String value, @QueryParam("display")String display, @QueryParam("rank") int rank){
		if (entity!=null && entity.length() > 0  && tries.containsKey(entity)){
			Trie trie = tries.get(entity);
			if (value!=null && value.length() > 0 && rank > 0){
				String cleanedKey = clean(value);
				if (key!=null && key.length() > 0){
					cleanedKey = clean(key);
				}
				if (display == null){
					trie.insert(cleanedKey, value, rank);
					//logger.info("updating:" + entity + " " + value + " " + rank);
				}else{
					trie.insert(cleanedKey, value, display, rank);
					//logger.info("updating:" + entity + " " + value + " " + display + " " + rank);
				}
				return Response.ok().build();
			}
		}
		return Response.notModified().build();
	}
	
	@GET
	@Path("/selected")
	public Response selected(@QueryParam("entity") String entity, @QueryParam("value") String value){
		if (entity!=null && entity.length() > 0  && tries.containsKey(entity)){
			Trie trie = tries.get(entity);
			if (value!=null && value.length() > 0){
				String cleanedKey = clean(value);
				trie.insertOrIncrement(cleanedKey, value);
				logger.info("selecting:" + entity + " " + value);
				return Response.ok().build();
			}
		}
		return Response.notModified().build();
	}
	
	@GET
	@Path("/deleteTrie")
	public Response deleteTrie(@QueryParam("entity") String entity){
		if (entity!=null && entity.length() > 0  && tries.containsKey(entity)){
			logger.info("Deleting trie: " + entity);
			tries.remove(entity);
			
			//delete file
			try{
				File f = new File(PATH+entity+".csv");
				if (f.exists()){
					f.delete();
				}else{
					f = new File(PATH+entity+".csv.gz");
					if (f.exists()){
						f.delete();
					}
				}
			}catch(Exception e){
				logger.error("Error deleting file: " + e);
			}
			
			return Response.ok().build();
		}
		return Response.notModified().build();
	}
	
	@GET
	@Path("/remove")
	public Response removeKey(@QueryParam("entity") String entity, @QueryParam("key") String key){
		if (entity!=null && entity.length() > 0 && tries.containsKey(entity)){
			Trie trie = tries.get(entity);
			if (key!=null && key.length() > 0){
				String cleanedKey = clean(key);
				trie.remove(cleanedKey);
				logger.info("Deleting:" + entity + " " + key);
				return Response.ok().build();
			}
		}
		return Response.notModified().build();
	}
	
	@GET
	@Path("/setNumResults")
	public Response setNumResults(@QueryParam("results") int numResults){
		if (numResults > 5){
			NUM_RESULTS = numResults;
			loadMaps();
			return Response.ok().build();
		}
		return Response.notModified().build();
	}
	
	private void loadProperties(){
	 	Properties prop = new Properties();
	 	 
    	try {
            //load a properties file
    		prop.load(new FileInputStream("config.properties"));
 
            //get the property value and print it out
            String val = prop.getProperty("numResults", "5");
            NUM_RESULTS = Integer.valueOf(val);
            logger.info("NUM_RESULTS:" + val);
            
            val = prop.getProperty("saveFrequency", "3600000");
            SAVE_FREQUENCY = Long.valueOf(val);
            logger.info("SAVE_FREQUENCY:" + val);
            
            val = prop.getProperty("filePath", "data/");
            if (!val.endsWith("/")){
            	val+="/";
            }
            PATH = val;
            logger.info("PATH:" + val);
            
            val = prop.getProperty("maxListSize", "1000");
            MAX_LIST_SIZE = Integer.valueOf(val);
            logger.info("MAX_LIST_SIZE:" + val);
    	} catch (IOException ex) {
    		logger.error("Error loading properties: " + ex);
        }
	}
	
	// save uploaded file to new location
	private void writeToFile(InputStream uploadedInputStream,
		String uploadedFileLocation) {
		try {
			OutputStream out = new FileOutputStream(new File(
					uploadedFileLocation));
			int read = 0;
			byte[] bytes = new byte[1024];
 
			out = new FileOutputStream(new File(uploadedFileLocation));
			while ((read = uploadedInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();
			
		} catch (IOException e) {
			logger.error("IO Exception writing file:" + e);
		}
	}
	
	private void writeCSV(SuggestTree tree, CSVWriter writer){
		try{
			com.glg.trie.SuggestTree.Iterator i = tree.iterator();
			Node n;
			while ((n = i.next())!=null){
				String[] vals = new String[4];
				//key, value, weight
				vals[0] = n.getSuggestion();
				vals[1] = n.getValue();
				vals[2] = n.getWeight() + "";
				vals[3] = n.getDisplay();
				writer.writeNext(vals);
			}
		}catch(Exception e){	
			logger.error("Error writing csv:" + e);
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
