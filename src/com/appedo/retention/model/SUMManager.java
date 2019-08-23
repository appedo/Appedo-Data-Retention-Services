package com.appedo.retention.model;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import com.appedo.commons.connect.DataBaseManager;
import com.appedo.commons.manager.AppedoMailer;
import com.appedo.commons.manager.AppedoMailer.MODULE_ID;
import com.appedo.commons.utils.UtilsFactory;
import com.appedo.manager.LogManager;
import com.appedo.retention.dbi.SUMDBI;
import com.appedo.retention.utils.Constants;

public class SUMManager {
	
	/**
	 * deletes SUM har files, `sum_har_test_results` table & execution log table user old data
	 *   DONT USE (hmDataRetentionMailDetails = null OR hmDataRetentionMailDetails = new  HashMap<String, Object>()), since obj. by ref
	 * 
	 * @param con
	 * @throws Throwable
	 */
	public void deleteSUMOlderData(Connection con, HashMap<String, Object> hmDataRetentionMailDetails) throws Throwable {
		HashMap<String, String> hmTotalSUMHarRecords = null, hmTotalSUMAuditLogsRecords = null;
		
		try {
			// deletes har files and `sum_har_test_results`
			hmTotalSUMHarRecords = deleteUsersSUMHarResultsOldRecords(con);
			LogManager.infoLog("Affected sum_har_test_results, "+hmTotalSUMHarRecords.get("total_hars_deleted")+" har files deleted and "+hmTotalSUMHarRecords.get("total_rows_affected")+" data deleted from DB");
			
			// deletes audit logs
			hmTotalSUMAuditLogsRecords = deleteUsersSUMAuditLogsOldRecords(con);
			LogManager.infoLog("Affected sum_execution_audit_log, "+hmTotalSUMAuditLogsRecords.get("total_rows_affected")+" data test execution deleted from DB");
			
			
			// sets SUM data retention mail details
			setSUMRetentionMailDetails(hmTotalSUMHarRecords, hmTotalSUMAuditLogsRecords, hmDataRetentionMailDetails);
			
		} catch(Throwable th) {
			//bExceptionOccurred = true;
			//strExceptionMsg = "An exception occurred while data retention:<BR>"+e.getMessage();
			throw th;
		} finally {
			/* all data retention mail, say SUM, Profiler & ASD, are send in single mail, ignored each modules separate mail
			// send mail 
			sendMail(hmTotalSUMHarRecords, hmTotalSUMAuditLogsRecords, bExceptionOccurred ? strExceptionMsg : null);
			LogManager.infoLog("Mail Sent");
			*/
		}
	}
	
	/**
	 * user old data deletes har files and `sum_har_test_results`
	 * 
	 * @param con
	 * @return
	 * @throws Throwable
	 */
	public HashMap<String, String> deleteUsersSUMHarResultsOldRecords(Connection con) throws Throwable {
		SUMDBI sumdbi = null;

		HashMap<String, String> hmUsersOldRecords = null;

		int nTotalRecordsAffected = 0, nTotalHarsDeleted = 0;
		
		try {
			sumdbi = new SUMDBI();
			
			hmUsersOldRecords = sumdbi.getUsersSUMHarResultsTotalRecords(con);
			
			// tried, delete har files in repository 
			nTotalHarsDeleted = deleteUserOlderHarFiles(con, Integer.parseInt(hmUsersOldRecords.get("total_hars")));
			
			// deletes `sum_har_test_results`
			nTotalRecordsAffected = sumdbi.deleteUsersSUMHarResultsOldRecords(con);
			hmUsersOldRecords.put("total_rows_affected", nTotalRecordsAffected+"");
			hmUsersOldRecords.put("total_hars_deleted", nTotalHarsDeleted+"");
			

			LogManager.infoLog("sum_har_test_results:");
			LogManager.infoLog("	Total records available: "+hmUsersOldRecords.get("total_hars"));
			LogManager.infoLog("	User Ids: "+hmUsersOldRecords.get("user_ids"));
			LogManager.infoLog("	SUM Test Ids: "+hmUsersOldRecords.get("test_ids"));
			LogManager.infoLog("	Total har files deleted: "+hmUsersOldRecords.get("total_hars_deleted"));
			LogManager.infoLog("	Total records affected: "+hmUsersOldRecords.get("total_rows_affected"));
			
			sumdbi = null;
		} catch (Throwable th) {
			throw th;
		}
		
		return hmUsersOldRecords;
	}
	
