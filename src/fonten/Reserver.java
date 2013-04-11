package fonten;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class Reserver extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Reserver.class.getName());
    private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    private MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
    //syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
    
    @Override
    public void doOptions(HttpServletRequest req, HttpServletResponse res) throws ServletException {
    	try{
    		res.setHeader("Access-Control-Allow-Origin","*");
    		res.setHeader("Access-Control-Allow-Method","POST, GET, OPTIONS");
    		res.setHeader("Access-Control-Max-Age", "604800");
    	} catch (Exception ex){
    		throw new ServletException(ex);
    	}
    }
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        try {
        	String sText = req.getParameter("text");
        	sText = URLDecoder.decode(sText, "UTF-8");
            Text text = new Text(sText);
            
            Date date = new Date();
            
            byte[] bText = sText.getBytes("UTF-8");
            final MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bMD5Text = md.digest(bText);
            String token = new BigInteger(1, bMD5Text).toString(16);
            LOGGER.info(token);
            Entity reservation = new Entity("Reservation", token);
            reservation.setProperty("text", text);
            reservation.setProperty("created", date);
            
            datastore.put(reservation);
            memcache.put(token, text);
            
            JSONObject json = new JSONObject();
            json.put("token", token);
            
            writeJSON(json, res);
            

        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }
    
    private void writeJSON(JSONObject json, HttpServletResponse res) throws ServletException {
    	try{
    		res.setHeader("Access-Control-Allow-Origin","*");
	    	res.getWriter().println(json.toString());
    	} catch(IOException ex){
    		throw new ServletException(ex);
    	}
    	
    }
}
