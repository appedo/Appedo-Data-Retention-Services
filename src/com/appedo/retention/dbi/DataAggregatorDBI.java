package com.appedo.retention.dbi;

import java.sql.Connection;
import java.sql.PreparedStatement;

import com.appedo.commons.connect.DataBaseManager;

public class DataAggregatorDBI {
	
	public static void aggregateData(Connection con, String strAggregationGroup) {
		PreparedStatement pstmt = null;
		
		try{
			pstmt = con.prepareStatement("SELECT aggregate_oad_collector_tables(?, null, null, null, null, null)");
			pstmt.setString(1, strAggregationGroup);
			
			pstmt.execute();
			
		} catch(Throwable th) {
			System.out.println("Exception in aggregateData(): "+th.getMessage());
			th.printStackTrace();
		} finally {
			DataBaseManager.close(pstmt);
			pstmt = null;
		}
	}
}
