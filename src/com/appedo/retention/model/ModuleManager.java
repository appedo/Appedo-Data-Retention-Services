package com.appedo.retention.model;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.appedo.commons.connect.DataBaseManager;
import com.appedo.commons.manager.AppedoMailer;
import com.appedo.commons.manager.AppedoMailer.MODULE_ID;
import com.appedo.commons.utils.UtilsFactory;
import com.appedo.manager.LogManager;
import com.appedo.retention.dbi.ModuleDBI;
import com.appedo.retention.utils.Constants;

public class ModuleManager {
	
	
	/**
	 * delete the profiler data, 
	 * prepare the data log and send in mail
	 *   DONT USE (hmDataRetentionMailDetails = null OR hmDataRetentionMailDetails = new  HashMap<String, Object>()), since obj. by ref
	 *   
	 * @param con
	 * @throws Throwable
	 */
	public void deleteProfilerData(Connection con, HashMap<String, Object> hmDataRetentionMailDetails) throws Throwable {
		Statement stmtProfilerDataRetention = null;
		ResultSet rstProfilerDataRetention = null;
		
		String[] saRtnDataLog = null;
		
		String strAllProfilerData = "", strUserWiseData = "";
		
		ModuleDBI moduleDBI = null;
		
		try {
			moduleDBI = new ModuleDBI();
			
			// deletes module's profiler table data
			stmtProfilerDataRetention = con.createStatement();
			rstProfilerDataRetention = moduleDBI.deleteModuleProfilerPartitionsData(stmtProfilerDataRetention);
			
			LogManager.infoLog("Profiler data deleted from database.");
			
			// prepare log
			saRtnDataLog = prepareProfilerDataRetentionLog(rstProfilerDataRetention);
			strAllProfilerData = saRtnDataLog[0];
			strUserWiseData = saRtnDataLog[1];
			
			// writes the data into file
			writeAllDataIntoFile(strAllProfilerData, "profiler_data_retention_"+UtilsFactory.nowFormattedDateyyyyMMdd_HHmmss()+".log");
			
			// sets Profiler data retention log for mail
			setProfilerRetentionMailDetails(strUserWiseData, hmDataRetentionMailDetails);
			
		} catch (Throwable th) {
			//bExceptionOccurred = true;
			//strExceptionMessage = "An exception occurred while data retention:<BR>"+e.getMessage(); 
			throw th;
		} finally {
			/* all data retention mail, say SUM, Profiler & ASD, are send in single mail, ignored each modules separate mail
			// send mail
			sendMail(MODULE_ID.PROFILER_DATA_RETENTION, strUserWiseData, bExceptionOccurred ? strExceptionMessage : null, "Profiler data retention on "+UtilsFactory.nowYYYYMMDD());
			*/
			
			DataBaseManager.close(rstProfilerDataRetention);
			rstProfilerDataRetention = null;
			
			DataBaseManager.close(stmtProfilerDataRetention);
			stmtProfilerDataRetention = null;
			
			saRtnDataLog = null;
			
			strAllProfilerData = null;
			strUserWiseData = null;
			
			moduleDBI = null;
		}
	}
	
