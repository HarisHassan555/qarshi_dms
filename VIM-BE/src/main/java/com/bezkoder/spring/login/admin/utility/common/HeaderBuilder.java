package com.bezkoder.spring.login.admin.utility.common;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.Charset;
import java.util.Collections;
public class HeaderBuilder {

	private static final String CONTENT_TYPE_KEY = "content-type";
    private static final String CONTENT_TYPE_XML = "text/xml";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String AUTHORIZATION = "Authorization";
    private static final String ACCEPT_TYPE = "Accept";

    private MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

    public HeaderBuilder contentType(String contentType) {
        return header(CONTENT_TYPE_KEY, contentType);
    }
    
    public HeaderBuilder acceptType(String acceptType){
    	return header(ACCEPT_TYPE,acceptType);
    }

    public HeaderBuilder withXmlContentType() {
        return contentType(CONTENT_TYPE_XML);
    }

    public HeaderBuilder withJsonContentType() {
        return contentType(CONTENT_TYPE_JSON);
    }

    public HeaderBuilder authorization(String userName, String password) {
        String auth = userName + ":" + password;
        byte[] encodedAuth = Base64.encode(
                auth.getBytes(Charset.forName("US-ASCII")) );
        String authHeader = "Basic " + new String( encodedAuth );
        return header(AUTHORIZATION, authHeader);
    }

    public HeaderBuilder header(String headerKey, String headerValue) {
        headers.put(headerKey, Collections.singletonList(headerValue));
        return this;
    }

    public MultiValueMap<String, String> build() {
        return headers;
    }
}
