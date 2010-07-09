package uk.ac.imperial.epi_collect.util.db;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
//import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log; 	 

public class DBAccess {
    public class Row extends Object {
    	public long rowId;
    	public String remoteId;
    	public Hashtable<String, Integer> spinners = new Hashtable<String, Integer>();
        public String gpslat;
        public String gpslon;
        public String gpsalt;
        public String gpsacc;
        public String photoid;
        public String ecdate;
        public String stored;
        public Hashtable<String, String> datastrings = new Hashtable<String, String>();
        public Hashtable<String, Boolean> checkboxes = new Hashtable<String, Boolean>();
        public Hashtable<String, Integer> rgroups = new Hashtable<String, Integer>();
        public boolean remote = false;
    }

    private static final String TAG = "EPI_Table";
    private static final String DATABASE_NAME = "epi_collect";
    private String DATABASE_TABLE = "data";
    private String DATABASE_PROJECT = "";
    //private static final String DATABASE_TABLE_PROJECTS = "projects";
    private static final int DATABASE_VERSION = 3;
    private final Context mCtx;
    private static String thumbdir; 
    private SQLiteDatabase db;
    private static String[] textviews = new String[0];
    private static String[] spinners = new String[0];
    private static String[] checkboxes = new String[0];
    private static String[] checkboxgroups = new String[0];
    private static Vector<String> doubles = new Vector<String>();
    private static Vector<String> integers = new Vector<String>();
    private static Hashtable <String, String[]>spinnershash = new Hashtable <String, String[]>();
    private static Hashtable <String, String[]>checkboxhash = new Hashtable <String, String[]>();
    private static Hashtable <String, String[]>spinnersvalueshash = new Hashtable <String, String[]>();
    private static Hashtable <String, String>checkboxvaluesvalueshash = new Hashtable <String, String>();
    private static Vector<String> requiredfields, requiredspinners;//, requiredradios;
    private static String image_url, synch_url; //, login_url;
    
