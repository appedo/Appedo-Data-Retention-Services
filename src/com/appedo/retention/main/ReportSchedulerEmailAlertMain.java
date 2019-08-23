package com.appedo.retention.main;

import java.util.Timer;
import java.util.TimerTask;

import com.appedo.manager.LogManager;
import com.appedo.retention.threads.ReportSchedulerTimerTask;
import com.appedo.retention.utils.Constants;

public class ReportSchedulerEmailAlertMain {

	public static TimerTask timerTaskReportSchedulerEmail = null;
	public static Timer timerReportSchedulerEmail = new Timer();
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {

			Constants.loadInitProperties("Report-Scheduler-Email-Alert", true);
		  
			LogManager.initializePropertyConfigurator(Constants.LOG4J_PROPERTIES_FILE);
  
			LogManager.infoLog("Report Schedular Email Service is starting ");
		  
			//new ReportSchedulerEmailThread();
			
			timerTaskReportSchedulerEmail = new ReportSchedulerTimerTask();
			timerReportSchedulerEmail.schedule(timerTaskReportSchedulerEmail, 1001, Constants.SCHEDULER_ALERT_SERVICE_RUNTIME_INTERVAL_MS);
			
		}catch (Throwable th) {
			 System.out.println("Exception in ReportSchedulerEmailAlertMain(): " + th.getMessage());
		     th.printStackTrace();// TODO: handle exception
		}
	}

}
