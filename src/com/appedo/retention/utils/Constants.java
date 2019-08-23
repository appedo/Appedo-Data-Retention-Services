package com.appedo.retention.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Properties;
import java.util.Timer;

import com.appedo.commons.connect.DataBaseManager;
import com.appedo.commons.manager.AppedoMailer;
import com.appedo.commons.utils.UtilsFactory;
import com.appedo.manager.AppedoConstants;
import com.appedo.manager.LogManager;

import net.sf.json.JSONObject;

/**
 * This class holds the application level variables which required through the application.
 * 
 * @author navin
 *
 */
public class Constants {
	
	public static String CONFIG_FILE_PATH = "";
	public static String RESOURCE_PATH = "";
	public static String APPEDO_CONFIG_FILE_PATH = "";
	public static String SMTP_MAIL_CONFIG_FILE_PATH = "";
	public static String EMAIL_TEMPLATES_PATH = "";
	public static String EMAIL_ATTACHMENT_PATH = "";
	
	//log4j properties file path
	public static String LOG4J_PROPERTIES_FILE = "";
	
	public static String THIS_JAR_PATH = "";
	
	public static String DATA_RETENTION_MAIL_TO_ADDRESSES = "";
	
	public static String HAR_REPOSITORY = "";
	
	public static int SLA_ALERT_SERVICE_RUNTIME_INTERVAL_MS = 5*60000; // one minute 
	
	//public static int SCHEDULER_ALERT_SERVICE_RUNTIME_INTERVAL_MS = 1000 * 60 * 60 * 1; // 1 Hour
	public static int SCHEDULER_ALERT_SERVICE_RUNTIME_INTERVAL_MS = 1000 * 60 * 10; // 10 Minutes
	
	public static HashMap<Integer, Timer> hmTimerTaskObj = new HashMap<Integer, Timer>();
	
	public static int ADD_EMAIL_HOURS;
	
	public static int ADD_EMAIL_MINUTES;
	
	public static String ADD_EMAIL_TIMEZONE;
	
	public static boolean IS_ADDING_EMIL_DATETIME = false;
	
	/**
	 * Loads constants properties 
	 * 
	 * @param srtConstantsPath
	 */
	public static void loadConstantsProperties(String srtConstantsPath) throws Throwable {
		Properties prop = new Properties();
		InputStream is = null;
		
		try {
			is = new FileInputStream(srtConstantsPath);
			prop.load(is);
			
			// Appedo application's resource directory path
			RESOURCE_PATH = prop.getProperty("RESOURCE_PATH");
			
			APPEDO_CONFIG_FILE_PATH = RESOURCE_PATH+prop.getProperty("APPEDO_CONFIG_FILE_PATH");
			
			EMAIL_TEMPLATES_PATH = RESOURCE_PATH+prop.getProperty("EMAIL_TEMPLATES_PATH");
			
			EMAIL_ATTACHMENT_PATH = RESOURCE_PATH+prop.getProperty("EMAIL_ATTACHMENT_PATH");
			
			// Mail configuration
			SMTP_MAIL_CONFIG_FILE_PATH = RESOURCE_PATH+prop.getProperty("SMTP_MAIL_CONFIG_FILE_PATH");
			
			DATA_RETENTION_MAIL_TO_ADDRESSES = prop.getProperty("DATA_RETENTION_MAIL_TO_ADDRESSES");
			
			HAR_REPOSITORY = prop.getProperty("HAR_REPOSITORY");
			
			if(prop.getProperty("ADD_EMAIL_HOURS") != null && prop.getProperty("ADD_EMAIL_MINUTES") != null && prop.getProperty("ADD_TIME_ZONE") != null) {
				ADD_EMAIL_HOURS = Integer.parseInt(prop.getProperty("ADD_EMAIL_HOURS"));
				ADD_EMAIL_MINUTES = Integer.parseInt(prop.getProperty("ADD_EMAIL_MINUTES"));
				ADD_EMAIL_TIMEZONE = prop.getProperty("ADD_TIME_ZONE");
				IS_ADDING_EMIL_DATETIME = true;
			}
			
			//LOG4J_PROPERTIES_FILE = RESOURCE_PATH+prop.getProperty("LOG4J_CONFIG_FILE_PATH");
		} catch(Throwable th) {
			System.out.println("Exception in loadConstantsProperties: "+th.getMessage());
			th.printStackTrace();
			
			throw th;
		} finally {
			UtilsFactory.close(is);
			is = null;
		}
	}
	
