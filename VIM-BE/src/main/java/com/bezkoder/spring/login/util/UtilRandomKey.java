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

import org.apache.log4j.Logger;
import org.springframework.data.domain.Sort.Direction;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Description: RandomKey Utility for BTI 
 * Name of Project: BTI
 * Created on: May 11, 2017
 * Modified on: May 12, 2017 5:39:32 PM
 * @author seasia
 * Version: 
 */
public class UtilRandomKey {

	private static Logger log = Logger.getLogger(UtilRandomKey.class);
    /**
     * it return the 6 digit random numeric number
     * @return
     */
	public static String getRandomOrderNumber() {
//		char[] chars = "1234567890".toCharArray();
//		StringBuilder sb = new StringBuilder();
//		Random random = new Random();
//		for (int i = 0; i < 6; i++) {
//			char c = chars[random.nextInt(chars.length)];
//			sb.append(c);
//		}
//		return sb.toString();
		
		System.out.println("Generating OTP using random() : "); 
        System.out.print("You OTP is : "); 
  
        // Using numeric values 
        String numbers = "0123456789"; 
        int len = 6;
  
        // Using random method 
        Random rndm_method = new Random(); 
  
        char[] otp = new char[len]; 
  
        for (int i = 0; i < len; i++) 
        { 
            // Use of charAt() method : to get character value 
            // Use of nextInt() as it is scanning the value as int 
            otp[i] = 
             numbers.charAt(rndm_method.nextInt(numbers.length())); 
        } 
        StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			char c = otp[i];
			sb.append(c);
		}
        return sb.toString();
	}
	
	public static String addZerosBeforeNumber (Integer number) {
		return String.format("%04d", number);
	}

	private SecureRandom random = new SecureRandom();

	/**
	 * @return
	 */
	public String nextRandomKey() {
		return new BigInteger(60, random).toString(32);
	}
	
	/**
	 * it generates encrypted session key
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	private static String nextRandomSessionKey() throws NoSuchAlgorithmException {
		KeyGenerator gen = KeyGenerator.getInstance("DES");
    	gen.init(56); /* 56-bit DES */
		SecretKey secret = gen.generateKey();
		byte[] binary = secret.getEncoded();
		return String.format("%08X", new BigInteger(+1, binary));
	}
	
    /**
     * it return session key string for logged used
     * @return
     */
	public static String generateSessionKey() {
		String key = null;
		try {
			key = nextRandomSessionKey();
		} catch (NoSuchAlgorithmException e) {
			log.error(e);
		}
		return key;
	}

	public static boolean isNotBlank(final String s) {
    	  // Null-safe, short-circuit evaluation.
		return s != null && !s.trim().isEmpty();
	}
	
	public static boolean isNotBlank(final Character c) {
		// Null-safe, short-circuit evaluation.
		return c != null;
	}

	public static boolean isNull(Object object) {
		return (object == null);
	}

	public static boolean isNotNull(Object object) {
		return (object != null);
	}

	public static Integer valueOf(String str) {
		if (isNotBlank(str)) {
			return Integer.valueOf(str);
		}
		return null;
	}
	public static boolean valuePresent(Integer str) {
		if (isNull(str)) {
			return false;
		}
		return true;
	}
	public static boolean validValue(Integer str) {
		if (isNotNull(str) && str.intValue() > 0) {
			return true;
		}
		return false;
	}
	
	public static Long stringToLong(String str) {
		if (isNotBlank(str)) {
			return Long.valueOf(str);
		}
		return null;
	}

	public static String valueOf(Double dbl) {
		if (isNotNull(dbl)) {
			return String.valueOf(dbl);
		}
		return null;
	}

	public static String valueOf(Integer str) {
		if (isNotNull(str)) {
			return String.valueOf(str);
		}
		return null;
	}

	public static Long valueOf(int str) {
		if (isNotNull(str)) {
			return Long.valueOf(str);
		}
		return null;
	}
	public static Integer valueOf(Long str) {
		if (isNotNull(str)) {
			return str.intValue();
		}
		return null;
	}

	public static String longToString(Long str) {
		if (isNotNull(str)) {
			return String.valueOf(str);
		}
		return null;
	}
	
    //Utility function to filter out list of objects by any key
    public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) 
    {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
    
    public static Direction getSortDirection(String sortBy) {
    	if(sortBy.equalsIgnoreCase("DESC")) {
    		return Direction.DESC;
    	}
		return Direction.ASC;
    }
    
    public static String ellipseLongString(String value, int stringLength) {
    	if(value!=null && value.length()>= stringLength+2) {
    		return value.substring(0, stringLength)+"..";	
    	}else {
    		return value;
    	}
    	
    }
    
   public static String generateBarCodeUsingRandomString() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 13;
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
          .limit(targetStringLength)
          .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
          .toString();
        
        return generatedString;

    }
   
   public static String generateBarCodeUsingRandomNumber() {

       String numbers = "0123456789"; 
       int len = 13;
 
       // Using random method 
       Random rndm_method = new Random(); 
 
       char[] otp = new char[len]; 
 
       for (int i = 0; i < len; i++) 
       { 
           // Use of charAt() method : to get character value 
           // Use of nextInt() as it is scanning the value as int 
           otp[i] = 
            numbers.charAt(rndm_method.nextInt(numbers.length())); 
       } 
       StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 13; i++) {
			char c = otp[i];
			sb.append(c);
		}
		
       return sb.toString();

   }
   
   public static <T, U> List<U> convertIntListToStringList(List<T> listOfInteger, Function<T, U> function){
		return listOfInteger.stream()
				.map(function)
				.collect(Collectors.toList());
	}
}
