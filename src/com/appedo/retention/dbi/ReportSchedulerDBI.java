package com.appedo.retention.dbi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.appedo.commons.connect.DataBaseManager;

public class ReportSchedulerDBI {

	public ReportSchedulerDBI() {
		// TODO Auto-generated constructor stub		
	}
	
	public String getQuery(Connection con, int chart_id, int user_id) throws Throwable {
		
		StringBuilder sbQuery = new StringBuilder();
		PreparedStatement pstmt = null;
		ResultSet rst = null;
		String query = "";
		
		try{
			sbQuery.append("SELECT * FROM chart_visual_").append(user_id)
					.append(" WHERE chart_id = ").append(chart_id);
			pstmt = con.prepareStatement( sbQuery.toString() );
			rst = pstmt.executeQuery();
			
			if( rst.next() ) {
				query = rst.getString("query");
			}
			
		} catch(Throwable th) {
			throw th;
		} finally {
			DataBaseManager.close(rst);
			rst = null;
			
			DataBaseManager.close(pstmt);
			pstmt = null;
		}
		return query;
	}

	public String getEmailIds(Connection con, String user_id) throws Throwable {
		
		StringBuilder sbQuery = new StringBuilder();
		PreparedStatement pstmt = null;
		ResultSet rst = null;
		String EmailIDs = "";
		
		try{
			sbQuery.append("SELECT string_agg(email_mobile,',') AS EmailIDs FROM so_alert WHERE is_valid = TRUE AND user_Id = ").append(user_id)
					.append(" AND alert_type = 'Email'");
			pstmt = con.prepareStatement( sbQuery.toString() );
			rst = pstmt.executeQuery();
			
			if( rst.next() ) {
				EmailIDs = rst.getString("EmailIDs");
			}
			
		} catch(Throwable th) {
			throw th;
		} finally {
			DataBaseManager.close(rst);
			rst = null;
			
			DataBaseManager.close(pstmt);
			pstmt = null;
		}
		return EmailIDs;
	}
	
	public ResultSet ExcuQuery(Connection con, String query) throws Throwable {
		PreparedStatement pstmt = null;
		ResultSet rst = null;	
		try{			
			pstmt = con.prepareStatement( query );
			rst = pstmt.executeQuery();						
		} catch(Throwable th) {
			throw th;
		}
		return rst;
	}
	
	public void ExcuQuery_v1(Connection con, String query, String filePath) throws Throwable {
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		try{
			sbQuery	.append("copy(").append(query)
					.append(" ) to '").append(filePath)
					.append("' delimiter ','csv header ");
			pstmt = con.prepareStatement( sbQuery.toString() );
			pstmt.execute();					
		} catch(Throwable th) {
			throw th;
		}
	}
}
