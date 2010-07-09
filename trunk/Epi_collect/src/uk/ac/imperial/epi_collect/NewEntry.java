package uk.ac.imperial.epi_collect;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

import uk.ac.imperial.epi_collect.camera.ImageSwitcher_epi_collect;
import uk.ac.imperial.epi_collect.maps.LocalMap;
import uk.ac.imperial.epi_collect.util.db.DBAccess;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.location.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class NewEntry extends Activity implements LocationListener {

	private static final int ACTIVITY_EDIT=1;
	private static final int ACTIVITY_LIST=2;
	private static final int ACTIVITY_MAP=3;
	private static final int ACTIVITY_PHOTO=4;
	//private static final String KEY_SOURCE = "source";
	private static final String KEY_ID = "id";
	private static final String KEY_DATE = "ecdate";
    private static final String KEY_REMOTEID = "remoteid";
    private static final String KEY_PHOTO = "photo";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String KEY_ALT = "alt";
    private static final String KEY_ACC = "gpsacc";
    private static final String KEY_STORED = "stored";
    private static final String KEY_ISSTORED = "isstored";
	private static final int LIST_ID = 1; //Menu.FIRST;
	private static final int NEW_ID = 2; //Menu.FIRST + 1;
	private static final int MAP_ID = 3; //Menu.FIRST + 2;
	private static final int CAP_PHOTO_ID = 4; //Menu.FIRST + 3;
	private static final int DELETE_ID = 5;
	private static final int CHANGE_GPS = 6;
	private static final int HOME = 7;
	private EditText idText;
	private EditText latText;
	//private EditText lonText;
	//private EditText altText;
	private EditText dateText;
	private String ecdate = "";
	private long id = 1;
	private String remoteid = "0";
	private String lat = "0", lon = "0", alt = "0", gpsacc = "";
	private DBAccess dbAccess;
	private Button storeButton,	 photoButton, notesButton, gpsButton, idButton; 
	private ImageView iview;
	//private boolean delete = false; 
	private LocationManager locationManager; 
   	private LocationProvider IP;
   	private String photoid = "-1";
   	private String stored = "N";
   	private int isstored = 0;
   	private ButtonListener myOnClickListener = new ButtonListener(), myOnClickListener2 = new ButtonListener();
	private static String[] textviews = new String[0];
    private static String[] spinners = new String[0];
    private static String[] checkboxes = new String[0];
	private Hashtable <String, Integer>spinnerselhash = new Hashtable <String, Integer>();
	private Hashtable <String, String>stringshash = new Hashtable <String, String>();
	private Hashtable <String, Boolean>checkboxhash = new Hashtable <String, Boolean>();
	private String thumbdir; 
	private static Vector<String> requiredfields, requiredspinners;//, requiredradios;
	private boolean todelete = false, changesynch = false;
	private HashMap<String, Integer> restore_remote = new HashMap<String, Integer>(), restore_local = new HashMap<String, Integer>();  
   	   	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
	    super.onCreate(icicle);
	       
	    setContentView(R.layout.new_entry);
        
	    dbAccess = new DBAccess(this);
	    dbAccess.open();
	    
	    super.setTitle("EpiCollect "+dbAccess.getProject());
	    
	    thumbdir = Environment.getExternalStorageDirectory()+"/EpiCollect/thumbs_epicollect_" + dbAccess.getProject(); // + this.getResources().getString(this.getResources().getIdentifier(this.getPackageName()+":string/project", null, null));
      	    
	    getValues();
	    
	    if(dbAccess.getValue("change_synch").equalsIgnoreCase("true"))
	    	changesynch = true;
        	        
	    idText = (EditText) findViewById(R.id.id);
	    idText.setFocusable(false); 
	    dateText = (EditText) findViewById(R.id.date);
	    dateText.setFocusable(false);
	    
	    latText = (EditText) findViewById(R.id.lat);
	    latText.setFocusable(false);
	    //lonText = (EditText) findViewById(R.id.lon);
	    //lonText.setFocusable(false);
	    //altText = (EditText) findViewById(R.id.alt);
	    //ltText.setFocusable(false);
	     
	    gpsButton = (Button) findViewById(R.id.gpsbut);
	    idButton = (Button) findViewById(R.id.idbut);
	    photoButton = (Button) findViewById(R.id.photo);
	    notesButton = (Button) findViewById(R.id.notes);
	    storeButton = (Button) findViewById(R.id.store);
	       
	    iview = (ImageView) findViewById(R.id.image);
   
	    locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE); 
       	IP = locationManager.getProvider("gps");
       	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

	    populateFields(icicle);
	      
	    gpsButton.setOnClickListener(new View.OnClickListener() {
	    	public void onClick(View arg0) {
	          	setGPS(0);
	        }
	           
	    });
	        
	    idButton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View arg0) {
	        	if(isstored == 0){
	    			showToast("Data has not been saved. Pressing ID again will reset current data");
	    			isstored = 1;
	    		}
	        	else{
	        		newID();
	        	}
	        	//delete = false;
	        }   
	    });
	    
	    photoButton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View arg0) {
	        	addPhoto("0");
	        }	           
	    });
	    
	    notesButton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View arg0) {
	        	createNote();
	        }	           
	    });
	        
	    storeButton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View arg0) {
	        	if(!changesynch && !remoteid.equalsIgnoreCase("0")){
	        		showAlert("Entry is synchronised and cannot be changed");
	        		isstored = 1;
	        		storeButton.setTextColor(Color.WHITE);
	        		storeButton.setText("4. Entry stored and synchronised");
	        		storeButton.setEnabled(false);
	        	}
	        	else if(!remoteid.equalsIgnoreCase("0")){
	        		confirmStore();
	        	}
	        	else{
	        		storeData();
	        		isstored = 1;
	        	}
	        }	           
	    });	    
	    
	}
	    
	private void confirmStore(){
		todelete = false;
		AlertDialog dialog = new AlertDialog.Builder(this).create();

		dialog.setMessage("This is a synchronised record. Modify?");
		dialog.setButton("Yes", myOnClickListener2);
		dialog.setButton2("No", myOnClickListener2);

		dialog.show();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);
	    menu.add(0, LIST_ID, 0, R.string.menu_list);
	    menu.add(0, NEW_ID, 0, R.string.menu_new_entry);
	    menu.add(0, MAP_ID, 0, R.string.menu_map);
	    //menu.add(0, CAP_PHOTO_ID, 0, R.string.menu_photo);
	    menu.add(0, DELETE_ID, 0, R.string.menu_delete);
	    menu.add(0, CHANGE_GPS, 0, R.string.menu_change_gps);
	    menu.add(0, HOME, 0, R.string.menu_home);
	    return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
	    super.onMenuItemSelected(featureId, item);
	    switch(item.getItemId()) {
	    case LIST_ID:
	    	listRecords();
	        break;
	    case NEW_ID:
	    	newID();
           	latText.setText("GPS Not Set");
           	gpsButton.setTextColor(Color.BLACK);
           	//lonText.setText("Lon:");
	    	break;
	    case MAP_ID:
	    	showMap();
	        break;
		case DELETE_ID:
			todelete = true;
			AlertDialog dialog = new AlertDialog.Builder(this).create();

			dialog.setMessage("Delete Record?");
			dialog.setButton("Yes", myOnClickListener);
			dialog.setButton2("No", myOnClickListener);

			dialog.show();

			break;
		case CHANGE_GPS:
			if(!changesynch && !remoteid.equalsIgnoreCase("0")){}
			else{
				gpsButton.setEnabled(true);
				gpsButton.setTextColor(Color.BLACK);
				showToast("This enables a stored GPS location to be changed");
			}
			break;
		case CAP_PHOTO_ID:
			addPhoto("1");
			break;	
		case HOME:
			confirmHome2();
			break;	
    	}
	        
	    return true;
	}
	
	public void listRecords() {
    	Intent i = new Intent(this, ListRecords.class);
    	startActivityForResult(i, ACTIVITY_LIST);
    }
	    	
	private void addPhoto(String type) {
    	Intent i = new Intent(this, ImageSwitcher_epi_collect.class);
    	if(id == 0){
    		id = dbAccess.getNewID();
    		idText.setText(""+id);
    	}
    	i = addExtras(i);
    	i.putExtra(KEY_ISSTORED, 0);
    	i.putExtra("GALLERY", type);
    	i.putExtra("KEY_DATE", ecdate);
    	startActivityForResult(i, ACTIVITY_PHOTO);
    	
    }
	
	private void createNote() {
    	Intent i = new Intent(this, EntryNote.class);
    	if(id == 0){
    		id = dbAccess.getNewID();
    		idText.setText(""+id);
    	}
    	i = addExtras(i);
    	//i.putExtra(KEY_ISSTOWHITE, 0);
    	i.putExtra(KEY_ISSTORED, isstored);
    	if(!changesynch && !remoteid.equalsIgnoreCase("0"))
    		i.putExtra("canupdate", 0);
    	else
    		i.putExtra("canupdate", 1);
    	startActivityForResult(i, ACTIVITY_EDIT);
    	
    }
	
	public void showMap() {
    	Intent i = new Intent(this, LocalMap.class);
    	i = addExtras(i);
    	i.putExtra("overlay_local", restore_local);
    	i.putExtra("overlay_remote", restore_remote);
    	startActivityForResult(i, ACTIVITY_MAP);
    }
	
	private Intent addExtras(Intent i){
		i.putExtra(KEY_ID, id);
		i.putExtra(KEY_REMOTEID, remoteid);
    	i.putExtra(KEY_DATE, ecdate);
    	i.putExtra(KEY_LAT, lat);
    	i.putExtra(KEY_LON, lon);
    	i.putExtra(KEY_ALT, alt);
    	i.putExtra(KEY_ACC, gpsacc);
    	
    	if(textviews != null){ 
    		for(String key : textviews){

    			if(stringshash.get(key) != null){
   					i.putExtra(key, stringshash.get(key));
    			}
    			else{     
   					i.putExtra(key, "");
    			}
    		}
    	}
    	
        for(String key : spinners){
        	i.putExtra(key, spinnerselhash.get(key));
        }
        
        for(String key : checkboxes){
        	i.putExtra(key, checkboxhash.get(key));
        }

    	i.putExtra(KEY_PHOTO, photoid);
    	i.putExtra(KEY_ISSTORED, isstored);
    	
    	return i;
	}
	
	public String setGPS(int type){
		long oldtime, newtime;
		
		Location gpslocation = locationManager.getLastKnownLocation(IP.getName());
				
		gpslocation = locationManager.getLastKnownLocation(IP.getName());
		       	
		try{
			oldtime = gpslocation.getTime();
		}
		catch(NullPointerException npe){
			if(type == 0){
				showAlert("GPS position fix not obtained or GPS unavailable. Wait for position fix and try again and ensure GPS is enabled");
				return "";
			}
			else
				return "GPS position fix not obtained or GPS unavailable. Wait for position fix and try again and ensure GPS is enabled";
		}

		try { 
            Thread.sleep ( 1000 ); 
        }  
        catch ( InterruptedException e ) { 
        } 
        
        gpslocation = locationManager.getLastKnownLocation(IP.getName()); 
        newtime = gpslocation.getTime();
        
        //if(oldtime == newtime){
        //	if(type == 0)
        //		showAlert("GPS not updated!");
        //	else
		//		return "GPS not updated!";
       	//}

        if(gpslocation.getLatitude() == 0 && gpslocation.getLongitude() == 0)
        	return "Initial GPS location not obtained yet";
        
        if(oldtime == newtime){
			if(type == 0){
				showAlert("GPS position fix not obtained. Wait for position fix and try again");
				return "";
			}
			else
				return "GPS position fix not obtained. Wait for position fix and try again";
       	}

        	
       	lat = Double.toString(gpslocation.getLatitude()); 
       	lon = Double.toString(gpslocation.getLongitude());
       	alt = Double.toString(gpslocation.getAltitude()); 
       	gpsacc = Double.toString(gpslocation.getAccuracy());
       	latText.setText("GPS Set - Accuracy "+gpsacc+"m");
       	gpsButton.setTextColor(Color.BLUE);
       	gpsButton.setText("1. GPS assigned - tap again to update");
       	//lonText.setText("Lon: "+lon);
       	//if(gpslocation.hasAltitude()) 
       	//	altText.setText("Alt: "+alt);
       	//else
       	//	altText.setText("Alt: Not Available");
       		
       	isstored = 0;
       	setStoreButton(isstored);

		//delete = false;
				
		/*if(oldtime == newtime){
			if(type == 0)
				showAlert("GPS position fix not obtained and last recorded position used. Wait for position fix and try again to update");
			else
				return "GPS position fix not obtained and last recorded position used. Wait for position fix and try again to update";
       	}
		else{
			gpsButton.setText("1. GPS assigned");
			gpsButton.setEnabled(false);
			gpsButton.setTextColor(Color.WHITE);
		}*/
		return "";
		//return true;

	}
	
	// Prevents warnings about casting from an object to a hashtable
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
	    		updateData(extras);
	    		restore_local = (HashMap<String, Integer>) extras.get("overlay_local");
	    		restore_remote = (HashMap<String, Integer>) extras.get("overlay_remote"); 
	    	} 
	        break; 
	    case ACTIVITY_EDIT:
	    	if(extras != null){
	    		updateData(extras);	
	    	}
	        break;
	    case ACTIVITY_PHOTO:
	    	if(extras != null){
	    		updateData(extras);   		
	    	}
	        break;
	    } 
	} 
	
	private void updateData(Bundle extras){
		boolean notes = true;
		id = extras.getLong(KEY_ID);
		remoteid = extras.getString(KEY_REMOTEID);
		idText.setText(""+extras.getLong(KEY_ID));
		ecdate = extras.getString(KEY_DATE);
		
		dateText.setText(getDate(Long.parseLong(ecdate)));

		isstored = extras.getInt(KEY_ISSTORED);
		
		//Log.i(getClass().getSimpleName(), "IS STORED "+isstored);
		
		setStoreButton(isstored); 
		//delete = false;
		
		lat = extras.getString(KEY_LAT);
		gpsacc = extras.getString(KEY_ACC);
		if(Double.parseDouble(lat) > 0){
			latText.setText("GPS Set - Accuracy "+gpsacc+"m");
			gpsButton.setTextColor(Color.WHITE);
			gpsButton.setEnabled(false);
			if(!changesynch && !remoteid.equalsIgnoreCase("0")){
				gpsButton.setText("1. GPS assigned - entry synchronised");
			}
			else{
				if(isstored == 1)
					gpsButton.setText("1. GPS assigned - entry stored. Tap again to update");
				else
					gpsButton.setText("1. GPS assigned - tap again to update");
				gpsButton.setEnabled(true);
				gpsButton.setTextColor(Color.BLUE);
				
			}
		}
		else{
			gpsButton.setEnabled(true);
			latText.setText("GPS Not Set");
			gpsButton.setTextColor(Color.BLACK);
			gpsButton.setText("1. Tap to assign GPS");
		}
		lon = extras.getString(KEY_LON);
		//lonText.setText("Lon: "+extras.getString(KEY_LON));
		//if(extras.getString(KEY_ALT).equalsIgnoreCase("0.0"))
		//	altText.setText("Not Available");
		//else
		//	altText.setText("Alt: "+extras.getString(KEY_ALT));
		
		//isstored = 0;

		photoid = extras.getString(KEY_PHOTO);
		if(photoid != null && !photoid.endsWith("-1")){
			try{
				iview.setImageURI(Uri.parse(thumbdir+"/"+photoid));
			}
			catch(Exception e){
				showAlert("Image not available");
			}
			photoButton.setTextColor(Color.WHITE);
			if(!changesynch && !remoteid.equalsIgnoreCase("0")){
				photoButton.setText("2. Photo added - entry synchronised");
				photoButton.setEnabled(false);
			}
			else{
				photoButton.setText("2. Photo added - tap to change");
				photoButton.setTextColor(Color.BLUE);
				photoButton.setEnabled(true);
			}
			
		}
		else{
			if(!changesynch && !remoteid.equalsIgnoreCase("0")){
				photoButton.setTextColor(Color.WHITE);
				photoButton.setText("2. No photo added - entry synchronised");
				photoButton.setEnabled(false);
			}
			else{
				photoButton.setEnabled(true);
				photoButton.setTextColor(Color.BLACK);
				photoButton.setText("2. Tap to add photo");
				iview.setImageBitmap(null);
			}
    	}
				
		if(textviews != null){
			for(String key : textviews){
				stringshash.put(key, extras.getString(key));
				if((extras.getString(key) == null || extras.getString(key).equalsIgnoreCase("")) && requiredfields.contains(key)){
					notes = false;
				}
			}
		}
        
        if(spinners != null){
        	for(String key : spinners){
        		spinnerselhash.put(key, extras.getInt(key));
        		if(extras.getInt(key) == 0 && requiredspinners.contains(key)){
        			notes = false;
        		}
        	}
        }
        
        if(checkboxes != null){
        	for(String key : checkboxes){
        		//Log.i(getClass().getSimpleName(), "CHECKBOX: "+key+" "+extras.getBoolean(key));

        		checkboxhash.put(key, extras.getBoolean(key));
        	}
        }

        if(notes){
        	notesButton.setTextColor(Color.BLUE);
        	if(!changesynch && !remoteid.equalsIgnoreCase("0"))
        		notesButton.setText("3. Form data entered - tap to view");
			else
				notesButton.setText("3. Form data entered - tap to view or edit");   	
        }
        else{
        	notesButton.setTextColor(Color.BLACK);
        	notesButton.setText("3. Tap to enter form data");
        }
	}
	
	private void populateFields(Bundle extras) {
		if(extras == null)
			extras = getIntent().getExtras();
		
		// If there is no KEY_ID this must be the first time the screen is loaded
		// so only the email is present
	    if (extras != null && extras.containsKey(KEY_ID)) {
	    	
	    	boolean notes = true;

	    	if(stored.equalsIgnoreCase("R")){ //  extras.getString(KEY_SOURCE) != null && extras.getString(KEY_SOURCE).equalsIgnoreCase("map_remote")){
	    		//id = dbAccess.getNewID();
	    		gpsButton.setEnabled(false);
	    		//idText.setText("");
	    	}

	    	//else{
	    		id = extras.getLong(KEY_ID);
	    		idText.setText(""+id);
	    	//}
	    	 
	    	isstored = extras.getInt(KEY_ISSTORED);
	    	stored = extras.getString(KEY_STORED);
	    	
	    	remoteid = extras.getString(KEY_REMOTEID);
	    	//Log.i(getClass().getSimpleName(), "STORED: "+stored);
	    		
	    	//if(isstored == 1)
	    	//	gpsButton.setEnabled(false);
	    	
	    	if(stored.equalsIgnoreCase("R")) // || (extras.getString(KEY_SOURCE) != null && extras.getString(KEY_SOURCE).equalsIgnoreCase("map_remote")))
		    	setStoreButton(2);
		    else
		    	setStoreButton(isstored);
	    	
	    	ecdate = extras.getString(KEY_DATE);
	    	
	    	dateText.setText(getDate(Long.parseLong(ecdate)));
	       	
	    	lat = extras.getString(KEY_LAT);
	    	lon = extras.getString(KEY_LON);
	    	alt = extras.getString(KEY_ALT);
	    	gpsacc = extras.getString(KEY_ACC);
	    	
	    	if(stored.equalsIgnoreCase("R")){ //  extras.getString(KEY_SOURCE) != null && extras.getString(KEY_SOURCE).equalsIgnoreCase("map_remote")){
	    		gpsButton.setEnabled(false);
	    		gpsButton.setTextColor(Color.WHITE);
	    		gpsButton.setText("1. GPS assigned - remote entry");
	    		latText.setText("GPS Set - Accuracy "+gpsacc+"m");
	    	}
	    	else if(Double.parseDouble(lat) > 0){
	    		latText.setText("GPS Set - Accuracy "+gpsacc+"m");
				if(gpsButton.isEnabled())
					gpsButton.setTextColor(Color.BLUE);
				else
					gpsButton.setTextColor(Color.WHITE);
				if(!changesynch && !remoteid.equalsIgnoreCase("0")){
					gpsButton.setText("1. GPS assigned - entry synchronised");
					gpsButton.setEnabled(false);
					gpsButton.setTextColor(Color.WHITE);
				}
				else{
					if(isstored == 1)
						gpsButton.setText("1. GPS assigned - entry stored. Tap again to update");
					else
						gpsButton.setText("1. GPS assigned - tap again to update");
					gpsButton.setEnabled(true);
					gpsButton.setTextColor(Color.BLUE);
					
				}
	    	}
			else{
				if(!changesynch && !remoteid.equalsIgnoreCase("0")){
					gpsButton.setEnabled(false);
					gpsButton.setTextColor(Color.WHITE);
					gpsButton.setText("1. GPS assigned - entry synchronised");
					latText.setText("GPS Not Set");
				}
				else{
					gpsButton.setEnabled(true);
					latText.setText("GPS Not Set");
					gpsButton.setTextColor(Color.BLACK);
					gpsButton.setText("1. Tap to assign GPS");
				}
			}
	    	//lonText.setText("Lon: "+lon);  
	    	//if(alt.equalsIgnoreCase("0.0"))
	    	//	altText.setText("Alt: Not Available");  
	    	//else
	    	//	altText.setText("Alt: "+alt);  
        	
	    	photoid = extras.getString(KEY_PHOTO);
	    	if(stored.equalsIgnoreCase("R")){ //extras.getString(KEY_SOURCE) != null && extras.getString(KEY_SOURCE).equalsIgnoreCase("map_remote")){
	    		photoButton.setTextColor(Color.WHITE);
	    		photoButton.setText("2. No photo. Remote entry");
	    		photoButton.setEnabled(false);
    		}
	    	else if(photoid != null && !photoid.endsWith("-1")){ 
	    		iview.setImageURI(Uri.parse(thumbdir+"/"+photoid));
	    		photoButton.setTextColor(Color.WHITE);
	    		if(!changesynch && !remoteid.equalsIgnoreCase("0")){
					photoButton.setText("2. Photo added - entry synchronised");
					photoButton.setEnabled(false);
				}
				else{
					photoButton.setTextColor(Color.BLUE);
					photoButton.setText("2. Photo added - tap to change");
					photoButton.setEnabled(true);
				}
	    	}
	    	else{
				if(!changesynch && !remoteid.equalsIgnoreCase("0")){
					photoButton.setTextColor(Color.WHITE);
					photoButton.setText("2. No photo added - entry synchronised");
					photoButton.setEnabled(false);
				}
				else{
					photoButton.setEnabled(true);
					photoButton.setTextColor(Color.BLACK);
					photoButton.setText("2. Tap to add photo");
					iview.setImageBitmap(null);
				}
	    	}
	    	
	    	if(textviews != null){
				for(String key : textviews){
					if((extras.getString(key) == null || extras.getString(key).equalsIgnoreCase("")) && requiredfields.contains(key)){
	        			notes = false;
					}
					stringshash.put(key, extras.getString(key));					
				}
			}
	    	
	        if(spinners != null){
	        	for(String key : spinners){
	        		spinnerselhash.put(key, extras.getInt(key));
	        		if(extras.getInt(key) == 0 && requiredspinners.contains(key)){
	        			notes = false;
	        		}
	        	}
	        }
	        
	        if(checkboxes != null){
	        	for(String key : checkboxes){
	        		checkboxhash.put(key, extras.getBoolean(key));
	        	}
	        }
	        
	        if(stored.equalsIgnoreCase("R")){ //extras.getString(KEY_SOURCE) != null && extras.getString(KEY_SOURCE).equalsIgnoreCase("map_remote")){
	        	notesButton.setTextColor(Color.BLUE);
	        	notesButton.setText("3. Form data entered - tap to view");
	        
	        }
	        else if(notes || (!changesynch && !remoteid.equalsIgnoreCase("0"))){
	        	notesButton.setTextColor(Color.BLUE);
	        	if(!changesynch && !remoteid.equalsIgnoreCase("0"))
	        		notesButton.setText("3. Form data entered - tap to view");
				else
					notesButton.setText("3. Form data entered - tap to view or edit"); 
	        }
	        else{
	        	notesButton.setTextColor(Color.BLACK);
	        	notesButton.setText("3. Tap to enter form data");
	        }
	     
	    }
		else{ 
			newID();
	    }	
	    //delete = false;
	}
	
	private void newID(){
		
		isstored = 0;
		remoteid = "0";
		//delete = false;
		gpsButton.setEnabled(true);
		gpsButton.setTextColor(Color.BLACK);
		id = dbAccess.getNewID();
       	idText.setText(""+id);
       	latText.setText("GPS Not Set");
       	gpsButton.setTextColor(Color.BLACK);
       	gpsButton.setText("1. Tap to assign GPS");
       	//lonText.setText("Lon: 0");
   		//altText.setText("Alt:");
   		lat = "0";
   		lon = "0";

       	notesButton.setTextColor(Color.BLACK);
       	notesButton.setText("3. Tap to enter form data");
       	storeButton.setTextColor(Color.BLACK);
       	storeButton.setText("4. Tap to store entry");
       	storeButton.setEnabled(true);
       	photoButton.setTextColor(Color.BLACK);
       	photoButton.setText("2. Tap to add photo");
       	photoButton.setEnabled(true);
   		photoid = "-1";
   		
       	iview.setImageDrawable(null);
       	
       	// YYYY-MM-DD HH:MM:SS
       	Calendar cal = Calendar.getInstance();
       	
       	ecdate = ""+cal.getTimeInMillis();
       	
       	int m = cal.get(Calendar.MONTH) + 1;
       	String month = ""+m;
       	if(m < 10)
       		month = "0"+month;  
       	
       	int d = cal.get(Calendar.DAY_OF_MONTH);
       	String day = ""+d;
       	if(d < 10)
       		day = "0"+day;
       		
       	String datefull = cal.get(Calendar.YEAR) +"-"+ month +"-"+ day +" "+cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND);
       	dateText.setText(datefull);
       	
       	spinnerselhash.clear();
       	stringshash.clear();
       	checkboxhash.clear();

	}
	
	@Override
    protected void onPause() {
        super.onPause();
        dbAccess.close();
        dbAccess = null;
        locationManager.removeUpdates(this);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (dbAccess == null) {
        	dbAccess = new DBAccess(this);
        	dbAccess.open();
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    } 
    
    private void storeData(){ //int full){
    	
    	//if(full == 1){
    	String message = "";
    	//boolean setgps = false;
    	if(lat.equalsIgnoreCase("0")){
    		message = setGPS(1);
   		}
    	
    // Use a hash to get these values from strings.xml
    	Hashtable<String, String> rowhash = new Hashtable<String, String>();
    	rowhash.put("ecrowId", ""+id);
    	rowhash.put("ecremoteId", ""+remoteid);
    	
    	if(textviews != null){
    		for(String key : textviews){
    			if(stringshash.get(key) == null){
    				rowhash.put("'"+key+"'", "");
    			}
    			else
    				rowhash.put("'"+key+"'", stringshash.get(key));
    		}
        }
    	
    	rowhash.put("ecgpslat", lat);
    	rowhash.put("ecgpslon", lon);
    	rowhash.put("ecgpsalt", alt);
    	rowhash.put("ecgpsacc", gpsacc);
    	
    	for(String key : spinners){
    		if(spinnerselhash.get(key) != null)
    			rowhash.put("'"+key+"'", ""+spinnerselhash.get(key));
    		else
    			rowhash.put("'"+key+"'", "0");
    	}
    	
    	for(String key : checkboxes){
    		if(checkboxhash.get(key) != null && checkboxhash.get(key) == true)
    			rowhash.put("'"+key+"'", "1");
    		else
    			rowhash.put("'"+key+"'", "0");
    	}
    	
    	String photo = photoid;
    	//Pattern pattern = Pattern.compile("(\\d+\\.jpg)");
        //Matcher matcher = pattern.matcher(photo);
        //if(matcher.find())
        //{
        //	photo = matcher.group(1);
        //}
  // Log.i(getClass().getSimpleName(), "STORE DATA PHOTOID = " + photoid +" PHOTO = "+photo);        
    	rowhash.put("ecphoto", photo);
    	rowhash.put("ecdate", ecdate);
    	rowhash.put("ecremote", ""+0);
    	rowhash.put("ecstored", "N");
    	
    	dbAccess.createRow(rowhash);
    	//if(full == 1){
    		storeButton.setTextColor(Color.BLUE);
    		storeButton.setText("4. Entry stored");
    		storeButton.setEnabled(true);
    		
    		if(Double.parseDouble(lat) > 0){
    			latText.setText("GPS Set - Accuracy "+gpsacc+"m");
    			gpsButton.setTextColor(Color.BLUE);
    			//gpsButton.setEnabled(false);
    			gpsButton.setText("1. GPS assigned - entry stored. Tap to update");
    		}
    		else{
    			latText.setText("GPS Not Set");
    			gpsButton.setTextColor(Color.BLACK);
    			gpsButton.setText("1. Tap to assign GPS");
    		}
    		//gpsButton.setEnabled(false);
    		//gpsButton.setText("1. GPS assigned - entry stored");
    		//gpsButton.setTextColor(Color.WHITE);
    		
    		//confirmHome();
    		
    		if(message.length() > 0)
    			confirmHome(message+"\n\n");
    		else
    			confirmHome("");
    		
    		//showToast("Record Saved");
    	//}
    }
    
    private void showToast(String text){
    	Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
    
    public void showAlert(String result){
    	new AlertDialog.Builder(this)
        .setTitle("Warning")
        .setMessage(result)
        .setNegativeButton("OK", new DialogInterface.OnClickListener() {

             public void onClick(DialogInterface dialog, int whichButton) {

             }
        }).show();	
    }
    
    
    
    @Override  
    protected void onSaveInstanceState(Bundle outState) {  
    	super.onSaveInstanceState(outState);  

    	outState.putLong(KEY_ID, id);
    	outState.putString(KEY_DATE, ecdate);
    	//outState.putString(KEY_STOWHITE, stored);
    	outState.putString(KEY_LAT, lat);
    	outState.putString(KEY_LON, lon);
    	outState.putString(KEY_ALT, alt);
    	outState.putString(KEY_ACC, gpsacc);
    	outState.putString(KEY_PHOTO, photoid);
    	outState.putInt(KEY_ISSTORED, isstored);
    	
    	if(textviews != null){ 
    		for(String key : textviews){
     			if(stringshash.get(key) != null){
     				outState.putString(key, stringshash.get(key));
    			}
    			else{    
    				outState.putString(key, "");
    			}
    		}
    	}
    	
    	if(spinners != null){
    		for(String key : spinners){
    			if(spinnerselhash.get(key) != null){
    				outState.putInt(key, spinnerselhash.get(key));
    			}
    		}
    	}
    	
    	if(checkboxes != null){
    		for(String key : checkboxes){
    			if(checkboxhash.get(key) != null){
    				outState.putBoolean(key, checkboxhash.get(key));
    			}
    			else{
    				outState.putBoolean(key, false);
    			}
    		}
    	}
    }  
     
    //@Override
    public void onLocationChanged(Location loc) {
    }

    //@Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    //@Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    //@Override
    public void onStatusChanged(String provider, int status, 
        Bundle extras) {
        // TODO Auto-generated method stub
    } 
    
    private void setStoreButton(int set){
    	if(set == 2){ // Remote entry
    		storeButton.setTextColor(Color.WHITE);
    		storeButton.setText("4. Remote entry ");
			storeButton.setEnabled(false);
    	}
    	else if(set == 0){
    		storeButton.setTextColor(Color.BLACK);
    		storeButton.setText("4. Tap to store entry");
    		storeButton.setEnabled(true);
    	}
    	else{
    		storeButton.setTextColor(Color.WHITE);
    		if(!changesynch && !remoteid.equalsIgnoreCase("0")){
    			storeButton.setText("4. Entry stored and synchronised");
    			storeButton.setEnabled(false);
    		}
    		else{
    			storeButton.setTextColor(Color.BLUE);
    		}
    	}
    }

    String getDate(Long time){
    	
    	Calendar cal = Calendar.getInstance();
       	
		cal.setTimeInMillis(time);
       	int m = cal.get(Calendar.MONTH) + 1;
       	String month = ""+m;
       	if(m < 10)
       		month = "0"+month;       		
       		
       	int d = cal.get(Calendar.DAY_OF_MONTH);
       	String day = ""+d;
       	if(d < 10)
       		day = "0"+day;
       	
       	return cal.get(Calendar.YEAR) +"-"+ month +"-"+ day +" "+cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND);
       	
    }
    
    class ButtonListener implements OnClickListener{
		public void onClick(DialogInterface dialog, int i) {
			switch (i) {
			case AlertDialog.BUTTON1:
			/* Button1 is clicked. Do something */
				if(todelete){
					dbAccess.deleteRow((int)id);
					newID();
					showToast("Entry deleted");
				}
				else{
					storeData();
	        		isstored = 1;
				}
			break;
			case AlertDialog.BUTTON2:
			/* Button2 is clicked. Do something */
			break;
			}
		}
      }
    
    private void getValues(){
    	
    	textviews = new String[0];
    	spinners = new String[0];
    	checkboxes = new String[0];

        //checkboxhash.clear();
        
		if(dbAccess.getValue("textviews") != null)
			textviews = (dbAccess.getValue("textviews")).split(",,"); // "CNTD", 
    	if(dbAccess.getValue("spinners") != null)
    		spinners = (dbAccess.getValue("spinners")).split(",,");
    	if(dbAccess.getValue("checkboxes") != null)
    		checkboxes = (dbAccess.getValue("checkboxes")).split(",,");

    	
    	//Log.i("NUMBERS: ", "...");
    	//for(String key : (dbAccess.getValue("doubles")).split(",,")){
        //	doubles.addElement(key);
        	//Log.i("DOUBLE: ", key);
        //}
        
        //for(String key : (dbAccess.getValue("integers")).split(",,")){
        //	integers.addElement(key);
        	//Log.i("INTEGER: ", key);
        //}
                
        List<String> list = Arrays.asList((dbAccess.getValue("requiredtext")).split(",,"));
        requiredfields = new Vector<String>(list);
        
        list = Arrays.asList((dbAccess.getValue("requiredspinners")).split(",,"));
        requiredspinners = new Vector<String>(list);
        
        
        
    }
    
    private void confirmHome(String message){
    	AlertDialog alertDialog = new AlertDialog.Builder(this).create();
    	alertDialog.setTitle("Entry Saved");
    	if(message.length() > 0)
    		alertDialog.setMessage(message +"To synchronise your data with the project website click 'List/Sync entries'");
    	else
    		alertDialog.setMessage("To synchronise your data with the project website click 'List/Sync entries'");
        alertDialog.setButton3("New Entry", new DialogInterface.OnClickListener(){
             public void onClick(DialogInterface dialog, int whichButton) {
            	newID();
             }
        });
        alertDialog.setButton("List/Synch Entries", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton) {
            	listRecords();
            	//goHome();
            }
       });
       // if(message.length() > 0){
        	alertDialog.setButton2("Return to Entry", new DialogInterface.OnClickListener(){
        		public void onClick(DialogInterface dialog, int whichButton) {
        			return;
        		}
        	});
       // }
        alertDialog.show();	
	}
    
    private void confirmHome2(){
    	AlertDialog alertDialog = new AlertDialog.Builder(this).create();
    	alertDialog.setTitle("Confirm");
    	alertDialog.setMessage("Any unsaved data will be lost. Are you sure?");
        alertDialog.setButton("Yes", new DialogInterface.OnClickListener(){
             public void onClick(DialogInterface dialog, int whichButton) {
            	goHome();
             }
        });

        	alertDialog.setButton2("No", new DialogInterface.OnClickListener(){
        		public void onClick(DialogInterface dialog, int whichButton) {
        			return;
        		}
        	});
       // }
        alertDialog.show();	
	}
    
    private void goHome(){
    	Intent i = new Intent(this, Epi_collect.class);
 	   	startActivity(i);
    }
}

