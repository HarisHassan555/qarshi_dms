/**
 * BTI - BAAN for Technology And Trade IntL. 
 * Copyright @ 2017 BTI. 
 * 
 * All rights reserved.
 * 
 * THIS PRODUCT CONTAINS CONFIDENTIAL INFORMATION  OF BTI. 
 * USE, DISCLOSURE OR REPRODUCTION IS PROHIBITED WITHOUT THE 
 * PRIOR EXPRESS WRITTEN PERMISSION OF BTI.
 */
package com.bezkoder.spring.login.util;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.chrono.IslamicChronology;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Description: date time utility
 * Name of Project: BTI
 * Created on: May 12, 2017
 * Modified on: May 12, 2017 3:20:38 PM
 * @author seasia
 * Version: 
 */
public class UtilDateAndTime {

    private static SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
	/**
	 * Method will return date in UTC format
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static Date localToUTC() {
		Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date gmt = new Date(sdf.format(date));
		return gmt;
	}
	
	/**
	 * Method will convert UTC date to local date
	 * @param date
	 * @return
	 */
	public Date utcToLocalDate(Date date) {
		String timeZone = Calendar.getInstance().getTimeZone().getID();
		Date local = new Date(date.getTime() + TimeZone.getTimeZone(timeZone).getOffset(date.getTime()));
		return local;
	}

	/**
	 * @param dateInString
	 * @return
	 * @throws ParseException
	 */
	public static Date stringToDate(String dateInString) throws ParseException {
		Date date = new Date();

		date = formatter.parse(dateInString);

		return date;
	}

