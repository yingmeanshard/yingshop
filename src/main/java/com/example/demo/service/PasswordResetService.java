package com.example.demo.service;

import com.example.demo.model.PasswordResetToken;
import com.example.demo.model.User;

public interface PasswordResetService {

    PasswordResetToken createToken(User user);

    PasswordResetToken validateToken(String token);

    void sendResetInstructions(PasswordResetToken token, String baseUrl);

    void resetPassword(PasswordResetToken token, String newPassword);
}