package uk.ac.imperial.epi_collect.util.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
//import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import uk.ac.imperial.epi_collect.util.db.DBAccess;

//import org.xml.sax.helpers.XMLReaderFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.util.Log;

public class ParseXML{

	InputStream xml_stream;
	//String xml_stream;
	StringBuffer views = new StringBuffer(""), textviews = new StringBuffer(""), checkboxes = new StringBuffer(""), 
	spinners = new StringBuffer(""), checkboxgroups = new StringBuffer(""), doubles = new StringBuffer(""), 
	integers = new StringBuffer(""), requiredtext = new StringBuffer(""), requiredspinners = new StringBuffer(""), 
	listfields = new StringBuffer(""), listspinners = new StringBuffer(""), listcheckboxes = new StringBuffer("");
	Hashtable<String, StringBuffer> spinnerrefs = new Hashtable<String, StringBuffer>();
	Hashtable<String, StringBuffer> spinnervalues = new Hashtable<String, StringBuffer>();
	Hashtable<String, StringBuffer> checkboxrefs = new Hashtable<String, StringBuffer>();
	Hashtable<String, StringBuffer> checkboxvalues = new Hashtable<String, StringBuffer>();
	String app_name="", project="", changesynch = "false";
	String url_string = "";
	String demoxml = "<xform> <model> <submission id=\"ahBlcGljb2xsZWN0c2VydmVycg8LEgdQcm9qZWN0GKbgFAw\" projectName=\"demoproject\" allowDownloadEdits=\"false\" versionNumber=\"1.1\"/> </model><input ref=\"name\" required=\"true\" title=\"true\"> <label>What is your name?</label> </input><select1 ref=\"age\" required=\"true\" chart=\"bar\"> <label>What is your age?</label> <item><label>below10</label><value>below10</value></item> <item><label>11to20</label><value>11to20</value></item> <item><label>21to30</label><value>21to30</value></item> <item><label>31to40</label><value>31to40</value></item> <item><label>41to50</label><value>41to50</value></item> <item><label>51to60</label><value>51to60</value></item> <item><label>61to70</label><value>61to70</value></item> <item><label>above70</label><value>above70</value></item> </select1><select1 ref=\"sex\" required=\"true\"> <label>Male or Female?</label> <item><label>Male</label><value>Male</value></item> <item><label>Female</label><value>Female</value></item> </select1><select1 ref=\"searchengine\" required=\"true\" chart=\"bar\"> <label>Which search engine do you most often use?</label> <item><label>Google</label><value>Google</value></item> <item><label>Yahoo</label><value>Yahoo</value></item> <item><label>Baidu</label><value>Baidu</value></item> <item><label>Bing</label><value>Bing</value></item> <item><label>Ask</label><value>Ask</value></item> <item><label>AOL</label><value>AOL</value></item> <item><label>AltaVista</label><value>AltaVista</value></item> <item><label>other</label><value>other</value></item> </select1><select ref=\"socialnetworks\" required=\"true\" chart=\"pie\"> <label>Which social networking sites do you use?</label> <item><label>MySpace</label><value>MySpace</value></item> <item><label>Facebook</label><value>Facebook</value></item> <item><label>Hi5</label><value>Hi5</value></item> <item><label>Friendster</label><value>Friendster</value></item> <item><label>Orkut</label><value>Orkut</value></item> <item><label>Bebo</label><value>Bebo</value></item> <item><label>Tagged</label><value>Tagged</value></item> <item><label>Xing</label><value>Xing</value></item> <item><label>Badoo</label><value>Badoo</value></item> <item><label>Xanga</label><value>Xanga</value></item> <item><label>51.com</label><value>51com</value></item> <item><label>Xiaonei</label><value>Xiaonei</value></item> <item><label>ChinaRen</label><value>ChinaRen</value></item> </select></xform>";
	String remote_xml = "http://epicollectserver.appspot.com/downloadFromServer";
    String image_url = "http://epicollectserver.appspot.com/uploadImageToServer";
    String synch_url = "http://epicollectserver.appspot.com/uploadToServer";
	
