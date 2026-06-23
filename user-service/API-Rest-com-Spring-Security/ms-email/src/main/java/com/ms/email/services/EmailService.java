package com.ms.email.services;

import com.ms.email.dtos.EmailRecordDto;
import com.ms.email.enums.StatusEmail;
import com.ms.email.models.EmailModel;
import com.ms.email.repositories.EmailRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class EmailService {

    private final EmailRepository emailRepository;
    private final JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String emailFrom;

    public EmailService(EmailRepository emailRepository, JavaMailSender emailSender) {
        this.emailRepository = emailRepository;
        this.emailSender = emailSender;
    }

    @Transactional
    public EmailModel sendEmail(EmailRecordDto emailRecordDto) {
        EmailModel emailModel = new EmailModel();
        emailModel.setUserId(emailRecordDto.userId());
        emailModel.setEmailTo(emailRecordDto.emailTo());
        emailModel.setSubject(emailRecordDto.subject());
        emailModel.setText(emailRecordDto.text());
        emailModel.setSendDateEmail(LocalDateTime.now());
        emailModel.setEmailFrom(emailFrom);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(emailModel.getEmailTo());
            message.setSubject(emailModel.getSubject());
            message.setText(emailModel.getText());
            emailSender.send(message);

            emailModel.setStatusEmail(StatusEmail.SENT);
        } catch (MailException e) {
            emailModel.setStatusEmail(StatusEmail.ERROR);
            System.err.println("[EMAIL ERROR] " + e.getMessage());
            if (e.getCause() != null) System.err.println("[EMAIL CAUSE] " + e.getCause().getMessage());
        }

        return emailRepository.save(emailModel);
    }
}
