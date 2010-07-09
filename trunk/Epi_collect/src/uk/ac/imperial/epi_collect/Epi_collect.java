package uk.ac.imperial.epi_collect;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import uk.ac.imperial.epi_collect.maps.LocalMap;
import uk.ac.imperial.epi_collect.util.db.DBAccess;
import uk.ac.imperial.epi_collect.util.xml.ParseXML;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageView;

public class Epi_collect extends Activity {
	
	private static final int ACTIVITY_NEW = 0;
    private static final int ACTIVITY_LIST = 1;
    private static final int ACTIVITY_MAP = 2;
    private static final int LOAD_PROJECT = 1;
    private static final int DELETE_PROJECT = 2;
    private ImageView iview;
    private DBAccess dbAccess;
    private HashMap<String, Integer> restore_remote = new HashMap<String, Integer>(), restore_local = new HashMap<String, Integer>();  
    Spinner proj_spin;
    private ProgressDialog myProgressDialog = null; 
    private Handler mHandler; 
    private ParseXML parseXML;
    private ButtonListener myOnClickListener = new ButtonListener();
    private String selected_project = ""; 
    private TextView urlview1, urlview2;
    
    /** Called when the activity is first created. 
     * @throws FileNotFoundException */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        super.setTitle("EpiCollect"); 
        setContentView(R.layout.main);
        
        try{
        	File f = new File(Environment.getExternalStorageDirectory()+"/EpiCollect");
        	if(!f.exists())
        		f.mkdir();
        	}
        catch(Exception e){
        	showAlert("Error", "SD card not present. Required for photo capture");
        }
        
        dbAccess = new DBAccess(this);
	    dbAccess.open();
        
	    mHandler = new Handler();

        Button newButton = (Button) findViewById(R.id.newbut);
        Button listButton = (Button) findViewById(R.id.listbut);
        Button mapButton = (Button) findViewById(R.id.mapbut);
        Button helpButton = (Button) findViewById(R.id.helpbut);
        iview = (ImageView) findViewById(R.id.image);
        urlview1 = (TextView) findViewById(R.id.urltext1);
        urlview2 = (TextView) findViewById(R.id.urltext2);
        
        proj_spin = (Spinner) findViewById(R.id.proj_spin);
        
        if(dbAccess.getFirstrun() == 0){
        	//Log.i(getClass().getSimpleName(), "FIRST RUN = " + dbAccess.getFirstrun());
	    	//String demo = "<xform> <model> <submission id=\"ahBlcGljb2xsZWN0c2VydmVycg8LEgdQcm9qZWN0GKbgFAw\" projectName=\"demoproject\" allowDownloadEdits=\"false\" versionNumber=\"1.1\"/> </model><input ref=\"name\" required=\"true\" title=\"true\"> <label>What is your name?</label> </input><select1 ref=\"age\" required=\"true\" chart=\"bar\"> <label>What is your age?</label> <item><label>below10</label><value>below10</value></item> <item><label>11to20</label><value>11to20</value></item> <item><label>21to30</label><value>21to30</value></item> <item><label>31to40</label><value>31to40</value></item> <item><label>41to50</label><value>41to50</value></item> <item><label>51to60</label><value>51to60</value></item> <item><label>61to70</label><value>61to70</value></item> <item><label>above70</label><value>above70</value></item> </select1><select1 ref=\"sex\" required=\"true\"> <label>Male or Female?</label> <item><label>Male</label><value>Male</value></item> <item><label>Female</label><value>Female</value></item> </select1><select1 ref=\"searchengine\" required=\"true\" chart=\"bar\"> <label>Which search engine do you most often use?</label> <item><label>Google</label><value>Google</value></item> <item><label>Yahoo</label><value>Yahoo</value></item> <item><label>Baidu</label><value>Baidu</value></item> <item><label>Bing</label><value>Bing</value></item> <item><label>Ask</label><value>Ask</value></item> <item><label>AOL</label><value>AOL</value></item> <item><label>AltaVista</label><value>AltaVista</value></item> <item><label>other</label><value>other</value></item> </select1><select ref=\"socialnetworks\" required=\"true\" chart=\"pie\"> <label>Which social networking sites do you use?</label> <item><label>MySpace</label><value>MySpace</value></item> <item><label>Facebook</label><value>Facebook</value></item> <item><label>Hi5</label><value>Hi5</value></item> <item><label>Friendster</label><value>Friendster</value></item> <item><label>Orkut</label><value>Orkut</value></item> <item><label>Bebo</label><value>Bebo</value></item> <item><label>Tagged</label><value>Tagged</value></item> <item><label>Xing</label><value>Xing</value></item> <item><label>Badoo</label><value>Badoo</value></item> <item><label>Xanga</label><value>Xanga</value></item> <item><label>51.com</label><value>51com</value></item> <item><label>Xiaonei</label><value>Xiaonei</value></item> <item><label>ChinaRen</label><value>ChinaRen</value></item> </select></xform>";
	    	parseXML = new ParseXML();
	    	parseXML.getDemoXML();
	    	buildProject();
	    	dbAccess.setFirstrun();
	    	parseXML = null;
	    }
        
