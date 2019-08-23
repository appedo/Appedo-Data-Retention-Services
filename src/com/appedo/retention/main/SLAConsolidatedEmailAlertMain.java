package com.appedo.retention.main;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.appedo.manager.LogManager;
import com.appedo.retention.threads.ConsolidatedSLAEmailTimerTask;
import com.appedo.retention.utils.Constants;

public class SLAConsolidatedEmailAlertMain {
	
	public static TimerTask timerTaskConsolidatedSLAEmail = null;
	public static Timer timerConsolidatedSLAEmail = new Timer();
	
	public static void main(String[] args) {
		
		try {
			// loads constants, log, DB, mail properties
			Constants.loadInitProperties("SLA-Consolidated-Email-Alert", false /* DB-Auto-Commit */);
			
			LogManager.initializePropertyConfigurator( Constants.LOG4J_PROPERTIES_FILE );
			
			LogManager.infoLog("Service will start on "+findNextMinute());
			
			timerTaskConsolidatedSLAEmail = new ConsolidatedSLAEmailTimerTask();
			timerConsolidatedSLAEmail.schedule(timerTaskConsolidatedSLAEmail, findNextMinute(), Constants.SLA_ALERT_SERVICE_RUNTIME_INTERVAL_MS);
			
		} catch (Throwable th) {
			System.out.println("Exception in ConsolidatedSLAEmailMain Main: "+th.getMessage());
			th.printStackTrace();
		}
	}
	
	private static Date findNextMinute() {
		int nMinutes = 0;
		
		Calendar calNow = Calendar.getInstance();
		nMinutes = calNow.get( Calendar.MINUTE );
		
		calNow.set(Calendar.MINUTE, nMinutes + 1);
		calNow.set(Calendar.SECOND, 0);
		calNow.set(Calendar.MILLISECOND, 0);
		
		return calNow.getTime();
	}
}
