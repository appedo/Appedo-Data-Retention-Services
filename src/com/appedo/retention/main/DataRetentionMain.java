package com.appedo.retention.main;

import java.sql.Connection;
import java.util.HashMap;

import com.appedo.commons.connect.DataBaseManager;
import com.appedo.commons.manager.AppedoMailer;
import com.appedo.commons.manager.AppedoMailer.MODULE_ID;
import com.appedo.commons.utils.UtilsFactory;
import com.appedo.manager.LogManager;
import com.appedo.retention.model.ModuleManager;
import com.appedo.retention.model.SUMManager;
import com.appedo.retention.utils.Constants;

public class DataRetentionMain {
	
	// retention log, mail data
	private HashMap<String, Object> hmDataRetentionMailDetails = null;
	
	
	public DataRetentionMain() throws Throwable {
		
		try {
			// loads constants, log, DB, mail properties
			Constants.loadInitProperties("Appedo-Data-Retention-Service", false /* DB-Auto-Commit */);
			
			hmDataRetentionMailDetails = new HashMap<String, Object>();
		} catch (Throwable th) {
			throw th;
		}
	}
	
	/**
	 * delete SUM 
	 * 
	 */
	private void deleteSUMData() {
		SUMManager sumManager = null;
		
		Connection con = null;
		
		try {
			sumManager = new SUMManager();
			
			LogManager.infoLog("---- SUM data retention is started on "+UtilsFactory.nowFormattedDateWithTimeZone());
			
			// get connection 
			con = DataBaseManager.giveConnection();
			
			// deletes SUM har files, 
			sumManager.deleteSUMOlderData(con, hmDataRetentionMailDetails);
			
			DataBaseManager.commit(con);
			
			LogManager.infoLog("---- SUM data retention is completed on "+UtilsFactory.nowFormattedDateWithTimeZone());
			
			sumManager = null;
		} catch (Throwable th) {
			DataBaseManager.rollback(con);
			
			LogManager.errorLog(th);
			
			hmDataRetentionMailDetails.put("SUM_EXCEPTION", "An exception occurred:<BR>"+th.getMessage());
		} finally {
			DataBaseManager.close(con);
			con = null;
		}
	}
	
	/**
	 * deletes profiler data
	 * 
	 */
	public void deleteProfilerData() {
		ModuleManager moduleManager = null;

		Connection con = null;
		
		try {
			moduleManager = new ModuleManager();
			
			LogManager.infoLog("---- Profiler data retention is started on "+UtilsFactory.nowFormattedDateWithTimeZone());
			
			// get connection 
			con = DataBaseManager.giveConnection();
			
			// delete profiler partitions data
			moduleManager.deleteProfilerData(con, hmDataRetentionMailDetails);

			DataBaseManager.commit(con);
			
			LogManager.infoLog("---- Profiler data retention is completed on "+UtilsFactory.nowFormattedDateWithTimeZone());
			
			moduleManager = null;
		} catch (Throwable th) {
			DataBaseManager.rollback(con);
			
			LogManager.errorLog(th);
			
			hmDataRetentionMailDetails.put("PROFILER_EXCEPTION", "An exception occurred:<BR>"+th.getMessage());
		} finally {
			DataBaseManager.close(con);
			con = null;
		}
	}
	
	/**
	 * deletes ASD data retention, collector, slow qry & slow procedure data
	 * 
	 */
	public void deleteASDData() {
		ModuleManager moduleManager = null;

		Connection con = null;
		
		try {
			moduleManager = new ModuleManager();
			
			LogManager.infoLog("---- ASD's collector, slow query & slow procedure; data retention is started on "+UtilsFactory.nowFormattedDateWithTimeZone());
			
			// get connection 
			con = DataBaseManager.giveConnection();
			
			// delete ASD's collector, so threshold breach, sla alert log, slow query & slow procedure partitions data, based on user's month's max license
			moduleManager.deleteASDCollectorData(con, hmDataRetentionMailDetails);
			
			DataBaseManager.commit(con);
			
			LogManager.infoLog("---- ASD's collector, slow query & slow procedure; data retention is completed on "+UtilsFactory.nowFormattedDateWithTimeZone());
			
			moduleManager = null;
		} catch (Throwable th) {
			DataBaseManager.rollback(con);
			
			LogManager.errorLog(th);
			
			hmDataRetentionMailDetails.put("ASD_EXCEPTION", "An exception occurred:<BR>"+th.getMessage());
		} finally {
			DataBaseManager.close(con);
			con = null;
		}
	}
	
