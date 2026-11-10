package com.seatendance;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class EmailSender {

    // Configure credentials outside the repository using environment variables.
    private static final String SENDER_EMAIL = System.getenv("SEATTENDANCE_SMTP_EMAIL");
    private static final String EMAIL_PASS = System.getenv("SEATTENDANCE_SMTP_PASSWORD");

    public static boolean sendAttendanceReport(String recipientEmail, String subject, String reportBody) {
        if (SENDER_EMAIL == null || SENDER_EMAIL.isBlank()
                || EMAIL_PASS == null || EMAIL_PASS.isBlank()) {
            System.err.println("Email sending is not configured. Set SEATTENDANCE_SMTP_EMAIL and SEATTENDANCE_SMTP_PASSWORD.");
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, EMAIL_PASS);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "SEAttendance System"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Attendance Summary Report - " + subject);
            message.setContent(reportBody, "text/html; charset=UTF-8");
            Transport.send(message);
            System.out.println("Attendance report sent successfully to " + recipientEmail);
            return true;
        } catch (MessagingException | UnsupportedEncodingException e) {
            System.out.println("Error sending email: " + e.getMessage());
            return false;
        }
    }
}