	/**
	 * prepare the data retention log, 
	 *   to writes into file and send in mail
	 * 
	 * @param rstProfilerDataRetention
	 * @return
	 * @throws Throwable
	 */
	private String[] prepareProfilerDataRetentionLog(ResultSet rstProfilerDataRetention) throws Throwable {
		StringBuilder sbData = new StringBuilder();
		String strUidTotalRowsAffected = "", strUserWiseData = "";
		
		LinkedHashMap<String, HashMap<String, Object>> lhmUserProfilerModules = new LinkedHashMap<String, HashMap<String, Object>>();
		HashMap<String, Object> hmModuleData = null;
		
		long lUserId = -1L, lTotalProfilerModules = 0;
		
		try {
			sbData	.append("Profiler data retention on "+UtilsFactory.nowFormattedDateWithTimeZone()).append("\n\n");
			
			sbData	.append("user_id,email_id,uid,guid,counter_type_name,total_rows_affected\n")
					.append("---------------------------------------------------------------\n");
			
			while( rstProfilerDataRetention.next() ) {
				lUserId = rstProfilerDataRetention.getLong("user_id");
				
				// all data
				sbData	.append(lUserId).append(",")
						.append(rstProfilerDataRetention.getString("email_id")).append(",")
						.append(rstProfilerDataRetention.getString("uid")).append(",")
						.append(rstProfilerDataRetention.getString("guid")).append(",")
						.append(rstProfilerDataRetention.getString("counter_type_name")).append(",")
						.append(rstProfilerDataRetention.getString("total_rows_affected")).append("\n");
				
				// group data based on `user_id`
				if ( lhmUserProfilerModules.containsKey( lUserId+"" )) {
					hmModuleData = lhmUserProfilerModules.get( lUserId+"" );
					
					lTotalProfilerModules = Long.parseLong(hmModuleData.get("total_profiler_modules")+"");
					strUidTotalRowsAffected = hmModuleData.get("uid_total_rows_affected").toString();
				} else {
					hmModuleData = new HashMap<String, Object>();
					
					// obj by ref
					lhmUserProfilerModules.put(lUserId+"", hmModuleData);
					lTotalProfilerModules = 0;
					strUidTotalRowsAffected = "";
				}
				lTotalProfilerModules = lTotalProfilerModules + 1;
				// retention data 
				strUidTotalRowsAffected += rstProfilerDataRetention.getString("uid")+"="+rstProfilerDataRetention.getString("total_rows_affected")+";";	
				
				hmModuleData.put("total_profiler_modules", lTotalProfilerModules);
				hmModuleData.put("uid_total_rows_affected", strUidTotalRowsAffected);
			}
			
			// format data to send in mail
			strUserWiseData = formatHTMLProfilerUserWiseData(lhmUserProfilerModules);
		} catch (Throwable th) {
			throw th;
		} finally {
			UtilsFactory.clearCollectionHieracy(lhmUserProfilerModules);
			lhmUserProfilerModules = null;
		}
		
		return new String[]{sbData.toString(), strUserWiseData};
	}
	
	/**
	 * writes the data into file/system
	 * 
	 * @param strData
	 * @throws Throwable
	 */
	public void writeAllDataIntoFile(String strData, String strFileName) throws Throwable {
		String strWriteDataIntoFilePath = "";
		
		try {
			strWriteDataIntoFilePath = Constants.THIS_JAR_PATH+"/logs/"+strFileName;
			UtilsFactory.writeDataIntoFile(strWriteDataIntoFilePath, strData);
			
			LogManager.infoLog("Data retention log written into the path: `"+strWriteDataIntoFilePath+"`");
		} catch (Throwable th) {
			throw th;
		} finally {
			strWriteDataIntoFilePath = null;
		}
	}
	
	/**
	 * send the data retention in mail
	 * 
	 * @param strUserWiseData
	 * @throws Throwable
	 */
	public void sendMail(MODULE_ID moduleId, String strUserWiseData, String strException, String strSubject) throws Throwable {
		HashMap<String, Object> hmMailDetails = null;
		
		AppedoMailer appedoMailer = null;
		
		try {
			appedoMailer = new AppedoMailer( Constants.EMAIL_TEMPLATES_PATH );
			
			// send mail
			hmMailDetails = new HashMap<String, Object>();
			//hmMailDetails.put("USERNAME", "DevOps");
			hmMailDetails.put("TIME", UtilsFactory.nowFormattedDateWithTimeZone());
			hmMailDetails.put("DATA_RETENTION_LOG", strUserWiseData);
			if ( strException != null && strException.length() > 0 ) {
				hmMailDetails.put("ERROR", strException);
			}
			appedoMailer.sendMail(moduleId, hmMailDetails, Constants.DATA_RETENTION_MAIL_TO_ADDRESSES.split(","), strSubject);
			
			LogManager.infoLog("Mail Sent ");
		} catch (Throwable th) {
			throw th;
		} finally {
			UtilsFactory.clearCollectionHieracy(hmMailDetails);
			hmMailDetails = null;
			
			strSubject = null;
			
			appedoMailer = null;
		}
	}
	
