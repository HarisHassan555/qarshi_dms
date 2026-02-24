package com.bezkoder.spring.login.admin.bll.servicesimpl;

import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;


@Service
@PropertySource(value = { "classpath:application.properties" })
public class EmailService {


    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private Properties properties;

    public EmailService() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                log.error("Unable to find application.properties for mail settings");
                return;
            }
            Properties allProps = new Properties();
            allProps.load(input);
            for (String key : allProps.stringPropertyNames()) {
                if (key.startsWith("mail.")) {
                    properties.setProperty(key, allProps.getProperty(key));
                }
            }
            if (properties.getProperty("mail.debug") == null) {
                properties.setProperty("mail.debug", "true");
            }
        } catch (IOException ex) {
            log.error("Failed to load mail properties", ex);
        }
    }

    public void sendEmail(List<String> recipients, String subject, String messageBody) {
        String username = sanitizeCredential(properties.getProperty("mail.smtp.username"));
        String password = sanitizeCredential(properties.getProperty("mail.smtp.password"));
        if (username == null || password == null) {
            log.error("Email not sent: missing SMTP username/password");
            return;
        }
        if (recipients == null || recipients.isEmpty()) {
            log.error("Email not sent: recipient list is empty");
            return;
        }

        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            log.info("Sending email subject='{}' to {} recipient(s)", subject, recipients.size());
            for (String recipient : recipients) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }
            message.setSubject(subject);
            message.setText(messageBody);

            Transport.send(message);
            log.info("Emails sent successfully!");

        } catch (MessagingException e) {
            log.error("Failed to send email", e);
        }
    }

    /**
     * Send HTML email with support for HTML content
     */
    public void sendHtmlEmail(List<String> recipients, String subject, String htmlContent) {
        String username = sanitizeCredential(properties.getProperty("mail.smtp.username"));
        String password = sanitizeCredential(properties.getProperty("mail.smtp.password"));
        if (username == null || password == null) {
            log.error("HTML email not sent: missing SMTP username/password");
            return;
        }
        if (recipients == null || recipients.isEmpty()) {
            log.error("HTML email not sent: recipient list is empty");
            return;
        }

        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            log.info("Sending HTML email subject='{}' to {} recipient(s)", subject, recipients.size());
            for (String recipient : recipients) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }
            message.setSubject(subject);
            
            // Set content as HTML
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(htmlContent, "text/html; charset=utf-8");
            
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);

            Transport.send(message);
            log.info("HTML emails sent successfully!");

        } catch (MessagingException e) {
            log.error("Failed to send HTML email", e);
        }
    }

    public void sendHtmlEmailWithAttachment(List<String> recipients, String subject, String htmlContent,
                                            byte[] attachmentBytes, String attachmentName, String attachmentMime) {
        String username = sanitizeCredential(properties.getProperty("mail.smtp.username"));
        String password = sanitizeCredential(properties.getProperty("mail.smtp.password"));
        if (username == null || password == null) {
            log.error("HTML email with attachment not sent: missing SMTP username/password");
            return;
        }
        if (recipients == null || recipients.isEmpty()) {
            log.error("HTML email with attachment not sent: recipient list is empty");
            return;
        }

        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            log.info("Sending HTML email with attachment subject='{}' to {} recipient(s)", subject, recipients.size());
            for (String recipient : recipients) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }
            message.setSubject(subject);

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(htmlContent, "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            if (attachmentBytes != null && attachmentBytes.length > 0) {
                String mime = attachmentMime != null ? attachmentMime : "application/pdf";
                String name = attachmentName != null ? attachmentName : "application.pdf";
                DataSource dataSource = new ByteArrayDataSource(attachmentBytes, mime);
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.setDataHandler(new DataHandler(dataSource));
                attachmentPart.setFileName(name);
                multipart.addBodyPart(attachmentPart);
            }

            message.setContent(multipart);
            Transport.send(message);
            log.info("HTML emails with attachment sent successfully!");
        } catch (MessagingException e) {
            log.error("Failed to send HTML email with attachment", e);
        }
    }

    public void sendHtmlEmailWithInlineImage(List<String> recipients, String subject, String htmlContent,
                                             byte[] imageBytes, String imageMime, String imageContentId) {
        String username = sanitizeCredential(properties.getProperty("mail.smtp.username"));
        String password = sanitizeCredential(properties.getProperty("mail.smtp.password"));
        if (username == null || password == null) {
            log.error("HTML email with inline image not sent: missing SMTP username/password");
            return;
        }
        if (recipients == null || recipients.isEmpty()) {
            log.error("HTML email with inline image not sent: recipient list is empty");
            return;
        }

        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            log.info("Sending HTML email with inline image subject='{}' to {} recipient(s)", subject, recipients.size());
            for (String recipient : recipients) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }
            message.setSubject(subject);

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart("related");
            multipart.addBodyPart(htmlPart);

            if (imageBytes != null && imageBytes.length > 0) {
                String mime = imageMime != null ? imageMime : "image/png";
                String cid = imageContentId != null ? imageContentId : "inline-image";
                DataSource dataSource = new ByteArrayDataSource(imageBytes, mime);
                MimeBodyPart imagePart = new MimeBodyPart();
                imagePart.setDataHandler(new DataHandler(dataSource));
                imagePart.setFileName("capf.png");
                imagePart.setDisposition(MimeBodyPart.INLINE);
                imagePart.setHeader("Content-ID", "<" + cid + ">");
                multipart.addBodyPart(imagePart);
            }

            message.setContent(multipart);
            Transport.send(message);
            log.info("HTML emails with inline image sent successfully!");
        } catch (MessagingException e) {
            log.error("Failed to send HTML email with inline image", e);
        }
    }

    public void sendHtmlEmailWithInlineImageAndAttachment(List<String> recipients, String subject, String htmlContent,
                                                          byte[] imageBytes, String imageMime, String imageContentId,
                                                          byte[] attachmentBytes, String attachmentName, String attachmentMime) {
        String username = sanitizeCredential(properties.getProperty("mail.smtp.username"));
        String password = sanitizeCredential(properties.getProperty("mail.smtp.password"));
        if (username == null || password == null) {
            log.error("HTML email with inline image + attachment not sent: missing SMTP username/password");
            return;
        }
        if (recipients == null || recipients.isEmpty()) {
            log.error("HTML email with inline image + attachment not sent: recipient list is empty");
            return;
        }

        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            log.info("Sending HTML email with inline image + attachment subject='{}' to {} recipient(s)", subject, recipients.size());
            for (String recipient : recipients) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }
            message.setSubject(subject);

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=utf-8");

            MimeMultipart related = new MimeMultipart("related");
            related.addBodyPart(htmlPart);

            if (imageBytes != null && imageBytes.length > 0) {
                String mime = imageMime != null ? imageMime : "image/png";
                String cid = imageContentId != null ? imageContentId : "inline-image";
                DataSource dataSource = new ByteArrayDataSource(imageBytes, mime);
                MimeBodyPart imagePart = new MimeBodyPart();
                imagePart.setDataHandler(new DataHandler(dataSource));
                imagePart.setFileName("inline.png");
                imagePart.setDisposition(MimeBodyPart.INLINE);
                imagePart.setHeader("Content-ID", "<" + cid + ">");
                related.addBodyPart(imagePart);
            }

            MimeBodyPart relatedPart = new MimeBodyPart();
            relatedPart.setContent(related);

            MimeMultipart mixed = new MimeMultipart("mixed");
            mixed.addBodyPart(relatedPart);

            if (attachmentBytes != null && attachmentBytes.length > 0) {
                String mime = attachmentMime != null ? attachmentMime : "application/pdf";
                String name = attachmentName != null ? attachmentName : "application.pdf";
                DataSource dataSource = new ByteArrayDataSource(attachmentBytes, mime);
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.setDataHandler(new DataHandler(dataSource));
                attachmentPart.setFileName(name);
                mixed.addBodyPart(attachmentPart);
            }

            message.setContent(mixed);
            Transport.send(message);
            log.info("HTML email with inline image + attachment sent successfully!");
        } catch (MessagingException e) {
            log.error("Failed to send HTML email with inline image + attachment", e);
        }
    }



    public void sendPassordinMail(String mail,String user_name,String pass)
    {

        String username = sanitizeCredential(properties.getProperty("mail.smtp.username"));
        String password = sanitizeCredential(properties.getProperty("mail.smtp.password"));
        if (username == null || password == null) {
            log.error("Password email not sent: missing SMTP username/password");
            return;
        }
        if (mail == null || mail.trim().isEmpty()) {
            log.error("Password email not sent: recipient is empty");
            return;
        }

        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });


//			 ConnectDB db=new ConnectDB();
//			 ResultSet rs= null;


        try {
            Multipart multipart = new MimeMultipart();



            Message mimeMessage = new MimeMessage(session);

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent("Your account User Name  is "+user_name+" and Password is "+pass, "text/html");


            // code to add attachment...will be revealed later
            MimeBodyPart attachPart = new MimeBodyPart();
            multipart.addBodyPart(messageBodyPart);
            mimeMessage.setContent(multipart);

            mimeMessage.setFrom(new InternetAddress(username)); //add new email
//		      mimeMessage.setFrom(new InternetAddress("mimeMessage.setFrom(new InternetAddress(\"iclportal5@gmail.com\"));"));
            mimeMessage.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(mail));
//
            mimeMessage.setSubject("Account Password");
            Transport.send(mimeMessage);

            log.info("Password email sent to {}", mail);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String sanitizeCredential(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.replaceAll("\\s+", "");
    }
    /*public static void main(String[] args) {
        EmailService emailService = new EmailService();
        emailService.sendEmail(
                List.of("user1@example.com", "user2@example.com"),
                "Test Subject",
                "This is a test email."
        );
    }*/
}
