package com.appedo.retention.dbi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import com.appedo.commons.connect.DataBaseManager;
import com.appedo.commons.utils.UtilsFactory;

public class SUMDBI {
	
	/**
	 * gets test ids and har files for the test ids
	 * 
	 * @param con
	 * @return
	 * @throws Throwable
	 */
	public ArrayList<HashMap<String, String>> getUsersSUMTestHarResults(Connection con) throws Throwable {
		PreparedStatement pstmt = null;
		ResultSet rst = null;
		
		StringBuilder sbQuery = new StringBuilder();
		
		ArrayList<HashMap<String, String>> alUsersSUMTestHarResults = null;
		
		HashMap<String, String> hmUsersSUMTestHarResults = new HashMap<String, String>();
		
		try {
			alUsersSUMTestHarResults = new ArrayList<HashMap<String, String>>();
			
			// all users license other than level3 delete old records based level's retention in days
			sbQuery	.append("SELECT shtr.test_id, shtr.harfilename ")
					.append("FROM sum_har_test_results shtr ")
					.append("INNER JOIN sum_test_master stm ON stm.test_id = shtr.test_id ") 
					.append("INNER JOIN usermaster um ON um.user_id = stm.user_id ")
					.append("  AND um.license_level <> 'level3' ")
					.append("INNER JOIN sum_config_parameters scp ON scp.lic_internal_name = um.license_level ") 
					.append("  AND shtr.received_on < now()::date - scp.max_retention_in_days * interval '1 day' ");
			
			pstmt = con.prepareStatement(sbQuery.toString());
			rst = pstmt.executeQuery();
			while( rst.next() ) {
				hmUsersSUMTestHarResults = new HashMap<String, String>();
				hmUsersSUMTestHarResults.put("test_id", rst.getString("test_id"));
				hmUsersSUMTestHarResults.put("harfilename", rst.getString("harfilename"));
				
				alUsersSUMTestHarResults.add(hmUsersSUMTestHarResults);
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
		
		return alUsersSUMTestHarResults;
	}
	
	/**
	 * gets test ids and har files for the test ids
	 * 
	 * @param stmt
	 * @return
	 * @throws Throwable
	 */
	public ResultSet getUsersSUMTestHarResults(Statement stmt) throws Throwable {
		ResultSet rst = null;
		
		StringBuilder sbQuery = new StringBuilder();
		
		try {
			// all users license other than level3 delete old records based level's retention in days
			sbQuery	.append("SELECT shtr.test_id, shtr.harfilename ")
					.append("FROM sum_har_test_results shtr ")
					.append("INNER JOIN sum_test_master stm ON stm.test_id = shtr.test_id ") 
					.append("INNER JOIN usermaster um ON um.user_id = stm.user_id ")
					//.append("  AND um.license_level <> 'level3' ")
					.append("INNER JOIN sum_config_parameters scp ON scp.lic_internal_name = um.license_level ") 
					.append("  AND shtr.received_on < now()::date - scp.max_retention_in_days * interval '1 day' ");
			
			rst = stmt.executeQuery(sbQuery.toString());
			
		} catch (Throwable th) {
			throw th;
		} finally {
			UtilsFactory.clearCollectionHieracy(sbQuery);
			sbQuery = null;
		}
		
		return rst;
	}
	
	/**
	 * gets users SUM har results older data 
	 * 
	 * @param con
	 * @return
	 * @throws Throwable
	 */
	public HashMap<String, String> getUsersSUMHarResultsTotalRecords(Connection con) throws Throwable {
		PreparedStatement pstmt = null;
		ResultSet rst = null;
		
		StringBuilder sbQuery = new StringBuilder();
		
		HashMap<String, String> hmUsersOldRecords = new HashMap<String, String>();
		
		try {
			// all users license other than level3 delete old records based level's retention in days
			sbQuery	.append("SELECT count(shtr.test_id) AS total_hars, group_concat(DISTINCT um.user_id) AS user_ids, group_concat(DISTINCT shtr.test_id) test_ids ")
					.append("FROM sum_har_test_results shtr ")
					.append("INNER JOIN sum_test_master stm ON stm.test_id = shtr.test_id ") 
					.append("INNER JOIN usermaster um ON um.user_id = stm.user_id ")
					/*.append("  AND um.license_level <> 'level3' ")*/
					.append("INNER JOIN sum_config_parameters scp ON scp.lic_internal_name = um.license_level ") 
					.append("  AND shtr.received_on < now()::date - scp.max_retention_in_days * interval '1 day' ");
			
			pstmt = con.prepareStatement(sbQuery.toString());
			rst = pstmt.executeQuery();
			if( rst.next() ) {
				hmUsersOldRecords.put("total_hars", rst.getString("total_hars"));
				hmUsersOldRecords.put("user_ids", rst.getString("user_ids"));
				hmUsersOldRecords.put("test_ids", rst.getString("test_ids"));
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
		
		return hmUsersOldRecords;
	}
	
	/**
	 * deletes `sum_har_test_results` older data
	 * 
	 * @param con
	 * @return
	 * @throws Throwable
	 */
	public int deleteUsersSUMHarResultsOldRecords(Connection con) throws Throwable {
		PreparedStatement pstmt = null;
		int nTotalRecordsAffected = 0;
		
		StringBuilder sbQuery = new StringBuilder();
		
		try {
			// all users license other than level3 delete old records based level's retention in days
			sbQuery	.append("DELETE ")
					.append("FROM sum_har_test_results shtr ")
					.append("USING sum_test_master stm, usermaster um, sum_config_parameters scp ") 
					.append("WHERE stm.test_id = shtr.test_id ")
					.append("  AND um.user_id = stm.user_id ")
					.append("  AND scp.lic_internal_name = um.license_level ")
					.append("  AND um.license_level <> 'level3' ")
					.append("  AND shtr.received_on < now()::date - scp.max_retention_in_days * interval '1 day' ");
			
			pstmt = con.prepareStatement(sbQuery.toString());
			nTotalRecordsAffected = pstmt.executeUpdate();
			
		} catch (Throwable th) {
			throw th;
		} finally {
			DataBaseManager.close(pstmt);
			pstmt = null;
			
			UtilsFactory.clearCollectionHieracy(sbQuery);
			sbQuery = null;
		}
		
		return nTotalRecordsAffected;
	}
	
	/**
	 * gets users SUM sum_execution_audit_log results older data 
	 * 
	 * @param con
	 * @return
	 * @throws Throwable
	 */
	public HashMap<String, String> getUsersSUMAuditLogsTotalRecords(Connection con) throws Throwable {
		PreparedStatement pstmt = null;
		ResultSet rst = null;
		
		StringBuilder sbQuery = new StringBuilder();

		HashMap<String, String> hmUsersOldRecords = new HashMap<String, String>();
		
		try {
			sbQuery	.append("SELECT count(seal.sum_test_id) AS total_audit_logs, group_concat(DISTINCT um.user_id) AS user_ids, group_concat(DISTINCT seal.sum_test_id) test_ids ")
					.append("FROM sum_execution_audit_log seal ")
					.append("INNER JOIN sum_test_master stm ON stm.test_id = seal.sum_test_id ") 
					.append("INNER JOIN usermaster um ON um.user_id = stm.user_id ")
					.append("  AND um.license_level <> 'level3' ")
					.append("INNER JOIN sum_config_parameters scp ON scp.lic_internal_name = um.license_level ") 
					.append("  AND seal.created_on < now()::date - scp.max_retention_in_days * interval '1 day' ");
			
			//System.out.println("sbQuery: "+sbQuery.toString());
			pstmt = con.prepareStatement(sbQuery.toString());
			rst = pstmt.executeQuery();
			if( rst.next() ) {
				hmUsersOldRecords.put("total_audit_logs", rst.getString("total_audit_logs"));
				hmUsersOldRecords.put("user_ids", rst.getString("user_ids"));
				hmUsersOldRecords.put("test_ids", rst.getString("test_ids"));
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
		
		return hmUsersOldRecords;
	}
	
	/**
	 * deletes `sum_execution_audit_log` older data
	 *  
	 * @param con
	 * @return
	 * @throws Throwable
	 */
	public int deleteUsersSUMAuditLogsOldRecords(Connection con) throws Throwable {
		PreparedStatement pstmt = null;
		int nTotalRecordsAffected = 0;
		
		StringBuilder sbQuery = new StringBuilder();
		
		try {
			// all users license other than level3 delete old records based level's retention in days
			sbQuery	.append("DELETE ")
					.append("FROM sum_execution_audit_log seal ")
					.append("USING sum_test_master stm, usermaster um, sum_config_parameters scp ") 
					.append("WHERE stm.test_id = seal.sum_test_id ")
					.append("  AND um.user_id = stm.user_id ")
					.append("  AND scp.lic_internal_name = um.license_level ")
					.append("  AND um.license_level <> 'level3' ")
					.append("  AND seal.created_on < now()::date - scp.max_retention_in_days * interval '1 day' ");
			
			pstmt = con.prepareStatement(sbQuery.toString());
			nTotalRecordsAffected = pstmt.executeUpdate();
			
		} catch (Throwable th) {
			throw th;
		} finally {
			DataBaseManager.close(pstmt);
			pstmt = null;
			
			UtilsFactory.clearCollectionHieracy(sbQuery);
			sbQuery = null;
		}
		
		return nTotalRecordsAffected;
	}
	
}