	/**
	 * @param date
	 * @return
	 */
	public static Date yyyymmddStringToDate(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		try {
			if (UtilRandomKey.isNotBlank(date)) {
				return formatter.parse(date);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String yyyymmddDateToString(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		try {
			if (UtilRandomKey.isNotNull(date)) {
				return formatter.format(date);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String ddmmyyyyDateToString(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
		try {
			if (UtilRandomKey.isNotNull(date)) {
				return formatter.format(date);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @param date
	 * @return
	 */
	public static Date ddmmyyyyStringToDate(String date) {
		if (UtilRandomKey.isNotBlank(date)) {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
		try {
				return formatter.parse(date);
		} catch (Exception e) {
			e.printStackTrace();
			return yyyymmddStringToDate(date);
		
		}
		}
		return null;
	}

	/**
	 * @param Date object
	 * @return formatted Date object
	 */
	public static Date ddmmyyyyStringToDate(Date date) {
		if (UtilRandomKey.isNotNull(date)) {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
			try {
				String dateStr = dateToStringddmmyyyy(date);
				return formatter.parse(dateStr);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static Date atStartOfDay(Date date) {
	    LocalDateTime localDateTime = dateToLocalDateTime(date);
	    LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
	    return localDateTimeToDate(startOfDay);
	}

	public static Date atEndOfDay(Date date) {
	    LocalDateTime localDateTime = dateToLocalDateTime(date);
	    LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
	    return localDateTimeToDate(endOfDay);
	}

	public static Date atStartOfDay(String date) {
	    LocalDateTime localDateTime = stringToLocalDateTime(date);
	    LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
	    return localDateTimeToDate(startOfDay);
	}

	public static Date atEndOfDay(String date) {
	    LocalDateTime localDateTime = stringToLocalDateTime(date);
	    LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
	    return localDateTimeToDate(endOfDay);
	}

	
	public static java.sql.Date dateToSqlDate(Date date) {
		if (UtilRandomKey.isNotNull(date)) {
			return new java.sql.Date(date.getTime( ));
		}
		return null;
	}

	/**
	 * @param date
	 * @return
	 */
	public static String dateToStringddmmyyyy(Date date) {
		if (UtilRandomKey.isNotNull(date)) {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
		try {
			return  formatter.format(date);
		} catch (Exception e) {
			e.printStackTrace();
		}
		}
		return null;
	}

	public static String dateToStringddmmyyyy(String date) {
		if(StringUtils.isEmpty( date )) {
			return null;	
		}
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		try {
			Date date1 = dbFormat.parse(date);
			return formatter.format(date1);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;		
	}
	
	public static String dateToStringyyyymmdd(String date) {
		if(StringUtils.isEmpty( date )) {
			return null;	
		}
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		try {
			Date date1 = formatter.parse(date);
			return dbFormat.format(date1);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;		
	}
	
	public static String dateToStringddmmyyyy(LocalDate date) {
		if (UtilRandomKey.isNotNull(date)) { 
			try {
                return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static String localDateToStringyyyyMMdd(LocalDate date) {
		if (UtilRandomKey.isNotNull(date)) {
			try {
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * @param date
	 * @return
	 */
	public static Date yyyymmddStringToDate2(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
		try {
			Date date1 = formatter.parse(date);
			return date1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


	/**
	 * @param date
	 * @return
	 */

	public static String dateToStringddmmyyyy(LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
		try {
			if (UtilRandomKey.isNotNull(date)) {
				return date.format(formatter);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param date
	 * @return
	 */
	public static String dateToStringyyyymmdd(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		try {
			return  formatter.format(date);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * used to convert java.util.Date to java.time.LocalDateTime
	 * @param Date
	 * @return
	 */
	public static LocalDateTime dateToLocalDateTime(Date date) {
		if (UtilRandomKey.isNotNull(date)) {
		        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	        }
		return null;
	}

	public static Date LocalDateToDate(LocalDate dateToConvert) {
		return java.sql.Date.valueOf(dateToConvert);
	}

	/**
	 * used to convert java.time.LocalDateTime to java.util.Date
	 * @param LocalDateTime
	 * @return
	 */
	public static Date localDateTimeToDate(LocalDateTime date) {
		if (UtilRandomKey.isNotNull(date)) {
		        return Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
	        }
		return null;
	}

	/**
	 * @param date
	 * @return
	 */
	public static Date ddmmyyyyStringTimeToDate(String time) {
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm aa", Locale.ENGLISH);
		try {
			Date date1 = formatter.parse(time);
			return date1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param date
	 * @return
	 */
	public static String dateToStringhhmmaa(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm aa", Locale.ENGLISH);
		try {
			String formatDate = formatter.format(date);
			return formatDate;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param time
	 * @return
	 */
	public static Time getTimeFromStringFrom12Formats(String time) {
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm aa", Locale.ENGLISH);
		try {
			Date date1 = formatter.parse(time);
			return new Time(date1.getTime());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * @param time
	 * @return
	 */
	public static String convertTimeToString12Formats(Time time) {
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss", Locale.ENGLISH);
        SimpleDateFormat outPutFormat = new SimpleDateFormat("hh:mm aa", Locale.ENGLISH);
		try {
			Date date1 = formatter.parse(""+time);
			return outPutFormat.format(date1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param date
	 * @return
	 */
	public static String convertDateDDMMYYYYFormatToDBFormat(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		try {
			Date date1 = formatter.parse(date);
			return dbFormat.format(date1);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @param time
	 * @return
	 */
	public static String convertTimeToString24Formats(Date time) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        SimpleDateFormat outPutFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
		try {
			Date date1 = formatter.parse(""+time);
			return outPutFormat.format(date1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @param time
	 * @return
	 */
	public static String convertDateTimeToStringTime24Formats(Date time) {
        SimpleDateFormat outPutFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
		try {
			return outPutFormat.format(time);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * @param time
	 * @return
	 */
	public static Time getTimeFromStringFrom24Formats(String time) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
		try {
			Date date1 = formatter.parse(time);
			return new Time(date1.getTime());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @param time
	 * @return
	 */
	public static Date getStaticDateWithTimeFromTime24Formats(String time) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
		try {
			Date date1 = formatter.parse(time);
			return date1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param date
	 * @return
	 */
	public static Date ddmmyyyyhhmmssStringToDate(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH);
		try {
			Date date1 = formatter.parse(date);
			return date1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @param date
	 * @return   2020-06-19 00:00:00.0
	 */
	public static String ddmmyyyyhhmmssStringToLocalDateTime(LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		String formattedDateTime = date.format(formatter);
		return formattedDateTime;
	}
	
	public static Date yyyymmddhhmmssStringToDate(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S", Locale.ENGLISH);
		try {
			Date date1 = formatter.parse(date);
			return date1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String ddmmyyyyhhmmssStringToLocalSADateTime(Date date) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
			//sdf.setTimeZone(TimeZone.getTimeZone("Asia/Riyadh"));           
			return (sdf.format(date)); 
		} catch (Exception e) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
				//sdf.setTimeZone(TimeZone.getTimeZone("Asia/Riyadh"));           
				return (sdf.format(date)); 
			} catch (Exception e2) {
				e.printStackTrace();
			}
		}
		
		return null;
		
	}
	
	public static String ddmmyyyyhhmmssStringTransactionDateTime(Date transactionDate, Date createdDate) {
		
		try {
			String time = UtilDateAndTime.convertTime(transactionDate);
			if (time.equals("00:00:00"))
				time = UtilDateAndTime.convertTime(createdDate);
			
			String date = UtilDateAndTime.yyyymmddDateToString(transactionDate);
			Date oldFormatDate = (new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")).parse(date + " " + time);
		    return ((new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")).format(oldFormatDate).toString());
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
		 
	}
	public static String convertTime(Date transactionDate) {
		SimpleDateFormat localDateFormat = new SimpleDateFormat("HH:mm:ss");
        String time = localDateFormat.format(transactionDate);
        return (time);
	}
	
	public static Date ddmmyyyyhhmmssDateTransactionDateTime(Date transactionDate, Date createdDate) {
		
		try {
			String time = UtilDateAndTime.convertTime(transactionDate);
			if (time.equals("00:00:00"))
				time = UtilDateAndTime.convertTime(createdDate);
			
			String date = UtilDateAndTime.yyyymmddDateToString(transactionDate);
			return ((new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")).parse(date + " " + time));
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
		 
	}
	
	public static Date ddmmyyyyhhmmssStringToDateTimeZone(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
		try {
			Date date1 = formatter.parse(date);
			return date1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String ddmmyyyyhhmmssStringTransactionDateTime(String strTransactionDate, String strCreatedDate) {
		
		try {
			Date transactionDate = UtilDateAndTime.ddmmyyyyhhmmssStringToDateTimeZone(strTransactionDate);
			Date createdDate = UtilDateAndTime.ddmmyyyyhhmmssStringToDateTimeZone(strCreatedDate);
			
			String time = UtilDateAndTime.convertTime(transactionDate);
			if (time.equals("00:00:00"))
				time = UtilDateAndTime.convertTime(createdDate);
			
			String date = UtilDateAndTime.yyyymmddDateToString(transactionDate);
			Date oldFormatDate = (new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")).parse(date + " " + time);
		    return ((new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")).format(oldFormatDate).toString());
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
		 
	}

	public static String localDateTimeToyyyymmddhhmmssString(LocalDateTime date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		String formattedDateTime = date.format(formatter);
		return formattedDateTime;
	}
	public static String convertTimeToString12Formats(Date time) {
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss", Locale.ENGLISH);
        SimpleDateFormat outPutFormat = new SimpleDateFormat("hh:mm aa", Locale.ENGLISH);
		try {
			Date date1 = formatter.parse("" + time);
			return outPutFormat.format(date1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}



	public static String convertDateToStringTime24Formats(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
		try {
			String HHMMSS = formatter.format(date);
			return HHMMSS;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


	public static Integer getWeekDayNumber(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
		return dayOfWeek;
	}

	public static boolean chechCurrentTimeLiesBetweenTwoTimes(String startTime, String endTime, String currentTime) {
		try {
            Date time1 = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).parse(startTime);
			Calendar calendar1 = Calendar.getInstance();
			calendar1.setTime(time1);

            Date time2 = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).parse(endTime);
			Calendar calendar2 = Calendar.getInstance();
			calendar2.setTime(time2);
			calendar2.add(Calendar.DATE, 1);

            Date d = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).parse(currentTime);
			Calendar calendar3 = Calendar.getInstance();
			calendar3.setTime(d);
			calendar3.add(Calendar.DATE, 1);

			Date x = calendar3.getTime();
			if (x.after(calendar1.getTime()) && x.before(calendar2.getTime())) {
				// checkes whether the current time is between 14:49:00 and 20:11:13.
				return true;
			}

		} catch (ParseException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static Date startDate(Integer noOfDays, Date hireDate) {
		Calendar c = Calendar.getInstance();
		if (noOfDays != 0) {
			c.setTime(hireDate); // Now hire date.
			c.add(Calendar.DATE, noOfDays); // Adding days
			hireDate = c.getTime();
		}
		return hireDate;
	}

	public static Date endDate(Integer noOfDays, Integer endDateDays, Date hireDate) {
		Calendar c = Calendar.getInstance();
		if (noOfDays != 0) {
			c.setTime(hireDate); // Now hire date.
			c.add(Calendar.DATE, noOfDays); // Adding days
			hireDate = c.getTime();
		}
		if (endDateDays != 0) {
			c.setTime(hireDate);
			c.add(Calendar.DATE, endDateDays);
			hireDate = c.getTime();

		}
		return hireDate;
	}

	public static Long startDate2(Integer noOfDays, Date hireDate) {
		Calendar c = Calendar.getInstance();
		if (noOfDays != 0) {
			c.setTime(hireDate); // Now hire date.
			c.add(Calendar.DATE, noOfDays); // Adding days
			hireDate = c.getTime();
		}
		return hireDate.getTime();
	}

	public static Long endDate2(Integer noOfDays, Integer endDateDays, Date hireDate) {
		Calendar c = Calendar.getInstance();
		if (noOfDays != 0) {
			c.setTime(hireDate); // Now hire date.
			c.add(Calendar.DATE, noOfDays); // Adding days
			hireDate = c.getTime();
		}
		if (endDateDays != 0) {
			c.setTime(hireDate);
			c.add(Calendar.DATE, endDateDays);
			hireDate = c.getTime();

		}
		return hireDate.getTime();
	}

	public static int getDiffInYears(Date first, Date last) {
		Calendar a = getCalendar(first);
		Calendar b = getCalendar(last);
		int diff = b.get(Calendar.YEAR) - a.get(Calendar.YEAR);
		if (a.get(Calendar.MONTH) > b.get(Calendar.MONTH)
				|| (a.get(Calendar.MONTH) == b.get(Calendar.MONTH) && a.get(Calendar.DATE) > b.get(Calendar.DATE))) {
			diff--;
		}
		return diff;
	}

	public static Calendar getCalendar(Date date) {
		Calendar cal = Calendar.getInstance(Locale.US);
		cal.setTime(date);
		return cal;
	}

	public static long differenceOfDaysbetweenDates(Date firstDate, Date secondDate) throws IOException {
		return ChronoUnit.DAYS.between(firstDate.toInstant(), secondDate.toInstant());
	}

	/* default format to follow */
	public static LocalDateTime stringToLocalDateTime(String date) {
		if (UtilRandomKey.isNotBlank(date)) {
			
			return dateToLocalDateTime(ddmmyyyyStringToDate(date));
		}
		return null;
	}
	
	/* default format to follow */
	public static LocalDateTime stringDdmmyyyyStringToLocalDateTime(String date) {
		if (UtilRandomKey.isNotBlank(date)) {
			
			return dateToLocalDateTime(ddmmyyyyStringToDate(date));
		}
		return null;
	}

	
	public static LocalDate dateToLocalDate(Date date) {

		return new java.sql.Date(date.getTime()).toLocalDate();
	
	}
	
//	DD/MM/yyyy = default date or application date format 
	public static Date parseAnyDateToAppDate(String date) {

		Date dateObj = null;
		try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
			dateObj = formatter.parse(date);
		}catch(Exception e) {
			try {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
				dateObj = formatter.parse(date);
			}catch(Exception a) {
				a.printStackTrace();
			}
		}
		
		return dateObj;
	
	}
	public static Date copyTimeToDate(Date date, Date time) {
	    Calendar t = Calendar.getInstance();
	    t.setTime(time);

	    Calendar c = Calendar.getInstance();
	    c.setTime(date);
	    c.set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY));
	    c.set(Calendar.MINUTE, t.get(Calendar.MINUTE));
	    c.set(Calendar.SECOND, t.get(Calendar.SECOND));
	    c.set(Calendar.MILLISECOND, t.get(Calendar.MILLISECOND));
	    return c.getTime();
	}
	
	public static Date gregorianDateToHijri(Date date) {
		if(date!=null) {
			 Chronology iso = ISOChronology.getInstanceUTC();
		     Chronology hijri = IslamicChronology.getInstanceUTC();
		     LocalDateTime localDateTime = dateToLocalDateTime(date);
		     org.joda.time.LocalDate todayIso = new org.joda.time.LocalDate(localDateTime.getYear(), localDateTime.getMonthValue(), localDateTime.getDayOfMonth(), iso);
		     org.joda.time.LocalDate todayHijri = new org.joda.time.LocalDate(todayIso.toDateTimeAtStartOfDay(),   hijri);
		     return todayHijri.toDate();
		}
	    return null;
	}
	
	public static String convertDateToDateTimeString(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss.SSS", Locale.ENGLISH).format(date);
	}
	
	public static String getTimeDifference(Date startDate, Date endDate) {
		String time = "";
		long differenceInMillis = endDate.getTime() - startDate.getTime(); 
	    long differenceInSeconds = (differenceInMillis  / 1000) % 60; 
	    long differenceInMinutes = (differenceInMillis / (1000 * 60)) % 60;
	    
	    time = "Total Time (in Millis): " + differenceInMillis 
	    		+ " ~ (in Secs): " + differenceInSeconds 
	    		+ " ~ (in Mins): " + differenceInMinutes;
	    
	    return time;
	}

	public static long getTimeDifferenceInMillis(Date startDate, Date endDate) {
		return endDate.getTime() - startDate.getTime(); 
	}
	
	public static long getTimeDifferenceInSeconds(Date startDate, Date endDate) {
	    return ((endDate.getTime() - startDate.getTime()) / 1000); 
	}
	
	public static long getTimeDifferenceInMinutes(Date startDate, Date endDate) {
		return ((endDate.getTime() - startDate.getTime()) / (1000 * 60));
	}

	public static long getTimeDifferenceInHours(Date startDate, Date endDate) {
		return ((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60));
	}

	public static long getTimeDifferenceInDays(Date startDate, Date endDate) {
		return ((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24));
	}

	public static String getTotalTimeDifferenceInUnits(Date startDate, Date endDate) {
		String time = "";
		long days = getTimeDifferenceInDays(startDate, endDate);
		long hours = getTimeDifferenceInHours(startDate, endDate);
		long mins = getTimeDifferenceInMinutes(startDate, endDate);
		long secs = getTimeDifferenceInSeconds(startDate, endDate);
		long millis = getTimeDifferenceInMillis(startDate, endDate);
		
		if (days > 0) {
			time = "(in days): " + days + " days, ~ ";
		}
		
		if (hours > 0) {
			time = time + "(in hours): " + hours  + " hours, ~ ";
		}
		
		if (mins > 0) {
			time = time + "(in mins): " + mins + " mins, ~ ";
		}
		
		if (secs > 0) {
			time = time + "(in seconds): " + secs + " seconds, ~ ";
		}
		
		if (millis > 0) {
			time = time + "(in milli seconds): " + millis + " millis";
		}

		return time;
	}
	

	public static Date dateWithoutTime(Date date) {
		DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		try {
			return formatter.parse(formatter.format(date));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static Date getLastDayOfYear(Date date) {
		DateTime dateTime = new DateTime(date);
		DateTime lastDate_ofyear = dateTime.dayOfYear().withMaximumValue();
		return  lastDate_ofyear.toDate();
	}
	
	public static Date getFirstDayOfYear(Date date) {
		DateTime dateTime = new DateTime(date);
		DateTime firstDate_ofyear = dateTime.dayOfYear().withMinimumValue();
		return  firstDate_ofyear.toDate();
	}

    /**
     * This method will be used to get the date and time with the <code>Locale.ENGLISH</code>
     * 
     * @return <code>Date</code>
     */
    public static Date getCurrentDate() {

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
        date = new Date(sdf.format(date));

        if (UtilRandomKey.isNotNull(date)) {
            return (date);
        }
        return null;
    }
    
	@SuppressWarnings("deprecation")
	public static Date getFirstDayOfMonth(Date date) {
		Calendar result = Calendar.getInstance();
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, date.getYear() + 1900);
		calendar.set(Calendar.MONTH, date.getMonth());
		calendar.set(Calendar.DATE, date.getDate());

		result.set(Calendar.DATE, calendar.getActualMinimum(Calendar.DATE));
		result.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
		result.set(Calendar.YEAR, calendar.get(Calendar.YEAR));

		return dateWithoutTime(result.getTime());
	}

	public static Date getLastDayOfMonth(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MONTH, 1);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.add(Calendar.DATE, -1);
		Date lastDayOfMonth = dateWithoutTime(calendar.getTime());
		return lastDayOfMonth;
	}
	
	public static Date getPreviousDayDate(Date date) {
		Calendar calendar = Calendar.getInstance();
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(date);
		cal1.add(Calendar.DAY_OF_YEAR, -1);
		Date previousDate = cal1.getTime();
		return previousDate;
	}

	public static String convertSecondsToHMmSs(long seconds) {
	    long s = seconds % 60;
	    long m = (seconds / 60) % 60;
	    long h = (seconds / (60 * 60)) % 24;
	    return String.format("%d:%02d:%02d", h,m,s);
	}
	public static int  calculateAgeFromDateOfBirth(Date dateOfBirth) throws ParseException {
	
	  Calendar c = Calendar.getInstance();
	  c.setTime(dateOfBirth);
	  int year = c.get(Calendar.YEAR);
	  int month = c.get(Calendar.MONTH) + 1;
	  int date = c.get(Calendar.DATE);
	  LocalDate l1 = LocalDate.of(year, month, date);
	  LocalDate now1 = LocalDate.now();
	  Period diff1 = Period.between(l1, now1);
	  
	  return diff1.getYears();
	 }
	public static String getDateToStringDateDDMMYYYY(Date d)
	{
		SimpleDateFormat form = new SimpleDateFormat("dd/MM/yyyy");
	    System.out.println(form.format(d));
	    String str = form.format(d); 
	    return str;
	}
	
	public static int getMonthFromDate(Date d)
	{
		LocalDate currentLocalDate = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		int currentMonth = currentLocalDate.getMonthValue();
		 return currentMonth;
	}
	
	public static String getMonthNameFromDate(Date d)
	{
		LocalDate currentLocalDate = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		int month = currentLocalDate.getMonthValue();
		String monthName = DateTime.now().withMonthOfYear(month).toString("MMM");
		 return monthName;
	}
	public static long getDifferenceOfDaysFromTwoStringddmmyyyyy(String stringDateddmmyyyy1 ,String stringDateddmmyyyy2) {
		LocalDateTime date1 = UtilDateAndTime.stringToLocalDateTime(stringDateddmmyyyy1);
		LocalDateTime date2 = UtilDateAndTime.stringToLocalDateTime(stringDateddmmyyyy2);
		return Duration.between(date1, date2).toDays();		  
	}
	
	public static String yyyymmddhhmmssToddmmyy(Date src) {
		  SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		  SimpleDateFormat formatterDisplay = new SimpleDateFormat("dd/MM/yyyy");
		  try {
			  Date date = formatter.parse(src.toString());
			 return formatterDisplay.format(date);
		  } catch(Exception e) {
			  e.printStackTrace();
		  }
		  return null;
	}
	
	
}