	/**
	 * Loads appedo config properties, from the system specifed path, 
	 * (Note.. loads other than db related)
	 *  
	 * @param strAppedoConfigPath
	 */
	public static void loadAppedoConfigProperties(String strAppedoConfigPath) throws Throwable {
		Properties prop = new Properties();
		InputStream is = null;
		
		JSONObject joAppedoCollector = null;
		
		try {
			is = new FileInputStream(strAppedoConfigPath);
			prop.load(is);
			
			if( prop.containsKey("SLA_ALERT_SERVICE_RUNTIME_INTERVAL_SEC") ) { 
				SLA_ALERT_SERVICE_RUNTIME_INTERVAL_MS = Integer.parseInt( AppedoConstants.getAppedoConfigProperty("SLA_ALERT_SERVICE_RUNTIME_INTERVAL_SEC") ) * 1000;
			}
		} catch(Throwable th) {
			throw th;
		} finally {
			UtilsFactory.close(is);
			is = null;
			
			UtilsFactory.clearCollectionHieracy(joAppedoCollector);
			joAppedoCollector = null;
		}	
	}
	
	
	/**
	 * Loads constants, log, DB, mail properties
	 * 
	 * @throws Throwable
	 */
	public static void loadInitProperties(String strApplicationName, boolean autoCommit) throws Throwable {
		Connection con = null;
		
		try {
			// ignored `user.dir`, since may not work
			//AGENT_BASE_DIR = System.getProperty("user.dir");
			
			THIS_JAR_PATH = UtilsFactory.findThisJARPath(Constants.class);
			//THIS_JAR_PATH ="F:/Siddiq/workspace/Appedo-Data-Retention-Services/";
			//THIS_JAR_PATH = "E:/applicationSetups/workspace/Appedo-Data-Retention-Services/";
			System.setProperty("APPEDO_THIS_JAR_HOME", THIS_JAR_PATH);
			
			CONFIG_FILE_PATH = THIS_JAR_PATH+File.separator+"config.properties";
			LOG4J_PROPERTIES_FILE = THIS_JAR_PATH+File.separator+"log4j.properties";
			
			// Loads log4j configuration properties
			LogManager.initializePropertyConfigurator( Constants.LOG4J_PROPERTIES_FILE );
			
			// Loads Constant properties
			System.out.println("Constants.CONFIG_FILE_PATH: "+Constants.CONFIG_FILE_PATH);
			loadConstantsProperties(Constants.CONFIG_FILE_PATH);
			
			// Initiate the DB Pool
			DataBaseManager.doConnectionSetupIfRequired(strApplicationName, APPEDO_CONFIG_FILE_PATH, autoCommit);
			
			// DB-Con to read configurations & white-labels
			con = DataBaseManager.giveConnection();
			
			loadAppedoWhiteLabels(con, null /*APPEDO_CONFIG_FILE_PATH*/);
			
			// Load variables from appedo_config.properties
			loadAppedoConfigProperties(APPEDO_CONFIG_FILE_PATH);
			
			AppedoMailer.loadPropertyFileConstants(SMTP_MAIL_CONFIG_FILE_PATH);
		} catch (Throwable th) {
			throw th;
		} finally {
			DataBaseManager.close(con);
		}
	}
	
	/**
	 * loads AppedoConstants, 
	 *   of loads Appedo whitelabels, replacement of word `Appedo` as configured in DB
	 * 
	 * @param strAppedoConfigPath
	 * @throws Throwable
	 */
	public static void loadAppedoWhiteLabels(Connection con, String strAppedoConfigPath) throws Throwable {
		
		try {
			AppedoConstants.getAppedoConstants().loadAppedoConstants(con);
		} catch (Throwable th) {
			throw th;
		}
	}
	
	/**
	 * Collector in JSON format return as URL
	 * 
	 * @param joAppedoCollector
	 * @return
	 */
	public static String getAsURL(JSONObject joAppedoCollector) {
		return joAppedoCollector.getString("protocol") +"://"+ joAppedoCollector.getString("server") + 
				( joAppedoCollector.getString("port").length() > 0 ? ":"+joAppedoCollector.getString("port") : "" ) + 
				"/"+ joAppedoCollector.getString("application_name");
	}
}