	public void deleteLOGData() {
		ModuleManager moduleManager = null;

		Connection con = null;
		
		try {
			moduleManager = new ModuleManager();
			
			LogManager.infoLog("---- LOG's collector; data retention is started on "+UtilsFactory.nowFormattedDateWithTimeZone());
			
			// get connection 
			con = DataBaseManager.giveConnection();
			
			// delete ASD's collector, so threshold breach, sla alert log, slow query & slow procedure partitions data, based on user's month's max license
			moduleManager.deleteLOGCollectorData(con, hmDataRetentionMailDetails);
			
			DataBaseManager.commit(con);
			
			LogManager.infoLog("---- LOG's transaction data; data retention is completed on "+UtilsFactory.nowFormattedDateWithTimeZone());
			
			moduleManager = null;
		} catch (Throwable th) {
			DataBaseManager.rollback(con);
			
			LogManager.errorLog(th);
			
			hmDataRetentionMailDetails.put("LOG_EXCEPTION", "An exception occurred:<BR>"+th.getMessage());
		} finally {
			DataBaseManager.close(con);
			con = null;
		}
	}
	
	public static void main(String[] args) {
		String strStartTime = "", strEndTime = "";
		
		DataRetentionMain dataRetentionMain = null;
		AppedoMailer appedoMailer = null;
		
		try {
			dataRetentionMain = new DataRetentionMain();
			appedoMailer = new AppedoMailer( Constants.EMAIL_TEMPLATES_PATH );
			
			strStartTime = UtilsFactory.nowFormattedDateWithTimeZone();
			System.out.println("---- Data retention for SUM, Profiler, ASD, LOG are started on "+strStartTime);
			LogManager.infoLog("---- Data retention for SUM, Profiler, ASD, LOG are started on "+strStartTime);
			dataRetentionMain.hmDataRetentionMailDetails.put("DATA_RETENTION_STARTED_ON", strStartTime);
			
			
			// delete SUM data
			dataRetentionMain.deleteSUMData();
			
			// profiler data retention
			dataRetentionMain.deleteProfilerData();
			
			// ASD data retention
			dataRetentionMain.deleteASDData();
			
			// LOG data retention
			dataRetentionMain.deleteLOGData();
			
			
			strEndTime = UtilsFactory.nowFormattedDateWithTimeZone();
			dataRetentionMain.hmDataRetentionMailDetails.put("DATA_RETENTION_COMPLETED_ON", strEndTime);
			
		} catch (Throwable th) {
			LogManager.errorLog(th);
		} finally {
			try {
				// sends mail
				appedoMailer.sendMail(MODULE_ID.DATA_RETENTION, dataRetentionMain.hmDataRetentionMailDetails, Constants.DATA_RETENTION_MAIL_TO_ADDRESSES.split(","), "Appedo data retention on "+UtilsFactory.nowYYYYMMDD());
				LogManager.infoLog("Mail sent");
			} catch (Throwable th) {
				LogManager.infoLog("Exception occurred while mail send");
				LogManager.errorLog(th);
			}
			
			UtilsFactory.clearCollectionHieracy(dataRetentionMain.hmDataRetentionMailDetails);
			dataRetentionMain.hmDataRetentionMailDetails = null;
			
			System.out.println("---- Data retention for SUM, Profiler, ASD, LOG are completed on "+strEndTime);
			LogManager.infoLog("---- Data retention for SUM, Profiler, ASD, LOG are completed on "+strEndTime);
			
			// Separate Thread will be running to update the log4j.properties
			LogManager.stopLogRefresh();
			
			dataRetentionMain = null;
			appedoMailer = null;
			strStartTime = null;
			strEndTime = null;
		}
	}
}
