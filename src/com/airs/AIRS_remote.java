/*
Copyright (C) 2011, Dirk Trossen, airs@dirk-trossen.de

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
package com.airs;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.os.PowerManager.WakeLock;
import android.widget.Toast;

import com.airs.helper.SerialPortLogger;
import com.airs.helper.Waker;
import com.airs.platform.Acquisition;
import com.airs.platform.Discovery;
import com.airs.platform.EventComponent;
import com.airs.platform.HandlerManager;

/**
 * @author trossen
 * @date Dec 3, 2004
 * 
 * Purpose: 
 */
public class AIRS_remote extends Service
{
	public static final int BATTERY_KILL 		= 3;
	public static final int SHOW_NOTIFICATION 	= 2;
	public static final int KILL_SERVICE 		= 1;
    public static final String TEXT = "TEXT";

    private  Context airs = null;
	private Discovery	 	 instDiscovery = null;
	private Acquisition 	 instAcquisition = null;
	private EventComponent	 instEC = null;
    private boolean Vibrate, Lights;
	private int Reminder_i;
    private int BatteryKill_i;
    private String LightCode;
    private NotificationManager mNotificationManager;
	private String IPAddress;
	private String IPPort;
	public boolean running = false, started = false, failed = false;
    // This is the object that receives interactions from clients
    private final IBinder mBinder = new LocalBinder();
    private VibrateThread Vibrator;
    private WakeLock wl;
    public int bytes_sent = 0, values_sent = 0;

	/**
	 * Sleep function 
	 * @param millis
	 */
	protected static void sleep(long millis) 
	{
		Waker.sleep(millis);
	}
    
	protected static void debug(String msg) 
	{
		SerialPortLogger.debug(msg);
	}

    public class LocalBinder extends Binder 
    {
    	AIRS_remote getService() 
        {
            return AIRS_remote.this;
        }
    }
    
	@Override
	public IBinder onBind(Intent intent) 
	{
		debug("RSA_remote::bound service!");
		return mBinder;
	}

	@Override
	public void onCreate() 
	{
	}
	
	@Override
	public void onDestroy() 
	{
		SerialPortLogger.debug("RSA_remote::destroying service...");

		if (instEC!=null)
			instEC.stop();
		
		SerialPortLogger.debug("RSA_remote::...terminating Vibrate thread");
  		 // if Vibrator is running, stop it!
		try
		{
	   		 // if Vibrator is running, stop it!
	   		 if (Vibrator!=null)
	   		 {
	   			Vibrator.stop = true;
	   			Vibrator.thread.interrupt(); 
	   		 }
		}
		catch(Exception e)
		{
			 debug("RSA::Exception when terminating Vibrator thread!");
		}
		 
		try
		{
			 SerialPortLogger.debug("RSA_remote::...destroy handlers");
			 if (started == true)
				 HandlerManager.destroyHandlers();	
		}
		catch(Exception e)
		{
			
		}
		 
		SerialPortLogger.debug("RSA_remote::...release wake lock");
		// create wake lock if held
		if (wl != null)
			if (wl.isHeld() == true)
				wl.release();

		SerialPortLogger.debug("RSA_remote::...onDestroy() finished");
	 }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		debug("RSA_remote::started service ID " + Integer.toString(startId));

		// return if intent is null -> service was restarted
		if (intent == null)
			return Service.START_NOT_STICKY;	

		// sensing running?
		if (running == true)
			return Service.START_NOT_STICKY;
		
		if (started == false)
			return Service.START_NOT_STICKY;

		// start the measurements if discovered
		running = startRSA();

		// stop service if starting failed
		if (running == false)
		{
     		Toast.makeText(getApplicationContext(), "Starting remote sensing failed!\nStart AIRS and try again.", Toast.LENGTH_LONG).show();		
	        Message msg = mHandler.obtainMessage(KILL_SERVICE);
	        mHandler.sendMessage(msg);
		}
		else
     		Toast.makeText(getApplicationContext(), "Starting remote sensing successful!\nStart monitoring by clicking on notification bar message.", Toast.LENGTH_LONG).show();		

