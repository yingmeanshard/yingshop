package com.example.demo.service;

import com.example.demo.model.User;
import java.util.List;

public interface UserService {
    
    List<User> getAllUsers();
    
    User getUserById(Long id);
    
    void saveUser(User user);
    
    void updateUser(Long id, User updatedUser);
    
    void deleteUser(Long id);
}
