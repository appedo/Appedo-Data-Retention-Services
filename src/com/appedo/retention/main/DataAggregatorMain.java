package com.appedo.retention.main;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.appedo.retention.threads.DataAggregatorTimerTask;
import com.appedo.retention.utils.Constants;

public class DataAggregatorMain {
	
	public static TimerTask timerTaskDataAggregator = null, timerTaskUserSetup = null;
	public static Timer timerDataAggregator = new Timer(), timerUserSetup = new Timer();
	
	public static void main(String[] args) {
		
		try {
			// loads constants, log, DB, mail properties
			Constants.loadInitProperties("Appedo-Data-Aggreagtion-Service", false /* DB-Auto-Commit */);
			
			timerTaskDataAggregator = new DataAggregatorTimerTask();
			timerDataAggregator.schedule(timerTaskDataAggregator, findNext5thMinute(), 5*60*1000);
			
			//timerDataAggregator.schedule(timerTaskDataAggregator, 0, 1*60*1000);
			
		} catch (Throwable th) {
			System.out.println("Exception in DataAggregatorMain Main: "+th.getMessage());
			th.printStackTrace();
		}
	}
	
	private static Date findNext5thMinute() {
		int nMinutesForNearest5th = 0;
		
		Calendar calNow = Calendar.getInstance();
		//nMinutesForNearest5th = 5 - calNow.get( Calendar.MINUTE ) % 5;
		nMinutesForNearest5th = (((calNow.get( Calendar.MINUTE )/5) + 1) * 5);
				
		System.out.println(calNow.get( Calendar.MINUTE ) + nMinutesForNearest5th);
		
		calNow.set(Calendar.MINUTE, nMinutesForNearest5th);
		calNow.set(Calendar.SECOND, 0);
		calNow.set(Calendar.MILLISECOND, 0);
		
		System.out.println("Data Aggregation starting at : "+calNow.getTime());
		
		return calNow.getTime();
	}
}