		return Service.START_NOT_STICKY;
	}
	
	// function blocks -> meant as server kind of 
	private boolean startRSA()
	{		
		Notification notification;
		PendingIntent contentIntent;
		
		// create timer/alarm handling
		Waker.init(this);
		
		this.airs = this.getApplicationContext();

		debug("create handlers...");
		HandlerManager.createHandlers(getApplicationContext());
		// started handlers
		started = true;
		
		// get preference variables from settings
		Reminder_i = HandlerManager.readRMS_i("Reminder", 0) * 1000;
		Vibrate    = HandlerManager.readRMS_b("Vibrator", true);
		Lights     = HandlerManager.readRMS_b("Lights", true);
		LightCode  = HandlerManager.readRMS("LightCode", "00ff00");
		IPAddress 	= HandlerManager.readRMS("IPStore", "127.0.0.1");
		IPPort	 	= HandlerManager.readRMS("IPPort", "9000");
		// find out whether or not to kill based on battery condition
		BatteryKill_i = HandlerManager.readRMS_i("BatteryKill", 0);

		// create notification
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notification = new Notification(R.drawable.icon, "Starting AIRS", System.currentTimeMillis());

		// create pending intent for starting the activity
		contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, AIRS_remotevalues.class),  Intent.FLAG_ACTIVITY_NEW_TASK);
		notification.setLatestEventInfo(getApplicationContext(), "AIRS Remote Sensing", "...is trying to connect since...", contentIntent);
		notification.flags = Notification.FLAG_NO_CLEAR;
		mNotificationManager.notify(0, notification);
		 
		// create local event component
		instEC			= new EventComponent();
		
		// trying to connect
		if (instEC.startEC(this, IPAddress, IPPort) == true)
		{
			 // create acquisition component
			 instAcquisition = new Acquisition(instEC);
			 // create discovery component
			 instDiscovery = new Discovery(instEC);		

			 // any initialization failed?
			 if (instAcquisition == null || instDiscovery==null)
				 return false;
			 
			 // vibrate?
			 if (Reminder_i>0)
				 Vibrator = new VibrateThread();

			 // update notification
			 notification = new Notification(R.drawable.icon, "Started AIRS", System.currentTimeMillis());

			 // create pending intent for starting the activity
			 contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, AIRS_remotevalues.class),  Intent.FLAG_ACTIVITY_NEW_TASK);
			 notification.setLatestEventInfo(getApplicationContext(), "AIRS Remote Sensing", "...is running since...", contentIntent);
			 notification.flags = Notification.FLAG_NO_CLEAR;
			 startForeground(1, notification);

			 // create new wakelock
			 PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
			 
			 wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AIRS Remote Lock");
			 wl.acquire();

			 // need battery monitor?
			 if (BatteryKill_i > 0)
			 {
				 // register intent for watching battery
				 IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
				 registerReceiver(mReceiver, filter);			 
			 }

			 return true;
		}
		else
		{
			failed = true;
			return false;
		}
	}
	
	
	 // The Handler that gets information back from the other threads, updating the values for the UI
	 public final Handler mHandler = new Handler() 
    {
       @Override
       public void handleMessage(Message msg) 
       {
    	   NotificationManager mNotificationManager;
    	   
           switch (msg.what) 
           {
           case BATTERY_KILL:
           	// stop foreground service
           	stopForeground(true);
           	
           	// now create new notification
           	mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
  		 	Notification notification = new Notification(R.drawable.icon, "Killed AIRS", System.currentTimeMillis());
  		 	Intent notificationIntent = new Intent(getApplicationContext(), AIRS_tabs.class);
  		 	PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
  			notification.setLatestEventInfo(getApplicationContext(), "AIRS Remote Sensing", "...has been killed at " + Integer.toString(BatteryKill_i) + "% battery...", contentIntent);
  			
  			// give full fanfare
  			notification.flags |= Notification.DEFAULT_SOUND | Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
  			notification.ledARGB = 0xffff0000;
  			notification.ledOffMS = 1000;
  			notification.ledOnMS = 1000;
  			
  			mNotificationManager.notify(17, notification);
  			
  			// stop service now!
           	stopSelf();
           	break;
           case SHOW_NOTIFICATION:
            Toast.makeText(getApplicationContext(), msg.getData().getString(TEXT), Toast.LENGTH_LONG).show();   
            break;
           case KILL_SERVICE:
        	// stop foreground service
            stopForeground(true);
      		// stop service now!
            stopSelf();
            // remove icon
           	mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancelAll();
            break;   
           default:  
           	break;
           }
       }
    };

	 // Vibrate watchdog
	 private class VibrateThread implements Runnable
	 {
		 public Thread thread;
		 public boolean stop = false;

		 VibrateThread()
		 {
			// save thread for later to stop 
			(thread = new Thread(this)).start();			 
		 }
		 
		 public void run()
		 {
			 long vibration[] = {0,200,0};

			 // get notification manager
			 mNotificationManager = (NotificationManager)airs.getSystemService(Context.NOTIFICATION_SERVICE); 

			 while (stop == false)
			 {		
				 // sleep for the agreed time
			     sleep(Reminder_i);

			     // prepare notification to user
			     Notification notif = new Notification();
			     notif.when              = System.currentTimeMillis(); 
			     notif.flags			|= Notification.FLAG_ONLY_ALERT_ONCE;
			     if (Vibrate == true)
			    	 notif.vibrate			 = vibration;
				 
				 if (Lights == true)
				 {
	                notif.ledARGB   = 0xff000000 | Integer.valueOf(LightCode, 16); 
	                notif.flags     |= Notification.FLAG_SHOW_LIGHTS; 
				 }
	              
				 // now shoot off alert
				 mNotificationManager.notify(0, notif);   
				 sleep(750);
				 
				 // and cancel
	             mNotificationManager.cancel(0);
			 }
		 }
	 }
	 
     public final BroadcastReceiver mReceiver = new BroadcastReceiver() 
     {
         @Override
         public void onReceive(Context context, Intent intent) 
         {
        	 int Battery = 100;
        	 
        	 // if battery changed...
             if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) 
             {
 	            int rawlevel = intent.getIntExtra("level", -1);
 	            int scale = intent.getIntExtra("scale", -1);
 	            if (rawlevel >= 0 && scale > 0) 
 	                Battery = (rawlevel * 100) / scale;
 	            
 	            // need to trigger battery kill action?
 	            if (Battery < BatteryKill_i)
 	            {
			        Message msg = mHandler.obtainMessage(BATTERY_KILL);
			        mHandler.sendMessage(msg);
 	            }
             }

         }
     };     
}
