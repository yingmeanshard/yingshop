package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import java.util.List;

public interface UserService {
    
    List<User> getAllUsers();
    
    User getUserById(Long id);
    
    User getUserByEmail(String email);

    void saveUser(User user, String rawPassword);

    void updateUser(Long id, User updatedUser, String rawPassword);

    User updateProfile(Long id, String name, String email, String phoneNumber);
    
    void deleteUser(Long id);
    
    boolean emailExists(String email, Long excludeUserId);

    void updatePassword(Long id, String rawPassword);

    void setDefaultAddress(Long userId, Long addressId);

    User registerUser(String name, String email, String rawPassword, UserRole role);
}