	final Vector<String> types = new Vector<String>();
	DBAccess dbAccess;
	
	public ParseXML(String url_s){ // "http://www.doc.ic.ac.uk/~dmh1/Android//xml.xml"ArrayList<Row>
		//Log.i(getClass().getSimpleName(), "URL: "+ url_s);
		demoxml = "";
		url_string = url_s;
	}
	
	public ParseXML(){ // "http://www.doc.ic.ac.uk/~dmh1/Android//xml.xml"ArrayList<Row>
	}

	public boolean getXML(){    
	
		types.add("select1");
		types.add("input");
		types.add("select");
		types.add("textarea");
		//types.add("submission");
		//types.add("id");
		
    	try{
    		
    		URL url = new URL(url_string); //("http://www.doc.ic.ac.uk/~dmh1/Android//xml.xml"); // xml_url); //"http://www.google.co.uk");
    		
            HttpURLConnection urlc = (HttpURLConnection)url.openConnection();
            urlc.setRequestMethod("GET");
            urlc.connect();

    		xml_stream = urlc.getInputStream();
    		Log.i(getClass().getSimpleName(), "VIEWS: "+ xml_stream.toString());
    		
    	}
    	catch(MalformedURLException  ex){
    		System.out.println("1 "+ex);
    		Log.i(getClass().getSimpleName(), "Failed 1 "+ex.toString());
    		return false;
    		}
    	catch (IOException ex) {
    		System.out.println("2 "+ex);
    		Log.i(getClass().getSimpleName(), "Failed 2 "+ex.toString());
    		return false;
    		}
    	
    	try {
    		 
    	      SAXParserFactory factory = SAXParserFactory.newInstance();
    	      SAXParser saxParser = factory.newSAXParser();
    	 
    	      XMLHandler handler = new XMLHandler();
    	      saxParser.parse(xml_stream, handler);   	 

    	      
    	      //saxParser.parse(xml_stream, handler);
    	 
    	    } catch (Exception e) {
    	      //e.printStackTrace();
    	      Log.i(getClass().getSimpleName(), "VIEWS ERROR "+e.toString());
    	      return false;
    	    }
    	    //Log.i(getClass().getSimpleName(), "VIEWS "+views.toString());
    	    //System.out.println(views.toString());
    	    
    	    //createTable();
    	    return true;
    	  }
	
	public boolean getDemoXML(){    
		
		types.add("select1");
		types.add("input");
		types.add("select");
		types.add("textarea");

    	
    	try {
    		 
    	      SAXParserFactory factory = SAXParserFactory.newInstance();
    	      SAXParser saxParser = factory.newSAXParser();
    	 
    	      XMLHandler handler = new XMLHandler();
    	      InputStream is = new ByteArrayInputStream(demoxml.getBytes("UTF-8"));
    	      saxParser.parse(is, handler);   	 

    	      
    	      //saxParser.parse(xml_stream, handler);
    	 
    	    } catch (Exception e) {
    	      //e.printStackTrace();
    	      Log.i(getClass().getSimpleName(), "VIEWS ERROR "+e.toString());
    	      return false;
    	    }
    	    //Log.i(getClass().getSimpleName(), "VIEWS "+views.toString());
    	    //System.out.println(views.toString());
    	    
    	    //createTable();
    	    return true;
    	  }


	public String getValues(){
		return views.toString();
	}

	public String getProject(){
		return project;
	}
	
	public StringBuffer createTable(){
		
		StringBuffer table = new StringBuffer("create table if not exists "+project +" (textviews text, checkboxes text, spinners text," +
				" checkboxgroups text, doubles text, integers text, requiredtext text, requiredspinners text," +
				" listfields text, listspinners text, listcheckboxes text, change_synch text, remote_xml text, image_url text, synch_url text");
		
		for(String key : spinnerrefs.keySet()){
			table.append(", '"+key+"' text");
		}
		
		for(String key : spinnervalues.keySet()){
			table.append(", '"+key+"' text");
		}
		
		for(String key : checkboxrefs.keySet()){
			table.append(", '"+key+"' text");
		}
		
		for(String key : checkboxvalues.keySet()){
			table.append(", '"+key+"' text");
		}
		
		table.append(", notes_layout text)");
		
		//dbAccess.createTable(table);
		return table;
	}
	
