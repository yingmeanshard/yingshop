package com.example.demo.service.impl;

import com.example.demo.dao.PasswordResetTokenDAO;
import com.example.demo.model.PasswordResetToken;
import com.example.demo.model.User;
import com.example.demo.service.MailService;
import com.example.demo.service.PasswordResetService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Duration EXPIRATION = Duration.ofHours(1);

    private final PasswordResetTokenDAO tokenDAO;
    private final MailService mailService;
    private final UserService userService;

    @Autowired
    public PasswordResetServiceImpl(PasswordResetTokenDAO tokenDAO,
                                    MailService mailService,
                                    UserService userService) {
        this.tokenDAO = tokenDAO;
        this.mailService = mailService;
        this.userService = userService;
    }

    @Override
    public PasswordResetToken createToken(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        PasswordResetToken token = new PasswordResetToken(UUID.randomUUID().toString(),
                user,
                Instant.now().plus(EXPIRATION));
        return tokenDAO.save(token);
    }

    @Override
    public PasswordResetToken validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        PasswordResetToken resetToken = tokenDAO.findByToken(token);
        if (resetToken == null) {
            return null;
        }
        if (resetToken.isExpired()) {
            tokenDAO.delete(resetToken);
            return null;
        }
        return resetToken;
    }

    @Override
    public void sendResetInstructions(PasswordResetToken token, String baseUrl) {
        if (token == null) {
            throw new IllegalArgumentException("Token must not be null");
        }
        String delimiter = baseUrl.contains("?") ? "&" : "?";
        String resetLink = baseUrl + delimiter + "token=" + token.getToken();
        mailService.sendPasswordResetEmail(token.getUser().getEmail(), resetLink);
    }

    @Override
    public void resetPassword(PasswordResetToken token, String newPassword) {
        if (token == null) {
            throw new IllegalArgumentException("Token must not be null");
        }
        if (token.isExpired()) {
            tokenDAO.delete(token);
            throw new IllegalArgumentException("Token 已過期");
        }
        userService.updatePassword(token.getUser().getId(), newPassword);
        tokenDAO.delete(token);
    }
}