	/**
	 * format in HTML, the data to send in mail
	 * 
	 * @param lhmUserProfilerModules
	 * @return
	 * @throws Throwable
	 */
	private String formatHTMLProfilerUserWiseData(LinkedHashMap<String, HashMap<String, Object>> lhmUserProfilerModules) throws Throwable {
		StringBuilder sbData = new StringBuilder();
		String strUserId = "";
		
		HashMap<String, Object> hmModuleData = null;
		
		try {
			sbData	.append("user_id,total_profiler_modules,uid_total_rows_affected<BR>")
					.append("------------------------------------------------------<BR>");
			
			Iterator<Map.Entry<String, HashMap<String, Object>>> itUserProfilerModules = lhmUserProfilerModules.entrySet().iterator();
			while (itUserProfilerModules.hasNext()) {
				Map.Entry<String, HashMap<String, Object>> entry = itUserProfilerModules.next();
				strUserId = entry.getKey();
				hmModuleData = entry.getValue();
				
				sbData	.append(strUserId).append(",")
						.append(hmModuleData.get("total_profiler_modules")).append(",")
						.append(hmModuleData.get("uid_total_rows_affected")).append("<BR>");
			}
			
		} catch (Throwable th) {
			throw th;
		}
		
		return sbData.toString();
	}
	
	/**
	 * sets Profiler data retention, mail details
	 * 
	 * @param strMailRetentionLog
	 * @param hmMailDetails
	 * @throws Throwable
	 */
	private void setProfilerRetentionMailDetails(String strMailRetentionLog, HashMap<String, Object> hmMailDetails) throws Throwable {
		try {
			hmMailDetails.put("PROFILER_DATA_RETENTION_TIME", UtilsFactory.nowFormattedDateWithTimeZone());
			hmMailDetails.put("PROFILER_DATA_RETENTION_LOG", strMailRetentionLog);
		} catch (Throwable th) {
			throw th;
		}
	}
	
	
	/**
	 * deletes ASD's, `collector_<uid>`, mapped SLAs breach data from `so_threshold_breach_<user_id>`, `sla_alert_log_<user_id>`,  `<type>_slowquery_<uid>` & `mssql_slowprocedure_<uid>` tables data, 
	 *   based on user's month's max license data retention days used;
	 *   say user's current month license has `level2` and degraded as `level0`, based on `level2`'s data retention days, data deletes from partitions tables
	 * 
	 *   DONT USE (hmDataRetentionMailDetails = null OR hmDataRetentionMailDetails = new HashMap<String, Object>()), since obj. by ref.
	 * 
	 * @param con
	 * @throws Throwable
	 */
	public void deleteASDCollectorData(Connection con, HashMap<String, Object> hmDataRetentionMailDetails) throws Throwable {
		Statement stmtASDDataRetention = null;
		ResultSet rstASDDataRetention = null;
		
		String[] saRtnDataLog = null;
		
		String strAllASDData = "", strUserWiseData = "";
		
		ModuleDBI moduleDBI = null;
		
		try {
			moduleDBI = new ModuleDBI();
			
			// deletes module's profiler table data
			stmtASDDataRetention = con.createStatement();
			rstASDDataRetention = moduleDBI.deleteModuleASDPartitionsData(stmtASDDataRetention);
			
			LogManager.infoLog("ASD's collector, slow qry & slow procedure; data deleted from database.");
			
			// prepare log
			saRtnDataLog = prepareASDDataRetentionLog(rstASDDataRetention);
			strAllASDData = saRtnDataLog[0];
			strUserWiseData = saRtnDataLog[1];
			
			// writes the data into file
			writeAllDataIntoFile(strAllASDData, "asd_data_retention_"+UtilsFactory.nowFormattedDateyyyyMMdd_HHmmss()+".log");
			//writeAllDataIntoFile(strUserWiseData, "asd_data_retention_test_"+UtilsFactory.nowFormattedDateyyyyMMdd_HHmmss()+".log");
			
			// sets ASD data retention log for mail
			setRetentionMailDetails("ASD",strUserWiseData, hmDataRetentionMailDetails);
			
		} catch (Throwable th) {
			//bExceptionOccurred = true;
			//strExceptionMessage = "An exception occurred while data retention:<BR>"+e.getMessage();
			throw th;
		} finally {
			/* all data retention mail, say SUM, Profiler & ASD, are send in single mail, ignored each modules separate mail
			// send mail
			sendMail(MODULE_ID.ASD_DATA_RETENTION, strUserWiseData, bExceptionOccurred ? strExceptionMessage : null, "ASD data retention on "+UtilsFactory.nowYYYYMMDD());
			*/
			
			DataBaseManager.close(rstASDDataRetention);
			rstASDDataRetention = null;
			
			DataBaseManager.close(stmtASDDataRetention);
			stmtASDDataRetention = null;
			
			saRtnDataLog = null;
			
			strAllASDData = null;
			strUserWiseData = null;
			
			moduleDBI = null;
		}
	}
	