	/**
	 * user old data deletes `sum_execution_audit_log`
	 * 
	 * @param con
	 * @return
	 * @throws Throwable
	 */
	public HashMap<String, String> deleteUsersSUMAuditLogsOldRecords(Connection con) throws Throwable {
		SUMDBI sumdbi = null;

		HashMap<String, String> hmUsersOldRecords = null;

		int nTotalRecordsAffected = 0;
		
		try {
			sumdbi = new SUMDBI();
			
			// gets details of `sum_execution_audit_log` total records, user_ids & test_ids
			hmUsersOldRecords = sumdbi.getUsersSUMAuditLogsTotalRecords(con);
			
			// deletes `sum_execution_audit_log`
			nTotalRecordsAffected = sumdbi.deleteUsersSUMAuditLogsOldRecords(con);
			hmUsersOldRecords.put("total_rows_affected", nTotalRecordsAffected+"");


			LogManager.infoLog("sum_execution_audit_log:");
			LogManager.infoLog("	Total records available: "+hmUsersOldRecords.get("total_audit_logs"));
			LogManager.infoLog("	User Ids: "+hmUsersOldRecords.get("user_ids"));
			LogManager.infoLog("	SUM Test Ids: "+hmUsersOldRecords.get("test_ids"));
			LogManager.infoLog("	Total records affected: "+hmUsersOldRecords.get("total_rows_affected"));
			
			sumdbi = null;
		} catch (Throwable th) {
			throw th;
		}
		
		return hmUsersOldRecords;
	}
	
	/**
	 * formats HTML content
	 * NOT USED
	 * 
	 * @param hmTotalSUMHarRecords
	 * @param hmTotalSUMAuditLogsRecords
	 * @return
	 * @throws Throwable
	 */
	private String formatHTMLContents(HashMap<String, String> hmTotalSUMHarRecords, HashMap<String, String> hmTotalSUMAuditLogsRecords) throws Throwable {
		StringBuilder sbHTML = new StringBuilder();
		
		try {
			sbHTML	.append("<DL>")
					.append("<DT>sum_har_test_results:</DT>")
					.append("<DD>Total records available before deletion: ").append(hmTotalSUMHarRecords.get("total_hars")).append("</DD>")
					.append("<DD>User Ids: ").append(hmTotalSUMHarRecords.get("user_ids")).append("</DD>")
					.append("<DD>SUM Test Ids: ").append(hmTotalSUMHarRecords.get("test_ids")).append("</DD>")
					.append("<DD>Total har files deleted: ").append(hmTotalSUMHarRecords.get("total_hars_deleted")).append("</DD>")
					.append("<DD>Total records affected: ").append(hmTotalSUMHarRecords.get("total_rows_affected")).append("</DD>")
					.append("</DL>")
					.append("<DL>")
					.append("<DT>sum_execution_audit_log:</DT>")
					.append("<DD>Total records available before deletion: ").append(hmTotalSUMAuditLogsRecords.get("total_audit_logs")).append("</DD>")
					.append("<DD>User Ids: ").append(hmTotalSUMAuditLogsRecords.get("user_ids")).append("</DD>")
					.append("<DD>SUM Test Ids: ").append(hmTotalSUMAuditLogsRecords.get("test_ids")).append("</DD>")
					.append("<DD>Total records affected: ").append(hmTotalSUMAuditLogsRecords.get("total_rows_affected")).append("</DD>")
					.append("</DL>");
		} catch (Throwable th) {
			throw th;
		}
		
		return sbHTML.toString();
	}
	
