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
*/
package com.airs.handlers;

import java.util.concurrent.Semaphore;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.os.Handler;
import android.os.Message;

import com.airs.helper.Waker;
import com.airs.platform.HandlerManager;
import com.airs.platform.History;
import com.airs.platform.SensorRepository;

public class PhoneSensorHandler implements com.airs.handlers.Handler
{
	public static final int INIT_LIGHT = 1;
	public static final int INIT_PROXIMITY = 2;
	public static final int INIT_ORIENTATION = 3;
	public static final int INIT_PRESSURE = 4;

	private Context nors;
	private boolean sensor_enable = false;
	private boolean startedOrientation = false, startedLight = false, startedProximity = false, startedPressure = false;
	private SensorManager sensorManager;
	private android.hardware.Sensor Orientation, Proximity, Light, Pressure;
	// polltime for sensor
	private int polltime = 10000, polltime2 = 10000;
	// sensor values
	private float azimuth, roll, pitch, proximity, light, pressure;
	private float azimuth_old, roll_old, pitch_old, proximity_old, light_old, pressure_old;
	private Semaphore pressure_semaphore 		= new Semaphore(1);
	private Semaphore light_semaphore 		= new Semaphore(1);
	private Semaphore proximity_semaphore 	= new Semaphore(1);
	private Semaphore azimuth_semaphore 	= new Semaphore(1);
	private Semaphore roll_semaphore 		= new Semaphore(1);
	private Semaphore pitch_semaphore 		= new Semaphore(1);
	
	/**
	 * Sleep function 
	 * @param millis
	 */
	protected void sleep(long millis) 
	{
		Waker.sleep(millis);
	}
	
	private void wait(Semaphore sema)
	{
		try
		{
			sema.acquire();
		}
		catch(Exception e)
		{
		}
	}

	/***********************************************************************
	 Function    : Acquire()
	 Input       : sensor input is ignored here!
	 Output      :
	 Return      :
	 Description : acquires current sensors values and sends to
	 		 	   QueryResolver component
	***********************************************************************/
	public byte[] Acquire(String sensor, String query)
	{
		boolean read = false;
		int value = 0;
		byte [] readings = null;
		
		// anything there?
		if (sensor_enable == true)
		{
			// see which sensors are requested
			if (sensor.equals("Az") == true)
			{	
				// has Azimuth been started?
				if (startedOrientation == false)
				{
					// send message to handler thread to start GPS
			        Message msg = mHandler.obtainMessage(INIT_ORIENTATION);
			        mHandler.sendMessage(msg);	
				}

				wait(azimuth_semaphore); 
				if (azimuth != azimuth_old)
				{
					read = true;
					value = (int)(azimuth*10);
					azimuth_old = azimuth;
				}
			}
			
			if (read == false)
				if (sensor.equals("Pi") == true)
				{					
					// has Pitch been started?
					if (startedOrientation == false)
					{
						// send message to handler thread to start GPS
				        Message msg = mHandler.obtainMessage(INIT_ORIENTATION);
				        mHandler.sendMessage(msg);	
					}

					wait(pitch_semaphore); 
					if (pitch != pitch_old)
					{
						read = true;
						value = (int)(pitch*10);
						pitch_old = pitch;
					}
				}

			if (read == false)
				if (sensor.equals("Ro") == true)
				{					
					// has Roll been started?
					if (startedOrientation == false)
					{
						// send message to handler thread to start GPS
				        Message msg = mHandler.obtainMessage(INIT_ORIENTATION);
				        mHandler.sendMessage(msg);	
					}

					wait(roll_semaphore); 
					if (roll != roll_old)
					{
						read = true;
						value = (int)(roll*10);
						roll_old = roll;
					}
				}				
			if (read == false)
				if (sensor.equals("PR") == true)
				{					
					// has Proximity been started?
					if (startedProximity == false)
					{
						// send message to handler thread to start proximity
				        Message msg = mHandler.obtainMessage(INIT_PROXIMITY);
				        mHandler.sendMessage(msg);	
					}

					wait(proximity_semaphore); 
					if (proximity != proximity_old)
					{
						read = true;
						value = (int)(proximity*10);
						proximity_old = proximity;
					}
				}				
			if (read == false)
				if (sensor.equals("LI") == true)
				{					
					// has Light been started?
					if (startedLight == false)
					{
						// send message to handler thread to start light
				        Message msg = mHandler.obtainMessage(INIT_LIGHT);
				        mHandler.sendMessage(msg);	
					}

					wait(light_semaphore); 
					if (light != light_old)
					{
						read = true;
						value = (int)(light*10);
						light_old = light;
					}
				}				
			if (read == false)
				if (sensor.equals("PU") == true)
				{					
					// has Pressure been started?
					if (startedPressure == false)
					{
						// send message to handler thread to start pressure
				        Message msg = mHandler.obtainMessage(INIT_PRESSURE);
				        mHandler.sendMessage(msg);	
					}

					wait(pressure_semaphore); 
					if (pressure != pressure_old)
					{
						read = true;
						value = (int)(pressure*10);
						pressure_old = pressure;
					}
				}				
		}
		
		// anything to report?
		if (read == true)
		{
			readings = new byte[6];
			readings[0] = (byte)sensor.charAt(0);
			readings[1] = (byte)sensor.charAt(1);
			readings[2] = (byte)((value>>24) & 0xff);
			readings[3] = (byte)((value>>16) & 0xff);
			readings[4] = (byte)((value>>8) & 0xff);
			readings[5] = (byte)(value & 0xff);
		}

		return readings;		
	}
	