	/**
	 * deletes LOG's, `collector_<uid>_<DAILY_PARTITION>` tables, 
	 *   based on user's month's max license data (following OAD's license) retention days used;
	 *   say user's current month license has `level2` and degraded as `level0`, based on `level2`'s data retention days, data deletes from partitions tables
	 * 
	 *   DONT USE (hmDataRetentionMailDetails = null OR hmDataRetentionMailDetails = new HashMap<String, Object>()), since obj. by ref.
	 * 
	 * @param con
	 * @throws Throwable
	 */
	public void deleteLOGCollectorData(Connection con, HashMap<String, Object> hmDataRetentionMailDetails) throws Throwable {
		Statement stmtLOGDataRetention = null;
		ResultSet rstLOGDataRetention = null;
		
		String[] saRtnDataLog = null;
		
		String strAllLOGData = "", strUserWiseData = "";
		
		ModuleDBI moduleDBI = null;
		
		try {
			moduleDBI = new ModuleDBI();
			
			// deletes module's profiler table data
			stmtLOGDataRetention = con.createStatement();
			rstLOGDataRetention = moduleDBI.deleteModuleLOGPartitionsData(stmtLOGDataRetention);
			
			LogManager.infoLog("LOG's collector; data deleted from database.");
			
			// prepare log
			saRtnDataLog = prepareLOGDataRetentionLog(rstLOGDataRetention);
			strAllLOGData = saRtnDataLog[0];
			strUserWiseData = saRtnDataLog[1];
			
			// writes the data into file
			writeAllDataIntoFile(strAllLOGData, "log_data_retention_"+UtilsFactory.nowFormattedDateyyyyMMdd_HHmmss()+".log");
			//writeAllDataIntoFile(strUserWiseData, "asd_data_retention_test_"+UtilsFactory.nowFormattedDateyyyyMMdd_HHmmss()+".log");
			
			// sets LOG data retention log for mail
			setRetentionMailDetails("LOG",strUserWiseData, hmDataRetentionMailDetails);
			
		} catch (Throwable th) {
			//bExceptionOccurred = true;
			//strExceptionMessage = "An exception occurred while data retention:<BR>"+e.getMessage();
			throw th;
		} finally {
			/* all data retention mail, say SUM, Profiler & ASD, are send in single mail, ignored each modules separate mail
			// send mail
			sendMail(MODULE_ID.ASD_DATA_RETENTION, strUserWiseData, bExceptionOccurred ? strExceptionMessage : null, "ASD data retention on "+UtilsFactory.nowYYYYMMDD());
			*/
			
			DataBaseManager.close(rstLOGDataRetention);
			rstLOGDataRetention = null;
			
			DataBaseManager.close(stmtLOGDataRetention);
			stmtLOGDataRetention = null;
			
			saRtnDataLog = null;
			
			strAllLOGData = null;
			strUserWiseData = null;
			
			moduleDBI = null;
		}
	}
	
