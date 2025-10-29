package com.example.demo.service;

public interface MailService {

    void sendPasswordResetEmail(String to, String resetLink);
}