package uk.ac.imperial.epi_collect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import uk.ac.imperial.epi_collect.util.db.DBAccess;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class EntryNote extends Activity {
 
	private ArrayAdapter<String> aspnLocs; 
	private Hashtable<String, EditText> textviewhash;
	private Hashtable<String, Spinner> thisspinnerhash;
	private Hashtable <String, ArrayList<String>>spinnershash = new Hashtable <String, ArrayList<String>>();
	private Hashtable<String, CheckBox> checkboxhash;
	private Vector<String> doubles = new Vector<String>();
	private Vector<String> integers = new Vector<String>();
	private static String[] textviews = new String[0];
    private static String[] spinners = new String[0];
    private static String[] checkboxes = new String[0];
    private static Vector<String> requiredfields, requiredspinners;
	private int alldata = 0;
	private static final int RESET_ID = 1;
	private static final int HOME = 2;
	private ViewFlipper f;
	private TextView tv, pagetv;
	private EditText et;
	private Spinner spin;
	private CheckBox cb;
	private DBAccess dbAccess;
	private boolean canupdate = true;
	private Button confirmButton;

	private String[] allviews = new String[0];
	int lastpage = 1;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
        //super.setTitle(this.getResources().getString(this.getResources().getIdentifier(this.getPackageName()+":string/app_name", null, null))); 
        
		dbAccess = new DBAccess(this);
	    dbAccess.open();
		
	    super.setTitle("EpiCollect "+dbAccess.getProject());
	    
	    getValues();
	    setContentView(setLayout()); 
        
	    Bundle extras = getIntent().getExtras();
        if (extras != null) {
        	if(extras.getInt("canupdate") == 0){
            	canupdate = false;
       			confirmButton.setTextColor(Color.WHITE);
        		confirmButton.setText("Record synchronised");
        		confirmButton.setEnabled(false);
            }
        	String text;
            for(String key : textviews){
           		text = extras.getString(key);
            	if(text == null){
           			text = "";
                }
            	
            	textviewhash.get(key).setText(text);
            	if(!canupdate)
            		textviewhash.get(key).setFocusable(false); 
            }
            
            int location;
            for(String key : spinners){
            	location = extras.getInt(key);
            	//Log.i(getClass().getSimpleName(), "SPINNER LOCATION: "+ key+" "+extras.getInt(key));
            	if(thisspinnerhash.get(key) != null)
            		thisspinnerhash.get(key).setSelection(location);
            	if(!canupdate)
            		thisspinnerhash.get(key).setClickable(false);
            }
            
            for(String key : checkboxes){
            	if(extras.getBoolean(key)){
            		checkboxhash.get(key).setChecked(true);
            	}
            	else{
            		checkboxhash.get(key).setChecked(false);
            	}
            	if(!canupdate)
            		checkboxhash.get(key).setClickable(false);
            }
            
            

        }
	}

	private void confirmData(){
		Bundle extras = getIntent().getExtras();
        
    	for(String key : textviews){
   			extras.putString(key, textviewhash.get(key).getText().toString());
    	}
    	               	
    	String result = "";
    	boolean store = true;
    	for(String key : textviews){
			if((textviewhash.get(key).getText().toString() == null) || (textviewhash.get(key).getText().toString().equalsIgnoreCase("")) && requiredfields.contains(key)){
				result += " " + key;
				store = false;
			}
		}
		
		for(String key : spinners){
			if(thisspinnerhash.get(key).getSelectedItemPosition() == 0 && requiredspinners.contains(key)){
				result += " " + key;
				store = false;
			}
		}
		
		if(!store && alldata == 0){
			showAlert("Entries required for: "+ result +". Confirm again to continue");
			alldata = 1;
			return;
		}
		
    	alldata = 0;
    	
        for(String key : spinners){
        	extras.putInt(key, thisspinnerhash.get(key).getSelectedItemPosition());
        }
        
        for(String key : checkboxes){
        	if(checkboxhash.get(key).isChecked()){
        		extras.putBoolean(key, true);
        	}
        	else{
        		extras.putBoolean(key, false);
        	}
        }

        extras.putInt("isstored", 0);
        getIntent().putExtras(extras);
        setResult(RESULT_OK, getIntent());
        finish();
  
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);
	    //menu.add(R.string.menu_reset);
	    //menu.add(R.string.menu_home);
	    //return super.onCreateOptionsMenu(menu);
	    menu.add(0, RESET_ID, 0, R.string.menu_reset);
	    menu.add(0, HOME, 0, R.string.menu_home);
	    return true;
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
    		if(canupdate)
    			confirmBack(event);
    		else{
    			Bundle extras = getIntent().getExtras();
            	getIntent().putExtras(extras);
                setResult(RESULT_OK, getIntent());
                finish();
    		}
    			
    	}
    	// Menu button doesn't work by default on this view for some reason
    	if(keyCode == KeyEvent.KEYCODE_MENU)
    		openOptionsMenu();

        return true;
    } 
    
    private void confirmBack(KeyEvent event){
    	AlertDialog alertDialog = new AlertDialog.Builder(this).create();
    	alertDialog.setTitle("Warning");
        alertDialog.setMessage("Save current data?");
        alertDialog.setButton("Yes", new DialogInterface.OnClickListener(){
             public void onClick(DialogInterface dialog, int whichButton) {
            	alldata = 1;
            	confirmData();
             }
        });
        alertDialog.setButton2("No", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton) {
            	Bundle extras = getIntent().getExtras();
            	getIntent().putExtras(extras);
                setResult(RESULT_OK, getIntent());
                finish();
            }
       });
        alertDialog.show();	
	}
    
    @Override  
    protected void onSaveInstanceState(Bundle outState) {  
    	super.onSaveInstanceState(outState);  
            
    	for(String key : textviews){
    		outState.putString(key, textviewhash.get(key).getText().toString());
    	   	}
    	
        for(String key : spinners){
        	outState.putInt(key, thisspinnerhash.get(key).getSelectedItemPosition());
        }
        
        for(String key : checkboxes){
        	if(checkboxhash.get(key).isChecked()){
        		outState.putBoolean(key, true);
        	}
        	else{
        		outState.putBoolean(key, false);
        	}
        }
    }  
    
    @Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
	    super.onMenuItemSelected(featureId, item);
	    switch(item.getItemId()) {
	    case RESET_ID:
	    	resetData();
	        break;
	    case HOME:
			confirmHome();
			break;	
	    }
	        
	    return true;
	}
    
    private void resetData(){
    	for(String key : textviews){
        	textviewhash.get(key).setText("");
        }
        
        for(String key : spinners){
        	thisspinnerhash.get(key).setSelection(0);
        }
        
        for(String key : checkboxes){
        	checkboxhash.get(key).setChecked(false);
        }
    }

    public void showAlert(String result){
    	new AlertDialog.Builder(this)
        .setTitle("Error")
        .setMessage(result)
        .setNegativeButton("OK", new DialogInterface.OnClickListener() {

             public void onClick(DialogInterface dialog, int whichButton) {

             }
        }).show();	
    }

    
    private void getValues(){
    	
    	// Rest values as otherwise odd values seem to get left behind:
    	// Project - data - back -back - new project - data -> causes crash
    	// If first project has spinner and second doesn't then second "spinners"
    	// still contains
    	textviews = new String[0];
    	spinners = new String[0];
    	checkboxes = new String[0];
    	
        //spinnershash.clear();
        //checkboxhash.clear();
        
       	doubles.clear();
    	integers.clear();
        
    	if(dbAccess.getValue("textviews") != null)
			textviews = (dbAccess.getValue("textviews")).split(",,"); // "CNTD", 
    	if(dbAccess.getValue("spinners") != null)
    		spinners = (dbAccess.getValue("spinners")).split(",,");
    	if(dbAccess.getValue("checkboxes") != null)
    		checkboxes = (dbAccess.getValue("checkboxes")).split(",,");
    	
    	//for(String t : textviews)
    	//	Log.i(getClass().getSimpleName(), "TEXT VIEW "+t);
    	
    	for(String key : (dbAccess.getValue("doubles")).split(",,")){
        	doubles.addElement(key);
        }
        
        for(String key : (dbAccess.getValue("integers")).split(",,")){
        	integers.addElement(key);
        }
                
        List<String> list = Arrays.asList((dbAccess.getValue("requiredtext")).split(",,"));
        requiredfields = new Vector<String>(list);
        
        list = Arrays.asList((dbAccess.getValue("requiredspinners")).split(",,"));
        requiredspinners = new Vector<String>(list);
                        
    }
    
    
    
    private RelativeLayout setLayout(){
    
    	textviewhash = new Hashtable<String, EditText>();
        thisspinnerhash = new Hashtable<String, Spinner>();
        checkboxhash = new Hashtable<String, CheckBox>();
    	
        String views =  dbAccess.getValue("notes_layout");// parser.getValues();
       
	    f = new ViewFlipper(this);
	    
	    RelativeLayout ll = new RelativeLayout(this);
	    ll.setLayoutParams( new ViewGroup.LayoutParams( LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT ) );
	    
	 // Calculate last page
	    
	    int count = 0, totalcount = 0;
	    String[] viewvalues;
	    
	    views.replaceFirst(",,,", "");
	    allviews = views.split(",,,");
	    
	    int checkcount = 0;
	    //String thisview;
	    //boolean incheck = false;
	    //for(int j = 0; j < allviews.length; j++){
	    //	String thisview = allviews[j];
	    boolean checkincremented = false;
	    for(String thisview : allviews){
	    	viewvalues = thisview.split(",,");
	    	if(viewvalues[0].equalsIgnoreCase("input") || viewvalues[0].equalsIgnoreCase("select1")){
	    		count++;
	    		totalcount++;
	    		checkincremented = false;
	    	}
	    	//boolean incremented = false;
	    	if(viewvalues[0].equalsIgnoreCase("select")){
	    		count++;
	    		for(int i = 3; i < viewvalues.length; i++){
	    			//if(!incremented || count % 5 != 0){
	    				checkcount++;
	        			//totalcount++;
	        			
	        		if(i == 3 && count == 6){
	        			// The checkbox is at the start of a new page so ensure lastpage is incremented
	        			lastpage++;
	        			count= 1;
	        		}
	    			//}
	    			//Log.i(getClass().getSimpleName(), "CHECK COUNT "+count);
	    			//incremented = true; // Ensure count incremented at least once
	        		i++; // The viewvalues now contains the label and the value for each checkbox
	    		}
	    	}
	    	// Check wardresources
	    	// If when checkboxes added the next page takes it
	    	// to exactly 5 then lastpage is one short
	    	//Log.i(getClass().getSimpleName(), "CHECK COUNT 1 "+count);
	    	if(count > 5 || (count + checkcount > 5)){ //|| (totalcount > 5 && count >= 5)){
	    		if(!checkincremented)
	    			lastpage++;

	    		// If there are multiple checkboxes only want to increment lastpage once
	    		// If there are 10 for example lastpage gets incremented twice without
	    		// the checkincremented flag
	    		
	    		if(count + checkcount > 5)
	    			checkincremented = true;
	    		count= 1;
	    		checkcount = 0;
	    		
	    	}
	    	
	    }
	   
	
	    RelativeLayout.LayoutParams linear1layout2 = new RelativeLayout.LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT ); 
	       
	    ll.addView(f, linear1layout2);
	    
	    //ScrollView.LayoutParams sp = new ScrollView.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
	    
	    ScrollView s = new ScrollView(this);
	    
	    f.addView(s); 
	    
	    TableLayout.LayoutParams lp = new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	    TableLayout l=new TableLayout(this);
	    
	    s.addView(l); 
	      
	    RelativeLayout rl2;
	    Button bp, bn;
	    RelativeLayout.LayoutParams rlp3=null, rlp4=null, rlp5=null;
	    
	    //boolean addbuttons = false;
	    
	    if(totalcount >= 7){
	    	//addbuttons = true;
	    	rl2 = new RelativeLayout(this);
	    	//RelativeLayout.LayoutParams rlp2 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	    
	    	bp = new Button(this);
	    	bp.setOnClickListener(listenerPrevious);
	    	bp.setWidth(100);
	    	bp.setText("Previous");
	
	    	rlp3 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	    	rlp3.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	    
	    	//rl2.addView(bp, rlp3);
	      
	    	rlp4 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	    	rlp4.addRule(RelativeLayout.CENTER_HORIZONTAL);
	    	rlp4.addRule(RelativeLayout.CENTER_VERTICAL);
	    
	    	pagetv = new TextView(this);
	    	pagetv.setText("Page: 1 of "+lastpage);
	    	pagetv.setWidth(100);
	    	pagetv.setTextSize(18);
	    	//pagetv.setGravity(Gravity.CENTER_VERTICAL);
	    	rl2.addView(pagetv, rlp4);
		
	    	bn = new Button(this);
	    	bn.setOnClickListener(listenerNext);
	    	bn.setWidth(100);
	    	bn.setText("Next");
	    
	    	rlp5 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	    	rlp5.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
	    
	    	rl2.addView(bn, rlp5);
	   
	    	l.addView(rl2);
	    	View v = new View(this);
    		v.setMinimumHeight(2);
    		v.setBackgroundColor(Color.WHITE);
    		
    		l.addView(v, lp);
    		
	    	}
	   
		count = 0;
		   
		int page = 2;
		
	    for(String thisview : allviews){
	    	viewvalues = thisview.split(",,");
	    	
	    	if(count >= 5 && totalcount >= 7){
	    		s = new ScrollView(this);
	    	    
	    	    f.addView(s); //, sp);
	    	    
	    	    l=new TableLayout(this);
	    	    
	    	     rl2 = new RelativeLayout(this);
	    	    
	    	    bp = new Button(this);
	    	    bp.setOnClickListener(listenerPrevious);
	    		bp.setWidth(100);
	    	    bp.setText("Previous");
	    	    rlp3.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	    	    
	    	    rl2.addView(bp, rlp3);
	    	    	    	    
	    	    rlp4.addRule(RelativeLayout.CENTER_HORIZONTAL);
	    	    
	    	    pagetv = new TextView(this);
	    	    pagetv.setGravity(Gravity.CENTER_VERTICAL);
	    	    pagetv.setText("Page: "+page+" of "+lastpage);
	    	    //pagetv.setWidth(100);
	    	    pagetv.setTextSize(18);
	    	    rl2.addView(pagetv, rlp4);
	    		
	    	    if(page < lastpage){	    	    
	    	    
	    		bn = new Button(this);
	    		bn.setOnClickListener(listenerNext);
	    		bn.setWidth(100);
	    	    bn.setText("Next");
	    	    
	    	    rlp5.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
	    	    
	    	    rl2.addView(bn, rlp5);
	    	    }
	    	    
	    	    page++;
	    	    
	    	    l.addView(rl2);
	    	    
	    	    s.addView(l, lp);
	
	    	    View v = new View(this);
	    		v.setMinimumHeight(2);
	    		v.setBackgroundColor(Color.WHITE);
	    		v.setPadding(0, 10, 0, 10);
	    		l.addView(v, lp);
	    		
	    		count = 0;
	    		}
	    	
	    	if(viewvalues[0].equalsIgnoreCase("input")){
	    		tv = new TextView(this);
	        	tv.setText(viewvalues[2]);
	        	tv.setTextSize(18);
	        	//tv.setWidth(100);
	        	l.addView(tv, lp);
	        	
	    		et = new EditText(this);
	    		if(doubles.contains(viewvalues[1]) || integers.contains(viewvalues[1])){
	    			et.setInputType(InputType.TYPE_CLASS_NUMBER);
	    		}
	    		l.addView(et, lp);
	    		Log.i(getClass().getSimpleName(), "VIEW VALUES "+viewvalues[1]);
	    		textviewhash.put(viewvalues[1], et);
	    		count++;
	    	}
	    	
	    	 String[] tempstring;
	                  
	    	if(viewvalues[0].equalsIgnoreCase("select1")){
	    		tv = new TextView(this);
	        	tv.setText(viewvalues[2]);
	        	tv.setTextSize(18);
	        	//tv.setWidth(100);
	        	l.addView(tv, lp);
	        	
	    		spin = new Spinner(this);
	    		l.addView(spin, lp);
	    		thisspinnerhash.put(viewvalues[1], spin);
	    		tempstring = (dbAccess.getValue("spinner_"+viewvalues[1])).split(",,");
	    		if(spinnershash.get(viewvalues[1]) == null){
	 	    		spinnershash.put(viewvalues[1], new ArrayList<String>());
	 	    	}
	 	    	for (int i = 0; i < tempstring.length; i++) {
	 	    		spinnershash.get(viewvalues[1]).add(tempstring[i]);
	 	    	}
	 	    	
	    		count++;
	    	}
	    	if(viewvalues[0].equalsIgnoreCase("select")){
	    		//for(String st : viewvalues)
	    		//	Log.i(getClass().getSimpleName(), "CHECKBOX VALS: "+st);
	    		tv = new TextView(this);
	        	tv.setText(viewvalues[2]);
	        	tv.setTextSize(18);
	        	//tv.setWidth(100);
	        	l.addView(tv, lp);
	        	
	        	for(int i = 3; i < viewvalues.length; i++){
	        		cb = new CheckBox(this);
	        		cb.setText(viewvalues[i]);
	        		l.addView(cb, lp);
	        		i++;
	        		checkboxhash.put(viewvalues[1]+"_"+viewvalues[i], cb);
	        		count++;
	        	}
	    	}
	    	
	    	if(count >= 5 && totalcount >= 7){
	    	    
	    		View v = new View(this);
	    		v.setMinimumHeight(2);
	    		v.setBackgroundColor(Color.WHITE);
	    		l.addView(v, lp);
	    		
	    		rl2 = new RelativeLayout(this);
	    	    
	    		if(page > 2){
	    			bp = new Button(this);
	    			bp.setOnClickListener(listenerPrevious);
	    			bp.setWidth(100);
	    			bp.setText("Previous");
	    		// rlp3.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	    	    
	    			rl2.addView(bp, rlp3);
	    		}
	    	    	    	    
	    	    //rlp4.addRule(RelativeLayout.CENTER_HORIZONTAL);
	    	    
	    	    pagetv = new TextView(this);
	    	    //pagetv.setGravity(Gravity.CENTER_VERTICAL);
	    	    pagetv.setText("Page: "+(page-1)+" of "+lastpage);
	    	    //pagetv.setWidth(100);
	    	    pagetv.setTextSize(18);
	    	    rl2.addView(pagetv, rlp4);
	    		
	    	   // page++;
	    	    
	    	    if(page-1 < lastpage){	   
	    	    	bn = new Button(this);
	    	    	bn.setOnClickListener(listenerNext);
	    	    	bn.setWidth(100);
	    	    	bn.setText("Next");
	    	    
	    	    	// rlp5.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
	    	    
	    	    	rl2.addView(bn, rlp5);
	    	    }
	    	    l.addView(rl2);
	    	    
	    	   // page++;
	    	   
	    		}
	    	
	    }
	    
	    // To add padding 
	    View v = new View(this);
		v.setMinimumHeight(10);
		l.addView(v, lp);
		
	    v = new View(this);
		v.setMinimumHeight(2);
		v.setBackgroundColor(Color.WHITE);
		// Doesn't seem to work!
		//v.setPadding(0, 20, 0, 20);
		l.addView(v, lp);
		
		// To add padding 
	    v = new View(this);
		v.setMinimumHeight(10);
		l.addView(v, lp);
		
		confirmButton = new Button(this);
		confirmButton.setText("Confirm");
	   
	    l.addView(confirmButton, lp);
	    
	    confirmButton.setOnClickListener(new View.OnClickListener() {
	    	public void onClick(View view) {
	    	 	confirmData();
	    	    }
	    	});
	    
	    for(String key : spinners){
	    	aspnLocs = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnershash.get(key));
	    	aspnLocs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    	thisspinnerhash.get(key).setAdapter(aspnLocs); 
	    }
	
	    return ll;
	    
	 }

	 
	 private OnClickListener listenerNext = new OnClickListener() {
	        public void onClick(View v) {
	        	f.showNext();
	        }

	    };

	    private OnClickListener listenerPrevious = new OnClickListener() {
	        public void onClick(View v) {
	        	f.showPrevious();
	        }

	    };
	    
	    
	    private void confirmHome(){
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

