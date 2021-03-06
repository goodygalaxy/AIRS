/*
Copyright (C) 2008-2011, Dirk Trossen, airs@dirk-trossen.de

This program is free software; you can redistribute it and/or modify it
under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation as version 2.1 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this library; if not, write to the Free Software Foundation, Inc.,
59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
*/package com.airs;

import java.io.File;

import com.airs.platform.HandlerManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore.Images;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class AIRS_measurements extends Activity implements OnItemClickListener, OnItemLongClickListener
{
	 private TextView mTitle;
	 private TextView mTitle2;
	 private AIRS_local AIRS_locally;
	 private ListView values;
	 private Activity act;

	   @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
	        // Set up the window layout
	        super.onCreate(savedInstanceState);
	        
	        act = this;
	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
			setContentView(R.layout.values);
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
	 
	        // get window title fields
	        mTitle = (TextView) findViewById(R.id.title_left_text);
	        mTitle2 = (TextView) findViewById(R.id.title_right_text);
	        mTitle.setText(R.string.app_name);
	        mTitle2.setText("Local Sensing");
		    
			// Find and set up the ListView for values
			values 	= (ListView)findViewById(R.id.valueList);        
	        // set listener for sensor list
			values.setOnItemClickListener(this);
			values.setOnItemLongClickListener(this);
	        // bind to service
	        if (bindService(new Intent(this, AIRS_local.class), mConnection, 0)==false)
	     		Toast.makeText(getApplicationContext(), "Measurement Activity::Binding to service unsuccessful!", Toast.LENGTH_LONG).show();
	    }

	    @Override
	    public synchronized void onRestart() 
	    {
	        super.onRestart();
	    	// ask service to update value adapter
	    	if (AIRS_locally!=null)
	    	{
	    		AIRS_locally.show_values = true;
	    		AIRS_locally.refresh_values();
	    	}
	    }

	    @Override
	    public void onStop() 
	    {
	        super.onStop();
	    	// stop service from updating value adapter
	    	if (AIRS_locally!=null)
	    		AIRS_locally.show_values = false;
	    }

	    @Override
	    public void onDestroy() 
	    {
	    	// stop service from updating value adapter
	    	if (AIRS_locally!=null)
	    	{
	    		AIRS_locally.show_values = false;
	    		unbindService(mConnection);
	    	}
	    		    	
	        super.onDestroy();
	    }

	    public void onActivityResult(int requestCode, int resultCode, Intent data) 
	    {
	    	return;
	    }
	    
	    @Override
	    public void onConfigurationChanged(Configuration newConfig) 
	    {
	      //ignore orientation change
	      super.onConfigurationChanged(newConfig);
	    }
	    
	   @Override
	    public boolean onPrepareOptionsMenu(Menu menu) 
	    {
	    	MenuInflater inflater;

	    	menu.clear();    		
	    	inflater = getMenuInflater();
	    	inflater.inflate(R.menu.options_local_sensing, menu);    		
	    	return true;
	    }

	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) 
	    {
	        switch (item.getItemId()) 
	        {
	        case R.id.main_about:
	        	Toast.makeText(getApplicationContext(), R.string.ValuesAbout, Toast.LENGTH_LONG).show();
	            return true;
	        case R.id.local_pause:
	        	if (AIRS_locally != null)
	        	{
			        mTitle2.setText("Local Sensing...Paused");
			        AIRS_locally.pause_threads();
	        	}
	        	break;
	        case R.id.local_resume:
	        	if (AIRS_locally != null)
	        	{
	        		mTitle2.setText("Local Sensing");
	        		AIRS_locally.resume_threads();
	        	}
	        	break;
	        case R.id.local_exit:
	    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    		builder.setMessage("Exit will stop your local recording and close the background service!\nAre you sure you want to exit?")
	    			   .setTitle("AIRS Local Sensing")
	    		       .setCancelable(false)
	    		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() 
	    		       {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {
	    		    		   if (AIRS_locally != null)
	    		    			   stopService(new Intent(act, AIRS_local.class));
	    		        	   finish();
	    		           }
	    		       })
	    		       .setNegativeButton("No", new DialogInterface.OnClickListener() 
	    		       {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {
	    		                dialog.cancel();
	    		           }
	    		       });
	    		AlertDialog alert = builder.create();
	    		alert.show();

		        return true;
	        }
	        return false;
	    }

	    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
	    {
	    	String value;
	    	
	    	// show info for sensor selected
		    if (AIRS_locally != null)
		    {
		    	// do we share media?
		    	if (AIRS_locally.getSymbol((int)id).compareTo("MW") == 0)
		    	{
		    		value = AIRS_locally.getValue((int)id);
		    		
					String [] tokens = value.split(":");
					// is it a camera picture?
					if (tokens[0] != null)
						if (tokens[1].trim().compareTo("camera") == 0)
						{
							// get filename
							if (tokens[2] != null)
							{
								// cut off '[file]' suffix
								String name = tokens[2].trim().substring(0, tokens[2].length() - " [file]".length());
								// form full path
								File file = new File(HandlerManager.readRMS("MediaWatcherHandler::CameraDirectory", Environment.getExternalStorageDirectory()+"/DCIM/Camera"), name.trim());
								Uri shareUri;
								
								// search for file in media store
								Cursor c = act.getContentResolver().query(Images.Media.EXTERNAL_CONTENT_URI, null, Images.Media.DATA + "=?", new String[] { file.getAbsolutePath() }, null);

								// if found -> share
								if (c.getCount() > 0 && c.moveToFirst())
								{
		                             shareUri = Uri.withAppendedPath(Images.Media.EXTERNAL_CONTENT_URI, ""+ c.getInt(c.getColumnIndex(Images.Media._ID)));

						 		       // now build and start chooser intent
						             Intent intent = new Intent(Intent.ACTION_SEND);
						                
						    	     intent.setType("*/*");
						    		 intent.putExtra(Intent.EXTRA_STREAM, shareUri);
						             intent.putExtra(Intent.EXTRA_TEXT, "Published from AIRS (https://market.android.com/details?id=com.airs)\n");
						    	     intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); 
						    		 act.startActivity(Intent.createChooser(intent,"Share AIRS snapshot to:"));			
								}
								else
						     		Toast.makeText(getApplicationContext(), "Nothing to share, it seems! Have you set the camera path to the right one (the path need to the SAME as shown for pictures in the Gallery applications!).", Toast.LENGTH_LONG).show();

								c.close();
								
							}
						}
						else
				     		Toast.makeText(getApplicationContext(), "Can only share pictures, sorry!", Toast.LENGTH_LONG).show();
		    	}
		    	else
		    	{
		    		// get sharing text first
		    		value = AIRS_locally.share((int)id);
		    		// only share if there's something to share
		    		if (value != null)
		    		{
		 		       // now build and start chooser intent
		                Intent intent = new Intent(Intent.ACTION_SEND);
		                intent.setType("text/plain");
		                intent.putExtra(Intent.EXTRA_TEXT, value + "\n\n-------------------------------------------\nPublished from AIRS (https://market.android.com/details?id=com.airs)\n");
		                act.startActivity(Intent.createChooser(intent,"Share AIRS Measurements via:"));
		    		}
		    	}
		    }
	    	return true;
	    }
	    
	    public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) 
	    {
	    	// show info for sensor selected
		    if (AIRS_locally != null)
		    	AIRS_locally.show_info((int)arg3);		    
	    }

	    private ServiceConnection mConnection = new ServiceConnection() 
	    {
	  	    public void onServiceConnected(ComponentName className, IBinder service) 
	  	    {
	  	        // This is called when the connection with the service has been
	  	        // established, giving us the service object we can use to
	  	        // interact with the service.  Because we have bound to a explicit
	  	        // service that we know is running in our own process, we can
	  	        // cast its IBinder to a concrete class and directly access it.
	     		AIRS_locally = ((AIRS_local.LocalBinder)service).getService();
	  	        if (AIRS_locally.running == false)
		     		Toast.makeText(getApplicationContext(), "AIRS local::Sensing not running!\nThe service might have crashed!", Toast.LENGTH_SHORT).show();
	  	        // now set the adapter for the values
				values.setAdapter(AIRS_locally.mValuesArrayAdapter);	
				
				// set threads to show values and refresh latest they have
				AIRS_locally.show_values = true;
				AIRS_locally.refresh_values();
		        // set title according to paused state of local service
		        if (AIRS_locally.paused == true)
			        mTitle2.setText("Local Sensing...Paused");
	  	    }

	  	    public void onServiceDisconnected(ComponentName className) {
	  	        // This is called when the connection with the service has been
	  	        // unexpectedly disconnected -- that is, its process crashed.
	  	        // Because it is running in our same process, we should never
	  	        // see this happen.
	     		AIRS_locally = null;
	  	    }
	  	};
}
