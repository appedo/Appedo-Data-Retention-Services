package com.appedo.retention.model;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.appedo.commons.manager.AppedoMailer;
import com.appedo.commons.manager.AppedoMailer.EMAILING_OPTION;
import com.appedo.commons.manager.AppedoMailer.MODULE_ID;
import com.appedo.commons.utils.UtilsFactory;
import com.appedo.manager.LogManager;
import com.appedo.retention.dbi.SLADBI;
import com.appedo.retention.utils.Constants;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class SLAManager {
	
	public HashMap<String, Object> formatOADSLAEmail (Connection con, long lUserId, int nAlertFreqInMin, HashMap<String, Object> hmMailDetails) throws Throwable {
		
		SLADBI slaDBI = new SLADBI();
		String strSLAName = null, strTRClass = null;
		StringBuilder sbQuery = new StringBuilder(), sbBreachedCounterDetailsHTML = new StringBuilder(), sbSLAPolicy = new StringBuilder(), sbTDTags = new StringBuilder();
		
		JSONArray jaSLACounters = null, jaSLAs = null;
		JSONObject joSLACounter = null, joUnsentBreachSummary = null;
		Iterator<String> iterSLAs = null;
		
		AppedoMailer appedoMailer = new AppedoMailer( Constants.EMAIL_TEMPLATES_PATH );
		long lMaxAlertLogId = -1;
		boolean bNeedColspan = true;
		int nColspanCnt = 0, nTDLength = 0;
		
		try {
			
			joUnsentBreachSummary = slaDBI.getUnsentOADBreachSummary(con, lUserId);	// Get details required for Consolidated Summary Email
			
			LogManager.infoLog("User-Id ("+lUserId+") have "+joUnsentBreachSummary.size()+" SLAs breached.");
		
			iterSLAs = joUnsentBreachSummary.keySet().iterator();
			
			// loop the SLA-Policies. Start idx as 1, to indicate the row number.
			for(int idx = 1; iterSLAs.hasNext(); idx++ ) {
				strSLAName = iterSLAs.next();
				jaSLAs = joUnsentBreachSummary.getJSONArray(strSLAName);
				
			    if( idx % 2 == 0 ) {
			    	strTRClass = "even";
			    } else {
			    	strTRClass = "odd";
			    }
				
				// Counters in SLA-Policies
				for (int i = 0; i < jaSLAs.size(); i = i + 1) {
					joSLACounter = jaSLAs.getJSONObject(i);
					
					// Get the max of SLA_Alert_Log table's PK. 
					// Need to update all the entries as SENT.
					if ( lMaxAlertLogId < joSLACounter.getLong("max_alert_log_id") ) {
						lMaxAlertLogId = joSLACounter.getLong("max_alert_log_id");
					}
					
					if ( AppedoMailer.getEmailOption() == EMAILING_OPTION.SMTP ) {
						if ( strSLAName.length() > 37 ) {
							bNeedColspan = true;
							nColspanCnt = ((strSLAName.length()-37)/11)+2;
							nTDLength = (9-((strSLAName.length()-37)/11)-2);
						} else {
							bNeedColspan = false;
							nColspanCnt = 1;
							nTDLength = 8;
						}
						
						sbSLAPolicy.append( appedoMailer.replaceMailVariables(MODULE_ID.SLA_CONSOLIDATED_ALERT_COUNTER_DETAILS, joSLACounter) ).append("\n");
					}
				}
				
				if ( AppedoMailer.getEmailOption() == EMAILING_OPTION.SMTP ) {
					joSLACounter.put("colspan_cnt", nColspanCnt);
					joSLACounter.put("sla_breached_counters", sbSLAPolicy.toString());
					
					for( int n = 1; n <= nTDLength; n++ ) {
						
						// Total CRITICAL column, which is the last one
						if( n == nTDLength ) {
							if( joSLACounter.getLong("critical_cnt") > 0 ) { // do bg-coloring only if value is non-zero
								sbTDTags.append("<td style=\"text-align: right; font-family: Arial; font-size:14px !important; background-color: RED; color: #FFF;\"></td>");
							} else {
								sbTDTags.append("<td style=\"text-align: right; font-family: Arial; font-size:14px !important;\"></td>");
							}
						}
						// Total WARNING column, which is last-but-one
						else if ( n == nTDLength-1 ) {
							if( joSLACounter.getLong("warning_cnt") > 0 ) { // do bg-coloring only if value is non-zero
								sbTDTags.append("<td style=\"text-align: right; font-family: Arial; font-size:14px !important; border-right:1px solid #CCC; background-color: ORANGE;\"></td>");
							} else {
								sbTDTags.append("<td style=\"text-align: right; font-family: Arial; font-size:14px !important; border-right:1px solid #CCC;\"></td>");
							}
						}
						// other columns after Counter-Name, till Total-Warning-Cnt
						else {
							sbTDTags.append("<td style=\"text-align: right; font-family: Arial; font-size:14px !important; border-right:1px solid #CCC;\"></td>");
						}
					}
					
					joSLACounter.put("tr_class", strTRClass);
					joSLACounter.put("remaining_td_tags", sbTDTags.toString());
					joSLACounter.put("sla_name", strSLAName);
					sbBreachedCounterDetailsHTML.append( appedoMailer.replaceMailVariables(MODULE_ID.SLA_CONSOLIDATED_ALERT_POLICY_DETAILS, joSLACounter) ).append("\n");
					
					joSLACounter.remove("remaining_td_tags");
					joSLACounter.remove("colspan_cnt");
					joSLACounter.remove("sla_breached_counters");
					joSLACounter.remove("sla_name");
					sbTDTags.setLength(0);
					sbSLAPolicy.setLength(0);
				}
			}
			
			// TODO: Like RUMManager.sendAlerts(), throw new Exception("Uid not found, module has been deleted.");
			// Add to hashmap only when the it has breached data.
			if (joUnsentBreachSummary.size() > 0) {
				hmMailDetails.put("oad_breached", true);
				hmMailDetails.put("breaches", joUnsentBreachSummary);
				hmMailDetails.put("oad_max_id", lMaxAlertLogId);
				hmMailDetails.put("breached_counter_details_html", sbBreachedCounterDetailsHTML.toString());
				hmMailDetails.put("alert_freq_min", nAlertFreqInMin);
			}
			
			
		} catch(Throwable th) {
			LogManager.errorLog(th);
			throw th;
		} finally {
			//UtilsFactory.clearCollectionHieracy(joSLACounter);
			//joSLACounter = null;
			
			UtilsFactory.clearCollectionHieracy(jaSLACounters);
			jaSLACounters = null;
			
			UtilsFactory.clearCollectionHieracy(sbQuery);
			sbQuery = null;
			
		}
		return hmMailDetails;
	}
	
	public HashMap<String, Object> formatLOGSLAEmail (Connection con, long lUserId, int nAlertFreqInMin, HashMap<String, Object> hmMailDetails) throws Throwable {
		
		SLADBI slaDBI = new SLADBI();
		
		String strSLAName = null, strTRClass = null;
		StringBuilder sbQuery = new StringBuilder(), sbBreachedLogDetailsHTML = new StringBuilder(), sbSLAPolicy = new StringBuilder(), sbTDTags = new StringBuilder();
		JSONArray jaSLACounters = null, jaSLAs = null;
		JSONObject joSLACounter = null, joLogUnsentBreachSummary = null;
		Iterator<String> iterSLAs = null;
		
		AppedoMailer appedoMailer = new AppedoMailer( Constants.EMAIL_TEMPLATES_PATH );
		long lMaxAlertLogId = -1;
		boolean bNeedColspan = true;
		int nColspanCnt = 0, nTDLength = 0;
		
		try {
			
			joLogUnsentBreachSummary = slaDBI.getUnsentLOGBreachSummary(con, lUserId);	// Get details required for Consolidated Summary Email
			
			LogManager.infoLog("User-Id ("+lUserId+") have "+joLogUnsentBreachSummary.size()+" SLAs breached.");
		
			iterSLAs = joLogUnsentBreachSummary.keySet().iterator();
			
			// loop the SLA-Policies. Start idx as 1, to indicate the row number.
			for(int idx = 1; iterSLAs.hasNext(); idx++ ) {
				strSLAName = iterSLAs.next();
				jaSLAs = joLogUnsentBreachSummary.getJSONArray(strSLAName);
				
			    if( idx % 2 == 0 ) {
			    	strTRClass = "even";
			    } else {
			    	strTRClass = "odd";
			    }
				
				// Counters in SLA-Policies
				for (int i = 0; i < jaSLAs.size(); i = i + 1) {
					joSLACounter = jaSLAs.getJSONObject(i);
					
					// Get the max of SLA_Alert_Log table's PK. 
					// Need to update all the entries as SENT.
					if ( lMaxAlertLogId < joSLACounter.getLong("max_alert_log_id") ) {
						lMaxAlertLogId = joSLACounter.getLong("max_alert_log_id");
					}
					
					if ( AppedoMailer.getEmailOption() == EMAILING_OPTION.SMTP ) {
						if ( strSLAName.length() > 37 ) {
							bNeedColspan = true;
							nColspanCnt = ((strSLAName.length()-37)/10)+2;
							nTDLength = (8-((strSLAName.length()-37)/10)-2);
						} else {
							bNeedColspan = false;
							nColspanCnt = 1;
							nTDLength = 7;
						}
						
						sbSLAPolicy.append( appedoMailer.replaceMailVariables(MODULE_ID.SLA_CONSOLIDATED_ALERT_LOG_DETAILS, joSLACounter) ).append("\n");
					}
				}
				
				if ( AppedoMailer.getEmailOption() == EMAILING_OPTION.SMTP ) {
					joSLACounter.put("colspan_cnt", nColspanCnt);
					joSLACounter.put("sla_breached_counters", sbSLAPolicy.toString());
					
					for( int n = 1; n <= nTDLength; n++ ) {
						
						// Total CRITICAL column, which is the last one
						if( n == nTDLength ) {
							if( joSLACounter.getLong("critical_cnt") > 0 ) { // do bg-coloring only if value is non-zero
								sbTDTags.append("<td style=\"text-align: right; font-family: Arial; font-size:14px !important; background-color: RED; color: #FFF;\"></td>");
							} else {
								sbTDTags.append("<td style=\"text-align: right; font-family: Arial; font-size:14px !important;\"></td>");
							}
						}
						// Total WARNING column, which is last-but-one
						else if ( n == nTDLength-1 ) {
							if( joSLACounter.getLong("warning_cnt") > 0 ) { // do bg-coloring only if value is non-zero
								sbTDTags.append("<td style=\"text-align: right; font-family: Arial; font-size:14px !important; border-right:1px solid #CCC; background-color: ORANGE;\"></td>");
							} else {
								sbTDTags.append("<td style=\"text-align: right; font-family: Arial; font-size:14px !important; border-right:1px solid #CCC;\"></td>");
							}
						}
						// other columns after Counter-Name, till Total-Warning-Cnt
						else {
							sbTDTags.append("<td style=\"text-align: right; font-family: Arial; font-size:14px !important; border-right:1px solid #CCC;\"></td>");
						}
					}
					
					joSLACounter.put("tr_class", strTRClass);
					joSLACounter.put("remaining_td_tags", sbTDTags.toString());
					joSLACounter.put("sla_name", strSLAName);
					sbBreachedLogDetailsHTML.append( appedoMailer.replaceMailVariables(MODULE_ID.SLA_CONSOLIDATED_ALERT_POLICY_LOG_DETAILS, joSLACounter) ).append("\n");
					
					joSLACounter.remove("remaining_td_tags");
					joSLACounter.remove("colspan_cnt");
					joSLACounter.remove("sla_breached_counters");
					joSLACounter.remove("sla_name");
					sbTDTags.setLength(0);
					sbSLAPolicy.setLength(0);
				}
			}
			
			if (joLogUnsentBreachSummary.size() > 0) {
				hmMailDetails.put("log_breached", true);
				hmMailDetails.put("log_max_id", lMaxAlertLogId);
				hmMailDetails.put("log_breaches", joLogUnsentBreachSummary);
				hmMailDetails.put("breached_log_details_html", sbBreachedLogDetailsHTML.toString());
				hmMailDetails.put("alert_freq_min", nAlertFreqInMin);
			}
			
		} catch(Throwable th) {
			LogManager.errorLog(th);
			throw th;
		} finally {
			//UtilsFactory.clearCollectionHieracy(joSLACounter);
			//joSLACounter = null;
			
			UtilsFactory.clearCollectionHieracy(jaSLACounters);
			jaSLACounters = null;
			
			UtilsFactory.clearCollectionHieracy(sbQuery);
			sbQuery = null;
			
		}
		return hmMailDetails;
	}
	
	public String combineMailContent(HashMap<String, Object> hmMailDetails) {
		
		JSONObject joRtnObj = new JSONObject();
		
		if (hmMailDetails.containsKey("oad_breached")) {
			joRtnObj.putAll(JSONObject.fromObject(hmMailDetails.get("breaches")));
		}
		if (hmMailDetails.containsKey("log_breached")) {
			joRtnObj.putAll(JSONObject.fromObject(hmMailDetails.get("log_breaches")));
		}
		//System.out.println(joRtnObj.size());
		return joRtnObj.toString();
	}
	
	public long getMaxId(HashMap<String, Object> hmMailDetails) {
		
		long lMaxId=-1L;
		
		if (hmMailDetails.containsKey("oad_breached")) {
			lMaxId = lMaxId > (long)hmMailDetails.get("oad_max_id")? lMaxId : (long)hmMailDetails.get("oad_max_id");
		}
		
		if (hmMailDetails.containsKey("log_breached")) {
			lMaxId =  lMaxId > (long)hmMailDetails.get("log_max_id")? lMaxId : (long)hmMailDetails.get("log_max_id");
		}
		
		//System.out.println(joRtnObj.size());
		return lMaxId;
	}
	
	public void sendInternalMonConsolidatedSLAEmail(Connection con, long lUserId, boolean oadSlaBreached, boolean logSlaBreached, int nAlertFreqInMin) throws Throwable {
		SLADBI slaDBI = new SLADBI();
		HashMap<String, String> hmAlertAddress = null;
		HashMap<String, Object> hmMailDetails =  null;
		ArrayList<HashMap<String, String>> alAlertAddresses = null;
		ArrayList<String> alEmailAddress = new ArrayList<String>();
		AppedoMailer appedoMailer = new AppedoMailer( Constants.EMAIL_TEMPLATES_PATH );
		long lMaxAlertLogId = -1;
		String strSubject;
		try {
			hmMailDetails = new HashMap<String, Object>();
			boolean bUserAlerted = slaDBI.isUserAlerted(con, lUserId, nAlertFreqInMin);
			
			if( ! bUserAlerted ) {
				
				if ( AppedoMailer.getEmailOption() == EMAILING_OPTION.SMTP ) {
					hmMailDetails.put("breached_counter_details_html", "");
					hmMailDetails.put("breached_log_details_html", "");
				}
				
				if (oadSlaBreached) {
					System.out.println("OAD metrics breached..");
					hmMailDetails = formatOADSLAEmail(con, lUserId, nAlertFreqInMin, hmMailDetails);
				}
				
				if (logSlaBreached) {
					System.out.println("LOG metrics breached..");
					hmMailDetails = formatLOGSLAEmail(con, lUserId, nAlertFreqInMin, hmMailDetails);
				}
				
				// get user's alert to addresses of emailIds & mobile numbers, with SLA details
				alAlertAddresses = slaDBI.getUserAlertToAddresses(con, lUserId, "Email");
				for (int i = 0; i < alAlertAddresses.size(); i = i + 1) {
					hmAlertAddress = alAlertAddresses.get(i);
					
					alEmailAddress.add( hmAlertAddress.get("emailMobile") );
				}
				
				if( alEmailAddress.size() > 0  && (hmMailDetails.containsKey("oad_breached") || hmMailDetails.containsKey("log_breached"))) {
					strSubject = "FYA: {{ appln_heading }}: SLA Alert";
					
					// Send Email
					appedoMailer.sendMail(MODULE_ID.SLA_CONSOLIDATED_ALERT_EMAIL, hmMailDetails, alEmailAddress.toArray(new String[alEmailAddress.size()]), strSubject);
					
					// Mark all unsent breaches with sla_alert_log_id and Email-Ids.
					slaDBI.updateEmailsSentDetail(con, lUserId, alEmailAddress.toString().substring(1, alEmailAddress.toString().length()-1), combineMailContent(hmMailDetails), getMaxId(hmMailDetails));
				}
			}
	
			
		} catch(Throwable th) {
			LogManager.errorLog(th);
			throw th;
		} finally {
			UtilsFactory.clearCollectionHieracy(alAlertAddresses);
			alAlertAddresses = null;
			
			UtilsFactory.clearCollectionHieracy(hmMailDetails);
			hmMailDetails = null;
			
			appedoMailer = null;
		}
		
	}
	
	/**
	 * Get all SLA-Policy breached user's breach details. 
	 * And send Consolidated-SLA-Email-Alert.
	 * 
	 * @param con
	 * @param lUserId
	 * @param nAlertFreqInMin
	 * @throws Throwable
	 */
	public void sendConsolidatedSLAEmail(Connection con, long lUserId, boolean oadSlaBreached, boolean logSlaBreached, int nAlertFreqInMin) throws Throwable {
		SLADBI slaDBI = new SLADBI();
		
		String strSubject = null, strSLAName = null, strTRClass = null;
		StringBuilder sbQuery = new StringBuilder(), sbBreachedCounterDetailsHTML = new StringBuilder(), sbSLAPolicy = new StringBuilder(), sbTDTags = new StringBuilder();
		
		ArrayList<HashMap<String, String>> alAlertAddresses = null;
		HashMap<String, String> hmAlertAddress = null;
		HashMap<String, Object> hmMailDetails =  null;
		ArrayList<String> alEmailAddress = new ArrayList<String>();
		
		JSONArray jaSLACounters = null, jaSLAs = null;
		JSONObject joSLACounter = null, joUnsentBreachSummary = null;
		Iterator<String> iterSLAs = null;
		
		AppedoMailer appedoMailer = new AppedoMailer( Constants.EMAIL_TEMPLATES_PATH );
		long lMaxAlertLogId = -1;
		boolean bNeedColspan = true;
		int nColspanCnt = 0, nTDLength = 0;
		
		try {
			boolean bUserAlerted = slaDBI.isUserAlerted(con, lUserId, nAlertFreqInMin);
			
			if( ! bUserAlerted ) {
				// breached counters
				joUnsentBreachSummary = slaDBI.getUnsentOADBreachSummary(con, lUserId);	// Get details required for Consolidated Summary Email
				
				if( joUnsentBreachSummary.size() == 0 ) {
					LogManager.infoLog("User-Id ("+lUserId+") does't have any breach.");
				} else {
					LogManager.infoLog("User-Id ("+lUserId+") have "+joUnsentBreachSummary.size()+" SLAs breached.");
					
					iterSLAs = joUnsentBreachSummary.keySet().iterator();
					
					// loop the SLA-Policies. Start idx as 1, to indicate the row number.
					for(int idx = 1; iterSLAs.hasNext(); idx++ ) {
						strSLAName = iterSLAs.next();
						jaSLAs = joUnsentBreachSummary.getJSONArray(strSLAName);
						
						if( idx % 2 == 0 ) {
							strTRClass = "even";
						} else {
							strTRClass = "odd";
						}
						
						// Counters in SLA-Policies
						for (int i = 0; i < jaSLAs.size(); i = i + 1) {
							joSLACounter = jaSLAs.getJSONObject(i);
							
							// Get the max of SLA_Alert_Log table's PK. 
							// Need to update all the entries as SENT.
							if ( lMaxAlertLogId < joSLACounter.getLong("max_alert_log_id") ) {
								lMaxAlertLogId = joSLACounter.getLong("max_alert_log_id");
							}
							
							if ( AppedoMailer.getEmailOption() == EMAILING_OPTION.SMTP ) {
								if ( strSLAName.length() > 37 ) {
									bNeedColspan = true;
									nColspanCnt = ((strSLAName.length()-37)/11)+2;
									nTDLength = (9-((strSLAName.length()-37)/11)-2);
								} else {
									bNeedColspan = false;
									nColspanCnt = 1;
									nTDLength = 8;
								}
								
								sbSLAPolicy.append( appedoMailer.replaceMailVariables(MODULE_ID.SLA_CONSOLIDATED_ALERT_COUNTER_DETAILS, joSLACounter) ).append("\n");
							}
						}
						
						if ( AppedoMailer.getEmailOption() == EMAILING_OPTION.SMTP ) {
							joSLACounter.put("colspan_cnt", nColspanCnt);
							joSLACounter.put("sla_breached_counters", sbSLAPolicy.toString());
							
							for( int n = 1; n <= nTDLength; n++ ) {
								
								// Total CRITICAL column, which is the last one
								if( n == nTDLength ) {
									if( joSLACounter.getLong("critical_cnt") > 0 ) { // do bg-coloring only if value is non-zero
										sbTDTags.append("<td style=\"text-align: right; font-family: Arial; font-size:14px !important; background-color: RED; color: #FFF;\"></td>");
									} else {
										sbTDTags.append("<td style=\"text-align: right; font-family: Arial; font-size:14px !important;\"></td>");
									}
								}
								// Total WARNING column, which is last-but-one
								else if ( n == nTDLength-1 ) {
									if( joSLACounter.getLong("warning_cnt") > 0 ) { // do bg-coloring only if value is non-zero
										sbTDTags.append("<td style=\"text-align: right; font-family: Arial; font-size:14px !important; border-right:1px solid #CCC; background-color: ORANGE;\"></td>");
									} else {
										sbTDTags.append("<td style=\"text-align: right; font-family: Arial; font-size:14px !important; border-right:1px solid #CCC;\"></td>");
									}
								}
								// other columns after Counter-Name, till Total-Warning-Cnt
								else {
									sbTDTags.append("<td style=\"text-align: right; font-family: Arial; font-size:14px !important; border-right:1px solid #CCC;\"></td>");
								}
							}
							
							joSLACounter.put("tr_class", strTRClass);
							joSLACounter.put("remaining_td_tags", sbTDTags.toString());
							joSLACounter.put("sla_name", strSLAName);
							sbBreachedCounterDetailsHTML.append( appedoMailer.replaceMailVariables(MODULE_ID.SLA_CONSOLIDATED_ALERT_POLICY_DETAILS, joSLACounter) ).append("\n");
							
							joSLACounter.remove("remaining_td_tags");
							joSLACounter.remove("colspan_cnt");
							joSLACounter.remove("sla_breached_counters");
							joSLACounter.remove("sla_name");
							sbTDTags.setLength(0);
							sbSLAPolicy.setLength(0);
						}
					}
					
					// TODO: Like RUMManager.sendAlerts(), throw new Exception("Uid not found, module has been deleted.");
					
					hmMailDetails = new HashMap<String, Object>();
					hmMailDetails.put("breaches", joUnsentBreachSummary);
					hmMailDetails.put("breached_counter_details_html", sbBreachedCounterDetailsHTML.toString());
					hmMailDetails.put("alert_freq_min", nAlertFreqInMin);
					
					// get user's alert to addresses of emailIds & mobile numbers, with SLA details
					alAlertAddresses = slaDBI.getUserAlertToAddresses(con, lUserId, "Email");
					for (int i = 0; i < alAlertAddresses.size(); i = i + 1) {
						hmAlertAddress = alAlertAddresses.get(i);
						
						alEmailAddress.add( hmAlertAddress.get("emailMobile") );
					}
					
					if( alEmailAddress.size() > 0 ) {
						strSubject = "FYA: {{ appln_heading }}: SLA Alert";
						
						// Send Email
						appedoMailer.sendMail(MODULE_ID.SLA_CONSOLIDATED_ALERT_EMAIL, hmMailDetails, alEmailAddress.toArray(new String[alEmailAddress.size()]), strSubject);
						
						// Mark all unsent breaches with sla_alert_log_id and Email-Ids.
						slaDBI.updateEmailsSentDetail(con, lUserId, alEmailAddress.toString().substring(1, alEmailAddress.toString().length()-1), hmMailDetails.get("breaches").toString(), lMaxAlertLogId);
					}
				}
			}
		} catch(Throwable th) {
			LogManager.errorLog(th);
			throw th;
		} finally {
			UtilsFactory.clearCollectionHieracy(joSLACounter);
			joSLACounter = null;
			
			UtilsFactory.clearCollectionHieracy(jaSLACounters);
			jaSLACounters = null;
			
			UtilsFactory.clearCollectionHieracy(alAlertAddresses);
			alAlertAddresses = null;
			
			UtilsFactory.clearCollectionHieracy(hmMailDetails);
			hmMailDetails = null;
			
			UtilsFactory.clearCollectionHieracy(sbQuery);
			sbQuery = null;
			
			appedoMailer = null;
		}
	}
	
	/*public static void main(String a[]) {
		HashMap<String, Object> hmMailDetails = new HashMap<String, Object>();
		
		//hmMailDetails.put("oad_breached", true);
		hmMailDetails.put("log_breached", true);
		hmMailDetails.put("breaches", "{\"5401::LogicalDisk:Avg. DiskBytes/Transfer\":[{\"max_alert_log_id\":45112,\"module_code\":\"SERVER\",\"module_name\":\"Sriraman-Laptop\",\"category\":\"LogicalDisk\",\"display_name\":\"Avg. Disk Bytes/Transfer\",\"unit\":\"bytes\",\"warning_cnt\":0,\"critical_cnt\":360,\"cnt_breached\":360,\"warning_threshold_value\":250,\"warning_breached_unit\":\"Bytes\",\"critical_threshold_value\":400,\"critical_breached_unit\":\"Bytes\",\"min_breached_value\":5.86,\"min_breached_unit\":\"KB\",\"avg_breached_value\":13.1,\"avg_breached_unit\":\"KB\",\"max_breached_value\":245.16,\"max_breached_unit\":\"KB\"}],\"5401::Process:ID Process\":[{\"max_alert_log_id\":45118,\"module_code\":\"SERVER\",\"module_name\":\"Sriraman-Laptop\",\"category\":\"Process\",\"display_name\":\"ID Process\",\"unit\":\"number\",\"warning_cnt\":0,\"critical_cnt\":360,\"cnt_breached\":360,\"warning_threshold_value\":0,\"warning_breached_unit\":\"number\",\"critical_threshold_value\":0,\"critical_breached_unit\":\"number\",\"min_breached_value\":3952,\"min_breached_unit\":\"number\",\"avg_breached_value\":3952,\"avg_breached_unit\":\"number\",\"max_breached_value\":3952,\"max_breached_unit\":\"number\"}]}");
		hmMailDetails.put("log_breaches", "{\"5401::LOG:Avg. DiskBytes/Transfer\":[{\"max_alert_log_id\":45112,\"module_code\":\"SERVER\",\"module_name\":\"Sriraman-Laptop\",\"category\":\"LogicalDisk\",\"display_name\":\"Avg. Disk Bytes/Transfer\",\"unit\":\"bytes\",\"warning_cnt\":0,\"critical_cnt\":360,\"cnt_breached\":360,\"warning_threshold_value\":250,\"warning_breached_unit\":\"Bytes\",\"critical_threshold_value\":400,\"critical_breached_unit\":\"Bytes\",\"min_breached_value\":5.86,\"min_breached_unit\":\"KB\",\"avg_breached_value\":13.1,\"avg_breached_unit\":\"KB\",\"max_breached_value\":245.16,\"max_breached_unit\":\"KB\"}],\"5401::LOGProcess:ID Process\":[{\"max_alert_log_id\":45118,\"module_code\":\"SERVER\",\"module_name\":\"Sriraman-Laptop\",\"category\":\"Process\",\"display_name\":\"ID Process\",\"unit\":\"number\",\"warning_cnt\":0,\"critical_cnt\":360,\"cnt_breached\":360,\"warning_threshold_value\":0,\"warning_breached_unit\":\"number\",\"critical_threshold_value\":0,\"critical_breached_unit\":\"number\",\"min_breached_value\":3952,\"min_breached_unit\":\"number\",\"avg_breached_value\":3952,\"avg_breached_unit\":\"number\",\"max_breached_value\":3952,\"max_breached_unit\":\"number\"}]}");
		
		System.out.println(combineMailContent(hmMailDetails));
	}*/
}