	/**
	 * prepares ASD data retention log
	 *   to writes into file and send in mail
	 *   
	 * @param rstASDDataRetention
	 * @return
	 * @throws Throwable
	 */
	private String[] prepareASDDataRetentionLog(ResultSet rstASDDataRetention) throws Throwable {
		StringBuilder sbData = new StringBuilder();
		String strUidTotalRowsAffected = "", strUidSlowQryTotalRowsAffected = "", strUidSlowProcedureTotalRowsAffected = "", strUserUidSLABreachTotalRowsAffected = "", strUserUidSLAAlertTotalRowsAffected = "";
		String strUserWiseData = "", strCounterTypeName = "";
		String strDroppedCollectorTable = "", strDroppedSlowQryTable = "", strDroppedSlowProcedure = "";
		
		LinkedHashMap<String, HashMap<String, Object>> lhmUserASDModules = new LinkedHashMap<String, HashMap<String, Object>>();
		HashMap<String, Object> hmModuleData = null;
		
		long lUserId = -1L, lUid = -1L, lTotalModules = 0;
		
		try {
			sbData	.append("ASD data retention on "+UtilsFactory.nowFormattedDateWithTimeZone()).append("\n\n");
			
			sbData	.append("user_id,email_id,uid,guid,module_code,module_name,counter_type_name,agent_version,date_retention_in_days,")
					.append("has_slow_query,has_slow_procedure,collector_total_rows_affected,slow_qry_total_rows_affected,slow_procedure_total_rows_affected")
					.append("so_threshold_breach_total_rows_affected,sla_alert_log_total_rows_affected, collector_dropped_table,")
					.append("slow_qry_dropped_table, slow_procedure_dropped_table \n")
					.append("---------------------------------------------------------------------------------------------------------------------------------\n");
			
			while( rstASDDataRetention.next() ) {
				lUserId = rstASDDataRetention.getLong("user_id");
				lUid = rstASDDataRetention.getLong("uid");
				strCounterTypeName = rstASDDataRetention.getString("counter_type_name");
				
				// all data
				sbData	.append(lUserId).append(",")
						.append(rstASDDataRetention.getString("email_id")).append(",")
						.append(lUid).append(",")
						.append(rstASDDataRetention.getString("guid")).append(",")
						.append(rstASDDataRetention.getString("module_code")).append(",")
						.append(rstASDDataRetention.getString("module_name")).append(",")
						.append(strCounterTypeName).append(",")
						.append(rstASDDataRetention.getString("agent_version")).append(",")
						.append(rstASDDataRetention.getString("date_retention_in_days")).append(",")
						.append(rstASDDataRetention.getBoolean("has_slow_query")).append(",")
						.append(rstASDDataRetention.getBoolean("has_slow_procedure")).append(",")
						.append(rstASDDataRetention.getString("collector_total_rows_affected")).append(",")
						.append(rstASDDataRetention.getString("slow_qry_total_rows_affected")).append(",")
						.append(rstASDDataRetention.getString("slow_procedure_total_rows_affected")).append(",")
						.append(rstASDDataRetention.getString("so_threshold_breach_total_rows_affected")).append(",")
						.append(rstASDDataRetention.getString("sla_alert_log_total_rows_affected"))
						.append(rstASDDataRetention.getString("collector_dropped_table")).append(",")
						.append(rstASDDataRetention.getString("slow_qry_dropped_table")).append(",")
						.append(rstASDDataRetention.getString("slow_procedure_dropped_table"))
						.append("\n");
				
				// group data based on `user_id`
				if ( lhmUserASDModules.containsKey( lUserId+"" )) {
					hmModuleData = lhmUserASDModules.get( lUserId+"" );
					
					lTotalModules = Long.parseLong(hmModuleData.get("total_modules")+"");
					strUidTotalRowsAffected = hmModuleData.get("collector_uid_total_rows_affected").toString();
					strUidSlowQryTotalRowsAffected = hmModuleData.get("slow_query_uid_total_rows_affected").toString();
					strUidSlowProcedureTotalRowsAffected = hmModuleData.get("slow_procedure_uid_total_rows_affected").toString();
					strUserUidSLABreachTotalRowsAffected = hmModuleData.get("uid_so_threshold_breach_total_rows_affected").toString();
					strUserUidSLAAlertTotalRowsAffected = hmModuleData.get("uid_sla_alert_log_total_rows_affected").toString();
					strDroppedCollectorTable = hmModuleData.get("collector_dropped_table").toString();
					strDroppedSlowQryTable = hmModuleData.get("slow_qry_dropped_table").toString();
					strDroppedSlowProcedure = hmModuleData.get("slow_procedure_dropped_table").toString();
				} else {
					hmModuleData = new HashMap<String, Object>();
					
					// obj by ref
					lhmUserASDModules.put(lUserId+"", hmModuleData);
					lTotalModules = 0;
					strUidTotalRowsAffected = "";
					strUidSlowQryTotalRowsAffected = "";
					strUidSlowProcedureTotalRowsAffected = "";
					strUserUidSLABreachTotalRowsAffected = "";
					strUserUidSLAAlertTotalRowsAffected = "";
					strDroppedCollectorTable = "";
					strDroppedSlowQryTable = "";
					strDroppedSlowProcedure = "";
				}
				lTotalModules = lTotalModules + 1;
				// retention data 
				strUidTotalRowsAffected += lUid+"="+rstASDDataRetention.getString("collector_total_rows_affected")+";";
				strDroppedCollectorTable += rstASDDataRetention.getString("collector_dropped_table");
				strUserUidSLABreachTotalRowsAffected += lUid+"="+rstASDDataRetention.getString("so_threshold_breach_total_rows_affected")+";";
				strUserUidSLAAlertTotalRowsAffected += lUid+"="+rstASDDataRetention.getString("sla_alert_log_total_rows_affected")+";";
				
				// slow query retention data
				if ( rstASDDataRetention.getBoolean("has_slow_query") ) {
					strUidSlowQryTotalRowsAffected += strCounterTypeName+"_s_qry_"+lUid+"="+rstASDDataRetention.getString("slow_qry_total_rows_affected")+";";
					strDroppedSlowQryTable += rstASDDataRetention.getString("slow_qry_dropped_table");
				}
				// slow procedure retention data
				if ( rstASDDataRetention.getBoolean("has_slow_procedure") ) {
					strUidSlowProcedureTotalRowsAffected += strCounterTypeName+"_s_proc_"+lUid+"="+rstASDDataRetention.getString("slow_procedure_total_rows_affected")+";";
					strDroppedSlowProcedure += rstASDDataRetention.getString("slow_procedure_dropped_table");
				}
				
				hmModuleData.put("total_modules", lTotalModules);
				hmModuleData.put("date_retention_in_days", rstASDDataRetention.getString("date_retention_in_days"));
				hmModuleData.put("collector_uid_total_rows_affected", strUidTotalRowsAffected);
				hmModuleData.put("slow_query_uid_total_rows_affected", strUidSlowQryTotalRowsAffected);
				hmModuleData.put("slow_procedure_uid_total_rows_affected", strUidSlowProcedureTotalRowsAffected);
				hmModuleData.put("uid_so_threshold_breach_total_rows_affected", strUserUidSLABreachTotalRowsAffected);
				hmModuleData.put("uid_sla_alert_log_total_rows_affected", strUserUidSLAAlertTotalRowsAffected);
				hmModuleData.put("collector_dropped_table", strDroppedCollectorTable);
				hmModuleData.put("slow_qry_dropped_table", strDroppedSlowQryTable);
				hmModuleData.put("slow_procedure_dropped_table", strDroppedSlowProcedure);
			}
			
			// format data to send in mail
			strUserWiseData = formatHTMLASDUserWiseData(lhmUserASDModules);
		} catch (Throwable th) {
			throw th;
		} finally {
			UtilsFactory.clearCollectionHieracy(lhmUserASDModules);
			lhmUserASDModules = null;
			
			strUidTotalRowsAffected = null;
			strUidSlowQryTotalRowsAffected = null;
			strUidSlowProcedureTotalRowsAffected = null;
			strCounterTypeName = null;
		}
		
		return new String[]{sbData.toString(), strUserWiseData};
	}
	
