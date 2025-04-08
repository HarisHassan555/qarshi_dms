package com.bezkoder.spring.login.sa.bll.dto;

import javax.xml.soap.SOAPMessage;

public class SOAPRequestResponseWrapper {

    private SOAPMessage soapRequest;
    private SOAPMessage soapResponse;

    public SOAPRequestResponseWrapper(SOAPMessage soapRequest, SOAPMessage soapResponse) {
        this.soapRequest = soapRequest;
        this.soapResponse = soapResponse;
    }

    public SOAPMessage getSoapRequest() {
        return soapRequest;
    }

    public SOAPMessage getSoapResponse() {
        return soapResponse;
    }

    public void setSoapRequest(SOAPMessage soapRequest) {
        this.soapRequest = soapRequest;
    }

    public void setSoapResponse(SOAPMessage soapResponse) {
        this.soapResponse = soapResponse;
    }
}
