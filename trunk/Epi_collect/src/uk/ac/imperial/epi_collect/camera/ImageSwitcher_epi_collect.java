/* 
 * 
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.imperial.epi_collect.camera;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.imperial.epi_collect.util.db.DBAccess;
import uk.ac.imperial.epi_collect.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.Toast;
import android.widget.Gallery.LayoutParams;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

public class ImageSwitcher_epi_collect extends Activity implements AdapterView.OnItemSelectedListener, ViewSwitcher.ViewFactory { 

private static final int DELETE_ID = 1; //Menu.FIRST;
private static final int SELECT_ID = 2; //Menu.FIRST + 1;
private static final int SYNCH_IMAGES_ID = 3; //Menu.FIRST + 1;
private static final int CAP_PHOTO_ID = 4;
private static final int ACTIVITY_CAP_PHOTO=1;
private static final String KEY_PHOTO = "photo";
private Integer photoid = -1;
private String imagefile = "0", ecdate;
private Gallery g;
private static String thumbdir; 
private static String picdir; 
ButtonListener myOnClickListener = new ButtonListener();
boolean havesdcard = true;
private DBAccess dbAccess;
private boolean gallery = true;

@Override
public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    dbAccess = new DBAccess(this); 
    dbAccess.open();
    
    thumbdir = Environment.getExternalStorageDirectory()+"/EpiCollect/thumbs_epicollect_" + dbAccess.getProject(); //this.getResources().getString(this.getResources().getIdentifier(this.getPackageName()+":string/project", null, null));
    picdir = Environment.getExternalStorageDirectory()+"/EpiCollect/picdir_epicollect_" + dbAccess.getProject(); //this.getResources().getString(this.getResources().getIdentifier(this.getPackageName()+":string/project", null, null));
    
    try{
       	File f = new File(thumbdir);
    	if(!f.exists())
    		f.mkdir();
    	f = new File(picdir);
    	if(!f.exists())
    		f.mkdir();
    	}
    catch(Exception e){
    	havesdcard = false;
    	showAlert("SD card not present. Required for photo capture");
    }
    
    setContentView(R.layout.imageswitcher_epi_collect);

    mSwitcher = (ImageSwitcher) findViewById(R.id.switcher);
    mSwitcher.setFactory(this);
    mSwitcher.setInAnimation(AnimationUtils.loadAnimation(this,
            android.R.anim.fade_in));
    mSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this,
            android.R.anim.fade_out)); 

    g = (Gallery) findViewById(R.id.gallery);
    g.setAdapter(new ImageAdapter(this));
    g.setOnItemSelectedListener(this);
	
    Bundle extras = getIntent().getExtras();
    
    ecdate = extras.getString("ecdate");
    
    Log.i("ImageSwitcher GALLERY", extras.getString("GALLERY"));
    if (extras != null && extras.getString("GALLERY") != null && extras.getString("GALLERY").equalsIgnoreCase("0")) {
    	gallery = false;
    	captureImage();
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

@Override
public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, DELETE_ID, 0, R.string.menu_delete_photo);
    menu.add(0, SELECT_ID, 0, R.string.menu_select);
    menu.add(0, SYNCH_IMAGES_ID, 0, R.string.menu_photo_synch);
    if(havesdcard)
    	menu.add(0, CAP_PHOTO_ID, 0, R.string.menu_photo);
    return true;
}

@Override
public boolean onMenuItemSelected(int featureId, MenuItem item) {
    super.onMenuItemSelected(featureId, item);
    switch(item.getItemId()) {
    case DELETE_ID:
    	AlertDialog dialog = new AlertDialog.Builder(this).create();

		dialog.setMessage("Delete Picture?");
		dialog.setButton("Yes", myOnClickListener);
		dialog.setButton2("No", myOnClickListener);

		dialog.show();
		break;
    case SELECT_ID:
    	Bundle extras = getIntent().getExtras();
        
    	String photo = "-1";
    	if(photoid > -1){
    		Pattern pattern = Pattern.compile("(\\d+\\.jpg)");
            Matcher matcher = pattern.matcher(mImageIds[photoid].toString());
            if(matcher.find())
            {
            	photo = matcher.group(1);
            }
    		extras.putString(KEY_PHOTO, photo); //mImageIds[photoid].toString());
    	}
    	else
    		extras.putString(KEY_PHOTO, "-1");
    	this.getIntent().putExtras(extras);
        setResult(RESULT_OK, this.getIntent());
        finish();
    	break;
    case CAP_PHOTO_ID:
    	captureImage();
   
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
			String fileloc = mImageIds[photoid].toString();
	    	File f = new File(fileloc);
	    	f.delete();
	    	fileloc = fileloc.replaceFirst("thumbs", "pictures");
	    	f = new File(fileloc);
	    	try{
	    		f.delete();
	    	}
	    	catch (Exception e) {
	    		Log.v( getClass().getSimpleName(), "KEY PHOTO 2"+ fileloc);
	    	}
			
			showToast("Picture deleted");
			onResume();
		break;
		case AlertDialog.BUTTON2:
		/* Button2 is clicked. Do something */
		break;
		}
	}
  }

	private void synchImages(){
		showAlert(dbAccess.uploadAllImages());
	}