	public Hashtable<String, String> createRow(){
		
		Hashtable<String, String> rowhash = new Hashtable<String, String>();
    	//rowhash.put("project", project);
		if(textviews.length() > 0)
			rowhash.put("textviews", textviews.toString().replaceFirst(",,", ""));
    	if(checkboxes.length() > 0)
    		rowhash.put("checkboxes", checkboxes.toString().replaceFirst(",,", ""));
    	if(spinners.length() > 0)
    		rowhash.put("spinners", spinners.toString().replaceFirst(",,", ""));  
    	if(checkboxgroups.length() > 0)
    		rowhash.put("checkboxgroups", checkboxgroups.toString().replaceFirst(",,", ""));
    	rowhash.put("doubles", doubles.toString().replaceFirst(",,", ""));
    	rowhash.put("integers", integers.toString().replaceFirst(",,", ""));
    	rowhash.put("requiredtext", requiredtext.toString().replaceFirst(",,", ""));
    	rowhash.put("requiredspinners", requiredspinners.toString().replaceFirst(",,", ""));
    	rowhash.put("listfields", listfields.toString().replaceFirst(",,", ""));
    	rowhash.put("listspinners", listspinners.toString().replaceFirst(",,", ""));
    	rowhash.put("listcheckboxes", listcheckboxes.toString().replaceFirst(",,", ""));
    	rowhash.put("change_synch", changesynch);
    	rowhash.put("remote_xml", remote_xml);
    	rowhash.put("image_url", image_url);
    	rowhash.put("synch_url", synch_url);
    	
    	for(String key : spinnerrefs.keySet()){
    		rowhash.put(key, spinnerrefs.get(key).toString());
		}
		
		for(String key : spinnervalues.keySet()){
			rowhash.put(key, spinnervalues.get(key).toString());
		}
		
		for(String key : checkboxrefs.keySet()){
			rowhash.put(key, checkboxrefs.get(key).toString());
		}
		
		for(String key : checkboxvalues.keySet()){
			rowhash.put(key, checkboxvalues.get(key).toString());
		}
		
		rowhash.put("notes_layout", views.toString());
		
		for(String key: rowhash.keySet())
			Log.i(getClass().getSimpleName(), "PROJECT ROW: "+ key +" = "+rowhash.get(key));	    
		return rowhash;
		//dbAccess.createProjectRow(rowhash);
	}
	    
	    public class XMLHandler extends DefaultHandler {
	   	 
	        boolean item = false;
	        boolean label = false;
	        boolean value = false;
	        //boolean submission = false;
	        //boolean allow = false;
	        boolean intype = false;
	        boolean inselect = false;
	        boolean inselect1 = false;
	        boolean ininput = false;
	        boolean download = false;
	        boolean upload = false;
	        boolean image = false;
	        String ref = "", req="", num="", title="";
	        String thislabel = "";

