package com.bezkoder.spring.login.admin.bll.servicesimpl;

import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

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


    private Properties properties;

    public EmailService() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find mail.properties");
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void sendEmail(List<String> recipients, String subject, String messageBody) {
        String username = properties.getProperty("mail.smtp.username");
        String password = properties.getProperty("mail.smtp.password");

        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            for (String recipient : recipients) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }
            message.setSubject(subject);
            message.setText(messageBody);

            Transport.send(message);
            System.out.println("Emails sent successfully!");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send HTML email with support for HTML content
     */
    public void sendHtmlEmail(List<String> recipients, String subject, String htmlContent) {
        String username = properties.getProperty("mail.smtp.username");
        String password = properties.getProperty("mail.smtp.password");

        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
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
            System.out.println("HTML emails sent successfully!");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void sendHtmlEmailWithAttachment(List<String> recipients, String subject, String htmlContent,
                                            byte[] attachmentBytes, String attachmentName, String attachmentMime) {
        String username = properties.getProperty("mail.smtp.username");
        String password = properties.getProperty("mail.smtp.password");

        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
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
            System.out.println("HTML emails with attachment sent successfully!");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }



    public void sendPassordinMail(String mail,String user_name,String pass)
    {

        String username = properties.getProperty("mail.smtp.username");
        String password = properties.getProperty("mail.smtp.password");

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

            System.out.println("Done");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
