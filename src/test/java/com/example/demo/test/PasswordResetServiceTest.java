package com.example.demo.test;

import com.example.demo.dao.PasswordResetTokenDAO;
import com.example.demo.model.PasswordResetToken;
import com.example.demo.model.User;
import com.example.demo.service.MailService;
import com.example.demo.service.UserService;
import com.example.demo.service.impl.PasswordResetServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenDAO tokenDAO;

    @Mock
    private MailService mailService;

    @Mock
    private UserService userService;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User user;

    @Before
    public void setup() {
        user = new User();
        user.setId(1L);
        user.setEmail("demo@example.com");
    }

    @Test
    public void createTokenShouldPersistAndNotify() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("abc");
        token.setUser(user);
        when(tokenDAO.save(any())).thenReturn(token);

        PasswordResetToken created = passwordResetService.createToken(user);
        passwordResetService.sendResetInstructions(created, "http://localhost/users/reset");

        verify(tokenDAO).save(any(PasswordResetToken.class));
        verify(mailService).sendPasswordResetEmail(eq("demo@example.com"), contains("token="));
    }

    @Test
    public void validateTokenShouldReturnNullWhenExpired() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expired");
        token.setExpiryDate(Instant.now().minusSeconds(10));
        when(tokenDAO.findByToken("expired")).thenReturn(token);

        PasswordResetToken result = passwordResetService.validateToken("expired");

        assertNull(result);
        verify(tokenDAO).delete(token);
    }

    @Test
    public void resetPasswordShouldEncodeAndRemoveToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("valid");
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusSeconds(60));

        passwordResetService.resetPassword(token, "newPass");

        verify(userService).updatePassword(1L, "newPass");
        verify(tokenDAO).delete(token);
    }
}