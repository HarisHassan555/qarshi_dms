package com.bezkoder.spring.login.util;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author    : Talib Hussain
 * @Date      : Jan 30, 2020
 * @version   : Ver. 1.0.0
 *
 * 						   <center><b>RestTemplateUtils.java</b></center>
 * 						<center><b>Modification History</b></center>
 * <pre>
 *
 * ________________________________________________________________________________________________
 *
 *  Developer				Date		     Version		Operation		Description
 * ________________________________________________________________________________________________ 
 *	
 * 
 * ________________________________________________________________________________________________
 * </pre>
 *
 */
public class UtilRestTemplate {
	
	static HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
	
//	private UtilRestTemplate() {
//		
//		try {
//			TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
//			SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
//			SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
//			CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
//			requestFactory.setHttpClient(httpClient);
//		} catch (Exception e) {
//			e.printStackTrace();
//			System.err.println("An error occurred in disabling SSL Certificate.");
//		}
//		
//		
//	}
	
/**
 * 
 * @author      : Aweem
 * @Date        : Jan 30, 2020
 *
 * @Description :
 *
 * @param url
 * @param data
 * @param header
 * @param queryParams
 * @return
 */
	public static ResponseEntity<String> doPost(String url, Object data, HttpHeaders header, Map<String, String> queryParams) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
		queryParams.entrySet().forEach(obj -> {
			builder.queryParam(obj.getKey(), obj.getValue());
		});
		Map<String, String> uriParams = new HashMap<String, String>();
		URI urlWithParameters = builder.buildAndExpand(uriParams).toUri();

		RestTemplate template = new RestTemplate(requestFactory);
		HttpEntity<Object> requestEntity = new HttpEntity(data, header);
		return template.exchange(urlWithParameters.toString(), HttpMethod.POST, requestEntity,
				String.class);

		
	}
	
	/**
	 * 
	 * @author      : Talib
	 * @Date        : Jan 30, 2020
	 *
	 * @Description :
	 *
	 * @param url
	 * @param data
	 * @param header
	 * @param queryParams
	 * @return
	 */
	public static ResponseEntity<String> doGet(String url, Object data, HttpHeaders header, Map<String, String> queryParams)	{

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
		queryParams.entrySet().forEach(obj -> {
			builder.queryParam(obj.getKey(), obj.getValue());
		});
		Map<String, String> uriParams = new HashMap<String, String>();
		URI urlWithParameters = builder.buildAndExpand(uriParams).toUri();

		RestTemplate template = new RestTemplate(requestFactory);
		HttpEntity<Object> requestEntity = new HttpEntity(data, header);
		return template.exchange(urlWithParameters.toString(), HttpMethod.GET, requestEntity,
				String.class); 
	}
	
	/**
	 * 
	 * @author      : Talib
	 * @Date        : Jan 30, 2020
	 *
	 * @Description :Used for files to download, mostly used in jasper reports downloading 
	 *
	 * @param url
	 * @param data
	 * @param header
	 * @param queryParams
	 * @return
	 */
	public static ResponseEntity<ByteArrayResource> doPostByteArrayResource(String url, Object data, HttpHeaders header, Map<String, String> queryParams) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
		queryParams.entrySet().forEach(obj -> {
			builder.queryParam(obj.getKey(), obj.getValue());
		});
		Map<String, String> uriParams = new HashMap<String, String>();
		URI urlWithParameters = builder.buildAndExpand(uriParams).toUri();

		RestTemplate template = new RestTemplate(requestFactory);
		template.getMessageConverters().add(
	            new ByteArrayHttpMessageConverter());
		
		HttpEntity<Object> requestEntity = new HttpEntity(data, header);
		return template.exchange(urlWithParameters.toString(), HttpMethod.POST, requestEntity,
				ByteArrayResource.class);

	}
	
	/**
	 *
	 * @Description :Used for posting multipart
	 *
	 * @param HttpHeaders headers = new HttpHeaders();
	 * @param LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
	 * @param Map<String,String> queryParams = new HashMap<String,String>()
	 * @return
	 */
	
	public static ResponseEntity<String> doPostMultiPart(String url, LinkedMultiValueMap<String, Object> body, HttpHeaders header, Map<String, String> queryParams) {
		
			header.setContentType(MediaType.MULTIPART_FORM_DATA);			
			Map<String, String> uriParams = new HashMap<String, String>();
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
			URI urlWithParameters = builder.buildAndExpand(uriParams).toUri();

			RestTemplate template = new RestTemplate(requestFactory);
			template.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
			HttpEntity<Object> requestEntity = new HttpEntity(body, header);
			return template.exchange(urlWithParameters.toString(), HttpMethod.POST, requestEntity,
					String.class);
	}
	
	
	public static HttpHeaders getRocketChatRestApiHeader(String xUserId, String xAuthToken) {
		HttpHeaders header = new HttpHeaders();
		
		header.add("Content-Type", "application/json");
		header.add("Accept", "*/*");
		header.add("Accept-Encoding", "gzip, deflate, br");
		header.add("Connection", "keep-alive");
		header.add("X-User-Id", xUserId);
		header.add("X-Auth-Token", xAuthToken);

		return header;
	}
	
	public HttpHeaders getHttpHeaders() {
		HttpHeaders header = new HttpHeaders();
//		header.add("Authorization", "Basic YWRtaW5Ac2lkYXQuY29tLnBrOlNpZGF0QDEyMw==");
		header.add("Authorization", "Bearer YWRtaW5Ac2lkYXQuY29tLnBrOlNpZGF0QDEyMw==");
//		header.add(headerName, headerValue);
//		header.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		header.setContentType(MediaType.APPLICATION_JSON);
//		header.set(CommonConstant.USER_ID, String.valueOf(getUserId()));
//		header.set(CommonConstant.TENANT_ID, getTenantId());
//		header.set(CommonConstant.LANG_ID, String.valueOf(getLanguageId()));
//		header.set(CommonConstant.SESSION, getSession());
		// header.set(CommonConstant.LANG_ORIENTATION, getLanguageOrientation());
		return header;
	}
	
	

}