	/**
	 * deletes har files from the system 
	 * 
	 * @param con
	 * @param nTotalhars
	 * @return
	 * @throws Throwable
	 */
	public int deleteUserOlderHarFiles(Connection con, int nTotalhars) throws Throwable {
		Statement stmt = null;
		ResultSet rstSUMTestHars = null;
		
		SUMDBI sumdbi = null;
		
/*
		ArrayList<HashMap<String, String>> alUsersSUMTestHarResults = null;
		
		HashMap<String, String> hmUsersSUMTestHarResults = null;
		
		Iterator<HashMap<String, String>> itALUsersSUMTestHarResults = null;
*/
		String strHarFilePath = "", strSUMTestFilepath = "";
		
		int nIdxDeleted = 0;
		boolean bHarFileDeleted = false;
		
		try {
			sumdbi = new SUMDBI();
			
			LogManager.infoLog("Starting to delete har file(s), expected no. of hars "+nTotalhars+" to delete...");
			System.out.println("Starting to delete har file(s), expected no. of hars "+nTotalhars+" to delete...");
/*
			alUsersSUMTestHarResults = sumdbi.getUsersSUMTestHarResults(con);
			itALUsersSUMTestHarResults = alUsersSUMTestHarResults.iterator();
			while( itALUsersSUMTestHarResults.hasNext() ) {
				hmUsersSUMTestHarResults = itALUsersSUMTestHarResults.next();
*/
			stmt = con.createStatement();
			rstSUMTestHars = sumdbi.getUsersSUMTestHarResults(stmt);
			while( rstSUMTestHars.next() ) {
				//strSUMTestFilepath = "/"+hmUsersSUMTestHarResults.get("test_id")+File.separator+hmUsersSUMTestHarResults.get("harfilename");
				strSUMTestFilepath = "/"+rstSUMTestHars.getString("test_id")+"/"+rstSUMTestHars.getString("harfilename");
				//strSUMTestFilepath = "/1"+"/"+"test_file_delete.txt";
				strHarFilePath = Constants.HAR_REPOSITORY + strSUMTestFilepath;
				System.out.println(strHarFilePath);
				// 
				bHarFileDeleted = UtilsFactory.deleteFile(strHarFilePath);
				
				if ( bHarFileDeleted ) {
					// deleted
					nIdxDeleted = nIdxDeleted + 1;
					LogManager.infoLog("The file "+strHarFilePath+" has been deleted.");
					System.out.println("The file "+strHarFilePath+" has been deleted.");
				} else {
					// not deleted
					LogManager.infoLog("The file "+strHarFilePath+" is not deleted, may exists in directory.");
					System.out.println("The file "+strHarFilePath+" is not deleted, may exists in directory.");
				}
				
				strSUMTestFilepath = null;
			}
			
			LogManager.infoLog("Finished delete har file(s), no. of hars "+nIdxDeleted+" deleted.");
			
			sumdbi = null;
		} catch (Throwable th) {
			throw th;
		} finally {
			DataBaseManager.close(rstSUMTestHars);
			rstSUMTestHars = null;
			DataBaseManager.close(stmt);
			stmt = null;
		}
		
		return nIdxDeleted;
	}
	