	        public void startElement(String uri, String qName, String localName, Attributes attributes) throws SAXException {

	        	/*if(attributes.getValue("downloadFromServerX") != null){
		    		  remote_xml = attributes.getValue("downloadFromServer");
		    		  Log.i(getClass().getSimpleName(), "XML REMOTE "+remote_xml);
		    	  }
		    	  if(attributes.getValue("downloadFromServer") != null){
		    		  image_url = attributes.getValue("uploadImageToServer");
		    		  Log.i(getClass().getSimpleName(), "XML IMAGE "+image_url);
		    	  }
		    	  if(attributes.getValue("uploadToServer") != null){
		    		  synch_url = attributes.getValue("downloadFromServer");
		    		  Log.i(getClass().getSimpleName(), "XML SYNCH "+synch_url);
		    	  } */
		    	  
	        if(attributes.getValue("ref") != null)
	      	  ref = attributes.getValue("ref");
	    	  req = attributes.getValue("required");
	    	  num = attributes.getValue("numeric");
	    	  title = attributes.getValue("title");
	    	  
	    	 // Log.i(getClass().getSimpleName(), "XML REMOTE "+image_url);
	        	//Log.i(getClass().getSimpleName(), "URI "+uri+" LOCAL "+localName+" QNAME "+qName);
	    	 // textviews = new StringBuffer(""), checkboxes = new StringBuffer(""), 
	    	//	spinners = new StringBuffer(""), checkboxgroups = new StringBuffer(""), doubles = new StringBuffer(""), 
	    	//	integers = new StringBuffer(""), requiredtext = new StringBuffer(""), requiredspinners = new StringBuffer(""), 
	    	//	listfields = new StringBuffer(""), listspinners = new StringBuffer(""), listcheckboxes = new StringBuffer("");
	    	  if(qName.equalsIgnoreCase("downloadFromServer")){
	    		  download = true;
	    	  }
	    	  if(qName.equalsIgnoreCase("uploadToServer")){
	    		  upload = true;
	    	  }
	    	  if(qName.equalsIgnoreCase("uploadImageToServer")){
	    		  image = true;
	    	  }
	    	  
	    	  if(types.contains(qName)){
	          	//System.out.println("Type :" + qName);
	          	intype = true;
	          	if(qName.equalsIgnoreCase("textarea")){
	          		views.append(",,,"+"input");
	          	}
	          	else{
	          		views.append(",,,"+qName);
	          	}
	          	views.append(",,"+ref);
	          	if(qName.equalsIgnoreCase("select")){
	          		inselect = true;
	          		checkboxgroups.append(",,"+ref);
	          	}
	          	else
	          		inselect = false;
	          	
	          	if(qName.equalsIgnoreCase("select1")){
	          		inselect1 = true;
	          		spinners.append(",,"+ref);
	          		if(req != null && req.equalsIgnoreCase("true"))
	          			requiredspinners.append(",,"+ref);
	          		if(title != null && title.equalsIgnoreCase("true"))
	          			listspinners.append(",,"+ref);
	          	}
	          	else
	          		inselect1 = false;
	          	
	          	if(qName.equalsIgnoreCase("input") || qName.equalsIgnoreCase("textarea")){
	          		ininput = true;
	          		textviews.append(",,"+ref);
	          		if(req != null && req.equalsIgnoreCase("true"))
	          			requiredtext.append(",,"+ref);
	          		if(title != null && title.equalsIgnoreCase("true"))
	          			listfields.append(",,"+ref);
	          		if(num != null && num.equalsIgnoreCase("true"))
	          			integers.append(",,"+ref);
	          	}
	          	else
	          		ininput = false;
	          }

	            if (qName.equalsIgnoreCase("ITEM")) {
	          	  item = true;
	            }

	            if (qName.equalsIgnoreCase("LABEL")) {
	          	  label = true;
	            }

	            if (qName.equalsIgnoreCase("VALUE")) {
	            	//if(inselect)
	            	//	views.append(",,"+ref);
	          	  value = true;
	            }

	            if (qName.equalsIgnoreCase("SUBMISSION")) {
	          	  //System.out.println("ID: "+attributes.getValue("id"));
	          	  //System.out.println("Project: "+attributes.getValue("projectName"));
	          	  //System.out.println("Edits: "+attributes.getValue("allowDownloadEdits"));
	          	  if(attributes.getValue("projectName") != null)
	          		  project = attributes.getValue("projectName");
	          	  app_name = "EpiCollect "+project;
	          	  if(attributes.getValue("allowDownloadEdits") != null)
	          		  changesynch = attributes.getValue("allowDownloadEdits");
	          	  //submission = true;
	          	  
	            }
	    	 
	            if (qName.equalsIgnoreCase("INPUT")) {
	          	  // Initialise text view here and complete in characters
	          	  //System.out.println("Ref: "+attributes.getValue("ref"));
	          	  //System.out.println("Required: "+attributes.getValue("required"));
	          	  //System.out.println("Numeric: "+attributes.getValue("numeric"));
	          	 // submission = true;
	            }
	            
	          }
	        

