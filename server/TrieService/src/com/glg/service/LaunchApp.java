package com.glg.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.ApplicationAdapter;
import com.sun.jersey.api.json.JSONConfiguration;


public class LaunchApp {
	static volatile boolean keepRunning = true;
	
	private final static Logger logger = LoggerFactory.getLogger(LaunchApp.class);
	
	private static int getPort(int defaultPort) {
		try{
			Properties prop = new Properties();
            //load a properties file
    		prop.load(new FileInputStream("config.properties"));
 
            //get the property value and print it out
            String port = prop.getProperty("port", ""+defaultPort);
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {}
		}catch(Exception e){}
        return defaultPort;
    }

    private static URI getBaseURI() {
        return UriBuilder.fromUri("http://0.0.0.0").port(getPort(8080)).build();
    }

    public static final URI BASE_URI = getBaseURI();

    public static HttpServer startServer() throws IOException{
        
        ApplicationAdapter rc = new ApplicationAdapter(new MyApplication());
        rc.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        
        return GrizzlyServerFactory.createHttpServer(BASE_URI, rc);
    }

    public static void main(String[] args) throws IOException {
    	final Thread mainThread = Thread.currentThread();
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		        keepRunning = false;
		        try {
					mainThread.join();
				} catch (InterruptedException e) {
					logger.error("main", e);
				}
		    }
		});		
		try {
			System.out.println("Starting TrieService");
			HttpServer httpServer = startServer();
			String logmsg = String.format(
					"TrieService started with WADL available at %s/application.wadl\n", BASE_URI);
			logger.info(logmsg);
			while(keepRunning){
				long sleep = 500L;
				Thread.sleep(sleep);
			} 
         	httpServer.stop();
    	    logger.info("STOPPING TRIESERVICE");
    	    
		} catch (Exception e) {
			logger.error("Error in TrieService", e);
		}
    }

}
