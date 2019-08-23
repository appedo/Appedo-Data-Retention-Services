package com.appedo.retention.threads;

import java.sql.Connection;
import java.util.TimerTask;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.appedo.commons.connect.DataBaseManager;
import com.appedo.commons.utils.UtilsFactory;
import com.appedo.retention.dbi.SLADBI;
import com.appedo.retention.model.SLAManager;

public class ConsolidatedSLAEmailTimerTask extends TimerTask {

	@Override
	public void run() {
		SLAManager slaManager = null;
		
		JSONArray jaUsers = null;
		JSONObject joUser = null;
		Connection con = null;
		
		try{
			slaManager = new SLAManager();
			
			con = DataBaseManager.giveConnection();
			
			jaUsers = SLADBI.getSLABreachedUserList(con);
			System.out.println("Total Breached Users : "+jaUsers.size());
			for( int i=0; i<jaUsers.size(); i++ ) {
				joUser = jaUsers.getJSONObject(i);
				
				slaManager.sendInternalMonConsolidatedSLAEmail(con, joUser.getLong("user_id"), joUser.getBoolean("oad_sla_breached"), joUser.getBoolean("log_sla_breached"), joUser.getInt("alert_freq_min"));
			}
			
			DataBaseManager.commit(con);
			
		} catch (Throwable th) {
			System.out.println("Exception in ConsolidatedSLAEmailTimerTask(): "+th.getMessage());
			th.printStackTrace();
		} finally {
			UtilsFactory.clearCollectionHieracy( jaUsers );
			
			DataBaseManager.close(con);
			con = null;
		}
	}
}
