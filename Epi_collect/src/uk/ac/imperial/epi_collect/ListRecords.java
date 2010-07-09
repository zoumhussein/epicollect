package uk.ac.imperial.epi_collect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import uk.ac.imperial.epi_collect.util.db.DBAccess;
import uk.ac.imperial.epi_collect.maps.LocalMap;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener; 
import android.content.res.Configuration;
import android.graphics.Color;

public class ListRecords extends ListActivity {
	private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_EDIT=1;
    private static final int ACTIVITY_MAP=3;
    //private static final String KEY_STORED = "stored";
    private static final String KEY_ISSTORED = "isstored";
    private static final String KEY_STORED = "stored";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String KEY_ALT = "alt";
    private static final String KEY_ACC = "gpsacc";
    private static final String KEY_PHOTO = "photo";
    private static final String KEY_ID = "id";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_REMOTEID = "remoteid";
    private static final String KEY_DATE = "ecdate";
    public static final String KEY_SYNCHRONIZED = "synch";
    private static final int INSERT_ID = 1; //Menu.FIRST;
    private static final int SYNCH_ID = 2; //Menu.FIRST + 2;
    private static final int SYNCH_IMAGES_ID = 3;
    private static final int LOCAL_ID = 4;
    private static final int REMOTE_ID = 5;
    private static final int ALL_ID = 6;
    private static final int MAP_ID = 7; //Menu.FIRST + 3;
    private static final int DELSYNCH_ID = 8; //Menu.FIRST + 3;
    private static final int DELREMOTE_ID = 9;
    private static final int DELALL_ID = 10; //Menu.FIRST + 3;
    private static final int HOME = 11;
    private DBAccess dbAccess;
    private List<DBAccess.Row> rows, localrows;
    private ProgressDialog myProgressDialog = null; 
    private ArrayAdapter<String> notes;
    private Handler mHandler; 
    private ButtonListener myOnClickListener1 = new ButtonListener();
    private ButtonListener2 myOnClickListener2 = new ButtonListener2();
    private ButtonListener3 myOnClickListener3 = new ButtonListener3();
    private String sIMEI, project;
    private static String[] textviews = new String[0];
    private static String[] spinners = new String[0];
    private static String[] checkboxes = new String[0];
    private static Hashtable <String, String[]>spinnershash = new Hashtable <String, String[]>();
    private static Vector<String> listfields, listspinners, listcheckboxes;
    private HashMap<String, Integer> restore_remote = new HashMap<String, Integer>(), restore_local = new HashMap<String, Integer>();  
   	private Button synchButton, helpButton;
   	private boolean allsynched = true;
   	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
    	try{
        super.onCreate(icicle);
              
        setContentView(R.layout.records_list);
        synchButton = (Button) findViewById(R.id.synch);
	    helpButton = (Button) findViewById(R.id.help);
	    
        dbAccess = new DBAccess(this); 
        dbAccess.open();
        
        super.setTitle("EpiCollect "+dbAccess.getProject());
        
        getValues();
        fillData();
        
        setSynchButton();
        	
        mHandler = new Handler();
    	}
    	catch (NullPointerException npe){
    		Log.i(getClass().getSimpleName(), "DB ERROR "+ npe);  
    		//showAlert(npe +"No Entries In Database");
    	}
    	
    	
    	synchButton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View arg0) {
	        	if(rows == null || rows.size() == 0){
	        		showAlert("No Local Entries In Database");
	        		return;
	        	}
	        	synchronizeData(false);
	        	
	        }	           
	    });
	    
	    helpButton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View arg0) {
	        	showHelp();
	        }	           
	    }); 
    }
    
    private void fillData() {
    	
    	allsynched = true;
        // We need a list of strings for the list items
    	List<String> items = new ArrayList<String>();

        // Get all of the rows from the database and create the item list
    	if(dbAccess == null){
    		//Log.i(getClass().getSimpleName(), "DB HERE 1");
    		dbAccess = new DBAccess(this); //, this);
            dbAccess.open();
    	}
        rows = dbAccess.fetchAllRows(0);
        localrows = rows;
        String listitem = "";
        //items.add("ID  Syn  Record");
        for (DBAccess.Row row : rows) {
        	listitem = getItem(row);  
            
            items.add(listitem);
        } 

        notes = new ArrayAdapter<String>(this, R.layout.records_row, items);
        setListAdapter(notes);
        setSynchButton();
    }
    
    public void updateData(int type){
    	
    	allsynched = true;
    	notes.clear();
    	if(type != 2)
        	rows = dbAccess.fetchAllRows(type);
    	else
    		rows = dbAccess.fetchAllRows(0);
        int count = 0;
        for (DBAccess.Row row : rows) {
            notes.insert(getItem(row), count); 
            count++;
        	} 
        
        if(type == 0 || type == 2)
        	localrows = rows; // For synchronization
        
        if(type == 2){
        	rows = dbAccess.fetchAllRows(1);
        	count = 0;
        	for (DBAccess.Row row : rows) {
        		notes.insert(getItem(row), count); 
        		count++;
        		}
        }
        
        notes.notifyDataSetChanged();
        setSynchButton();
    }
    
    private String getItem(DBAccess.Row row){
    	
    	String listitem = String.format("%3d %4s ", row.rowId, row.stored);
    	//String listitem = ""+row.rowId + " | " + row.stored;
    	if(row.stored.equalsIgnoreCase("N"))
    		allsynched = false;
        
        for(String key : textviews){
        	if(listfields.contains(key))
        		listitem += " " + row.datastrings.get(key)+" ";
        }
                   
        for(String key : spinners){
        	if(listspinners.contains(key))
        		listitem += " " + spinnershash.get(key)[row.spinners.get(key)]+" ";
        }
        
        for(String key : checkboxes){
        	if(listcheckboxes.contains(key)){
        		if(row.checkboxes.get(key))
        			listitem += " " + key + " = T ";
        		else
        			listitem += " " + key + " = F ";
        	}
        }

       return listitem;
    }
    
    /**
     * Add stripes to the list view.
     */
 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, INSERT_ID, 0, R.string.menu_new_entry);    
        menu.add(0, LOCAL_ID, 0, R.string.menu_listlocal);
        menu.add(0, REMOTE_ID, 0, R.string.menu_listremote);
        menu.add(0, ALL_ID, 0, R.string.menu_listall);
        menu.add(0, MAP_ID, 0, R.string.menu_map);
        menu.add(0, DELSYNCH_ID, 0, R.string.menu_delsynch);
        menu.add(0, DELREMOTE_ID, 0, R.string.menu_delremote);
        menu.add(0, DELALL_ID, 0, R.string.menu_delall);
        menu.add(0, SYNCH_IMAGES_ID, 0, R.string.menu_photo_synch);
        menu.add(0, SYNCH_ID, 0, R.string.menu_synch);
        menu.add(0, HOME, 0, R.string.menu_home);
        return true;
    }
  
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        super.onMenuItemSelected(featureId, item);
        switch(item.getItemId()) {
        case INSERT_ID:
            createEntry();
            break;
        case SYNCH_ID:  
        	if(rows == null || rows.size() == 0){
        		showAlert("No Local Entries In Database");
        		break;
        	}
        	synchronizeData(false); 
        	//setSynchButton();
            break;
        case LOCAL_ID:  
        	updateData(0);
        	//setSynchButton();
            break;
        case REMOTE_ID:  
        	updateData(1);
        	//setSynchButton();
            break;
        case ALL_ID:  
        	updateData(2);
        	//setSynchButton();
            break;
        case DELREMOTE_ID:  
        	try{
        		AlertDialog dialog = new AlertDialog.Builder(this).create();

        		dialog.setMessage("Delete All Remote Records?");
        		dialog.setButton("Yes", myOnClickListener3);
        		dialog.setButton2("No", myOnClickListener3);

        		dialog.show();
        	}
        	catch (NullPointerException npe){
        		//showAlert("No Remote Entries In Database");
        	}
        	//setSynchButton();
            break;
        case MAP_ID:
	    	showMap();
	        break;
        case DELSYNCH_ID:
        	//if(rows == null || rows.size() == 0){
        	//	showAlert("No Entries In Database");
        	//	break;
        	//}
        	try{
        		AlertDialog dialog = new AlertDialog.Builder(this).create();

        		dialog.setMessage("Delete Synchronised Records?");
        		dialog.setButton("Yes", myOnClickListener1);
        		dialog.setButton2("No", myOnClickListener1);

        		dialog.show();
        	}
        	catch (NullPointerException npe){
        		showAlert("No Local Entries In Database");
        	}
        	//setSynchButton();
	        break;
        case DELALL_ID:
        	//if(rows == null || rows.size() == 0){
        	//	showAlert("No Entries In Database");
        	//	break;
        	//}
        	try{
        		AlertDialog dialog = new AlertDialog.Builder(this).create();

        		dialog.setMessage("Delete All Records?");
        		dialog.setButton("Yes", myOnClickListener2);
        		dialog.setButton2("No", myOnClickListener2);

        		dialog.show();
        	}
        	catch (NullPointerException npe){
        		showAlert("No Entries In Database");
        	}
        	//setSynchButton();
	        break;
        case HOME:
        	Intent i = new Intent(this, Epi_collect.class);
     	   	startActivity(i);
    		break;	
        case SYNCH_IMAGES_ID:
    		synchImages();
    		break;
        }
        
        return true;
    }

    class ButtonListener implements OnClickListener{
		public void onClick(DialogInterface dialog, int i) {
			switch (i) {
			case AlertDialog.BUTTON1:
			/* Button1 is clicked. Do something */
				dbAccess.deleteSynchRows();
				
				mHandler.post(new Runnable() {
                    public void run() { 
                    	updateData(0); }
                  });
				
				showToast("Records deleted");
			break;
			case AlertDialog.BUTTON2:
			/* Button2 is clicked. Do something */
			break;
			}
		}
      }
    
    class ButtonListener2 implements OnClickListener{
		public void onClick(DialogInterface dialog, int i) {
			switch (i) {
			case AlertDialog.BUTTON1:
			/* Button1 is clicked. Do something */
				dbAccess.deleteAllRows();
				
				mHandler.post(new Runnable() {
                    public void run() { 
                    	updateData(0); }
                  });
				
				showToast("Records deleted");
			break;
			case AlertDialog.BUTTON2:
			/* Button2 is clicked. Do something */
			break;
			}
		}
      }
    
    class ButtonListener3 implements OnClickListener{
		public void onClick(DialogInterface dialog, int i) {
			switch (i) {
			case AlertDialog.BUTTON1:
			/* Button1 is clicked. Do something */
				dbAccess.deleteRemoteRows();
				
				mHandler.post(new Runnable() {
                    public void run() { 
                    	updateData(1); }
                  });
				
				showToast("Records deleted");
			break;
			case AlertDialog.BUTTON2:
			/* Button2 is clicked. Do something */
			break;
			}
		}
      }
    
    private void showToast(String text){
    	Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
    
    private void createEntry() {
    	Intent i = new Intent(this, NewEntry.class);
    	startActivityForResult(i, ACTIVITY_CREATE);
    }

    
    Builder ad;
    String result = "Success";
    private void synchronizeData(final boolean images){ 
    	  	
    	//final String email = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID); //emailtext.getText().toString();   
	    
    	TelephonyManager mTelephonyMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
    	sIMEI = mTelephonyMgr.getDeviceId();
    	
    	myProgressDialog = ProgressDialog.show(this,     
                "Please wait...", "Synchronising Data...", true);

      new Thread() {
           public void run() {
        	   result = "Synchronisation Failed";
                try{
                	if(!images)
                		result = dbAccess.synchronize(localrows, sIMEI, project); //email,    password, sIMEI);
                	else
                		result = dbAccess.uploadAllImages();
                } catch (Exception e) {
                	Log.i(getClass().getSimpleName(), "ERROR: "+ e);
                }
                // Dismiss the Dialog
                myProgressDialog.dismiss();
                Looper.prepare();
                showAlert(result);
                /*You can use threads but all the views, and all the views related APIs,
                must be invoked from the main thread (also called UI thread.) To do
                this from a background thread, you need to use a Handler. A Handler is
                an object that will post messages back to the UI thread for you. You
                can also use the various post() method from the View class as they
                will use a Handler automatically. */
                if(!images){
                	mHandler.post(new Runnable() {
                		public void run() { 
                			updateData(0); }
                	});
                }
                Looper.loop();
                Looper.myLooper().quit(); 
                
                 
           }
      }.start();

    }
      
    public void showAlert(String result){
    	
    	new AlertDialog.Builder(this)
        .setTitle("Completed")
        .setMessage(result)
        .setNegativeButton("OK", new DialogInterface.OnClickListener() {

             public void onClick(DialogInterface dialog, int whichButton) {
            	 //updateData();
             }
        }).show();	
    	
    	
    }
    
    public void showMap() {
    	Intent i = new Intent(this, LocalMap.class);
    	//i.putExtra(KEY_EMAIL, email);
    	i.putExtra("overlay_local", restore_local);
   		i.putExtra("overlay_remote", restore_remote);
    	startActivityForResult(i, ACTIVITY_MAP);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, NewEntry.class);
        i.putExtra(KEY_SOURCE, "list");
        i.putExtra(KEY_ID, rows.get(position).rowId);
        i.putExtra(KEY_DATE, rows.get(position).ecdate);
        //i.putExtra(KEY_STORED, rows.get(position).stored);
        i.putExtra(KEY_ISSTORED, 1);
        i.putExtra(KEY_STORED, rows.get(position).stored);
        i.putExtra(KEY_REMOTEID, rows.get(position).remoteId);
        
        for(String key : textviews){
        	i.putExtra(key, rows.get(position).datastrings.get(key));
        }
        
        for(String key : spinners){
    		i.putExtra(key, rows.get(position).spinners.get(key));
        }
        
        for(String key : checkboxes){
        	Log.i(getClass().getSimpleName(), "CHECKBOX: "+ key +" "+rows.get(position).checkboxes.get(key));  
    		i.putExtra(key, rows.get(position).checkboxes.get(key));
        }
        
        i.putExtra(KEY_LAT, rows.get(position).gpslat);
        i.putExtra(KEY_LON, rows.get(position).gpslon);
        i.putExtra(KEY_ALT, rows.get(position).gpsalt);
        i.putExtra(KEY_ACC, rows.get(position).gpsacc);
        
        try{
        	i.putExtra(KEY_PHOTO, rows.get(position).photoid);
        }
        catch(Exception e){
        	i.putExtra(KEY_PHOTO, "-1");
        }
        startActivityForResult(i, ACTIVITY_EDIT);   
    }

    @SuppressWarnings("unchecked")
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	//Bundle extras = data.getExtras();
        switch(requestCode) {
        case ACTIVITY_CREATE:
            fillData();
            break;
        case ACTIVITY_EDIT:
            fillData();
            break; 
        case ACTIVITY_MAP:
        	Bundle extras = null;
    	   	if(data != null)
    	   		extras = data.getExtras();
	    	if(extras != null){
	    		restore_local = (HashMap<String, Integer>) extras.get("overlay_local");
	    		restore_remote = (HashMap<String, Integer>) extras.get("overlay_remote"); 
	    	} 
	        break; 
        }            
    } 

 // To prevent crashes when screen orientation changes and onProgressDialog is showing.
    // Also, in manifest file have to addthe statement android:configChanges="keyboardHidden|orientation" for that activity.
    // so when your screen orientation is changed, it wont call the onCreate() method again.

    public void onConfigurationChanged(Configuration arg0)
         {
                 super.onConfigurationChanged(arg0);
         }

    private void getValues(){
    	
    	textviews = new String[0];
    	spinners = new String[0];
    	checkboxes = new String[0];
    	
        spinnershash.clear();
        
    	if(dbAccess.getValue("textviews") != null)
			textviews = (dbAccess.getValue("textviews")).split(",,"); // "CNTD", 
    	if(dbAccess.getValue("spinners") != null)
    		spinners = (dbAccess.getValue("spinners")).split(",,");
    	if(dbAccess.getValue("checkboxes") != null)
    		checkboxes = (dbAccess.getValue("checkboxes")).split(",,");
    	
    	List <String>list = Arrays.asList(dbAccess.getValue("listfields").split("\\s+"));
        listfields = new Vector<String>(list);
        
        list = Arrays.asList(dbAccess.getValue("listspinners").split("\\s+"));
        listspinners = new Vector<String>(list);
        
        list = Arrays.asList(dbAccess.getValue("listcheckboxes").split("\\s+"));
        listcheckboxes = new Vector<String>(list);
    	
    	String[] tempstring;
        for(String key : spinners){       	
        	tempstring = dbAccess.getValue("spinner_"+key).split(",,");
	    	spinnershash.put(key, tempstring);
        }       
        
        project = dbAccess.getProject();
        
    }
    
    private void setSynchButton(){
    	
    	if(allsynched){
        	synchButton.setTextColor(Color.WHITE);
        	synchButton.setText("No Entries to Synchronise");
        	synchButton.setEnabled(false);
        }
        else{
        	synchButton.setTextColor(Color.BLACK);
        	synchButton.setText("Tap to Synchronise Entries");
        	synchButton.setEnabled(true);
        }
    	
    }
    
    private void showHelp() {
    	Intent i = new Intent(this, Help.class);
    	i.putExtra("HELP_TEXT", 2); //"The list help text will appear here ...\n...");
    	startActivity(i);
    }
    
    private void synchImages(){
    	if(dbAccess.checkImages())
    		showAlert("Synchronize all entries before synchronising images");
    	else
    		synchronizeData(true);
    	//showAlert(dbAccess.uploadAllImages());
	}
    
    @Override
    protected void onPause() {
        super.onPause();
        dbAccess.close();
        dbAccess = null;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (dbAccess == null) {
        	dbAccess = new DBAccess(this);
        	dbAccess.open();
        }
    } 
    
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
    	if(keyCode == KeyEvent.KEYCODE_BACK){ // event.KEYCODE_BACK
    		Intent i = new Intent(this, Epi_collect.class);
     	   	startActivity(i);
    			
    	}
    	

        return false;
    } 
    
}
