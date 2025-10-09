package com.example.demo.dao;

import com.example.demo.model.User;
import java.util.List;

public interface UserDAO {
    
    List<User> findAll();
    
    User findById(Long id);
    
    void save(User user);
    
    void delete(Long id);
}
