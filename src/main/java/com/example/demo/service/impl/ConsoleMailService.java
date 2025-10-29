package com.example.demo.service.impl;

import com.example.demo.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleMailService.class);

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        log.info("Sending password reset link to {} -> {}", to, resetLink);
    }
}