private void showToast(String text){
	Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
}

@Override
 protected void onActivityResult(int requestCode, int resultCode, Intent data) {
   	super.onActivityResult(requestCode, resultCode, data);
   	//Log.i(getClass().getSimpleName(), "VIEW: HERE 2"); 
   	
   	File file = new File(Environment.getExternalStorageDirectory(), "temp.jpg");
   	
 // If cancel on camera pressed
	if(!file.exists()){
		
		if(!gallery){
			Bundle extras = getIntent().getExtras();
			extras.putString(KEY_PHOTO, "-1");
	    	this.getIntent().putExtras(extras);
            setResult(RESULT_OK, this.getIntent());
            finish();
		}
	return;
	}
   	
	//copyFile(file, new File(picdir, +imagefile+".jpg"));
   copyFile(file, new File(picdir+"/"+imagefile+".jpg"));
   	
   	try{
   		file.delete();
   	}
   	catch (Exception e){
   		
   	}

    try {
    	// load the origial BitMap (500 x 500 px)
    	Bitmap bmp = BitmapFactory.decodeFile(picdir+"/"+imagefile+".jpg");
    				
    	int width = bmp.getWidth();
    	int height = bmp.getHeight();
    	int newWidth, newHeight;
    		        
    	if(width > height){
    	  	newWidth = 512;
    	   	newHeight = 384;
    	}
    	else{
    	  	newWidth = 384;
    	   	newHeight = 512;
    	}
    		       
    	// calculate the scale - in this case = 0.4f
    	float scaleWidth = ((float) newWidth) / width;
    	float scaleHeight = ((float) newHeight) / height;
    		       
    	// create a matrix for the manipulation
    	Matrix matrix = new Matrix();
    	// resize the bit map
    	matrix.postScale(scaleWidth, scaleHeight);
    	// rotate the Bitmap
    	//matrix.postRotate(45);

    	// recreate the new Bitmap
    	Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true); 
    	FileOutputStream out = new FileOutputStream(thumbdir+"/"+imagefile+".jpg" );//this.openFileOutput("ping_media.jpg",MODE_PRIVATE);
    	resizedBitmap.compress(CompressFormat.JPEG, 50, out) ;
    	out.close() ;
    				
    	//media_path = "/sdcard/dcim/.thumbnails/" ;
    	} catch (FileNotFoundException e) {
    		Log.i("ImageSwitcher","FileNotFoundException generated when using camera") ;
    	} catch (IOException e) {
    		Log.i("ImageSwitcher","IOException generated when using camera") ;
    	}

    		if(!gallery){
    			 Bundle extras = getIntent().getExtras();
   	    		extras.putString(KEY_PHOTO, imagefile+".jpg");
     	    	this.getIntent().putExtras(extras);
                setResult(RESULT_OK, this.getIntent());
                finish();
    		}
} 

