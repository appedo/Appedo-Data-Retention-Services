package com.appedo.retention.threads;

import java.sql.Connection;
import java.util.Calendar;
import java.util.TimerTask;

import com.appedo.commons.connect.DataBaseManager;
import com.appedo.retention.dbi.DataAggregatorDBI;

public class DataAggregatorTimerTask extends TimerTask {

	public Long lLastStarted15MinData = null;
	public Long lLastStarted30MinData = null;
	public Long lLastStarted1HrData = null;
	
	@Override
	public void run() {
		Connection con = null;
		
		Calendar calNow = Calendar.getInstance();
		
		try{
			
			System.out.println("DataAggregation timer task started...."+ calNow.getTime());
			con = DataBaseManager.giveConnection();
			
			DataAggregatorDBI.aggregateData(con, "5 MINUTES");
			DataBaseManager.commit(con);
			
			if(lLastStarted15MinData == null || (lLastStarted15MinData+900000 < calNow.getTimeInMillis())) {
				lLastStarted15MinData = calNow.getTimeInMillis();
				DataAggregatorDBI.aggregateData(con, "15 MINUTES");
				DataBaseManager.commit(con);
			}
			
			if(lLastStarted30MinData == null || (lLastStarted30MinData+1800000 < calNow.getTimeInMillis())) {
				lLastStarted30MinData = calNow.getTimeInMillis();
				DataAggregatorDBI.aggregateData(con, "30 MINUTES");
				DataBaseManager.commit(con);
			}
			
			if(lLastStarted1HrData == null || (lLastStarted1HrData+3600000 < calNow.getTimeInMillis())) {
				lLastStarted1HrData = calNow.getTimeInMillis();
				DataAggregatorDBI.aggregateData(con, "1 HOUR");
				DataBaseManager.commit(con);
			}
			
		} catch (Throwable th) {
			DataBaseManager.rollback(con);
			
			System.out.println("Exception in DataAggregateTimerTask(): "+th.getMessage());
			th.printStackTrace();
		} finally {
			DataBaseManager.close(con);
			con = null;
		}
	}
}
