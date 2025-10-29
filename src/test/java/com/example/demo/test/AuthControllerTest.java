package com.example.demo.test;

import com.example.demo.controller.AuthController;
import com.example.demo.model.PasswordResetToken;
import com.example.demo.model.User;
import com.example.demo.service.PasswordResetService;
import com.example.demo.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    public void requestResetShouldRedirectWithMessage() throws Exception {
        User user = new User();
        user.setEmail("demo@example.com");
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("abc");

        when(userService.getUserByEmail("demo@example.com")).thenReturn(user);
        when(passwordResetService.createToken(user)).thenReturn(token);

        mockMvc.perform(post("/users/request-reset").param("email", "demo@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/request-reset"));

        verify(passwordResetService).sendResetInstructions(eq(token), anyString());
    }

    @Test
    public void resetWithInvalidTokenShouldRedirectToRequest() throws Exception {
        when(passwordResetService.validateToken("bad"))
                .thenReturn(null);

        mockMvc.perform(get("/users/reset").param("token", "bad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/request-reset"));
    }
}