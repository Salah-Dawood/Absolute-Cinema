package com.cinemasaar.moviesservice.config;

import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

// Transactions protect scheduling and parent-child validation from concurrent edits.
@Configuration
@EnableTransactionManagement
public class MongoConfig {
    @Bean public MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }
}
