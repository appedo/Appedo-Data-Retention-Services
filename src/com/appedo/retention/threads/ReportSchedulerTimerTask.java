package com.appedo.retention.threads;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import com.appedo.commons.connect.DataBaseManager;
import com.appedo.retention.dbi.SLADBI;
import com.appedo.retention.model.ReportSchedulerManager;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ReportSchedulerTimerTask extends TimerTask{

	public ReportSchedulerTimerTask() {
		// TODO Auto-generated constructor stub
	}

	public void run() {
		Connection con = null;
		ReportSchedulerManager rsManager = null;
		HashMap<String, JSONArray> hmReportList = null;
		JSONObject joResponse = null;
		try {
			
			con = DataBaseManager.giveConnection();
			rsManager = new ReportSchedulerManager();
			
			//hmReportList = SLADBI.getEmailSendReportList(con);
			
			ResultSet rst = SLADBI.getEmailSendReportListV1(con);
			
			hmReportList = rsManager.getEmailSendReportList(rst);
			
			for (Map.Entry<String, JSONArray> entry : hmReportList.entrySet()) {
				joResponse = rsManager.sendReportSchedulerEmail(con, entry.getKey(), entry.getValue());
				
				if(!joResponse.isNullObject() && joResponse.getBoolean("bEmailSent")) {
					SLADBI.updateLastSendTime(con, entry.getValue(), joResponse.getLong("lEndTime"));
				}
			}
			
			
		}catch (Throwable th) {
			System.out.println("Exception in ReportSchedulerTimerTask(): "+th.getMessage());
			th.printStackTrace();
		} finally {
			DataBaseManager.close(con);
			con = null;
		}
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
