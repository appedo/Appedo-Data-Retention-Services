package com.appedo.retention.model;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.appedo.commons.manager.AppedoMailer;
import com.appedo.commons.manager.AppedoMailer.MODULE_ID;
import com.appedo.manager.LogManager;
import com.appedo.retention.dbi.ReportSchedulerDBI;
import com.appedo.retention.utils.AppedoUtils;
import com.appedo.retention.utils.Constants;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ReportSchedulerManager {

	public ReportSchedulerManager() {
		// TODO Auto-generated constructor stub
	}

	public void validateSendToReports() throws Throwable{
		
		AppedoMailer appedoMailer = null;
		
		HashMap<String, Object> hmMailDetails = null;
		boolean bEmailSent = false;
		
		try {
			appedoMailer = new AppedoMailer( Constants.EMAIL_TEMPLATES_PATH );
			hmMailDetails = new HashMap<String, Object>();
			hmMailDetails.put("table_data", strBody());
			
			String strSubject = "Appedo: Sample Table Html";
			String strMobileOrEmail = "siddiqa@softsmith.com";
			bEmailSent = appedoMailer.sendMail(MODULE_ID.REPORT_STATUS_EMAIL, hmMailDetails, strMobileOrEmail.split(","), strSubject);
			
			System.out.println("Is Email Sent : "+ bEmailSent);
			
		}catch (Throwable th) {
			// TODO: handle exception
			LogManager.errorLog(th);
			throw th;
		}
	}

	public static String strBody() {
		StringBuilder sbBody = new StringBuilder();
		try {
			
			sbBody	.append("<tr>")
					.append("<th>").append("column1").append("</th>")
					.append("<th>").append("column2").append("</th>")
					.append("<th>").append("column3").append("</th>")
					.append("</tr><tr>")
					.append("<td>").append("R1Data1jdfkjaskjfksjdznkcjsnksjsnkdjznfkjnc").append("</td>")
					.append("<td>").append("R1Data1").append("</td>")
					.append("<td>").append("R1Data1").append("</td>")
					.append("</tr><tr>")
					.append("<td>").append("R2Data1").append("</td>")
					.append("<td>").append("R2Data1").append("</td>")
					.append("<td>").append("R2Data1").append("</td>")
					.append("</tr><tr>")
					.append("<td>").append("R2Data1").append("</td>")
					.append("<td>").append("R2Data1").append("</td>")
					.append("<td>").append("R2Data1").append("</td>")
					.append("</tr>");
			
		}catch (Throwable th) {
			System.out.println("Exception in strBody : "+ th.getMessage());
			// TODO: handle exception
		}
		return sbBody.toString();
	}

	public static String strBody(ResultSet rst, FileWriter writer) {
		
		StringBuilder sbBody = new StringBuilder();
		List<String> alColumnName = new ArrayList<String>();
		List<String> alColumnValue = null;
		
		try {
			
			/*sbBody	.append("<tr>")
					.append("<th>").append("column1").append("</th>")
					.append("<th>").append("column2").append("</th>")
					.append("<th>").append("column3").append("</th>")
					.append("</tr><tr>")
					.append("<td>").append("R1Data1jdfkjaskjfksjdznkcjsnksjsnkdjznfkjnc").append("</td>")
					.append("<td>").append("R1Data1").append("</td>")
					.append("<td>").append("R1Data1").append("</td>")
					.append("</tr><tr>")
					.append("<td>").append("R2Data1").append("</td>")
					.append("<td>").append("R2Data1").append("</td>")
					.append("<td>").append("R2Data1").append("</td>")
					.append("</tr><tr>")
					.append("<td>").append("R2Data1").append("</td>")
					.append("<td>").append("R2Data1").append("</td>")
					.append("<td>").append("R2Data1").append("</td>")
					.append("</tr>");*/
			
			sbBody.append("<tr>");			
			for (int count=1; count <= rst.getMetaData().getColumnCount(); count++) {
				alColumnName.add(rst.getMetaData().getColumnLabel(count));
				sbBody.append("<th>").append(rst.getMetaData().getColumnLabel(count)).append("</th>");
			}
			sbBody.append("</tr>");
			//AppedoUtils.writeLine(writer, Arrays.asList("a", "b", "c", "d"));
			AppedoUtils.writeLine(writer, alColumnName);
			
			while( rst.next() ) {
				 alColumnValue = new ArrayList<String>();
				 sbBody.append("<tr>");
				 for (int count=1; count <= rst.getMetaData().getColumnCount(); count++) {
					 alColumnValue.add(rst.getString(count));
					 if(rst.getString(count).equalsIgnoreCase("up")) {
						 sbBody.append("<td style=\"background :#00b050;\">").append(rst.getString(count)).append("</td>");
					 }else if(rst.getString(count).equalsIgnoreCase("down")) {
						 sbBody.append("<td style=\"background :red;\">").append(rst.getString(count)).append("</td>");
					 }else {
						 sbBody.append("<td>").append(rst.getString(count)).append("</td>"); 
					 } 
				 }
				 AppedoUtils.writeLine(writer, alColumnValue);
				 sbBody.append("</tr>");
			}
			
		}catch (Throwable th) {
			System.out.println("Exception in strBody : "+ th.getMessage());
			// TODO: handle exception
		}
		return sbBody.toString();
	}
	
	public static void fileWriting(ResultSet rst, FileWriter writer) {
		List<String> alColumnName = new ArrayList<String>();
		List<String> alColumnValue = null;
		try {
			for (int count=1; count <= rst.getMetaData().getColumnCount(); count++) {
				alColumnName.add(rst.getMetaData().getColumnLabel(count));
			}
			AppedoUtils.writeLine(writer, alColumnName);
			
			while( rst.next() ) {
				 alColumnValue = new ArrayList<String>();
				 for (int count=1; count <= rst.getMetaData().getColumnCount(); count++) {
					 alColumnValue.add(rst.getString(count)); 
				 }
				 AppedoUtils.writeLine(writer, alColumnValue);
			}
		}catch (Throwable th) {
			System.out.println("Exception in fileWriting : "+ th.getMessage());
			// TODO: handle exception
		}
	}
	
	public static String emailBodyContent(ResultSet rst, String reportName) {
		StringBuilder sbBody = new StringBuilder();
		try {
			
			sbBody	.append("<p><b>Report Name : </b>")
					.append(reportName)
					.append("</p><br>");
			
			sbBody.append("<table id=\"t01\"><tr>");			
			for (int count=1; count <= rst.getMetaData().getColumnCount(); count++) {
				String columnName = rst.getMetaData().getColumnLabel(count);
				sbBody.append("<th>").append(StringUtils.capitaliseAllWords(columnName.replaceAll("_", " "))).append("</th>");
			}
			sbBody.append("</tr>");
			
			while( rst.next() ) {
				 sbBody.append("<tr>");
				 for (int count=1; count <= rst.getMetaData().getColumnCount(); count++) {
					 if(rst.getString(count).equalsIgnoreCase("up") || rst.getString(count).equalsIgnoreCase("ok")) {
						 sbBody.append("<td style=\"background :#00b050;\">").append(rst.getString(count)).append("</td>");
					 }else if(rst.getString(count).equalsIgnoreCase("down")) {
						 sbBody.append("<td style=\"background :red;\">").append(rst.getString(count)).append("</td>");
					 }else {
						 sbBody.append("<td>").append(rst.getString(count)).append("</td>"); 
					 } 
				 }
				 sbBody.append("</tr>");
			}
			sbBody.append("</table><br><br>");
		}catch (Throwable th) {
			System.out.println("Exception in strBody : "+ th.getMessage());
			// TODO: handle exception
		}
		return sbBody.toString();
	}

	public HashMap<String, JSONArray> getEmailSendReportList(ResultSet rst) throws Throwable {
		JSONObject joReport = null;
		
		HashMap<String, JSONArray> hmReportList = new HashMap<String, JSONArray>();
		try{
						
			while( rst.next() ) {
				
				String sent_to_email = rst.getString("send_to") == null ? "" : rst.getString("send_to");
				String user_id;
				
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
				joReport.put("last_send_epoc_time", rst.getLong("last_send_epoc_time"));
				joReport.put("send_to", sent_to_email);
				
				if(sent_to_email.isEmpty()) {
					user_id = "user_id-#@#-"+rst.getString("user_id");
				}else {
					user_id = sent_to_email;
				}
				
				hmReportList = setUserAndEmailWiseReportList(hmReportList, user_id, joReport);
				
			}
			
		} catch(Throwable th) {
			throw th;
		} finally {
			/*DataBaseManager.close(rst);
			rst = null;*/
		}
		return hmReportList;
	}
	
	private HashMap<String, JSONArray> setUserAndEmailWiseReportList(HashMap<String, JSONArray> hmReportList, String reportKeys, JSONObject joReport){
		
		String[] reportkey = reportKeys.split(",");
		
		for(String key : reportkey) {
			
			if(hmReportList.containsKey(key)) {					
				hmReportList.get(key).add(joReport);
			}else{
				JSONArray jaReports = new JSONArray();
				jaReports.add( joReport );
				
				hmReportList.put(key, jaReports);
			}
		}
		
		return hmReportList;
	}
	
	/**
	 * 
	 * @param con
	 * @param user_id
	 * @param jaReports
	 * 
	 * @return JSONObject in successfully sent_to user_id's
	 * @throws Throwable
	 * 
	 */
	
	public JSONObject sendReportSchedulerEmail(Connection con, String user_id, JSONArray jaReports) throws Throwable {
		ReportSchedulerDBI rsDBI = new ReportSchedulerDBI();
		
		AppedoMailer appedoMailer = null;
		
		HashMap<String, Object> hmMailDetails = null;
		
		String strSubject = "", strMobileOrEmail="", csvFile = "", strBody = "", reportName = "";
		long lStartTime, lEndTime = -1L; 
		boolean bEmailSent = false;
		ResultSet rst = null;
		JSONArray jaSendMailBodyReportData = new JSONArray();
		JSONArray jaSendAttachmentReport = new JSONArray();
		JSONObject joResponseData = new JSONObject();
		try {
			
			//long currentTimeMillis = System.currentTimeMillis();
			//lEndTime = currentTimeMillis - (currentTimeMillis%3600000);
			//lEndTime = System.currentTimeMillis(); // 2018-06-28 16:45:32.567
			
			if(jaReports.size() > 1) {
				//strSubject = "scheduled report as of "+AppedoUtils.userFormattedDate(lEndTime);
				strSubject = "Scheduled Reports";
			}
			
			//Attachment file List
			StringBuilder sbAttachmentBody = new StringBuilder();
			sbAttachmentBody.append("<p> Herewith enclosing daily status report. below the report enclosed.")
							.append("<ul style=\"list-style-type:circle; color:blue;\">");
			
			
			for( int i=0; i<jaReports.size(); i++ ) {
				JSONObject joReport = jaReports.getJSONObject(i);
				
				String query = rsDBI.getQuery(con, joReport.getInt("chart_id"), joReport.getInt("user_id"));

				//lEndTime = joReport.getLong("last_send_epoc_time");
				//lStartTime = lEndTime - (1000 * 60 * 60 * joReport.getInt("alert_freq_hour")) ; // 2018-06-28 14:45:32.566
				lStartTime = joReport.getLong("last_send_epoc_time");
				lEndTime = lStartTime + (60 * 60 * joReport.getInt("alert_freq_hour"));
				
				reportName = joReport.getString("report_name");
				reportName = reportName.replace("@frequency_time@", joReport.getString("alert_freq_hour"));
				reportName = reportName.replace("@startDate@", AppedoUtils.userFormattedDate(lStartTime));
				reportName = reportName.replace("@endDate@", AppedoUtils.userFormattedEndTime(lEndTime));
				
				if(!query.isEmpty()) {
					query = query.replace("@startDate@", (lStartTime)+"");
					query = query.replace("@endDate@", (lEndTime)+"");
					
					if(joReport.getString("attachment_format").equalsIgnoreCase("pg_csv")) {
						String fileName = "statusReport_"+joReport.getString("report_id")+".csv";
						csvFile = Constants.EMAIL_ATTACHMENT_PATH+fileName;
						rsDBI.ExcuQuery_v1(con, query, csvFile);
					}else {
						rst = rsDBI.ExcuQuery(con, query);
					}
					
					JSONObject joReportData = new JSONObject();
					joReportData.put("send_as_attachment", joReport.getBoolean("send_as_attachment"));
					
					if(joReport.getBoolean("send_as_attachment")){
						if(!joReport.getString("attachment_format").equalsIgnoreCase("pg_csv")) {
							String fileName = "statusReport_"+joReport.getString("report_id")+".csv";
							csvFile = Constants.EMAIL_ATTACHMENT_PATH+fileName;
							FileWriter writer = new FileWriter(csvFile);
							fileWriting(rst, writer);
							writer.flush();
					        writer.close();
						}
				        joReportData.put("attachment_path", csvFile);
				        joReportData.put("report_name", reportName);
				        jaSendAttachmentReport.add(joReportData);
				        sbAttachmentBody.append("<li>").append(joReportData.getString("report_name")).append("</li>");
					}else {
						strBody = emailBodyContent(rst, reportName);
						joReportData.put("EmailBodyContent", strBody);
						jaSendMailBodyReportData.add(joReportData);
					}
				}
				if(strSubject.isEmpty()) {
					strSubject = joReport.getString("subject");
					strSubject = strSubject.replace("@frequency_time@", joReport.getString("alert_freq_hour"));
					strSubject = strSubject.replace("@startDate@", AppedoUtils.userFormattedDate(lStartTime));
					strSubject = strSubject.replace("@endDate@", AppedoUtils.userFormattedEndTime(lEndTime));
				}
			}
			
			sbAttachmentBody.append("</ul></p><br>");
						
			if(user_id.contains("user_id-")) {
				strMobileOrEmail = rsDBI.getEmailIds(con, user_id.split("-#@#-")[1]);
			}else {
				strMobileOrEmail = user_id;
			}
			//strMobileOrEmail = "siddiqa@softsmith.com,krishnanv@softsmith.com";
			
			String strEmailBody = "";
			for(int i=0; i<jaSendMailBodyReportData.size(); i++) {
				JSONObject joResult = (JSONObject) jaSendMailBodyReportData.get(i);
				strEmailBody = strEmailBody+joResult.getString("EmailBodyContent");
			}
			
			appedoMailer = new AppedoMailer( Constants.EMAIL_TEMPLATES_PATH );
			hmMailDetails = new HashMap<String, Object>();
			if(jaSendAttachmentReport.isEmpty()) {
				hmMailDetails.put("mailBody", strEmailBody);
				bEmailSent = appedoMailer.sendMail(MODULE_ID.REPORT_STATUS_EMAIL, hmMailDetails, strMobileOrEmail.split(","), strSubject);
			}else {
				hmMailDetails.put("mailBody", strEmailBody+sbAttachmentBody.toString());
				bEmailSent = appedoMailer.sendMailWithAttachment(MODULE_ID.REPORT_STATUS_EMAIL, hmMailDetails, strMobileOrEmail.split(","), strSubject, jaSendAttachmentReport);
			}
			
			System.out.println("mailSent at " + AppedoUtils.formattedDate(System.currentTimeMillis())+" : "+ bEmailSent);
			
			if(bEmailSent) {
				for(int i=0; i<jaSendAttachmentReport.size(); i++) {
					JSONObject joAttachmentFileReport = (JSONObject) jaSendAttachmentReport.get(i);
					File file = new File(joAttachmentFileReport.getString("attachment_path"));
					file.delete();
				}
			}
			
			joResponseData.put("bEmailSent", bEmailSent);
			joResponseData.put("lEndTime", lEndTime);
		}catch (Throwable th) {
			// TODO: handle exception
			LogManager.errorLog(th);
			throw th;
		}
		return joResponseData;
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try {
			/*DateFormat opFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			//DateFormat opFormatter = new SimpleDateFormat("dd-MM-yyyy '@' HH a");
			
			Calendar calNow = Calendar.getInstance();
			long i = System.currentTimeMillis();

			calNow.setTimeInMillis(i);			
			System.out.println(i);
			System.out.println(opFormatter.format(calNow.getTime()));
			
			long db = (1532000127);
			calNow.setTimeInMillis(db*1000);	
			System.out.println(i/1000);
			System.out.println(db);
			System.out.println("after change time : "+opFormatter.format(calNow.getTime()));*/
			
			/*i = i - (i%3600000);
			
			calNow.setTimeInMillis(i);
			System.out.println(i);
			System.out.println(opFormatter.format(calNow.getTime()));*/
			
			String QueryRes = "SELECT 'd.datname' as \"Name\",pg_catalog.pg_get_userbyid(d.datdba) as \"Owner\",";
			
			System.out.println(QueryRes);
			
			System.out.println(QueryRes.replaceAll("['\"]", ""));
		}catch (Throwable th ) {
			System.out.println(th);
			// TODO: handle exception
		}
	}

}