	          public void endElement(String uri, String qName, String localName) throws SAXException {

	               // System.out.println("End Element :" + qName);

	          }

	          public void characters(char ch[], int start, int length) throws SAXException {
	          	
	        	  
	        	  if(download){
		    		  remote_xml = new String(ch, start, length);
		    		  //Log.i(getClass().getSimpleName(), "XML REMOTE "+remote_xml);
		    		  download = false;
		    	  }
		    	  if(image){
		    		  image_url = new String(ch, start, length);
		    		  //Log.i(getClass().getSimpleName(), "XML IMAGE "+image_url);
		    		  image = false;
		    	  }
		    	  if(upload){
		    		  synch_url = new String(ch, start, length);
		    		  //Log.i(getClass().getSimpleName(), "XML SYNCH "+synch_url);
		    		  upload = false;
		    	  }
		    	  
	            if (item) {
	              //System.out.println("In Item");
	              item = false;
	            }

	            if (label) {
	            	thislabel = new String(ch, start, length);
	                //System.out.println("Label : "+ new String(ch, start, length));
	                label = false;
	                if(intype){
	              	  views.append(",,"+new String(ch, start, length));
	              	  intype = false;
	                }
	                else if(inselect){
	              	  views.append(",,"+new String(ch, start, length));   
	              	  //checkboxes.append(",,"+ref+"_"+new String(ch, start, length));
	                }
	             }

	            if (value) {
	                //System.out.println("Value : "+ new String(ch, start, length));
	                
	                if(inselect){
	                	checkboxes.append(",,"+ref+"_"+new String(ch, start, length));
	                	views.append(",,"+new String(ch, start, length));    
	                	//views.append(",,"+ref);
	                	if(checkboxrefs.get("checkbox_"+ref) == null){
	                		checkboxrefs.put("checkbox_"+ref, new StringBuffer(ref+"_"+new String(ch, start, length)));
	                	}
	                	else
	                		checkboxrefs.get("checkbox_"+ref).append(",,"+ref+"_"+new String(ch, start, length));
	                	
	                	if(checkboxvalues.get("checkbox_values_"+ref) == null){
	                		checkboxvalues.put("checkbox_values_"+ref, new StringBuffer(new String(ch, start, length)));
	                	}
	                	else
	                		checkboxvalues.get("checkbox_values_"+ref).append(",,"+new String(ch, start, length));
	                	
	                	//checkboxes.append(",,"+ref+"_"+new String(ch, start, length));
	                	if(title != null && title.equalsIgnoreCase("true"))
		          			listcheckboxes.append(",,"+ref+"_"+new String(ch, start, length));
	                }
	                
	                if(inselect1){
	                	if(spinnerrefs.get("spinner_"+ref) == null)
	                		spinnerrefs.put("spinner_"+ref, new StringBuffer("Select"));
	                	
	                	spinnerrefs.get("spinner_"+ref).append(",,"+thislabel);
	                	
	                	if(spinnervalues.get("spinner_values_"+ref) == null)
	                		spinnervalues.put("spinner_values_"+ref, new StringBuffer("Null"));
	                	
	                	spinnervalues.get("spinner_values_"+ref).append(",,"+new String(ch, start, length));
	                	
	                }
	                
	                value = false;
	             }

	            //if (submission) {
	            //    System.out.println("Submission : "
	            //        + new String(ch, start, length));
	            //    submission = false;
	            // }

	           // System.out.println("FINAL : " + new String(ch, start, length));
	          }

	          
	        }


	//private int getLocID(String loc){

//public static void main(String[] args) {
//	new ParseXML();
//}
}