	/**
	 * prepares LOG data retention log
	 *   to writes into file and send in mail
	 *   
	 * @param rstLOGDataRetention
	 * @return
	 * @throws Throwable
	 */
	private String[] prepareLOGDataRetentionLog(ResultSet rstLOGDataRetention) throws Throwable {
		StringBuilder sbData = new StringBuilder();
		String strUidTotalRowsAffected = "";
		String strUserWiseData = "";
		String strDroppedCollectorTable = "";
		
		LinkedHashMap<String, HashMap<String, Object>> lhmUserLOGModules = new LinkedHashMap<String, HashMap<String, Object>>();
		HashMap<String, Object> hmModuleData = null;
		
		long lUserId = -1L, lUid = -1L, lTotalModules = 0;
		
		try {
			sbData	.append("LOG data retention on "+UtilsFactory.nowFormattedDateWithTimeZone()).append("\n\n");
			
			sbData	.append("user_id,email_id,uid,guid,module_code,module_name,date_retention_in_days,")
					.append("collector_total_rows_affected, collector_dropped_table \n")
					.append("---------------------------------------------------------------------------------------------------------------------------------\n");
			
			while( rstLOGDataRetention.next() ) {
				lUserId = rstLOGDataRetention.getLong("user_id");
				lUid = rstLOGDataRetention.getLong("uid");
				
				// all data
				sbData	.append(lUserId).append(",")
						.append(rstLOGDataRetention.getString("email_id")).append(",")
						.append(lUid).append(",")
						.append(rstLOGDataRetention.getString("guid")).append(",")
						.append(rstLOGDataRetention.getString("module_code")).append(",")
						.append(rstLOGDataRetention.getString("module_name")).append(",")
						.append(rstLOGDataRetention.getString("date_retention_in_days")).append(",")
						.append(rstLOGDataRetention.getString("collector_total_rows_affected")).append(",")
						.append(rstLOGDataRetention.getString("collector_dropped_table")).append(",")
						.append("\n");
				
				// group data based on `user_id`
				if ( lhmUserLOGModules.containsKey( lUserId+"" )) {
					hmModuleData = lhmUserLOGModules.get( lUserId+"" );
					
					lTotalModules = Long.parseLong(hmModuleData.get("total_modules")+"");
					strUidTotalRowsAffected = hmModuleData.get("collector_uid_total_rows_affected").toString();
					strDroppedCollectorTable = hmModuleData.get("collector_dropped_table").toString();
				} else {
					hmModuleData = new HashMap<String, Object>();
					
					// obj by ref
					lhmUserLOGModules.put(lUserId+"", hmModuleData);
					lTotalModules = 0;
					strUidTotalRowsAffected = "";
					strDroppedCollectorTable = "";
				}
				lTotalModules = lTotalModules + 1;
				// retention data 
				strUidTotalRowsAffected += lUid+"="+rstLOGDataRetention.getString("collector_total_rows_affected")+";";
				strDroppedCollectorTable += rstLOGDataRetention.getString("collector_dropped_table");
				
				hmModuleData.put("total_modules", lTotalModules);
				hmModuleData.put("date_retention_in_days", rstLOGDataRetention.getString("date_retention_in_days"));
				hmModuleData.put("collector_uid_total_rows_affected", strUidTotalRowsAffected);
				hmModuleData.put("collector_dropped_table", strDroppedCollectorTable);
			}
			
			// format data to send in mail
			strUserWiseData = formatHTMLLOGUserWiseData(lhmUserLOGModules);
		} catch (Throwable th) {
			throw th;
		} finally {
			UtilsFactory.clearCollectionHieracy(lhmUserLOGModules);
			lhmUserLOGModules = null;
			
			strUidTotalRowsAffected = null;
		}
		
		return new String[]{sbData.toString(), strUserWiseData};
	}
	
