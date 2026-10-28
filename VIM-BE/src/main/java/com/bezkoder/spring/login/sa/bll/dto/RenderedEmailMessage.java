package com.bezkoder.spring.login.sa.bll.dto;

public class RenderedEmailMessage {

    private final String subject;
    private final String htmlBody;

    public RenderedEmailMessage(String subject, String htmlBody) {
        this.subject = subject;
        this.htmlBody = htmlBody;
    }

    public String getSubject() {
        return subject;
    }

    public String getHtmlBody() {
        return htmlBody;
    }
}
