package com.example.demo.config;

import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.InitializingBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserService userService;

    @Autowired
    public AdminAccountInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void afterPropertiesSet() {
        User existing = userService.getUserByEmail("ying");
        if (existing == null) {
            userService.registerUser("管理者", "ying", "55688", UserRole.ADMIN);
            log.info("Default admin account 'ying' has been created.");
        }
    }
}