	/***********************************************************************
	 Function    : Share()
	 Input       : sensor input is ignored here!
	 Output      :
	 Return      :
	 Description : acquires current sensors values and sends to
	 		 	   QueryResolver component
	***********************************************************************/
	public String Share(String sensor)
	{		
		// see which sensors are requested
		if (sensor.equals("Az") == true)
			return "The current azimuth is " + String.valueOf(azimuth) + " degrees!";
		
		if (sensor.equals("Pi") == true)
			return "The current pitch is " + String.valueOf(pitch) + " degrees!";

		if (sensor.equals("Ro") == true)
			return "The current roll is " + String.valueOf(roll) + " degrees!";

		if (sensor.equals("PR") == true)
			return "The current proximity is " + String.valueOf(proximity);

		if (sensor.equals("LI") == true)
			return "The current light is " + String.valueOf(light) + " lux!";
		
		if (sensor.equals("PU") == true)
			return "The current pressure is " + String.valueOf(pressure) + " hPa!";

		return null;		
	}
	
	/***********************************************************************
	 Function    : History()
	 Input       : sensor input for specific history views
	 Output      :
	 Return      :
	 Description : calls historical views
	***********************************************************************/
	public void History(String sensor)
	{
		// see which sensors are requested
		if (sensor.equals("Az") == true)
			History.timelineView(nors, "Azimuth [degrees]", sensor);
		
		if (sensor.equals("Pi") == true)
			History.timelineView(nors, "Pitch [degrees]", "Pi");

		if (sensor.equals("Ro") == true)
			History.timelineView(nors, "Roll [degrees]", "Ro");

		if (sensor.equals("LI") == true)
			History.timelineView(nors, "Light [Lux]", "LI");
		
		if (sensor.equals("PU") == true)
			History.timelineView(nors, "Pressure [hPa]", "PU");
	}

	
	/***********************************************************************
	 Function    : Discover()
	 Input       : 
	 Output      : string with discovery information
	 Return      : 
	 Description : provides discovery information of this particular acquisition 
	 			   module, hardcoded 
	***********************************************************************/
	public void Discover()
	{
		if (sensor_enable == true)
		{
		   if (Orientation != null)
		   {
			   SensorRepository.insertSensor(new String("Az"), new String("degrees"), new String("Azimuth"), new String("int"), -1, 0, 3600, true, polltime, this);
			   SensorRepository.insertSensor(new String("Pi"), new String("degrees"), new String("Pitch"), new String("int"), -1, -1800, 1800, true, polltime, this);
			   SensorRepository.insertSensor(new String("Ro"), new String("degrees"), new String("Roll"), new String("int"), -1, -900, 900, true, polltime, this);	
		   }
		   if (Proximity != null)
			   SensorRepository.insertSensor(new String("PR"), new String("-"), new String("Proximity"), new String("int"), -1, 0, 1000, false, polltime2, this);	
		   if (Light != null)
			   SensorRepository.insertSensor(new String("LI"), new String("Lux"), new String("Light"), new String("int"), -1, 0, 50000, true, polltime2, this);	
		   if (Pressure != null)
			   SensorRepository.insertSensor(new String("PU"), new String("hPa"), new String("Pressure"), new String("int"), -1, 0, 50000, true, polltime2, this);	
		}
	}
	
