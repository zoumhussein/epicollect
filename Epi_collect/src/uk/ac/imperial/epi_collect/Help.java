package uk.ac.imperial.epi_collect;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Help extends Activity {
	
	
	String main1 = "<b>Help</b><br><br>For full instructions please visit :<br><br>http://www.epicollect.net<br><br>EpiCollect is a generic data collection tool that allows you to collect and submit geotagged data forms (along with photos) to a central project website. For example, questionnaires, surveys etc.  All data collected can then be viewed at the project website using Google Maps and Earth and data fields can be charted and filtered for display. You can also view data collected using Google Maps on your phone.<br><br>You can create your own projects online at http://www.epicollect.net<br><br>";
	String main2 = "Multiple projects can be loaded at one time and full instructions are available at the website. However, we have preloaded a demoproject to get you started. The demoproject contains a simple questionnaire for you to fill in, attach a photo, record a location and synchronise to the project website.<br><br>Briefly, the workflow is as follows:<br><br>Select ‘New Entry’ to enter data for the project.<br><br>";
	String main3 = "<b>New Entry</b><br><br>There are 4 stages to record a new entry:<br>  1) Assign GPS – record your location (your GPS must be turned on). When assigning a GPS position you should really be outside and sometimes getting a GPS fix can take time. When you do get a fix, the GPS accuracy is returned (in metres) and you can tap again to update.<br>  2) Add Photo – using your phones camera.<br>  3) Enter Form data – answer the questions required for a project. For text fields tap on the field to enter data via your phones keyboard. Scroll down the list of form fields to enter data and at the bottom of the page click ‘Confirm’ to save data.<br>  4) Store Entry – This stores all data within the phones database.<br><br>You can enter as many entries as you like but in order to synchronise the data with the project website you must go to the ‘List/Synchronise Entries’ screen.<br><br>";
	String main4 = "<b>List / Synchronise Entries – More help is available when here.</b><br><br>To synchronise data you must have a network connection (eg 2G/3G/4G or wireless).  You DO NOT need a network connection to collect data (meaning you can collect in more remote areas), only to synchronise.<br><br>On the List Entries section you can see all the entries you have collected and synchronise with the project website.  You can then view all entries submitted (by multiple users) at the project website.  For the demoproject the project homepage is:<br><br>http://www.epicollect.net/project.html?name=demoproject<br><br><b>Display Map</b><br><br>Providing you have network access you can view the entries you have collected using Google Maps and also retrieve entries others have submitted to the project website.<br><br>EpiCollect is a free , open source project and as such we rely on user  feedback for improvements etc. Please let us know any comments / bugs / suggestions at www.epicollect.net.<br><br>EpiCollect has been developed at Imperial College London and is funded by The Wellcome Trust<br><br><br><br>";
	String list1 = "<b>List Entries Help</b><br><br>For full instructions please visit<br><br>http://www.epicollect.net<br><br>The List Entries screen shows details of all the entries you have collected (and which are stored in the phone’s database.<br><br>Each entry listed contains three pieces of information:<br><br>";
	String list2 = "<b>1) Unique ID</b><br>Firstly, the unique ID of the entry is listed – this refers to an automatically assigned number stored in your phones database<br><br><b>2) Synchronisation Status</b><br>Secondly, the upload status of the entry is listed:<br>N means that the entry has not been synchronised with the project website.<br>Y means that the entry has been synchronised with the project website (and cannot be amended.<br>R means that the entry is a remote entry and was submitted by someone else to the project website (Please see instructions at website for retrieving remote entries).<br><br>";
	String list3 = "<b>3) Title</b><br>Thirdly, the title of the entry can be viewed – you can click on each to see the entries full details.<br><br><b>Synchronising Entries</b>To synchronise entries with the project website, simply tap the ‘synchronise’ button.<br><br>";  
	String list4 = "Note: You must have network connection (eg 2G/3G/4G or wireless) to sync records. However, do not worry if you don’t as you can carry on collecting data (you do not need network access to collect data) and synchronise when you return to an area with network access.<br><br><br><br>";
		
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        super.setTitle("EpiCollect Help"); 
	
        setContentView(R.layout.help);
        
        TextView helpview = (TextView) findViewById(R.id.helptext);
        Button helpbutton = (Button) findViewById(R.id.helpbut);
        
        if(getIntent().getExtras().getInt("HELP_TEXT") == 1){
        	helpview.setText(Html.fromHtml(main1+main2+main3+main4));
        }
        else if(getIntent().getExtras().getInt("HELP_TEXT") == 2){
        	helpview.setText(Html.fromHtml(list1+list2+list3+list4));
        }
        
        //Log.i(getClass().getSimpleName(), "HTML: "+ getIntent().getExtras().getString("HELP_TEXT"));  
        //helpview.setText(Html.fromHtml("<b>title</b><br /><small> description </small><br /><small> DateAdded </small>"));

        helpbutton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
            	finish();
            }
           
        });
        
        
	}
}
	
