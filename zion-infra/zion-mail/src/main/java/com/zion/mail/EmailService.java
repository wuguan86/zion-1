package com.zion.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * 邮件服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final MailConfigProvider configProvider;

    /**
     * 动态创建 JavaMailSender
     * 根据数据库配置动态创建邮件发送器
     */
    private JavaMailSender createMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // 从配置中获取邮件服务器信息
        String host = configProvider.getEmailHost();
        int port = configProvider.getEmailPort();
        String username = configProvider.getEmailUsername();
        String password = configProvider.getEmailPassword();
        boolean ssl = configProvider.isEmailSsl();
        
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding("UTF-8");
        
        // 配置邮件属性
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.connectiontimeout", "10000");
        
        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", host);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        
        return mailSender;
    }

    /**
     * 发送简单文本邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content 内容
     */
    public void sendSimpleMail(String to, String subject, String content) {
        if (!configProvider.isEmailEnabled()) {
            throw new RuntimeException("邮件服务未启用");
        }
        
        try {
            JavaMailSender mailSender = createMailSender();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(getFromAddress());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("简单邮件发送成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("简单邮件发送失败: to={}, subject={}", to, subject, e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送HTML邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content HTML内容
     */
    public void sendHtmlMail(String to, String subject, String content) {
        if (!configProvider.isEmailEnabled()) {
            throw new RuntimeException("邮件服务未启用");
        }
        
        try {
            JavaMailSender mailSender = createMailSender();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(getFromAddress());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("HTML邮件发送成功: to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("HTML邮件发送失败: to={}, subject={}", to, subject, e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送测试邮件
     *
     * @param to 收件人邮箱
     */
    public void sendTestMail(String to) {
        String siteName = configProvider.getSiteName();
        String subject = "【" + siteName + "】测试邮件";
        String content = "这是一封测试邮件，如果您收到此邮件，说明邮件服务配置正确。\n\n" +
                "发送时间：" + java.time.LocalDateTime.now().toString() + "\n" +
                "发件服务器：" + configProvider.getEmailHost() + "\n" +
                "发件人：" + configProvider.getEmailUsername();
        
        sendSimpleMail(to, subject, content);
    }

    /**
     * 发送验证码邮件
     *
     * @param to     收件人
     * @param code   验证码
     * @param expire 过期时间（分钟）
     */
    public void sendVerifyCode(String to, String code, int expire) {
        String template = configProvider.getEmailTemplateVerifyCode();
        String content = template.replace("{code}", code)
                .replace("{expire}", String.valueOf(expire));
        
        String siteName = configProvider.getSiteName();
        String subject = "【" + siteName + "】验证码";
        
        sendSimpleMail(to, subject, content);
    }

    /**
     * 发送重置密码邮件
     *
     * @param to     收件人
     * @param code   验证码
     * @param expire 过期时间（分钟）
     */
    public void sendResetPassword(String to, String code, int expire) {
        String template = configProvider.getEmailTemplateResetPassword();
        String content = template.replace("{code}", code)
                .replace("{expire}", String.valueOf(expire));
        
        String siteName = configProvider.getSiteName();
        String subject = "【" + siteName + "】重置密码";
        
        sendSimpleMail(to, subject, content);
    }

    /**
     * 获取发件人地址
     */
    private String getFromAddress() {
        String fromName = configProvider.getEmailFromName();
        String username = configProvider.getEmailUsername();
        
        if (fromName != null && !fromName.isEmpty()) {
            return fromName + " <" + username + ">";
        }
        return username;
    }
}