private void captureImage(){
	//Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	
	/*Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
	intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(FILEPATH));
	startActivityForResult(intent, 0); */

	//long pic_num = 0;
	//java.util.Date date = new java.util.Date();
	//pic_num = date.getTime();
	
	TelephonyManager mTelephonyMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
	imagefile = mTelephonyMgr.getDeviceId()+ "_"+ecdate;
	
	//imagefile = pic_num-1;
	photoid = mImageIds.length;

	File file = new File(Environment.getExternalStorageDirectory(), "temp.jpg");
	
	Intent imageCaptureIntent = new Intent("android.media.action.IMAGE_CAPTURE");
	imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
	//startActivityForResult(intent, 0);

	//imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file)); //Uri.fromFile(new File(picdir, +pic_num+".jpg")));
    startActivityForResult(imageCaptureIntent, ACTIVITY_CAP_PHOTO);
    
}

@SuppressWarnings("unchecked")
public void onItemSelected(AdapterView parent, View v, int position, long id) {
	photoid = position;
	mSwitcher.setImageURI(mImageIds[position]);
}

@SuppressWarnings("unchecked")
public void onNothingSelected(AdapterView parent) {
}

public View makeView() {
    ImageView i = new ImageView(this);
    i.setScaleType(ImageView.ScaleType.FIT_CENTER);
    i.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.FILL_PARENT,
            LayoutParams.FILL_PARENT));
    return i;
} 

@Override
protected void onPause() {
    super.onPause();
    //storeData(0);
    dbAccess.close();
    dbAccess = null;
}
    
// Updates gallery if camera has been used
@Override
protected void onResume() {
    super.onResume();
    if (dbAccess == null) {
    	dbAccess = new DBAccess(this);
    	dbAccess.open();
    }
    
    g.setAdapter(new ImageAdapter(this));
    g.setSelection(mImageIds.length-1, true);
    photoid = mImageIds.length-1;
} 

private ImageSwitcher mSwitcher;
private Uri[] mImageIds; // Integer[] 
private String[] mFiles= null;
private Context mContext;

class ImageFilter implements FilenameFilter
{
	public boolean accept(File dir, String name)
	{
		return (name.endsWith(".jpg"));
	}
}

public class ImageAdapter extends BaseAdapter {
    public ImageAdapter(Context c) {
        mContext = c;
    }

    public int getCount() {
    	
    	if(!havesdcard)
    		return 0;
    	
		File images = new File(thumbdir+"/"); // Environment.getExternalStorageDirectory();
		File[] imagelist = images.listFiles(new ImageFilter());
        
		mFiles = new String[imagelist.length];

		for(int i= 0 ; i< imagelist.length; i++){
			//Log.i(getClass().getSimpleName(), "Image List Length "+imagelist.length+ " PATH "+imagelist[i].getAbsolutePath());
			mFiles[i] = imagelist[i].getAbsolutePath();
		}
		mImageIds = new Uri[mFiles.length];

		for(int i=0; i < mFiles.length; i++){
			mImageIds[i] = Uri.parse(mFiles[i]);   
		}	

		if(mImageIds.length == 0){
			String text = "No images available. Use Capture Photo menu option to take picture";
			Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show(); 
		}
		photoid = mImageIds.length; // pic_num
        return mImageIds.length;
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView i = new ImageView(mContext);

        i.setImageURI(mImageIds[position]);
        i.setAdjustViewBounds(true);
        i.setLayoutParams(new Gallery.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        i.setBackgroundResource(android.R.drawable.picture_frame);

        return i;
    }

    public float getAlpha(boolean focused, int offset) {
        return Math.max(0.2f, 1.0f - (0.2f * Math.abs(offset)));
    }

    public float getScale(boolean focused, int offset) {
        return Math.max(0, offset == 0 ? 1.0f : 0.6f);
    }

}

private static void copyFile(File f1, File f2){ //String srFile, String dtFile){
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
  }


}

