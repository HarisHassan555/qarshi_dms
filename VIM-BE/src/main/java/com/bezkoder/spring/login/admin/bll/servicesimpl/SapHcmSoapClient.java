package com.bezkoder.spring.login.admin.bll.servicesimpl;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Calls SAP RFC web service for HCM employee pad data and returns the SOAP XML response body stream.
 */
@Service
public class SapHcmSoapClient {

    private static final Logger logger = LogManager.getLogger(SapHcmSoapClient.class);

    private static final String DEFAULT_OPERATION = "ZHCM_EMP_INFO_PAD_WS_WORKFLOW";

    @Value("${sap.hcm.soap.endpoint:}")
    private String endpointUrl;

    @Value("${sap.hcm.soap.operation:" + DEFAULT_OPERATION + "}")
    private String soapOperation;

    @Value("${sap.hcm.soap.username:}")
    private String username;

    @Value("${sap.hcm.soap.password:}")
    private String password;

    @Value("${sap.hcm.soap.soap-action:}")
    private String soapAction;

    @Value("${sap.hcm.soap.connect-timeout-ms:60000}")
    private int connectTimeoutMs;

    @Value("${sap.hcm.soap.read-timeout-ms:900000}")
    private int readTimeoutMs;

    public boolean isConfigured() {
        return endpointUrl != null && !endpointUrl.trim().isEmpty()
                && username != null && !username.trim().isEmpty();
    }

    /**
     * POST SOAP request; buffers the response so the connection can close before XML import.
     */
    public InputStream fetchEmployeeSoapXml() throws IOException {
        if (!isConfigured()) {
            throw new IOException("SAP HCM SOAP is not configured (sap.hcm.soap.endpoint / username).");
        }
        HttpURLConnection conn = null;
        try {
            URL url = new URL(resolveBindingEndpointUrl(endpointUrl));
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.setRequestProperty("Accept", "text/xml");

            String auth = username + ":" + password;
            conn.setRequestProperty("Authorization", "Basic "
                    + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8)));

            String action = soapAction == null ? "" : soapAction.trim();
            conn.setRequestProperty("SOAPAction", action.isEmpty() ? "\"\"" : action);

            byte[] payload = buildSoapEnvelope().getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(payload);
            }

            int status = conn.getResponseCode();
            InputStream in = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (status < 200 || status >= 300) {
                String errBody = readStreamSafe(in, 64 * 1024);
                throw new IOException("SAP HCM SOAP HTTP " + status + ": " + abbreviate(errBody, 2000));
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            logger.info("SAP HCM SOAP response size: " + buffer.size() + " bytes");
            return new java.io.ByteArrayInputStream(buffer.toByteArray());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readStreamSafe(InputStream in, int maxBytes) throws IOException {
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int total = 0;
        int read;
        while ((read = in.read(chunk)) != -1 && total < maxBytes) {
            buf.write(chunk, 0, read);
            total += read;
        }
        return buf.toString(StandardCharsets.UTF_8.name());
    }

    private static String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max) + "...";
    }

    private String buildSoapEnvelope() {
        String operation = soapOperation == null || soapOperation.trim().isEmpty()
                ? DEFAULT_OPERATION
                : soapOperation.trim();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:urn=\"urn:sap-com:document:sap:rfc:functions\">"
                + "<soapenv:Header/>"
                + "<soapenv:Body>"
                + "<urn:" + operation + "/>"
                + "</soapenv:Body>"
                + "</soapenv:Envelope>";
    }

    /**
     * Accepts either the RFC binding URL or a WSDL URL that contains {@code /bndg_url/}.
     */
    static String resolveBindingEndpointUrl(String configured) throws IOException {
        if (configured == null) {
            throw new IOException("SAP HCM SOAP endpoint is empty.");
        }
        String trimmed = configured.trim();
        String marker = "/bndg_url/";
        int markerIndex = trimmed.indexOf(marker);
        if (markerIndex < 0) {
            return trimmed;
        }
        URL wsdlUrl = new URL(trimmed);
        String pathAndQuery = trimmed.substring(markerIndex + marker.length());
        if (pathAndQuery.startsWith("/")) {
            pathAndQuery = pathAndQuery.substring(1);
        }
        String protocol = wsdlUrl.getProtocol();
        String host = wsdlUrl.getHost();
        int port = wsdlUrl.getPort();
        String authority = port > 0 ? host + ":" + port : host;
        return protocol + "://" + authority + "/" + pathAndQuery;
    }
}