	public PhoneSensorHandler(Context activity)
	{
		nors = activity;
		
		// read polltime
		polltime  = HandlerManager.readRMS_i("PhoneSensorsHandler::OrientationPoll", 5) * 1000;
		polltime2 = HandlerManager.readRMS_i("PhoneSensorsHandler::ProximityPoll", 5) * 1000;
		
		// try to open sensor services
		try
		{
			sensorManager = (SensorManager)nors.getSystemService(Context.SENSOR_SERVICE);
			Orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			Proximity 	= sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
			Light		= sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
			Pressure	= sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
			sensor_enable = true;
			// arm semaphores
			wait(pressure_semaphore); 
			wait(light_semaphore); 
			wait(proximity_semaphore); 
			wait(azimuth_semaphore); 
			wait(roll_semaphore); 
			wait(pitch_semaphore); 
		}
		catch(Exception e)
		{
			sensor_enable = false;
		}
	}
	
	public void destroyHandler()
	{
		if (startedLight == true || startedProximity == true || startedOrientation == true || startedPressure == true)
			sensorManager.unregisterListener(sensorlistener);	
	}

	// The Handler that gets information back from the other threads, initializing phone sensors
	// We use a handler here to allow for the Acquire() function, which runs in a different thread, to issue an initialization of the invidiaul sensors
	// since registerListener() can only be called from the main Looper thread!!
	public final Handler mHandler = new Handler() 
    {
       @Override
       public void handleMessage(Message msg) 
       {        	
           switch (msg.what) 
           {
           case INIT_PRESSURE:
        	   startedPressure = sensorManager.registerListener(sensorlistener, Pressure, SensorManager.SENSOR_DELAY_NORMAL);
	           break;  
           case INIT_LIGHT:
        	   startedLight = sensorManager.registerListener(sensorlistener, Light, SensorManager.SENSOR_DELAY_NORMAL);
	           break;  
           case INIT_PROXIMITY:
        	   startedProximity = sensorManager.registerListener(sensorlistener, Proximity, SensorManager.SENSOR_DELAY_NORMAL);
	           break;  
           case INIT_ORIENTATION:
        	   startedOrientation = sensorManager.registerListener(sensorlistener, Orientation, SensorManager.SENSOR_DELAY_NORMAL);
	           break;  
           default:  
           	break;
           }
       }
    };

	
    private SensorEventListener sensorlistener = new SensorEventListener() 
    {
    	public void	 onSensorChanged(SensorEvent event)    
    	{
    		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION)
    		{
				 azimuth=event.values[0];
				 pitch=event.values[1];
				 roll=event.values[2];	

				 // now release the semaphores
				 azimuth_semaphore.release(); 
				 roll_semaphore.release(); 
				 pitch_semaphore.release(); 
    		}

    		if (event.sensor.getType() == Sensor.TYPE_PROXIMITY)
    		{
				 proximity=event.values[0];
				 // now release the semaphores
				 proximity_semaphore.release(); 
    		}
    		if (event.sensor.getType() == Sensor.TYPE_LIGHT)
    		{
				 light=event.values[0];
				 // now release the semaphores
				 light_semaphore.release(); 
    		}
    		if (event.sensor.getType() == Sensor.TYPE_PRESSURE)
    		{
				 pressure=event.values[0];
				 // now release the semaphores
				 pressure_semaphore.release(); 
    		}
       	}

		public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) 
		{
		}
    };  
}