    /**
     * This class helps open and create the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
        	super(context, DATABASE_NAME, null, DATABASE_VERSION);            
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	
        	db.execSQL("create table projects (project text primary key, active text);");
        	db.execSQL("create table firstrun (demoloaded int primary key);");
        	db.execSQL("replace into firstrun values(0)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	
        	Cursor c = db.rawQuery("select project from projects", null);
        	int numRows = c.getCount();
            if (numRows > 0) {
                c.moveToFirst();
                for (int i = 0; i < numRows; i++){ 
                	db.execSQL("DROP TABLE IF EXISTS data_"+c.getString(0));
                	c.moveToNext();
                }
            }
            
        	c.close();
        	
        	db.execSQL("DROP TABLE IF EXISTS projects");
        	db.execSQL("DROP TABLE IF EXISTS firstrun");
        	db.execSQL("DROP TABLE IF EXISTS firstrun");
        	//db.execSQL("create table firstrun (demoloaded int primary key);");
        	//db.execSQL("replace into firstrun values(0)");
        	
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
           // db.execSQL("DROP TABLE IF EXISTS data");
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;
   
    public DBAccess (Context ctx) {
        this.mCtx = ctx;
    }	
    
    public DBAccess open() throws SQLException {
    	mOpenHelper = new DatabaseHelper(mCtx);
    	db = mOpenHelper.getWritableDatabase();
    	return this;
    }

    public void close() {
    	mOpenHelper.close();
    }
    
    public void setFirstrun(){
    	
    	db.execSQL("update firstrun set demoloaded = 1");
    	
    	Cursor c = db.rawQuery("select demoloaded from firstrun", null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            //Log.i(getClass().getSimpleName(), "UPDATED FIRST RUN = " + c.getInt(0));
        }
        c.close();
    	
    }
    
    public int getFirstrun(){
    	int loaded = 0;
    	Cursor c = db.rawQuery("select demoloaded from firstrun", null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            loaded = c.getInt(0);
            //Log.i(getClass().getSimpleName(), "DBACCESS FIRST RUN = " + c.getInt(0));
        }
        c.close();
        return loaded;
    }

    public void dropTable(String table){
    	db.execSQL("drop table if exists "+table);
    }
   
    public void createProjectTable(StringBuffer table, String project){
    	db.execSQL(table.toString());  
    	Log.i("DATABASE 1", table.toString());
    }
    
    public void createDataTable( String project){
    	setActiveProject(project);
    	getValues();
    	
    	StringBuffer sb = new StringBuffer("create table if not exists data_"+project+" (ecrowId integer primary key, ecremoteId text, "
    			+ "ecgpslat text, ecgpslon text, ecgpsalt text, ecgpsacc text, ecphoto text, ecdate text, ecstored text, ecremote int");
    	
    	for(String key : textviews){
    		sb.append(", '"+key+"' text");
    	}
    	
    	for(String key : spinners){
    		sb.append(", '"+key+"' text");
    	}
    	
    	for(String key : checkboxes){
    		sb.append(", '"+key+"' text");
    	}
    	
    	sb.append(");");
    	
    	Log.i("DATABASE 2", sb.toString());
    	
    	db.execSQL(sb.toString());
    }
    
    public void createProjectRow(Hashtable<String, String> values, String p) {
        
    	getActiveProject();
      	ContentValues initialValues = new ContentValues();
      	
      	for(String key : values.keySet()){
      		initialValues.put(key, values.get(key));
      	}

          db.replace(DATABASE_PROJECT, null, initialValues); // replace
         
      }
    
    public void getActiveProject(){
    	Cursor c = db.query("projects", new String[] {
            		"project"}, "active='Y'", null, null, null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            DATABASE_TABLE = "data_"+c.getString(0);
            DATABASE_PROJECT = c.getString(0);
        }
        c.close();
    }
    
    public String getProject(){
    	Cursor c = db.query("projects", new String[] {
            		"project"}, "active='Y'", null, null, null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            DATABASE_PROJECT = c.getString(0);
        }
        c.close();
        return DATABASE_PROJECT;
    }
    
    public void setActiveProject(String project){
    	
    	db.execSQL("update projects set active = 'N'");
    	
    	db.execSQL("replace into projects values('"+project+"', 'Y')");
    	
    	// In case project already exists
    	db.execSQL("update projects set active = 'Y' where project = '"+project+"'");
    	
    	DATABASE_TABLE = "data_"+project;
        DATABASE_PROJECT = project;
        //Log.i(getClass().getSimpleName(), "ACTIVE PROJECT SET - TABLE "+DATABASE_TABLE);
    }
    
    public void deleteProject(String project){
    	
    	dropTable(project);
    	dropTable("data_"+project);
    	db.execSQL("delete from projects where project = '"+project+"'");
    }

    public String getValue(String column){
    	getActiveProject();
    	//Log.i(getClass().getSimpleName(), "ACTIVE PROJECT - GET VALUE "+DATABASE_PROJECT); 	
     	String result;
    	Cursor c = db.rawQuery("select "+column+" from "+DATABASE_PROJECT, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            result = c.getString(0);
            c.close();
            return result;
        }
        c.close();
        return "";
    	
    }   
    
    public boolean checkImages(){
    	getActiveProject();
    	
    	Cursor c = db.rawQuery("select ecrowId from "+DATABASE_TABLE+" where ecstored = 'N'", null);
        if (c.getCount() > 0) // There are unsyncronized record. Don't upload photos until all thumbnails are uploaded
        	return true;
        c.close();
        return false;
    	
    }   
    
    public boolean checkValue(String remoteid){
    	getActiveProject();
    	
    	Cursor c = db.rawQuery("select ecrowId from "+DATABASE_TABLE+" where ecremoteId='"+remoteid+"'", null);
        if (c.getCount() > 0) {
            return true;
        }
        c.close();
        return false;
    	
    }   
    
    public String[] getProjects(){
    	getActiveProject();
    	Cursor c = db.rawQuery("select project from projects order by active desc", null);
    	StringBuffer sb = new StringBuffer("");
    	int numRows = c.getCount();
        if (numRows > 0) {
            c.moveToFirst();
            for (int i = 0; i < numRows; i++){ 
            	sb.append(",,"+c.getString(0));
            	c.moveToNext();
            }
        }
        
        String allprojects = sb.toString();
    	allprojects.replaceFirst(",,,", "");
        
    	c.close();
    	return allprojects.split(",,");
    	
    }
    
    public void createRow(Hashtable<String, String> values) {
    	getActiveProject();
    	ContentValues initialValues = new ContentValues();
    	
    	for(String key : values.keySet()){
    		initialValues.put(key, values.get(key));
    	}
        
        //initialValues.put("stored", "N");
    	Log.i(getClass().getSimpleName(), "ACTIVE PROJECT TABLE "+DATABASE_TABLE);
        db.replace(DATABASE_TABLE, null, initialValues); // replace
       
    }


    public void deleteRow(int rowId) {
    	getActiveProject();
    	String pic;
    	//File picfile;
    	Cursor c = db.rawQuery("select ecphoto from "+DATABASE_TABLE+" where ecrowId='"+rowId+"'", null);
        if (c.getCount() > 0) {
        	c.moveToFirst();
        	pic = c.getString(0);
        	deleteImage(pic, true);
        	/*try{
            	picfile = new File(Environment.getExternalStorageDirectory()+"/EpiCollect/picdir_epicollect_" + getProject()+"/"+pic);
            	picfile.delete();
            	picfile = new File(Environment.getExternalStorageDirectory()+"/EpiCollect/thumbs_epicollect_" + getProject()+"/"+pic);
            	picfile.delete();
            	}
            catch(Exception e){}*/
        }
        c.close();
 
        db.delete(DATABASE_TABLE, "ecrowId=" + rowId, null);
    }
    
    public void deleteSynchRows() {
    	getActiveProject();
    	String pic;
    	//File picfile;
    	Cursor c = db.rawQuery("select ecphoto from "+DATABASE_TABLE+" where ecstored = 'Y'", null);
    	int numRows = c.getCount();
    	c.moveToFirst();
        for (int i = 0; i < numRows; i++){ 
        	pic = c.getString(0);
        	deleteImage(pic, false);
        	/*try{
            	picfile = new File(Environment.getExternalStorageDirectory()+"/EpiCollect/thumbs_epicollect_" + getProject()+"/"+pic);
            	picfile.delete();
            	}
            catch(Exception e){}*/
        	c.moveToNext();
        }
        c.close();
        db.delete(DATABASE_TABLE, "ecstored = 'Y'", null);
    }

    public void deleteRemoteRows() {
    	getActiveProject();
        db.delete(DATABASE_TABLE, "ecstored = 'R'", null);
    }
    
    public void deleteAllRows() {
    	getActiveProject();
    	/*String pic;
    	//File picfile;
    	Cursor c = db.rawQuery("select photo from "+DATABASE_TABLE+" where rowId >= 0", null);
    	int numRows = c.getCount();
    	c.moveToFirst();
        for (int i = 0; i < numRows; i++){ 
        	pic = c.getString(0);
        	deleteImage(pic, false);
        	//try{
            //	picfile = new File(Environment.getExternalStorageDirectory()+"/EpiCollect/thumbs_epicollect_" + getProject()+"/"+pic);
            //	picfile.delete();
            //	}
            //catch(Exception e){} 
        	c.moveToNext();
        } */
    	//SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        // Ensures images of synched records only are deleted first
        deleteSynchRows();
        db.delete(DATABASE_TABLE, "ecrowId >= 0", null);
    }
    
