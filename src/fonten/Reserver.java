package fonten;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import freemarker.template.*;
import org.json.JSONObject;
//import org.mortbay.util.ajax.JSON;

public class Reserver extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Reserver.class.getName());
    private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    private MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
    //syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
    private Configuration cfg;
    
    public void init(){
    	//initialize FreeMarker configuration
    	cfg = new Configuration();
    	cfg.setServletContextForTemplateLoading(getServletContext(), "WEB-INF/templates");
    }
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        try {
            String text = req.getParameter("text");
            String callback = req.getParameter("callback");
            
            
            Date date = new Date();
            
            String token = UUID.randomUUID().toString().substring(24);
            LOGGER.info(token);
            Entity reservation = new Entity("Reservation", token);
            reservation.setProperty("text", text);
            reservation.setProperty("created", date);
            
            datastore.put(reservation);
            memcache.put(token, text);
            
            JSONObject json = new JSONObject();
            json.put("token", token);
            writeJSONP(callback, json, res);
            

        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }
    
    private void writeJSONP(String callback, JSONObject json, HttpServletResponse res) 
    		throws ServletException {
    	try{
	    	String output;
	    	if(callback == null){
	    		 output = json.toString();
	    	}else{
	    		output = callback + "(" + json.toString() + ")";
	    	}
	    	res.getWriter().println(output);
    	} catch(IOException ex){
    		throw new ServletException(ex);
    	}
    	
    }
}
