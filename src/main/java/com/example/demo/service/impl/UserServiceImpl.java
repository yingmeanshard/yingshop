package com.example.demo.service.impl;

import com.example.demo.dao.UserDAO;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Service
@Transactional
public class UserServiceImpl implements UserService {
	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

	
    @Autowired
    private UserDAO userRepository;
 

    @Override
    public List<User> getAllUsers() {
    	logger.info("info" );
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public void saveUser(User user) {
        userRepository.save(user);
    }

    @Override
    public void updateUser(Long id, User updatedUser) {
        User existingUser = userRepository.findById(id);
        if (existingUser != null) {
            existingUser.setName(updatedUser.getName());
            existingUser.setEmail(updatedUser.getEmail());
            userRepository.save(existingUser);
        }
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.delete(id);
    }
}