public List<Row> fetchAllRows(int remote) {
    	
    	getActiveProject();
    	getValues();
    	Cursor c = db.rawQuery("select * from "+DATABASE_TABLE+" where ecremote = "+remote, null);
    	//Cursor c = db.query(DATABASE_TABLE, new String[] {"*"}, null, null, null, null, null);
    	
        ArrayList<Row> ret = new ArrayList<Row>();
        //Log.i("DBACCESS: ", "Fetch all rows");
        try {
            int numRows = c.getCount();
            c.moveToFirst();
            for (int i = 0; i < numRows; ++i) {
            	//Log.i("DBACCESS: ", "Fetch all rows loop "+i);
            	Row row = getRow(c);
               
                ret.add(row); 
                c.moveToNext();
            }
        } catch (SQLException e) {
            Log.e("booga", e.toString());
        }
        c.close();
        return ret;
    }
        
    private Row getRow(Cursor c){
    	//Cursor c = thisc;
		Row row = new Row();

        row.rowId = c.getInt(0);
        row.remoteId = c.getString(1);
        row.gpslat = c.getString(2);
        row.gpslon = c.getString(3);
        row.gpsalt = c.getString(4);
        row.gpsacc = c.getString(5);
        row.photoid = c.getString(6);
        row.ecdate = c.getString(7);
        row.stored = c.getString(8);
        //Log.i("DBACCESS: ", "Fetch all rows " + row.stored);
        if(c.getInt(9) == 0)
        	row.remote = false;
        else
        	row.remote = true;
        
        int pos = 10;
       // Log.i("DB ROWS", "IN HERE");
        for(String key : textviews){
        	row.datastrings.put(key, c.getString(pos));
        	pos++;
        }
        
        for(String key : spinners){
        	row.spinners.put(key, c.getInt(pos));
        	pos++;
        }
        
        for(String key : checkboxes){
        	if(c.getInt(pos) == 1)
        		row.checkboxes.put(key, true);
        	else
        		row.checkboxes.put(key, false);
        	pos++;
        }
        //thisc.close();
        return row;
	}
    
    public long getNewID() {
    	getActiveProject();
        long maxid = 0;
        Cursor c = db.rawQuery("select max(ecrowId) from "+DATABASE_TABLE, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            maxid = c.getLong(0);
            maxid++;
            c.close();
            return maxid;
        } 

        c.close();
        return 1;
    }

    public void updateRecordID(long rowid, String remoteid, String stored){
    	getActiveProject();
    	ContentValues args = new ContentValues();
    	args.put("ecremoteId", remoteid);
        args.put("ecstored", stored);
        db.update(DATABASE_TABLE, args, "ecrowId=" + rowid, null);
    }
    
    // Used to check if a remote record is also in the local database.
    // If so it is not displayed on the map as a remote record
    public boolean checkremoteID(String remoteid) {
    	getActiveProject();
        Cursor c = db.rawQuery("select count(ecremoteId) from "+DATABASE_TABLE+" where ecremoteId = '"+remoteid+"'", null);
        if (c.getCount() > 0) {
        	c.moveToFirst();
            if(c.getLong(0) > 0){
            	Log.i(getClass().getSimpleName(), "IT'S IN THE DATABASE");
            	c.close();
            	return true;
            }
        } 
        c.close();
        return false;
    }
    
    public String synchronize(List<Row> rows, String sIMEI, String project){ //String email, , String password, String sIMEI){
    		
    	String result = "", photoresult = "";
    	boolean store = true;
    	
    	synch_url = getValue("synch_url"); //context.getResources().getString(context.getResources().getIdentifier(this.getClass().getPackage().getName()+":string/synch_url", null, null));
    	image_url = getValue("image_url");
    	
    	//Log.i(getClass().getSimpleName(), "DB SYNCH: "+ synch_url);  
    	//Log.i(getClass().getSimpleName(), "DB IMAGE: "+ image_url);  
    	
    	String data; //, thisphotoid;
    	
    	getActiveProject();
    	
    	try {
            // Construct data
            //String data;
    		data = "";
            for (Row row : rows) {
            	store = true;
            	if(row.stored.equals("N")){ 
             		
            		// First check the required fields
            		for(String key : row.datastrings.keySet()){
            			if((row.datastrings.get(key) == null || row.datastrings.get(key).equalsIgnoreCase("")) && requiredfields.contains(key)){
            				result += " " + row.rowId + " - " + key;
            				store = false;
            			}
            		}
            		
            		for(String key : row.spinners.keySet()){
            			if(row.spinners.get(key) == 0 && requiredspinners.contains(key)){
            				result += " " + row.rowId + " - " + key;
            				store = false;
            			}
            		}
            		            		
            		if(!store)
            			continue;
            		
            		//if(row.photoid != null && row.photoid.length() > 2)
            		//	uploadImage(row.photoid); //(row.photoid, time);
             		
            		String thisremoteid = sIMEI + "_" + row.ecdate;
            		data = URLEncoder.encode("epicollect_insert", "UTF-8") + "=" + URLEncoder.encode("form1", "UTF-8");
            		data += "&" + URLEncoder.encode("ecEntryId", "UTF-8") + "=" + URLEncoder.encode(thisremoteid, "UTF-8");
            		if(row.remoteId.equalsIgnoreCase("0"))
            			data += "&" + URLEncoder.encode("ecRemoteId", "UTF-8") + "=" + URLEncoder.encode(thisremoteid, "UTF-8");
            		else
            			data += "&" + URLEncoder.encode("ecRemoteId", "UTF-8") + "=" + URLEncoder.encode(row.remoteId, "UTF-8");
            		            		
            		String value;
            		for(String key : row.datastrings.keySet()){
            			if(row.datastrings.get(key) == null || row.datastrings.get(key).equalsIgnoreCase("")){
            				if(doubles.contains(key))
            					value = "0.0";
            				else if(integers.contains(key))
            					value = "0";
            				else
            					value = "N/A";
            			}
            			else
            				value = row.datastrings.get(key);
            			
            			data += "&" + URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
            		}
            		
            		for(String key : row.spinners.keySet()){
            			value = spinnersvalueshash.get(key)[row.spinners.get(key)];
            			
            			data += "&" + URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
            		}
            		
                    String[] tempstring;
                    String tempres;
                    int count;
            		for(String key : checkboxgroups){
            			tempres = "";
            			tempstring = checkboxhash.get(key);
            			count = 0;
            			for(String box : tempstring){
            				if(row.checkboxes.get(box) && count == 0){
            					tempres = checkboxvaluesvalueshash.get(box);
            					count++;
            				}
            				else if(row.checkboxes.get(box)){
            					tempres += ","+checkboxvaluesvalueshash.get(box);
            					count++;
            				}
            			}
            		if(tempres.length() > 0){
            			if(tempres.startsWith(","))
            				tempres.replaceFirst(",", "");
            			
            			data += "&" + URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(tempres, "UTF-8");
            		}
            			
            		}
            		
            		String email = sIMEI;
            		//thisphotoid = sIMEI+ "_"+row.ecdate+".jpg";
            		data += "&" + URLEncoder.encode("ecLatitude", "UTF-8") + "=" + URLEncoder.encode(row.gpslat, "UTF-8");
            		data += "&" + URLEncoder.encode("ecLongitude", "UTF-8") + "=" + URLEncoder.encode(row.gpslon, "UTF-8");
            		data += "&" + URLEncoder.encode("ecAltitude", "UTF-8") + "=" + URLEncoder.encode(row.gpsalt, "UTF-8");
            		data += "&" + URLEncoder.encode("ecAccuracy", "UTF-8") + "=" + URLEncoder.encode(row.gpsacc, "UTF-8");
            		data += "&" + URLEncoder.encode("ecTimeCreated", "UTF-8") + "=" + URLEncoder.encode(row.ecdate, "UTF-8");
            		data += "&" + URLEncoder.encode("ecLastEdited", "UTF-8") + "=" + URLEncoder.encode(row.ecdate, "UTF-8");
            		data += "&" + URLEncoder.encode("ecPhotoPath", "UTF-8") + "=" + URLEncoder.encode(row.photoid+".jpg", "UTF-8"); // row.photoid+"_"+time, "UTF-8");
            		data += "&" + URLEncoder.encode("ecDeviceID", "UTF-8") + "=" + URLEncoder.encode(sIMEI, "UTF-8"); // row.photoid+"_"+time, "UTF-8");
            		data += "&" + URLEncoder.encode("ecUserEmail", "UTF-8") + "=" + URLEncoder.encode(email, "UTF-8");
            		data += "&" + URLEncoder.encode("ecAppName", "UTF-8") + "=" + URLEncoder.encode(project, "UTF-8");

            		// Send data
            		//Log.i(getClass().getSimpleName(), "DATA: "+ data);
            		//Log.i(getClass().getSimpleName(), "URL: "+ synch_url);
            		URL url = new URL(synch_url); // "http://www.spatialepidemiology.net/epicollect1/test/insert.asp");

            		//URL url = new URL(R.string.data_url);
            		URLConnection conn = url.openConnection();
            		conn.setDoOutput(true);
            		OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            		wr.write(data);
            		wr.flush();
            		//Log.i(getClass().getSimpleName(), "WEB RETURN: GOT HERE");
            		// Get the response
           			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
           			String line;
           			while ((line = rd.readLine()) != null) {
           				Log.i(getClass().getSimpleName(), "WEB RETURN: "+ line);  
           				// Process line...
           				//if(line.contains("0"))
           				//	return "Login Failed. Check email and password";
           					
           				updateRecordID(row.rowId, thisremoteid, "Y"); //row.rowId);
           			}
           			wr.close();
           			rd.close(); 
           			
           			//The thisphotoid ensures that every photo has a unique id
           			if(row.photoid != null && row.photoid.length() > 2){
            			if(!uploadImage(row.photoid, false)) //, thisphotoid); //(row.photoid, time);
            				photoresult = "Images not uploaded";
           			}
           		}
            }
        } catch (Exception e) {
        	Log.i(getClass().getSimpleName(), "WEB ERROR: "+ e.toString()); 
        	return "Synchronisation Failed";
        }

        if(result.length() != 0)
        	return "Synchronisation Failed - Entries required for: "+result;
        else if(photoresult.length() != 0)
        	return "Synchronisation successful but some/all images failed";
        else
        	return "Synchronisation Successful";
           	
	}
       
	
    // Prevents warnings about List type
	@SuppressWarnings("unchecked")
	public List fetchXML(String xml_url, Vector<String> selectvec){ //, String lat, String lon){  // ArrayList<Row>
    	
		deleteRemoteRows();
		Hashtable<String, String> rowhash = new Hashtable<String, String>();
		
		getActiveProject();
		getValues();
		
		String selectstring = "";
		if(selectvec.size() > 0){
			selectstring = selectvec.elementAt(0);
		for(int i = 1; i < selectvec.size(); i++)
			selectstring += ",,"+selectvec.elementAt(i); 
		}
		
		InputStream xml_stream = null;
    	ArrayList<Row> ret = new ArrayList<Row>();
    	Element elmnt;
    	NodeList nmElmntLst;
    	Element nmElmnt;
    	NodeList elNm;
    	xml_url += "?project="+DATABASE_PROJECT;
    	Log.i(getClass().getSimpleName(), "XML URL: "+ xml_url);
    	try{
    		
    		URL url = new URL(xml_url);
    		
            HttpURLConnection urlc = (HttpURLConnection)url.openConnection();
            urlc.setRequestMethod("GET");
            urlc.connect();

    		xml_stream = urlc.getInputStream();
    		
    		Log.i(getClass().getSimpleName(), "XML RETURN: "+ xml_stream.toString());

    	}
    	catch(MalformedURLException  ex){
    		Log.i(getClass().getSimpleName(), "XML 1: "+ ex.toString());
    		return null;
    		}
    	catch (IOException ex) {
    		Log.i(getClass().getSimpleName(), "XML 2: "+ ret.toString());
    		return null;
    		}
    	
    	try {
    		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    		DocumentBuilder db = dbf.newDocumentBuilder();
    		Document doc = db.parse(xml_stream); 
    		doc.getDocumentElement().normalize();
    		NodeList nodeLst;
    		try{
    		nodeLst = doc.getElementsByTagName("entry");
    		}catch(NullPointerException npe){
    			Log.i(getClass().getSimpleName(), "XML ERROR 8: "+ npe.toString());
    		}
    		nodeLst = doc.getElementsByTagName("entry");
    		//long newID = -1; 
    		for (int s = 0; s < nodeLst.getLength(); s++) { 
    			try{
    				Row row = new Row();
    				rowhash.clear();
    			
    				// Initisalise all checkbox values to false
    				String[] tempstring;
    				for(String key : checkboxgroups){
    					tempstring = checkboxhash.get(key);
    					for(String box : tempstring){
    						row.checkboxes.put(box, false);
    					}
    				}
    				row.remote = true;
    				Node fstNode = nodeLst.item(s);
    	    
    				if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
    				
    					elmnt = (Element) fstNode;
    				    		
    					nmElmntLst = elmnt.getElementsByTagName("ecEntryId");
    					nmElmnt = (Element) nmElmntLst.item(0);
    					elNm = nmElmnt.getChildNodes();
    					row.remoteId = ((Node) elNm.item(0)).getNodeValue();
    					
    					//Log.i(getClass().getSimpleName(), "XML CHECK 1: "+ ((Node)elNm.item(0)).getNodeValue());
    					// This can throw a NullPointerException as last node seems to be incomplete
    					
    					row.rowId = getNewID();
    					rowhash.put("ecrowId", ""+row.rowId);
    					rowhash.put("ecremoteId", ""+row.remoteId);
    					//row.rowId = newID; //getNewID();
    					//newID--;
    					
    					nmElmntLst = elmnt.getElementsByTagName("ecLatitude");
	    				nmElmnt = (Element) nmElmntLst.item(0);
	    				elNm = nmElmnt.getChildNodes();
	    				row.gpslat = ((Node) elNm.item(0)).getNodeValue();
	    	           
	    				//Log.i(getClass().getSimpleName(), "XML CHECK 2: "+ ((Node)elNm.item(0)).getNodeValue());
	    				nmElmntLst = elmnt.getElementsByTagName("ecLongitude");
	    				nmElmnt = (Element) nmElmntLst.item(0);
	    				elNm = nmElmnt.getChildNodes();
	    				row.gpslon = ((Node) elNm.item(0)).getNodeValue();
	    				//Log.i(getClass().getSimpleName(), "XML CHECK 3: "+ ((Node)elNm.item(0)).getNodeValue());
	    					
	    				nmElmntLst = elmnt.getElementsByTagName("ecAltitude");
	    				nmElmnt = (Element) nmElmntLst.item(0);
	    				elNm = nmElmnt.getChildNodes();
	    				row.gpsalt = ((Node) elNm.item(0)).getNodeValue();
	    				//Log.i(getClass().getSimpleName(), "XML CHECK 4: "+ ((Node)elNm.item(0)).getNodeValue());
	    				try{
	    				nmElmntLst = elmnt.getElementsByTagName("ecAccuracy");
	    				nmElmnt = (Element) nmElmntLst.item(0);
	    				elNm = nmElmnt.getChildNodes();
	    				row.gpsacc = ((Node) elNm.item(0)).getNodeValue();
	    				//Log.i(getClass().getSimpleName(), "XML CHECK 5: "+ ((Node)elNm.item(0)).getNodeValue());
	    				}
	    				catch(NullPointerException npe){
	    					row.gpsacc = "N/A";
	    				}
	    				
	    				nmElmntLst = elmnt.getElementsByTagName("ecTimeCreated");
	    				nmElmnt = (Element) nmElmntLst.item(0);
	    				elNm = nmElmnt.getChildNodes();
	    				row.ecdate = ((Node) elNm.item(0)).getNodeValue();
	    				//Log.i(getClass().getSimpleName(), "XML CHECK 6: "+ ((Node)elNm.item(0)).getNodeValue());
	    				
	    				rowhash.put("ecgpslat", row.gpslat);
	    				rowhash.put("ecgpslon", row.gpslon);
	    				rowhash.put("ecgpsalt", row.gpsalt);
	    				rowhash.put("ecgpsacc", row.gpsacc);
	    				rowhash.put("ecdate", row.ecdate); 
    				
	    				
	    				for(String key : textviews){
	    					try{
	            			//Log.i(getClass().getSimpleName(), "XML TEXTVIEW 1: "+ key);
	            			nmElmntLst = elmnt.getElementsByTagName(key);
	            			if(nmElmntLst.getLength()>=0){
	            				nmElmnt = (Element) nmElmntLst.item(0);
	        					elNm = nmElmnt.getChildNodes();
	        					row.datastrings.put(key, ((Node) elNm.item(0)).getNodeValue());
	        					rowhash.put(key, ((Node) elNm.item(0)).getNodeValue());
	        					//Log.i(getClass().getSimpleName(), "XML CHECK 7: "+ ((Node)elNm.item(0)).getNodeValue());
	        					//Log.i(getClass().getSimpleName(), "XML TEXTVIEW 1: "+ key+" "+((Node) elNm.item(0)).getNodeValue());
	            				}
	            			else{
	            				row.datastrings.put(key, "N/A");
	            				rowhash.put(key, "");
	            				//Log.i(getClass().getSimpleName(), "XML TEXTVIEW 1: "+ key+" N/A");
	            			}
	    				
	    					}	
	    					catch(NullPointerException npe){
	    						Log.i(getClass().getSimpleName(), "XML ERROR 6: "+ npe.toString());
            			 	} 
	    				}
	            		// Initialise spinners
	            		for(String val : spinners){
	    					row.spinners.put(val, 0);
	    					}
	
	            		String value;
	            		int index;
	            		
	            		for(String key : spinners){
	            			try{
	            			nmElmntLst = elmnt.getElementsByTagName(key);
	            			if(nmElmntLst.getLength()>=0){
	            				nmElmnt = (Element) nmElmntLst.item(0);
	            				elNm = nmElmnt.getChildNodes();
	            				value = ((Node) elNm.item(0)).getNodeValue();
	            				if(value.length() == 0)
	            					continue;
	            				//Log.i(getClass().getSimpleName(), "XML CHECK 8: "+ ((Node)elNm.item(0)).getNodeValue());
	        				
	            				String[] spinners;
	            				index = 0;
	            				spinners = spinnershash.get(key); 
	        				
	            				for(String val : spinners){
	            					//Log.i(getClass().getSimpleName(), "SPINNER XML INDEX: "+ index+" KEY "+key+" VALUE "+value+" VAL "+val);
	            					if(val.equalsIgnoreCase(value)){
	            						row.spinners.put(value, index);
	            						rowhash.put(key, ""+index);
	            						//Log.i(getClass().getSimpleName(), "SET SPINNER XML INDEX: "+ index+" KEY "+key+" VALUE "+value+" VAL "+val);
		            					continue;
	            					}
	            					index++;
	            				}
	            			}
	            			}
	            			catch(NullPointerException npe){
	            				Log.i(getClass().getSimpleName(), "XML ERROR 5: "+ npe.toString());
	            			} 
	            		}
	            		
	            		String[] tempstring2;
	            		
	            		for(String key : checkboxgroups){
	            			try{
	            			nmElmntLst = elmnt.getElementsByTagName(key);
	            			if(nmElmntLst.getLength()>=0){
	            				nmElmnt = (Element) nmElmntLst.item(0);
	            				elNm = nmElmnt.getChildNodes();
	            				if(((Node) elNm.item(0)).getNodeValue().length() == 0)
	            					continue;
	            				//Log.i(getClass().getSimpleName(), "XML CHECK A: "+ ((Node)elNm.item(0)).getNodeValue());
	            				tempstring = checkboxhash.get(key);
	            				//Log.i(getClass().getSimpleName(), "XML CHECK B: "+ ((Node)elNm.item(0)).getNodeValue());
	            				tempstring2 = ((Node) elNm.item(0)).getNodeValue().split(",");
	            				//Log.i(getClass().getSimpleName(), "XML CHECK C: "+ ((Node)elNm.item(0)).getNodeValue());
	                			for(String box : tempstring){
	                				//Log.i(getClass().getSimpleName(), "XML CHECK 9: "+ ((Node)elNm.item(0)).getNodeValue());
	                				for(String box2 : tempstring2){
	                					if(box2.equalsIgnoreCase(checkboxvaluesvalueshash.get(box))){
	                						row.checkboxes.put(box2, true);
	                						//if(checkboxhash.get(box2) != null && checkboxhash.get(box2) == true)
	                							rowhash.put(box, "1");
	                						//else
	                						//	rowhash.put(box2, "0");
	                					}
	                				}
	                			}
	            			}
	            			
	            		} 
	            		catch(NullPointerException npe){
	            			Log.i(getClass().getSimpleName(), "XML ERROR 4: "+ npe.toString());
	            			}
	            		}
	            		}
	            		
	            		rowhash.put("ecremote", ""+1);
	            		rowhash.put("ecstored", "R");
	            		
	            		if(!checkValue(row.remoteId)){
	            			
	            			createRow(rowhash);
	            			ret.add(row);
	            			}
	            		
	    			
	    		}
				catch(NullPointerException npe){
					Log.i(getClass().getSimpleName(), "XML ERROR 1: "+ npe.toString());
				}
					
	    		}
	    	}	 
	    	catch (Exception e) {
	    	    e.printStackTrace();
	    	    return null;
	    	} 
	    	Log.i(getClass().getSimpleName(), "XML HERE: FINISHED");	
	    	return ret;
	    } 
	
	public String uploadAllImages(){
		
		File dir = new File(Environment.getExternalStorageDirectory()+"/EpiCollect/picdir_epicollect_" + getProject());
		File picfile;
	    String[] chld = dir.list();
	    int total = 0, count = 0;
	    if(chld == null){
	      return "No images to upload";
	    }
	    else{
	    	total = chld.length;
	    	for(int i = 0; i < chld.length; i++){
	    		//String fileName = chld[i];
	    		if(chld[i].length() > 3 && uploadImage(chld[i], false)){
	    			count++;
	    			try{
	    	        	picfile = new File(Environment.getExternalStorageDirectory()+"/EpiCollect/picdir_epicollect_" + getProject()+"/"+chld[i]);
	    	        	picfile.delete();
	    	        	}
	    	        catch(Exception e){}
	    		}
	    		else{
	    			total--;
	    		}
	    	}
	    }
		
	    if(total == 0)
	    	return "No images to upload";
	    else if(count == total)
	    	return "Upload of "+total+" images successful";
	    else
	    	return "Upload of images failed. " +count+" of "+ total+" images uploaded";
	}
	
	private boolean uploadImage(String photoid, boolean thumb){ //, String thisphotoid){ 
		
		getValues();
		
		String imagedir;
		//Log.i("DBAccess UPLOADIMAGE","PHOTOID "+ photoid);
		
		HttpURLConnection conn = null;
	    DataOutputStream dos = null;
	    DataInputStream inStream = null;

	   // photoid += ".jpg";
	   String existingFileName = photoid;
       // Is this the place are you doing something wrong.

       String lineEnd = "\r\n";
       String twoHyphens = "--";
       String boundary =  "*****";

       int bytesRead, bytesAvailable, bufferSize;
       byte[] buffer;
       int maxBufferSize = 1*1024*1024;
       
       if(thumb)
    	   imagedir = Environment.getExternalStorageDirectory()+"/EpiCollect/thumbs_epicollect_" + getProject(); //context.getResources().getString(context.getResources().getIdentifier(context.getPackageName()+":string/project", null, null));
       else
    	   imagedir = Environment.getExternalStorageDirectory()+"/EpiCollect/picdir_epicollect_" + getProject(); //context.getResources().getString(context.getResources().getIdentifier(context.getPackageName()+":string/project", null, null));
           	   
       image_url = getValue("image_url"); // context.getResources().getString(context.getResources().getIdentifier(this.getClass().getPackage().getName()+":string/image_url", null, null));
       

       //String responseFromServer = "";

       String urlString = image_url;
       
       try{
        //------------------ CLIENT REQUEST
	     
	       Log.e("DBAccess","Inside second Method");
	       
	
	       FileInputStream fileInputStream = new FileInputStream(new File(imagedir+"/" + existingFileName) );
	
	        // open a URL connection to the Servlet
	
	        URL url = new URL(urlString);
	
	        // Open a HTTP connection to the URL
	
	        conn = (HttpURLConnection) url.openConnection();
	
	        // Allow Inputs
	        conn.setDoInput(true);
	        // Allow Outputs
	        conn.setDoOutput(true);
	
	        // Don't use a cached copy.
	        conn.setUseCaches(false);
	
	        // Use a post method.
	        conn.setRequestMethod("POST");
	
	        conn.setRequestProperty("Connection", "Keep-Alive");
		     
	        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
	
	        dos = new DataOutputStream( conn.getOutputStream() );
	
	        dos.writeBytes(twoHyphens + boundary + lineEnd);
	        //dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + existingFileName +"\"" + lineEnd); // existingFileName
	        //dos.writeBytes("Content-Disposition: form-data; name=\""+existingFileName+"\";filename=\"" + existingFileName +"\"" + lineEnd); // uploadedfile
	        dos.writeBytes("Content-Disposition: form-data; name=\""+photoid+".jpg\";filename=\"" + existingFileName +"\"" + lineEnd);
	        
	        dos.writeBytes(lineEnd);
	
	        Log.e("DBAccess","Headers are written");
	
	        // create a buffer of maximum size
	
	        bytesAvailable = fileInputStream.available();
	        bufferSize = Math.min(bytesAvailable, maxBufferSize);
	        buffer = new byte[bufferSize];
	
	        // read file and write it into form...
	
	        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
	
	        while (bytesRead > 0){
		         dos.write(buffer, 0, bufferSize);
		         bytesAvailable = fileInputStream.available();
		         bufferSize = Math.min(bytesAvailable, maxBufferSize);
		         bytesRead = fileInputStream.read(buffer, 0, bufferSize);
	        }
	
	        // send multipart form data necesssary after file data...
	
	        dos.writeBytes(lineEnd);
	        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
	
	        // close streams
	        Log.i("DBAccess","File is written "+ existingFileName);
	        fileInputStream.close();
	        dos.flush();
	        dos.close();


       }
       catch (MalformedURLException ex){
            Log.e("DBAccess", "error: " + ex.getMessage(), ex);
            return false;
       }

       catch (IOException ioe){
	        Log.e("DBAccess", "error: " + ioe.getMessage(), ioe);
	        return false;
	   }

       //------------------ read the SERVER RESPONSE

       try {
             inStream = new DataInputStream ( conn.getInputStream() );
             String str;
	           
             while (( str = inStream.readLine()) != null)
             {
                  Log.e("DBAccess","Server Response "+str);
             }
             inStream.close();
	       }
       catch (IOException ioex){
            Log.e("DBAccess", "error: " + ioex.getMessage(), ioex);
       }

       return true;
     } 
		
	private void getValues(){
		
    	textviews = new String[0];
    	spinners = new String[0];
    	checkboxes = new String[0];
    	checkboxgroups = new String[0];
    	
        spinnershash.clear();
        checkboxhash.clear();
        spinnersvalueshash.clear();
        checkboxvaluesvalueshash.clear();
        
       	doubles.clear();
    	integers.clear();
    	
		if(getValue("textviews") != null && getValue("textviews").length() > 0)
			textviews = (getValue("textviews")).split(",,"); // "CNTD", 
    	if(getValue("spinners") != null && getValue("spinners").length() > 0)
    		spinners = (getValue("spinners")).split(",,");
    	if(getValue("checkboxes") != null && getValue("checkboxes").length() > 0)
    		checkboxes = (getValue("checkboxes")).split(",,");
    	if(getValue("checkboxgroups") != null && getValue("checkboxgroups").length() > 0)
    		checkboxgroups = (getValue("checkboxgroups")).split(",,");
    	
    	for(String key : (getValue("doubles")).split(",,")){
        	doubles.addElement(key);
        }
        
        for(String key : (getValue("integers")).split(",,")){
        	integers.addElement(key);
        }
                           
        String[] tempstring = null;
        for(String key : spinners){       	
        	tempstring = getValue("spinner_"+key).split(",,");
	    	spinnershash.put(key, tempstring);
        }       
        
        for(String key : spinners){       	
        	tempstring = getValue("spinner_values_"+key).split(",,");
	    	spinnersvalueshash.put(key, tempstring);
        }
        
        String[] tempstring2;
        for(String key : checkboxgroups){
        	if(getValue("checkbox_"+key) != null){
        		tempstring = getValue("checkbox_"+key).split(",,");
 	    		checkboxhash.put(key, tempstring);
        	}
 	    	
        	if(getValue("checkbox_values_"+key) != null){
        		tempstring2 = getValue("checkbox_values_"+key).split(",,");
        		for(int i = 0; i < tempstring.length; i++){
        			checkboxvaluesvalueshash.put(tempstring[i], tempstring2[i]);
 	    			}
	    		}
        	}
        List<String> list = Arrays.asList(getValue("requiredtext").split(",,"));
        requiredfields = new Vector<String>(list);
        
        list = Arrays.asList(getValue("requiredspinners").split(",,"));
        requiredspinners = new Vector<String>(list);
        
        
        
    }
	
	private void deleteImage(String pic, boolean full){
		File picfile;
		try{
			if(full){
				picfile = new File(Environment.getExternalStorageDirectory()+"/EpiCollect/picdir_epicollect_" + getProject()+"/"+pic);
				picfile.delete();
			}
        	picfile = new File(Environment.getExternalStorageDirectory()+"/EpiCollect/thumbs_epicollect_" + getProject()+"/"+pic);
        	picfile.delete();
        	}
        catch(Exception e){}
	}
	/*private static void copyFile(File f1, File f2){ //String srFile, String dtFile){
        try{

          InputStream in = new FileInputStream(f1);

          //For Overwrite the file.
          OutputStream out = new FileOutputStream(f2);

          byte[] buf = new byte[1024];
          int len;
          while ((len = in.read(buf)) > 0){
            out.write(buf, 0, len);
          }
          in.close();
          out.close();
        }
        catch(FileNotFoundException ex){
        	Log.i("ImageSwitcher", ex.toString());
        }
        catch(IOException e){
        	Log.i("ImageSwitcher", e.toString());
        }
      }*/
}