	/**
	 * sends mail, for SUM data retention
	 * 
	 * @param hmTotalSUMHarRecords
	 * @param hmTotalSUMAuditLogsRecords
	 * @throws Throwable
	 */
	public void sendMail(HashMap<String, String> hmTotalSUMHarRecords, HashMap<String, String> hmTotalSUMAuditLogsRecords, String strExceptionMsg) throws Throwable {
		String strSubject = "", strDateTime = "", strDate = "";
		
		HashMap<String, Object> hmMailDetails = null;
		
		AppedoMailer appedoMailer = null;
		
		try {
			appedoMailer = new AppedoMailer( Constants.EMAIL_TEMPLATES_PATH );
			
			// 
			strDate = UtilsFactory.nowYYYYMMDD();
			strDateTime = UtilsFactory.nowFormattedDateWithTimeZone();
			
			// send mail
			hmMailDetails = new HashMap<String, Object>();
			//hmMailDetails.put("USERNAME", "DevOps");
			hmMailDetails.put("TIME", strDateTime);
			
			// har results table
			if ( hmTotalSUMHarRecords != null ) {
				hmMailDetails.put("TOTAL_HAR_RESULTS_AVAILABLE_BEFORE_DELETION", hmTotalSUMHarRecords.get("total_hars"));
				hmMailDetails.put("HAR_RESULTS_USER_IDS", hmTotalSUMHarRecords.get("user_ids"));
				hmMailDetails.put("HAR_RESULTS_SUM_TEST_IDS", hmTotalSUMHarRecords.get("test_ids"));
				hmMailDetails.put("TOTAL_HAR_FILES_DELETED", hmTotalSUMHarRecords.get("total_hars_deleted"));
				hmMailDetails.put("TOTAL_HAR_RESULTS_RECORDS_DELETED", hmTotalSUMHarRecords.get("total_rows_affected"));	
			}
			// sum audit logs 
			if ( hmTotalSUMAuditLogsRecords != null ) {
				hmMailDetails.put("TOTAL_AUDIT_LOG_AVAILABLE_BEFORE_DELETION", hmTotalSUMAuditLogsRecords.get("total_audit_logs"));
				hmMailDetails.put("AUDIT_LOG_USER_IDS", hmTotalSUMAuditLogsRecords.get("user_ids"));
				hmMailDetails.put("AUDIT_LOG_SUM_TEST_IDS", hmTotalSUMAuditLogsRecords.get("test_ids"));
				hmMailDetails.put("TOTAL_AUDIT_LOG_RECORDS_DELETED", hmTotalSUMAuditLogsRecords.get("total_rows_affected"));
			}
			if ( strExceptionMsg != null && strExceptionMsg.length() > 0 ) {
				hmMailDetails.put("ERROR", strExceptionMsg);
			}
			strSubject = "SUM data retention on "+strDate;
			
			appedoMailer.sendMail(MODULE_ID.SUM_DATA_RETENTION, hmMailDetails, Constants.DATA_RETENTION_MAIL_TO_ADDRESSES.split(","), strSubject);
			
		} catch (Throwable th) {
			throw th;
		} finally {
			UtilsFactory.clearCollectionHieracy(hmMailDetails);
			hmMailDetails = null;
			
			strSubject = null;
			strDateTime = null;
			strDate = null;
			
			appedoMailer = null;
		}
	}
	
	/**
	 * sets SUM data retention log, mail data
	 * 
	 * @param hmTotalSUMHarRecords
	 * @param hmTotalSUMAuditLogsRecords
	 * @param hmMailDetails
	 */
	private void setSUMRetentionMailDetails(HashMap<String, String> hmTotalSUMHarRecords, HashMap<String, String> hmTotalSUMAuditLogsRecords, HashMap<String, Object> hmMailDetails) {
		
		try {
			hmMailDetails.put("SUM_DATA_RETENTION_TIME", UtilsFactory.nowFormattedDateWithTimeZone());
			
			// har results table
			if ( hmTotalSUMHarRecords != null ) {
				hmMailDetails.put("TOTAL_HAR_RESULTS_AVAILABLE_BEFORE_DELETION", hmTotalSUMHarRecords.get("total_hars"));
				hmMailDetails.put("HAR_RESULTS_USER_IDS", hmTotalSUMHarRecords.get("user_ids"));
				hmMailDetails.put("HAR_RESULTS_SUM_TEST_IDS", hmTotalSUMHarRecords.get("test_ids"));
				hmMailDetails.put("TOTAL_HAR_FILES_DELETED", hmTotalSUMHarRecords.get("total_hars_deleted"));
				hmMailDetails.put("TOTAL_HAR_RESULTS_RECORDS_DELETED", hmTotalSUMHarRecords.get("total_rows_affected"));	
			}
			// sum audit logs 
			if ( hmTotalSUMAuditLogsRecords != null ) {
				hmMailDetails.put("TOTAL_AUDIT_LOG_AVAILABLE_BEFORE_DELETION", hmTotalSUMAuditLogsRecords.get("total_audit_logs"));
				hmMailDetails.put("AUDIT_LOG_USER_IDS", hmTotalSUMAuditLogsRecords.get("user_ids"));
				hmMailDetails.put("AUDIT_LOG_SUM_TEST_IDS", hmTotalSUMAuditLogsRecords.get("test_ids"));
				hmMailDetails.put("TOTAL_AUDIT_LOG_RECORDS_DELETED", hmTotalSUMAuditLogsRecords.get("total_rows_affected"));
			}
		} catch(Throwable th) {
			throw th;
		}
	}
}
