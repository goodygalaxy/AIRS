/*
Copyright (C) 2012, Dirk Trossen, airs@dirk-trossen.de

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
*/
package com.airs.platform;

import com.airs.MapViewerActivity;
import com.airs.TimelineActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class History 
{ 
    // start timeline activity
    static public void timelineView(Context airs, String title, String Symbol)
    {
    	Intent intent = new Intent(airs.getApplicationContext(), TimelineActivity.class);
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    	// create bundle
    	Bundle bundle = new Bundle();

    	bundle.putString("com.airs.Title", title);
    	
    	bundle.putString("com.airs.Symbol", Symbol);  
    	
        intent.putExtras(bundle);

 	   	airs.startActivity(intent);
    }
    
    // start mapview activity
    static public void mapView(Context airs, String title, String Symbol)
    {
    	Intent intent = new Intent(airs.getApplicationContext(), MapViewerActivity.class);
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    	// create bundle
    	Bundle bundle = new Bundle();

    	// place title
    	bundle.putString("com.airs.Title", title);
    	
    	bundle.putString("com.airs.Symbol", Symbol);  

        intent.putExtras(bundle);

    	airs.startActivity(intent);    	
    }
}