	/**
	 * formats ASD retention data, for mail 
	 * 
	 * @param lhmUserASDModules
	 * @return
	 * @throws Throwable
	 */
	private String formatHTMLASDUserWiseData(LinkedHashMap<String, HashMap<String, Object>> lhmUserASDModules) throws Throwable {
		StringBuilder sbData = new StringBuilder();
		String strUserId = "";
		
		HashMap<String, Object> hmModuleData = null;
		
		try {
			sbData	.append("user_id,total_modules,date_retention_in_days,collector_dropped_table,slow_qry_dropped_table,slow_procedure_dropped_table,")
					.append("so_threshold_breach_total_rows_affected,sla_alert_log_total_rows_affected <BR>")
					.append("------------------------------------------------------------------------------------------------------------------------------------<BR>");
			
			Iterator<Map.Entry<String, HashMap<String, Object>>> itUserProfilerModules = lhmUserASDModules.entrySet().iterator();
			while (itUserProfilerModules.hasNext()) {
				Map.Entry<String, HashMap<String, Object>> entry = itUserProfilerModules.next();
				strUserId = entry.getKey();
				hmModuleData = entry.getValue();
				
				sbData	.append(strUserId).append(",")
						.append(hmModuleData.get("total_modules")).append(",")
						.append(hmModuleData.get("date_retention_in_days")).append(",")
						//.append(hmModuleData.get("collector_uid_total_rows_affected")).append(",")
						//.append(hmModuleData.get("slow_query_uid_total_rows_affected")).append(",")
						//.append(hmModuleData.get("slow_procedure_uid_total_rows_affected")).append(",")
						.append(hmModuleData.get("collector_dropped_table")).append(",")
						.append(hmModuleData.get("slow_qry_dropped_table")).append(",")
						.append(hmModuleData.get("slow_procedure_dropped_table")).append(",")
						.append(hmModuleData.get("uid_so_threshold_breach_total_rows_affected")).append(",")
						.append(hmModuleData.get("uid_sla_alert_log_total_rows_affected")).append("<BR>");
			}
			
		} catch (Throwable th) {
			throw th;
		}
		
		return sbData.toString();
	}
	
