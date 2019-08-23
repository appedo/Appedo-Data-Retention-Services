package com.appedo.retention.dbi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.appedo.commons.connect.DataBaseManager;
import com.appedo.commons.utils.UtilsFactory;
import com.appedo.manager.LogManager;
import com.appedo.utils.HumanReadableFormatter;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class SLADBI {
	
	/**
	 * Get all the users, for whom SLA-Policy is marked as breached in <b>user_pvt_table</b>.
	 * 
	 * @param con
	 * @return
	 * @throws Throwable
	 */
	public static JSONArray getSLABreachedUserList(Connection con) throws Throwable {
		JSONArray jaUsers = new JSONArray();
		JSONObject joUser = null;
		
		StringBuilder sbQuery = new StringBuilder();
		PreparedStatement pstmt = null;
		ResultSet rst = null;
		
		try{
			sbQuery.append("SELECT * FROM get_sla_policy_breached_users()");
			pstmt = con.prepareStatement( sbQuery.toString() );
			rst = pstmt.executeQuery();
			
			while( rst.next() ) {
				joUser = new JSONObject();
				joUser.put("user_id", rst.getLong("user_id"));
				joUser.put("oad_sla_breached", rst.getBoolean("oad_sla_breached"));
				joUser.put("log_sla_breached", rst.getBoolean("log_sla_breached"));
				joUser.put("alert_freq_min", rst.getInt("trigger_alert_every_in_min"));
				
				jaUsers.add( joUser );
			}
			
		} catch(Throwable th) {
			throw th;
		} finally {
			DataBaseManager.close(rst);
			rst = null;
			
			DataBaseManager.close(pstmt);
			pstmt = null;
		}
		
		return jaUsers;
	}
	
	/***
	 * 
	 * get all the Reports, scheduling for user set alert frequency hours.
	 * 
	 * @param con
	 * @return
	 * @throws Throwable
	 */
	public static JSONArray getReportList(Connection con) throws Throwable {
		JSONArray jaReports = new JSONArray();
		JSONObject joReport = null;
		
		StringBuilder sbQuery = new StringBuilder();
		PreparedStatement pstmt = null;
		ResultSet rst = null;
		
		try{
			sbQuery.append("SELECT * FROM report_scheduler_setting");
			pstmt = con.prepareStatement( sbQuery.toString() );
			rst = pstmt.executeQuery();
			
			while( rst.next() ) {
				joReport = new JSONObject();
				joReport.put("user_id", rst.getLong("user_id"));
				joReport.put("alert_freq_hour", rst.getInt("alert_frequency_hour"));
				joReport.put("report_name", rst.getString("report_name"));
				joReport.put("chart_id", rst.getLong("chart_id"));
				joReport.put("attachment_format", rst.getString("attachment_format"));
				joReport.put("subject", rst.getString("subject"));
				joReport.put("last_send_time", rst.getString("last_send_time"));
				
				jaReports.add( joReport );
			}
			
		} catch(Throwable th) {
			throw th;
		} finally {
			DataBaseManager.close(rst);
			rst = null;
			
			DataBaseManager.close(pstmt);
			pstmt = null;
		}
		
		return jaReports;
	}
	
	
	/***
	 * 
	 * get all the Reports, scheduling for user set alert frequency hours.
	 * 
	 * @param con
	 * @return
	 * @throws Throwable
	 */
	public static HashMap<String, JSONArray> getEmailSendReportList(Connection con) throws Throwable {
		JSONArray jaReports = null;
		JSONObject joReport = null;
		
		StringBuilder sbQuery = new StringBuilder();
		PreparedStatement pstmt = null;
		ResultSet rst = null;
		
		HashMap<String, JSONArray> hmReportList = new HashMap<String, JSONArray>();
		try{
			sbQuery	.append("Select * FROM ( select * , (CASE WHEN last_send_time IS NULL THEN now() - interval ")
					.append("'1 hour' *alert_frequency_hour ELSE last_send_time END) AS ls_time")
					.append(" from report_scheduler_setting ) AS a  WHERE a.ls_time + interval ")
					.append("'1 hour' *alert_frequency_hour  <= now()");
			pstmt = con.prepareStatement( sbQuery.toString() );
			rst = pstmt.executeQuery();
			
			while( rst.next() ) {
				
				joReport = new JSONObject();
				joReport.put("report_id", rst.getLong("report_id"));
				joReport.put("user_id", rst.getLong("user_id"));
				joReport.put("alert_freq_hour", rst.getInt("alert_frequency_hour"));
				joReport.put("report_name", rst.getString("report_name"));
				joReport.put("chart_id", rst.getLong("chart_id"));
				joReport.put("attachment_format", rst.getString("attachment_format"));
				joReport.put("subject", rst.getString("subject"));
				joReport.put("send_as_attachment", rst.getBoolean("send_as_attachment"));
				joReport.put("last_send_time", rst.getString("last_send_time"));
				joReport.put("send_to", rst.getString("send_to")== null ? "" : rst.getString("send_to"));
				
				String sent_to_email = rst.getString("send_to") == null ? "" : rst.getString("send_to");
				String user_id;
				if(sent_to_email.isEmpty()) {
					user_id = rst.getString("user_id");
				}else {
					user_id = "send_to_"+rst.getLong("report_id");
				}
				
				if(hmReportList.containsKey(user_id)) {					
					hmReportList.get(user_id).add(joReport);
				}else{
					jaReports = new JSONArray();
					jaReports.add( joReport );
					
					hmReportList.put(user_id, jaReports);
				}
			}
			
		} catch(Throwable th) {
			throw th;
		} finally {
			DataBaseManager.close(rst);
			rst = null;
			
			DataBaseManager.close(pstmt);
			pstmt = null;
		}
		
		//return jaReports;
		return hmReportList;
	}
	
	public static ResultSet getEmailSendReportListV1(Connection con) throws Throwable {
		StringBuilder sbQuery = new StringBuilder();
		PreparedStatement pstmt = null;
		ResultSet rst = null;
		try{
			
			sbQuery	.append("Select *, EXTRACT(EPOCH FROM a.ls_time) AS last_send_epoc_time FROM ( ")
					.append("select * , (CASE WHEN last_send_time IS NULL THEN now() - interval ")
					.append("'1 hour' *alert_frequency_hour ELSE last_send_time END) AS ls_time")
					.append(" from report_scheduler_setting ) AS a  WHERE a.ls_time + interval ")
					.append("'1 hour' *alert_frequency_hour  <= now() ORDER By report_id ");
			pstmt = con.prepareStatement( sbQuery.toString() );
			rst = pstmt.executeQuery();
			
		} catch(Throwable th) {
			throw th;
		} finally {
			/*DataBaseManager.close(pstmt);
			pstmt = null;*/
		}
		return rst;
	}
	
	public static void updateLastSendTime(Connection con, JSONArray jasentReportList, long lastSentTime) throws Throwable {
		
		HashSet<String> hsReportId = new HashSet<String>();
		StringBuilder sbQuery = new StringBuilder();
		PreparedStatement pstmt = null;
		
 		try {
			for( int i=0; i<jasentReportList.size(); i++ ) {
				 hsReportId.add(jasentReportList.getJSONObject(i).getString("report_id"));
			}
			
			sbQuery .append("update report_scheduler_setting set last_send_time = to_timestamp(")
					.append(lastSentTime).append(") WHERE report_id IN (")
					.append(hsReportId.toString().replace("[", "").replace("]", "")).append(")");
			
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.execute();
		} catch(Throwable th) {
			throw th;
		} finally {
			DataBaseManager.close(pstmt);
			pstmt = null;
		}
	}
	
	
	/**
	 * Check whether user is alerted, with Summary Email, in given Alert-Frequency.
	 * 
	 * @param con
	 * @param lUserID
	 * @param nAlertFreq
	 * @return
	 * @throws Throwable
	 */
	public boolean isUserAlerted(Connection con, long lUserID, int nAlertFreq) throws Throwable {
		PreparedStatement pstmt = null;
		ResultSet rst = null;
		
		StringBuilder sbQuery = new StringBuilder();
		
		boolean bAlerted = false;
		
		try {
			sbQuery .append("SELECT count(*) as alerted_count ")
					.append("FROM sla_alert_log_").append(lUserID).append(" sal ")
					.append("INNER JOIN ( SELECT DISTINCT sla_id FROM so_sla_counter ) AS ssc ON ssc.sla_id = sal.sla_id ")
					.append("WHERE sal.is_sent = true ")
					.append("AND sal.received_on > now() - (?||' min')::interval ");
			
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setInt(1, nAlertFreq);
			rst = pstmt.executeQuery();
			if( rst.next() ) {
				bAlerted = rst.getLong("alerted_count") > 0;
			}
		} catch (Throwable th) {
			throw th;
		} finally {
			DataBaseManager.close(rst);
			rst = null;
			
			DataBaseManager.close(pstmt);
			pstmt = null;
			
			UtilsFactory.clearCollectionHieracy(sbQuery);
			sbQuery = null;
		}
		
		return bAlerted;
	}
	
	/**
	 * Get the list of SLA-Alerts, which are not sent to the user in last Summary-Email.
	 * 
	 * @param con
	 * @param lUserId
	 * @return
	 * @throws Throwable
	 */
	public JSONObject getUnsentOADBreachSummary(Connection con, long lUserId) throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rst = null;
		
		StringBuilder sbQuery = null;
		String strSLAName = null;
		
		JSONArray jaCounters = null;
		JSONObject joSLA = new JSONObject(), joCounter = null;
		
		Object[] aryValueUnit = null;
		
		try{
			sbQuery = new StringBuilder();
			sbQuery .append("SELECT * FROM get_unsent_breach_summary(?)");
					
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setLong(1, lUserId);
//			pstmt.setLong(2, lAlertLogId);
			rst = pstmt.executeQuery();
			
			while( rst.next() ) {
				strSLAName = rst.getString("sla_name");
				
				if( ! joSLA.containsKey(strSLAName) ) {
					joSLA.put(strSLAName, new JSONArray());
				}
				jaCounters = joSLA.getJSONArray(strSLAName);
				
				joCounter = new JSONObject();
				joCounter.put("max_alert_log_id", rst.getLong("max_alert_log_id"));
				
				joCounter.put("module_code", rst.getString("module_code"));
				joCounter.put("module_name", rst.getString("module_name"));
				joCounter.put("category", rst.getString("category"));
				if(rst.getString("process_name") != null &&  rst.getString("process_name").replaceAll("\"\"", "").trim().length() > 0) {
					joCounter.put("display_name", rst.getString("display_name")+" (\""+rst.getString("process_name")+"\")" );
				}else {
					joCounter.put("display_name", rst.getString("display_name"));
				}
				
				joCounter.put("unit", rst.getString("unit"));
				
				joCounter.put("warning_cnt", rst.getLong("warning_cnt"));
				joCounter.put("critical_cnt", rst.getLong("critical_cnt"));
				joCounter.put("cnt_breached", rst.getLong("cnt_breached"));
				
				aryValueUnit = HumanReadableFormatter.getHumanReadableFormat(rst.getDouble("warning_threshold_value"), rst.getString("unit"), false, false);
				joCounter.put("warning_threshold_value", aryValueUnit[0]);
				joCounter.put("warning_breached_unit", aryValueUnit[1]);
				
				aryValueUnit = HumanReadableFormatter.getHumanReadableFormat(rst.getDouble("critical_threshold_value"), rst.getString("unit"), false, false);
				joCounter.put("critical_threshold_value", aryValueUnit[0]);
				joCounter.put("critical_breached_unit", aryValueUnit[1]);
				
				aryValueUnit = HumanReadableFormatter.getHumanReadableFormat(rst.getDouble("min_breached"), rst.getString("unit"), false, false);
				joCounter.put("min_breached_value", aryValueUnit[0]);
				joCounter.put("min_breached_unit", aryValueUnit[1]);

				aryValueUnit = HumanReadableFormatter.getHumanReadableFormat(rst.getDouble("avg_breached"), rst.getString("unit"), false, false);
				joCounter.put("avg_breached_value", aryValueUnit[0]);
				joCounter.put("avg_breached_unit", aryValueUnit[1]);
				
				aryValueUnit = HumanReadableFormatter.getHumanReadableFormat(rst.getDouble("max_breached"), rst.getString("unit"), false, false);
				joCounter.put("max_breached_value", aryValueUnit[0]);
				joCounter.put("max_breached_unit", aryValueUnit[1]);
				
				jaCounters.add(joCounter);
			}
		} catch (Throwable th) {
			LogManager.errorLog(th, sbQuery);
			throw th;
		} finally {
			DataBaseManager.close(rst);
			rst = null;
			
			DataBaseManager.close(pstmt);
			pstmt = null;
			
			UtilsFactory.clearCollectionHieracy(sbQuery);
		}
		
		return joSLA;
	}
	
	/**
	 * Get the list of SLA-Alerts, which are not sent to the user in last Summary-Email.
	 * 
	 * @param con
	 * @param lUserId
	 * @return
	 * @throws Exception
	 */
	public JSONObject getUnsentLOGBreachSummary(Connection con, long lUserId) throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rst = null;
		
		StringBuilder sbQuery = null;
		String strSLAName = null;
		
		JSONArray jaCounters = null;
		JSONObject joSLA = new JSONObject(), joCounter = null;
		
		Object[] aryValueUnit = null;
		
		try{
			sbQuery = new StringBuilder();
			sbQuery .append("SELECT * FROM get_log_unsent_breach_summary(?)");
					
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setLong(1, lUserId);
//			pstmt.setLong(2, lAlertLogId);
			rst = pstmt.executeQuery();
			
			while( rst.next() ) {
				strSLAName = rst.getString("sla_name");
				
				if( ! joSLA.containsKey(strSLAName) ) {
					joSLA.put(strSLAName, new JSONArray());
				}
				jaCounters = joSLA.getJSONArray(strSLAName);
				
				joCounter = new JSONObject();
				joCounter.put("max_alert_log_id", rst.getLong("max_alert_log_id"));
				
				joCounter.put("module_code", rst.getString("module_code"));
				joCounter.put("module_name", rst.getString("module_name"));
				joCounter.put("log_grok_name", rst.getString("log_grok_name"));
				
				joCounter.put("warning_cnt", rst.getLong("warning_cnt"));
				joCounter.put("critical_cnt", rst.getLong("critical_cnt"));
				joCounter.put("cnt_breached", rst.getLong("cnt_breached"));
				
				joCounter.put("grok_column", UtilsFactory.replaceNull(rst.getString("grok_column"), "-"));
				joCounter.put("breach_pattern", UtilsFactory.replaceNull(rst.getString("breach_pattern"), "-"));
				joCounter.put("warning_threshold_value", UtilsFactory.replaceNull(rst.getString("warning_threshold_value"), "-"));
				joCounter.put("critical_threshold_value", UtilsFactory.replaceNull(rst.getString("critical_threshold_value"), "-"));
				
				jaCounters.add(joCounter);
			}
		} catch (Exception ex) {
			LogManager.errorLog(ex, sbQuery);
			throw ex;
		} finally {
			DataBaseManager.close(rst);
			rst = null;
			
			DataBaseManager.close(pstmt);
			pstmt = null;
			
			UtilsFactory.clearCollectionHieracy(sbQuery);
		}
		
		return joSLA;
	}
	
	/**
	 * Gets user's alert configured email_ids & mobile numbers
	 * If strAlertType = Email then return only EmailIds,
	 * If strAlertType = SMS then return only Mobile-Numbers,
	 * If strAlertType = "" then return all EmailIds and Mobile-Numbers.
	 * 
	 * @param con
	 * @param lSLAId
	 * @return
	 * @throws Throwable
	 */
	public ArrayList<HashMap<String, String>> getUserAlertToAddresses(Connection con, long lUserId, String strAlertType) throws Throwable {
		PreparedStatement pstmt = null;
		ResultSet rst = null;
		
		StringBuilder sbQuery = new StringBuilder();
		
		ArrayList<HashMap<String, String>> alAlertAddresses = null;
		HashMap<String, String> hmAlertAddress = null;
		
		try {
			alAlertAddresses = new ArrayList<HashMap<String, String>>();
			
			sbQuery .append("SELECT alert_type, email_mobile, telephone_code ")
					.append("FROM so_alert ")
					.append("WHERE is_valid = TRUE ")
					.append("AND user_Id = ? ")
					.append("AND alert_type = ? ");
			
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setLong(1, lUserId);
			pstmt.setString(2, strAlertType);
			rst = pstmt.executeQuery();
			
			while( rst.next() ) {
				hmAlertAddress = new HashMap<String, String>();
				hmAlertAddress.put("alertType", rst.getString("alert_type"));
				hmAlertAddress.put("emailMobile", rst.getString("email_mobile"));
				hmAlertAddress.put("telephoneCode", UtilsFactory.replaceNull(rst.getString("telephone_code"), ""));
				
				alAlertAddresses.add(hmAlertAddress);
			}
			
		} catch (Throwable th) {
			throw th;
		} finally {
			DataBaseManager.close(rst);
			rst = null;
			
			DataBaseManager.close(pstmt);
			pstmt = null;
			
			UtilsFactory.clearCollectionHieracy(sbQuery);
			sbQuery = null;
		}
		
		return alAlertAddresses;
	}
	
	/**
	 * Mark all the unsent <b>sla_alert_log_&lt;USER-ID&gt;</b> table entries with the latest entry's PK.
	 * Update the latest entry with email's content and sent-to-emailIds.
	 * 
	 * @param con
	 * @param lUserId
	 * @param strEmailIds
	 * @param strSentEmailContent
	 * @param lMaxAlertLogId
	 * @throws Throwable
	 */
	public void updateEmailsSentDetail(Connection con, long lUserId, String strEmailIds, String strSentEmailContent, long lMaxAlertLogId) throws Throwable {
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		
		try {
			sbQuery .append("UPDATE sla_alert_log_").append(lUserId).append(" AS sal SET is_sent = TRUE, is_success = TRUE, sent_alert_log_id = ? ")
					.append("FROM ( select sla_id from ( select distinct sla_id from so_sla_counter  union select distinct sla_id from so_sla_log) t ) AS ssc ")
					.append("WHERE ssc.sla_id = sal.sla_id AND sal.alert_log_id <= ? AND sal.is_sent = FALSE");
			
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setLong(1, lMaxAlertLogId);
			pstmt.setLong(2, lMaxAlertLogId);
			
			pstmt.execute();
			
			sbQuery.setLength(0);
			sbQuery .append("UPDATE sla_alert_log_").append(lUserId).append(" SET email_id = ?, alert_content_email = ? ")
					.append("WHERE alert_log_id = ? ");
			
			DataBaseManager.close(pstmt);
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setString(1, strEmailIds);
			pstmt.setString(2, strSentEmailContent);
			pstmt.setLong(3, lMaxAlertLogId);
			
			pstmt.execute();
		} catch(Throwable th) {
			LogManager.errorLog(th, sbQuery);
			throw th;
		} finally {
			DataBaseManager.close(pstmt);
			pstmt = null;
			
			UtilsFactory.clearCollectionHieracy(sbQuery);
			sbQuery = null;
		}
	}
}
