package fonten;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceException;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.typography.font.sfntly.Font;
import com.google.typography.font.sfntly.FontFactory;
import com.google.typography.font.sfntly.Tag;
import com.google.typography.font.sfntly.data.FontInputStream;
import com.google.typography.font.sfntly.data.FontOutputStream;
import com.google.typography.font.sfntly.data.WritableFontData;
import com.google.typography.font.tools.conversion.eot.EOTWriter;
import com.google.typography.font.tools.conversion.woff.WoffWriter;
import com.google.typography.font.tools.fontinfo.FontUtils;
import com.google.typography.font.tools.subsetter.HintStripper;
import com.google.typography.font.tools.subsetter.RenumberingSubsetter;
import com.google.typography.font.tools.subsetter.Subsetter;

public class FontSubsetter extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(FontSubsetter.class.getName());
    private static final String OPT_EOT = "eot";
    private static final String OPT_WOFF = "woff";
    private static final String FONT_CACHE_PREFIX = "Font:";
    private static final String BLOBKEY_CACHE_PREFIX = "BlobKey:";
    //private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    private MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

    public enum FontFormat {
        Undef, Eot, Woff
    }
    
        
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        try {
            String fontID = req.getParameter("id").toLowerCase();
            String text = req.getParameter("text");
            String format = req.getParameter("format");
            String strip = req.getParameter("strip");
            String token = req.getParameter("token");
            text = URLDecoder.decode(text, "UTF-8");
            
            //unique values in string
            //Set<String> temp = new HashSet<String>(Arrays.asList(text));
            //text = temp.toArray().toString();
            //String text = sText;
            
            //decide font format
            FontFormat fontFormat = FontFormat.Undef;
            if( format == OPT_EOT){
            	fontFormat = FontFormat.Eot;
            }else if( format == OPT_WOFF){
            	fontFormat = FontFormat.Woff;
            }
            
            //decide strip hinting
            boolean hinting = true;
            if( strip == "0" || strip == "false"){
            	hinting = false;
            }
            
            Font font = null;
            boolean enableSubFontCache = true; //this should be enabled if same font requests are alike
            if( enableSubFontCache ){
            	if(token != null){
            		font = getSubFontFromMemcache(fontID, token, hinting);
            	}else{
            		font = getSubFontFromMemcache(fontID, text, hinting);
            	}
            }
            if( font != null ){
            	LOGGER.info("asdf");
            }else{
            	text = getTextFromMemcacheOrDatastore(text, token);
            	LOGGER.info(text);
            	font = getFontFromMemcacheOrBlobstore(fontID);
            	font = subsetFont(font, text, hinting);
            }
            //BlobKey blobKey = getBlobKeyFromMemcacheOrDatastore(fontID);
            //Font font = getFontFromBlobstore(blobKey);
            //Font font = getFontFromStatic(blobKey);
            
        	respondFont(font, fontFormat, res);
        	if( enableSubFontCache ){
        		cacheSubFont(font, fontID, text, hinting);
        	}
        	
        	
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }
    
    /**
     * Get Text from memcache or datastore
     * @param text
     * @param token
     * @return text
     * @throws ServletException
     */
    private String getTextFromMemcacheOrDatastore(String text, String token) throws ServletException{
    	try {
	    	if( text == null && token != null){
	        	String tokenText = (String) memcache.get(token);
	        	if( tokenText != null){
	        		text = tokenText;
	        	}else{
	        		Key rKey = KeyFactory.createKey("Reservation", token);
	        		Entity reservation = datastore.get(rKey);
	        		text = reservation.getProperty("text").toString();
	        	}
	        }
	    	return text;
    	} catch (Exception ex){
    		throw new ServletException(ex);
    	}
    }
    
    
    /**
     * Get Font From Blob Key cache and blobstore.
     * @param fontID
     * @return font
     * @throws ServletException
     */
    private Font getFontFromMemcacheOrBlobstore(String fontID) throws ServletException{
    	try {
    		
    		BlobKey blobKey;
    		String cachedBlobKey;
    		FontFactory fontFactory = FontFactory.getInstance();
			cachedBlobKey = (String) memcache.get(BLOBKEY_CACHE_PREFIX+fontID);
			if( cachedBlobKey != null){
				blobKey = new BlobKey(cachedBlobKey.toString());
			}else{
				Key fontKey = KeyFactory.createKey("Font", fontID);
				Entity entityFont = datastore.get(fontKey);
				LOGGER.info(entityFont.toString());
				
				blobKey = new BlobKey(entityFont.getProperty("blobkey").toString());
				LOGGER.info(blobKey.getKeyString());
				memcache.put(BLOBKEY_CACHE_PREFIX+fontID, blobKey.getKeyString());
			}
			LOGGER.warning("start reading");
			Font font = fontFactory.loadFonts(new BlobstoreInputStream(blobKey))[0];
			LOGGER.warning("start reading");
    			
			return font; 
    	} catch (Exception ex){
    		throw new ServletException(ex);
    	}
    }
    
    /**
     * Get Font From Font cache otherwise from Blob Key cache and from blobstore.
     * This is experimental, the memcache size of a single item in GAE is limited to 1MB.
     * @param fontID
     * @return font
     * @throws ServletException
     */
    private Font getFontFromMemcacheOrBlobstore2(String fontID) throws ServletException{
    	try {
    		
	    	BlobKey blobKey;
	    	String cachedBlobKey;
	    	FontFactory fontFactory = FontFactory.getInstance();
	    	Font cachedFont = null;
	    	byte[] fontB = (byte[]) memcache.get(FONT_CACHE_PREFIX+fontID);
	    	if(fontB != null){
	    		ByteArrayInputStream bin = new ByteArrayInputStream(fontB);
	    		FontInputStream fin = new FontInputStream(bin);
	    		cachedFont = fontFactory.loadFonts(fin)[0];
	    	}
	    	if( cachedFont != null){
	    		return cachedFont;
	    	}else{
		        cachedBlobKey = (String) memcache.get(BLOBKEY_CACHE_PREFIX+fontID);
		        if( cachedBlobKey != null){
		        	blobKey = new BlobKey(cachedBlobKey.toString());
		        }else{
		        	Key fontKey = KeyFactory.createKey("Font", fontID);
		        	Entity entityFont = datastore.get(fontKey);
		        	LOGGER.info(entityFont.toString());
		        	
		        	blobKey = new BlobKey(entityFont.getProperty("blobkey").toString());
		        	LOGGER.info(blobKey.getKeyString());
		        	memcache.put(BLOBKEY_CACHE_PREFIX+fontID, blobKey.getKeyString());
		        }
		        LOGGER.warning("start reading");
		        Font font = fontFactory.loadFonts(new BlobstoreInputStream(blobKey))[0];
		        LOGGER.warning("start reading");
		        
		        try {
			        ByteArrayOutputStream out = new ByteArrayOutputStream();
			        FontOutputStream os = new FontOutputStream(out);
			        fontFactory.serializeFont(font, os);
		        	memcache.put(FONT_CACHE_PREFIX+fontID, out.toByteArray());
		        	os.close();
		        } catch (MemcacheServiceException ex){
		        	LOGGER.warning(ex.toString());
		        }
	        	
	    		return font; 
	    	}
    	} catch (Exception ex){
    		throw new ServletException(ex);
    	}
    }
    
    /**
     * Get Font From Blobstore
     * @param blobKey
     * @return font
     * @throws ServletException
     */
    private Font getFontFromBlobstore(BlobKey blobKey) throws ServletException{
    	try {
    		
    		//FileService fileService = FileServiceFactory.getFileService();
            //AppEngineFile fontFile = fileService.getBlobFile(blobKey);
            //FileReadChannel readChannel = fileService.openReadChannel(fontFile, false);
            //BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, "UTF8"));
            //InputStream is = Channels.newInputStream(readChannel);
    		
    		//BlobstoreInputStream is much faster than the above AppEngineFile
        	Font font = FontUtils.getFonts(new BlobstoreInputStream(blobKey))[0];
        	memcache.put(blobKey, font);
        	return font;
    	} catch (IOException ex){
    		throw new ServletException(ex);
    	}
    }
    
    
    /**
     * Get font from static font file.
     * This is experimental, test to see if reading a static file can be faster than blobstore.
     * @param blobKey
     * @return
     * @throws ServletException
     */
    private Font getFontFromStatic(BlobKey blobKey) throws ServletException{
    	try {
    		ServletContext context = getServletContext();
    		String fullPath = context.getRealPath("/WEB-INF/fonts/wthc06.ttf");
    		LOGGER.warning("getfonts");
    		Font font = FontUtils.getFonts(fullPath)[0];
    		LOGGER.warning("done");
    		return font;
    	} catch (IOException ex){
    		throw new ServletException(ex);
    	}
    }
    
    
    /**
     * Respond subset font based on given params.
     * @param font
     * @param text
     * @param hinting
     * @param res request respond
     * @return font
     * @throws IOException
     */
    private Font subsetFont( Font font, String text, boolean hinting) throws IOException {
    	
    	Font newFont = font;
    	FontFactory fontFactory = FontFactory.getInstance();
    	if( text != null){
    	
	    	//List<CMapTable.CMapId> cmapIds = new ArrayList<CMapTable.CMapId>();
	        //cmapIds.add(CMapTable.CMapId.WINDOWS_BMP);
	        
	    	Subsetter subsetter = new RenumberingSubsetter(newFont, fontFactory);
	    	//subsetter.setCMaps(cmapIds, 1);
	    	List<Integer> glyphs = GlyphCoverage.getGlyphCoverage(font, text);
	    	subsetter.setGlyphs(glyphs);
	    	Set<Integer> removeTables = new HashSet<Integer>();
	    	
	    	//update tables
	    	removeTables.add(Tag.GDEF);
	        removeTables.add(Tag.GPOS);
	        removeTables.add(Tag.GSUB);
	        removeTables.add(Tag.kern);
	        removeTables.add(Tag.hdmx);
	        removeTables.add(Tag.vmtx);
	        removeTables.add(Tag.VDMX);
	        removeTables.add(Tag.LTSH);
	        removeTables.add(Tag.DSIG);
	        removeTables.add(Tag.intValue(new byte[]{'m', 'o', 'r', 't'}));
	        removeTables.add(Tag.intValue(new byte[]{'m', 'o', 'r', 'x'}));
	        subsetter.setRemoveTables(removeTables);
	        newFont = subsetter.subset().build();
    	}
        
    	//strip hinting
    	if( hinting ){
    		Subsetter hintStripper = new HintStripper(newFont, fontFactory);
    		Set<Integer> removeTables = new HashSet<Integer>();
	    	removeTables.add(Tag.fpgm);
	        removeTables.add(Tag.prep);
	        removeTables.add(Tag.cvt);
	        removeTables.add(Tag.hdmx);
	        removeTables.add(Tag.VDMX);
	        removeTables.add(Tag.LTSH);
	        removeTables.add(Tag.DSIG);
	        hintStripper.setRemoveTables(removeTables);
	        newFont = hintStripper.subset().build();
    	}
    	
    	return newFont;
    }
    
    private void respondFont( Font font, FontFormat format, HttpServletResponse res) throws IOException {
    	boolean mtx = false;
    	
    	//set cache
    	res.setHeader("Last-Modified", new Date().toString());
    	res.setHeader("Cache-Control", "max-age=86400, public");
    	
    	//allow cross domain for firefox
    	res.setHeader("Access-Control-Allow-Origin","*");
    	//output font file by format
    	
    	OutputStream os = res.getOutputStream();
    	if (format == FontFormat.Eot) {
    		res.setContentType("application/vnd.ms-fontobject");
    		WritableFontData eotData = new EOTWriter(mtx).convert(font);
    		eotData.copyTo(os);
    	} else if (format == FontFormat.Woff) {
    		res.setContentType("application/x-font-woff");
    		WritableFontData woffData = new WoffWriter().convert(font);
    		woffData.copyTo(os);
    	} else {
    		FontFactory fontFactory = FontFactory.getInstance();
    		//res.setContentType("application/x-font-ttf");
    		fontFactory.serializeFont(font, os);
    	}
    	
    }
    
    private Font getSubFontFromMemcache( String fontID, String token, boolean hinting) throws IOException{
    	Font cachedFont = null;
    	byte[] fontB = (byte[]) memcache.get(composeSubFontCacheKey(fontID, hinting, token));
    	if(fontB != null){
    		FontFactory fontFactory = FontFactory.getInstance();
    		ByteArrayInputStream bin = new ByteArrayInputStream(fontB);
    		FontInputStream fin = new FontInputStream(bin);
    		cachedFont = fontFactory.loadFonts(fin)[0];
    	}
    	return cachedFont;
    }
    
    private	void cacheSubFont( Font font, String fontID, String text, boolean hinting) throws IOException {
    	FontFactory fontFactory = FontFactory.getInstance();
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	FontOutputStream os = new FontOutputStream(out);
    	fontFactory.serializeFont(font, os);
    	try {
        	memcache.put(composeSubFontCacheKey(fontID, hinting, text), out.toByteArray());
        } catch (MemcacheServiceException ex){
        	LOGGER.warning(ex.toString());
        }
    	os.close();
    }
    
    private String composeSubFontCacheKey( String fontID, boolean hinting, String token){
    	String sHinting = "0";
    	if (hinting){
    		sHinting = "1";
    	}
    	return FONT_CACHE_PREFIX + fontID + ":" + sHinting + ":" + token;
    }
}