	/**
	 * formats LOG retention data, for mail 
	 * 
	 * @param lhmUserLOGModules
	 * @return
	 * @throws Throwable
	 */
	private String formatHTMLLOGUserWiseData(LinkedHashMap<String, HashMap<String, Object>> lhmUserLOGModules) throws Throwable {
		StringBuilder sbData = new StringBuilder();
		String strUserId = "";
		
		HashMap<String, Object> hmModuleData = null;
		
		try {
			sbData	.append("user_id,total_modules,date_retention_in_days,collector_dropped_table <BR>")
					.append("------------------------------------------------------------------------------------------------------------------------------------<BR>");
			
			Iterator<Map.Entry<String, HashMap<String, Object>>> itUserProfilerModules = lhmUserLOGModules.entrySet().iterator();
			while (itUserProfilerModules.hasNext()) {
				Map.Entry<String, HashMap<String, Object>> entry = itUserProfilerModules.next();
				strUserId = entry.getKey();
				hmModuleData = entry.getValue();
				
				sbData	.append(strUserId).append(",")
						.append(hmModuleData.get("total_modules")).append(",")
						.append(hmModuleData.get("date_retention_in_days")).append(",")
						.append(hmModuleData.get("collector_dropped_table")).append(",");
			}
			
		} catch (Throwable th) {
			throw th;
		}
		
		return sbData.toString();
	}
	
	/**
	 * sets ASD data retention, mail details
	 * 
	 * @param strMailRetentionLog
	 * @param hmMailDetails
	 * @throws Throwable
	 */
	private void setRetentionMailDetails(String moduleCode,String strMailRetentionLog, HashMap<String, Object> hmMailDetails) throws Throwable {
		try {
			hmMailDetails.put(moduleCode+"_DATA_RETENTION_TIME", UtilsFactory.nowFormattedDateWithTimeZone());
			hmMailDetails.put(moduleCode+"_DATA_RETENTION_LOG", strMailRetentionLog);
		} catch (Throwable th) {
			throw th;
		}
	}
}
