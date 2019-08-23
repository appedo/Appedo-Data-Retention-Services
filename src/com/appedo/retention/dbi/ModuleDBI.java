package com.appedo.retention.dbi;

import java.sql.ResultSet;
import java.sql.Statement;


public class ModuleDBI {
	
	/**
	 * deletes ASD's, `collector_<uid>`, mapped SLAs breach data from `so_threshold_breach_<user_id>`, `sla_alert_log_<user_id>`,  `<type>_slowquery_<uid>` & `mssql_slowprocedure_<uid>` tables data, 
	 *   based on user's month's max license data retention days used;
	 *   say user's current month license has `level2` and degraded as `level0`, based on `level2`'s data retention days, data deletes from partitions tables
	 * 
	 * @param stmt
	 * @return
	 * @throws Throwable
	 */
	public ResultSet deleteModuleASDPartitionsData(Statement stmt) throws Throwable {
		ResultSet rst = null;
		
		String strQuery = "";
		
		try {
			strQuery = "SELECT * FROM delete_asd_tables_data()";
			
			rst = stmt.executeQuery(strQuery);
		} catch (Throwable th) {
			throw th;
		} finally {
			strQuery = null;
		}
		
		return rst;
	}
	
	/**
	 * deletes LOG's, `collector_<uid>` tables data, 
	 *   based on user's month's max license (following OAD's licence module) data retention days used;
	 *   say user's current month license has `level2` and degraded as `level0`, based on `level2`'s data retention days, data deletes from partitions tables
	 * 
	 * @param stmt
	 * @return
	 * @throws Throwable
	 */
	public ResultSet deleteModuleLOGPartitionsData(Statement stmt) throws Throwable {
		ResultSet rst = null;
		
		String strQuery = "";
		
		try {
			strQuery = "SELECT * FROM delete_log_tables_data()";
			
			rst = stmt.executeQuery(strQuery);
		} catch (Throwable th) {
			throw th;
		} finally {
			strQuery = null;
		}
		
		return rst;
	}
	
	/**
	 * delete the profiler data < LAST '15 days'
	 * 
	 * @param stmt
	 * @return
	 * @throws Throwable
	 */
	public ResultSet deleteModuleProfilerPartitionsData(Statement stmt) throws Throwable {
		ResultSet rst = null;
		
		String strQuery = "";
		
		try {
			strQuery = "SELECT * FROM delete_profiler_tables_data()";
			
			rst = stmt.executeQuery(strQuery);
		} catch (Throwable th) {
			throw th;
		} finally {
			strQuery = null;
		}
		
		return rst;
	}
}
