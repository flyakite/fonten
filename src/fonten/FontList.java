package fonten;

import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;

import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import freemarker.template.*;

public class FontList extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(FontList.class.getName());
    private static final String FONT_FILE = "fontFile";
    private static final String FONT_NAME = "fontName";
    private static final String FONT_ID = "fontID";
    private static final String FONTLIST_PATH = "/fontlist";
    private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();	
    private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    private Configuration cfg;
    
    public void init(){
    	//initialize FreeMarker configuration
    	cfg = new Configuration();
    	cfg.setServletContextForTemplateLoading(getServletContext(), "WEB-INF/templates");
    }
    
    
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        try {
        	Query query = new Query("Font").addSort("created", Query.SortDirection.ASCENDING);
        	List<Entity> fonts = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(100));
        	String fontUploadUrl = blobstoreService.createUploadUrl(FONTLIST_PATH);
        	Template template = cfg.getTemplate("fontList.ftl");
        	Writer writer = res.getWriter();
            res.setContentType("text/html; charset=utf-8");
            
            //prepare variables for template
            SimpleHash root = new SimpleHash();
            root.put("fonts", fonts);
            root.put("fontUploadUrl", fontUploadUrl);
            root.put("FONT_FILE", FONT_FILE);
            root.put("FONT_NAME", FONT_NAME);
            root.put("FONT_ID", FONT_ID);
        	template.process(root, writer);
            
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        try {
            
            //save font file to blobstore
            Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(req);
            List<BlobKey> blobKeyList = blobs.get(FONT_FILE);
            
            String fontID = req.getParameter(FONT_ID);
            String fontName = req.getParameter(FONT_NAME);
            if( fontID == null || fontName == null || blobKeyList == null){
            	LOGGER.warning("missing arguments");
            	res.sendRedirect(FONTLIST_PATH);
            	return;
            }
            BlobKey blobKey = blobKeyList.get(0);
            
            Date date = new Date();
            
            Entity enFont = new Entity("Font", fontID);
            enFont.setProperty("fontname", fontName);
            enFont.setProperty("created", date);
            enFont.setProperty("blobkey", blobKey.getKeyString());
            
            
            datastore.put(enFont);
            
            res.sendRedirect("/fontlist");

        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }
}
