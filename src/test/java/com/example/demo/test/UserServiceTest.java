package com.example.demo.test;

import com.example.demo.dao.UserDAO;
import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.service.impl.UserServiceImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {
	

    @Mock
    private UserDAO userDAO;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void saveUserShouldEncodePasswordAndPersist() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Tester");

        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(userDAO.findByEmail("test@example.com")).thenReturn(null);

        userService.saveUser(user, "secret");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDAO).save(captor.capture());
        assertEquals("encoded-secret", captor.getValue().getPasswordHash());
        assertEquals(UserRole.CUSTOMER, captor.getValue().getRole());
    }

    @Test
    public void saveUserShouldRejectDuplicateEmail() {
        User user = new User();
        user.setEmail("duplicate@example.com");
        user.setName("Tester");

        when(userDAO.findByEmail("duplicate@example.com")).thenReturn(new User());

        expectedException.expect(IllegalArgumentException.class);
        userService.saveUser(user, "secret");
    }

    @Test
    public void setDefaultAddressShouldUpdateUser() {
        User user = new User();
        user.setId(1L);

        when(userDAO.findById(1L)).thenReturn(user);

        userService.setDefaultAddress(1L, 99L);

        assertEquals(Long.valueOf(99L), user.getDefaultAddressId());
        verify(userDAO).save(user);
    }

    @Test
    public void updateProfileShouldReturnUpdatedEntity() {
        User existing = new User();
        existing.setId(5L);
        existing.setEmail("old@example.com");

        when(userDAO.findById(5L)).thenReturn(existing);
        when(userDAO.findByEmail("new@example.com")).thenReturn(null);

        User result = userService.updateProfile(5L, "New Name", "new@example.com", "0987-123456");

        assertSame(existing, result);
        assertEquals("New Name", existing.getName());
        assertEquals("new@example.com", existing.getEmail());
        assertEquals("0987-123456", existing.getPhoneNumber());
        verify(userDAO).save(existing);
    }

    @Test
    public void updateProfileShouldRejectDuplicateEmail() {
        User existing = new User();
        existing.setId(8L);
        existing.setEmail("old@example.com");

        User other = new User();
        other.setId(99L);

        when(userDAO.findById(8L)).thenReturn(existing);
        when(userDAO.findByEmail("duplicate@example.com")).thenReturn(other);

        expectedException.expect(IllegalArgumentException.class);
        userService.updateProfile(8L, "Name", "duplicate@example.com", null);
    }

    @Test
    public void registerUserShouldPersistWithSelectedRole() {
        when(userDAO.findByEmail("admin@example.com")).thenReturn(null);
        when(passwordEncoder.encode("admin-pass")).thenReturn("encoded-admin");

        userService.registerUser("Admin", "admin@example.com", "admin-pass", UserRole.ADMIN);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDAO).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Admin", saved.getName());
        assertEquals(UserRole.ADMIN, saved.getRole());
        assertEquals("encoded-admin", saved.getPasswordHash());
    }

}
