package com.appedo.retention.utils;

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class AppedoUtils {

	public AppedoUtils() {
		// TODO Auto-generated constructor stub
	}

	private static final char DEFAULT_SEPARATOR = ',';
	
	public static void writeLine(Writer w, List<String> values) throws IOException {
        writeLine(w, values, DEFAULT_SEPARATOR, ' ');
    }
	
	public static void writeLine(Writer w, List<String> values, char separators) throws IOException {
        writeLine(w, values, separators, ' ');
    }
	
	//https://tools.ietf.org/html/rfc4180
    private static String followCVSformat(String value) {

        String result = value;
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"");
        }
        return result;

    }
    
    public static void writeLine(Writer w, List<String> values, char separators, char customQuote) throws IOException {

        boolean first = true;

        //default customQuote is empty

        if (separators == ' ') {
            separators = DEFAULT_SEPARATOR;
        }

        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!first) {
                sb.append(separators);
            }
            if (customQuote == ' ') {
                sb.append(followCVSformat(value));
            } else {
                sb.append(customQuote).append(followCVSformat(value)).append(customQuote);
            }

            first = false;
        }
        sb.append("\n");
        w.append(sb.toString());


    }
    
    public static String formattedDate(long milliSeconds){
		String opDate = "";
		try{
			Calendar calNow = Calendar.getInstance();
			calNow.setTimeInMillis(milliSeconds);
			DateFormat opFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			opDate = opFormatter.format(calNow.getTime());
		}catch(Exception e){
			System.out.println("Exception in nowFormattedDate(): "+e.getMessage());
			e.printStackTrace();
		}
		return opDate;
	}
    
    public static String userFormattedDate(long Seconds){
		String opDate = "";
		DateFormat opFormatter = null;
		try{
			Calendar calNow = Calendar.getInstance();
			calNow.setTimeInMillis(Seconds*1000);
			if(Constants.IS_ADDING_EMIL_DATETIME) {
				calNow.add(Calendar.HOUR_OF_DAY, Constants.ADD_EMAIL_HOURS);
				calNow.add(Calendar.MINUTE, Constants.ADD_EMAIL_MINUTES);
				opFormatter = new SimpleDateFormat("dd-MM-yyyy  HH:mm '"+Constants.ADD_EMAIL_TIMEZONE+"'");
			}else {
				opFormatter = new SimpleDateFormat("dd-MM-yyyy  HH:mm");
			}
			
			//DateFormat opFormatter = new SimpleDateFormat("dd-MM-yyyy '@' HH a");
			//DateFormat opFormatter = new SimpleDateFormat("dd-MM-yyyy '@' HH:mm a");
			
			opDate = opFormatter.format(calNow.getTime());
		}catch(Exception e){
			System.out.println("Exception in nowFormattedDate(): "+e.getMessage());
			e.printStackTrace();
		}
		return opDate;
	}
    
    public static String userFormattedEndTime(long seconds){
		String opDate = "";
		DateFormat opFormatter = null;
		try{
			Calendar calNow = Calendar.getInstance();
			calNow.setTimeInMillis(seconds*1000);
			if(Constants.IS_ADDING_EMIL_DATETIME) {
				calNow.add(Calendar.HOUR_OF_DAY, Constants.ADD_EMAIL_HOURS);
				calNow.add(Calendar.MINUTE, Constants.ADD_EMAIL_MINUTES);
				opFormatter = new SimpleDateFormat("HH:mm '"+Constants.ADD_EMAIL_TIMEZONE+"'");
			}else {
				opFormatter = new SimpleDateFormat("HH:mm");
			}
			
			opDate = opFormatter.format(calNow.getTime());
		}catch(Exception e){
			System.out.println("Exception in nowFormattedDate(): "+e.getMessage());
			e.printStackTrace();
		}
		return opDate;
	}
    
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String name = "[2,3,4,5]";
		
		System.out.println(name.replace("[", "").replace("]", ""));
	}

}
