package com.example.demo.config;

import java.sql.Connection;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@PropertySource("classpath:application.properties")
@EnableTransactionManagement
public class HibernateConfig {

    private static final Logger log = LoggerFactory.getLogger(HibernateConfig.class);

    @Autowired
    private Environment env;

    private final AtomicBoolean usingEmbeddedDatabase = new AtomicBoolean(false);

    @Bean
    public DataSource dataSource() {
        String jdbcUrl = env.getProperty("jdbc.url", "");
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            log.warn("未設定 jdbc.url，改用 H2 記憶體資料庫。");
            usingEmbeddedDatabase.set(true);
            return createEmbeddedDatabase();
        }

        DriverManagerDataSource mysqlDataSource = new DriverManagerDataSource();
        mysqlDataSource.setDriverClassName(env.getProperty("jdbc.driverClassName", "com.mysql.cj.jdbc.Driver"));
        mysqlDataSource.setUrl(jdbcUrl);
        mysqlDataSource.setUsername(env.getProperty("jdbc.username", ""));
        mysqlDataSource.setPassword(env.getProperty("jdbc.password", ""));

        if (canConnect(mysqlDataSource)) {
            log.info("成功連線到 MySQL：{}", jdbcUrl);
            return mysqlDataSource;
        }

        log.warn("無法連線到 MySQL：{}，改用 H2 記憶體資料庫。", jdbcUrl);
        usingEmbeddedDatabase.set(true);
        return createEmbeddedDatabase();
    }

    private boolean canConnect(DataSource dataSource) {
        try (Connection ignored = dataSource.getConnection()) {
            return true;
        } catch (Exception ex) {
            log.debug("資料庫連線測試失敗", ex);
            return false;
        }
    }

    private DataSource createEmbeddedDatabase() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("yingshop")
                .build();
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(dataSource());
        sessionFactory.setPackagesToScan("com.example.demo.model");
        sessionFactory.setHibernateProperties(hibernateProperties());
        return sessionFactory;
    }

    private Properties hibernateProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.show_sql", env.getProperty("hibernate.show_sql", "false"));
        properties.put("hibernate.format_sql", env.getProperty("hibernate.format_sql", "false"));
        properties.put("hibernate.hbm2ddl.auto", env.getProperty("hibernate.hbm2ddl.auto", "update"));
        if (usingEmbeddedDatabase.get()) {
            properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        } else {
            properties.put("hibernate.dialect", env.getProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect"));
        }
        return properties;
    }

    @Bean
    public HibernateTransactionManager transactionManager() {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(sessionFactory().getObject());
        return transactionManager;
    }
}