        String[] allprojects = dbAccess.getProjects();
    	
        ArrayList<String> temparray = new ArrayList<String>();
        for (int i = 0; i < allprojects.length; i++) {
        	temparray.add(allprojects[i]);
	    	}

        ArrayAdapter<String> aspnLocs = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, temparray);
    	aspnLocs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	proj_spin.setAdapter(aspnLocs); 
           
    	if(allprojects.length > 1){
    	try{
         	proj_spin.setSelection(1);
         }
    	 catch(IndexOutOfBoundsException e){}
    	}
    	Bitmap bitMap = BitmapFactory.decodeResource(this.getResources(), R.drawable.epi_icon);
		iview.setImageBitmap(bitMap); 
    	
		proj_spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
		    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		    	String project = proj_spin.getSelectedItem().toString();
		    	if(project == null || project.equalsIgnoreCase("")){
		    		urlview1.setText("");
		    		urlview2.setText("");
		    	}
		    	else{
		    		urlview1.setText("Project URL is:");
		    		urlview2.setText("http://www.epicollect.net/project.html?name="+project);
		    	}
		        //Object item = parent.getItemAtPosition(pos);
		    }
		    public void onNothingSelected(AdapterView<?> parent) {
		    }
		});
		
        newButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
            	createEntry();
            }
           
        });
        
        listButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
            	listRecords();
            }
           
        });
        
        mapButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
            	showMap();
            }
           
        });
        
        helpButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
            	showHelp();
            }
           
        });
        
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);
	    menu.add(0, LOAD_PROJECT, 0, R.string.load_project);
	    menu.add(0, DELETE_PROJECT, 0, R.string.delete_project);
	    return true;
	}
    
    @Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
	    super.onMenuItemSelected(featureId, item);
	    switch(item.getItemId()) {
	    case LOAD_PROJECT:
	    	loadProject();
	        break;
		case DELETE_PROJECT:
	    	try{
	    		selected_project = proj_spin.getSelectedItem().toString();
	    	}
	    	catch(NullPointerException npe){
	    		showAlert("Error", "Project Required");
	    		return false;
	    	}
	    	
	    	if(selected_project.equalsIgnoreCase("")){
	    		showAlert("Error", "Project Required");
	    		return false;
	    	}
			AlertDialog dialog = new AlertDialog.Builder(this).create();

			dialog.setMessage("Delete Project "+selected_project+"? All data and photos will be lost!");
			dialog.setButton("Yes", myOnClickListener);
			dialog.setButton2("No", myOnClickListener);

			dialog.show();

			break;
    	}
	    return true;
	}
        
    public void getProject(String newproject){
    	
    	String proj_url = this.getResources().getString(this.getResources().getIdentifier(this.getClass().getPackage().getName()+":string/project_url", null, null));
           	
    	if(newproject == null || newproject.equalsIgnoreCase("")){
    		showAlert("Error", "Project name required");
    		return;
    	}
    	
    	if(newproject.startsWith("http:"))
    		parseXML = new ParseXML(newproject); //"http://www.doc.ic.ac.uk/~dmh1/Android/CNTD"); // 
    	else
    		parseXML = new ParseXML(proj_url+newproject);
	    
    	//Log.i(getClass().getSimpleName(), "GET PROJECT "+proj_url+newproject); 
    	
    	getProjectForm();
    	
    }
    
    public void createEntry() {
    	
    	if(checkProject()){
    	   	Intent i = new Intent(this, NewEntry.class);
    	   	startActivityForResult(i, ACTIVITY_NEW);
    	}
    }
    
    public void listRecords() {
    	if(checkProject()){
    		Intent i = new Intent(this, ListRecords.class);
    		startActivityForResult(i, ACTIVITY_LIST);
    	}
    }
    
    public void showMap() {
    	if(checkProject()){
    		Intent i = new Intent(this, LocalMap.class);
    		i.putExtra("overlay_local", restore_local);
    		i.putExtra("overlay_remote", restore_remote);
    		startActivityForResult(i, ACTIVITY_MAP);
    	}
    }
    
    public void showHelp() {
    	Intent i = new Intent(this, Help.class);
    	i.putExtra("HELP_TEXT", 1); //this.getString(R.string.help_text));
    	startActivity(i);
    }
   	
    private boolean checkProject(){
    	String project;
    	try{
    		project = proj_spin.getSelectedItem().toString();
    	}
    	catch(NullPointerException npe){
    		showAlert("Error", "Project Required");
    		return false;
    	}
    	
    	if(project.equalsIgnoreCase("")){
    		showAlert("Error", "Project Required");
    		return false;
    	}
    	
    	dbAccess.setActiveProject(project);
    	return true;
    }
    
    @SuppressWarnings("unchecked")
	@Override
	 protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	   	super.onActivityResult(requestCode, resultCode, data);
	        
	   	Bundle extras = null;
	   	if(data != null)
	   		extras = data.getExtras();
	    switch(requestCode) {
	    case ACTIVITY_MAP:
	    	if(extras != null){
	    		restore_local = (HashMap<String, Integer>) extras.get("overlay_local");
	    		restore_remote = (HashMap<String, Integer>) extras.get("overlay_remote"); 
	    	} 
	        break; 
	    } 
	} 
    
    public void showAlert(String title, String result){
    	new AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(result)
        .setNegativeButton("OK", new DialogInterface.OnClickListener() {

             public void onClick(DialogInterface dialog, int whichButton) {

             }
        }).show();	
    }
    
    Builder ad;
    boolean result = false;
    private void getProjectForm(){
    	
    	myProgressDialog = ProgressDialog.show(this,     
                "Please wait...", "Loading Project...", true);

      new Thread() {
           public void run() {
        	   result = false;
                try{
                	result = parseXML.getXML();
                	
                } catch (Exception e) {
                	Log.i(getClass().getSimpleName(), "ERROR: "+ e);
                }
                // Dismiss the Dialog
                myProgressDialog.dismiss();
                Looper.prepare();
                if(result)
                	showAlert("Success", "Project Loaded");
                else
                	showAlert("Error", "Project retrieval failed. Is project name correct?");
                	
                /*You can use threads but all the views, and all the views related APIs,
                must be invoked from the main thread (also called UI thread.) To do
                this from a background thread, you need to use a Handler. A Handler is
                an object that will post messages back to the UI thread for you. You
                can also use the various post() method from the View class as they
                will use a Handler automatically. */
                if(result){
                	mHandler.post(new Runnable() {
	                	public void run() { 
	                    	buildProject(); }
                	});
                }
                Looper.loop();
                Looper.myLooper().quit(); 
                
                 
           }
      }.start();

    }
    
    public void buildProject(){
    	String project = parseXML.getProject();
  	    dbAccess.setActiveProject(project);
  	  Log.i(getClass().getSimpleName(), "ACTIVE PROJECT: "+ project);	    
  	    dbAccess.dropTable("data_"+project);
  	    dbAccess.dropTable(project);
  	    
  	    StringBuffer table = parseXML.createTable();
  	    dbAccess.createProjectTable(table, project); 
  	    
  	    Hashtable<String, String> rowhash = parseXML.createRow();
  	    dbAccess.createProjectRow(rowhash, project); 
  	    
  	    dbAccess.createDataTable(project); 
  	    
	  	 String[] allprojects = dbAccess.getProjects();
	  	
	     ArrayList<String> temparray = new ArrayList<String>();
	     for (int i = 0; i < allprojects.length; i++) {
	    	 temparray.add(allprojects[i]);
		 }
	
	     ArrayAdapter<String> aspnLocs = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, temparray);
	  	 aspnLocs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	  	 proj_spin.setAdapter(aspnLocs); 
	  	 proj_spin.setSelection(1);
    }
    
    private void loadProject(){
    	
    	LayoutInflater factory = LayoutInflater.from(this);
    	final View filterView = factory.inflate(R.layout.main_project_load, null); 
    	
    	final TextView proj_text = (TextView) filterView.findViewById(R.id.proj_text);
        proj_text.setText("");
        
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);  
        	
    	alert.setTitle("Load Project");  
    	   alert.setView(filterView);    	   
 	                 
    	  alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {  
    	  public void onClick(DialogInterface dialog, int whichButton) {
    		  String project = proj_text.getText().toString();
    		  getProject(project);
    		  
    	  }
    	  });  
    	    
    	  alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {  
    	    public void onClick(DialogInterface dialog, int whichButton) {  
    	    }  
    	  });  
    	     
    	  alert.show();  
    	
    }
    
    private void deleteProject(){
    	
    	dbAccess.deleteProject(selected_project);
    	
    	String thumbdir = Environment.getExternalStorageDirectory()+"/EpiCollect/thumbs_epicollect_" + selected_project; 
        String picdir = Environment.getExternalStorageDirectory()+"/EpiCollect/picdir_epicollect_" + selected_project; 
        
        File dir;
        try{
        	dir = new File(thumbdir);
        	deleteDirectory(dir);
        	}
        catch(Exception e){}
        
        try{
        	dir = new File(picdir);
        	deleteDirectory(dir);
        	}
        catch(Exception e){}

	   	String[] allprojects = dbAccess.getProjects();
	  	
	      ArrayList<String> temparray = new ArrayList<String>();
	      for (int i = 0; i < allprojects.length; i++) {
	    	  temparray.add(allprojects[i]);
		  }

	      ArrayAdapter<String> aspnLocs = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, temparray);
	  		aspnLocs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	  		proj_spin.setAdapter(aspnLocs); 
	  		if(allprojects.length > 1){
	  			try{
	  				proj_spin.setSelection(1);
	  			}
	  			catch(IndexOutOfBoundsException e){}
	  	    }
    }
    
    private void deleteDirectory(File path){
    	try{
    		if( path.exists() ) {
    			File[] files = path.listFiles();
    			for(int i=0; i<files.length; i++) {
    				if(files[i].isDirectory()) {
    					deleteDirectory(files[i]);
    				}
    				else {
    					files[i].delete();
    				}
    			}
    			path.delete();
    	    }
    	}
    	catch(Exception e){}

    }
    
    class ButtonListener implements OnClickListener{
		public void onClick(DialogInterface dialog, int i) {
			switch (i) {
			case AlertDialog.BUTTON1:
				deleteProject();
			break;
			case AlertDialog.BUTTON2:
			/* Button2 is clicked. Do something */
			break;
			}
		}
      }
}