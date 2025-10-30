package com.example.demo.service.impl;

import com.example.demo.dao.UserDAO;
import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;



@Service
@Transactional
public class UserServiceImpl implements UserService {
	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserDAO userRepository;
    private final PasswordEncoder passwordEncoder;

	
    @Autowired
    public UserServiceImpl(UserDAO userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
 

    @Override
    public List<User> getAllUsers() {
    	  logger.debug("Fetching all users");
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void saveUser(User user, String rawPassword) {
        ensureEmailUnique(user.getEmail(), null);
        user.setRole(user.getRole());
        user.setPasswordHash(passwordEncoder.encode(requirePassword(rawPassword)));
        userRepository.save(user);
    }

    @Override
    public void updateUser(Long id, User updatedUser, String rawPassword) {
        User existingUser = userRepository.findById(id);
        if (existingUser != null) {
        	 if (!existingUser.getEmail().equalsIgnoreCase(updatedUser.getEmail())) {
                 ensureEmailUnique(updatedUser.getEmail(), id);
             }
            existingUser.setName(updatedUser.getName());
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
            existingUser.setDefaultAddressId(updatedUser.getDefaultAddressId());
            if (updatedUser.getRole() != null) {
                existingUser.setRole(updatedUser.getRole());
            }
            if (rawPassword != null && !rawPassword.isBlank()) {
                existingUser.setPasswordHash(passwordEncoder.encode(rawPassword));
            }
            userRepository.save(existingUser);
        }
    }

    @Override
    public User updateProfile(Long id, String name, String email, String phoneNumber) {
        User existingUser = userRepository.findById(id);
        if (existingUser == null) {
            throw new IllegalArgumentException("找不到使用者");
        }
        if (!existingUser.getEmail().equalsIgnoreCase(email)) {
            ensureEmailUnique(email, id);
        }
        existingUser.setName(name);
        existingUser.setEmail(email);
        existingUser.setPhoneNumber(phoneNumber);
        userRepository.save(existingUser);
        return existingUser;
    }

    
    @Override
    public void deleteUser(Long id) {
        userRepository.delete(id);
    }
    @Override
    public boolean emailExists(String email, Long excludeUserId) {
        User existing = userRepository.findByEmail(email);
        if (existing == null) {
            return false;
        }
        if (excludeUserId == null) {
            return true;
        }
        return !existing.getId().equals(excludeUserId);
    }

    @Override
    public void updatePassword(Long id, String rawPassword) {
        User existingUser = userRepository.findById(id);
        if (existingUser != null) {
            existingUser.setPasswordHash(passwordEncoder.encode(requirePassword(rawPassword)));
            userRepository.save(existingUser);
        }
    }

    @Override
    public void setDefaultAddress(Long userId, Long addressId) {
        User user = userRepository.findById(userId);
        if (user != null) {
            user.setDefaultAddressId(addressId);
            userRepository.save(user);
        }
    }

    @Override
    public User registerUser(String name, String email, String rawPassword, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        if (role != null) {
            user.setRole(role);
        }
        saveUser(user, rawPassword);
        return user;
    }

    private void ensureEmailUnique(String email, Long excludeId) {
        User existing = userRepository.findByEmail(email);
        if (existing != null && (excludeId == null || !existing.getId().equals(excludeId))) {
            throw new IllegalArgumentException("Email重複註冊");
        }
    }

    private String requirePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("密碼不可為空");
        }
        return rawPassword;
    }
}
