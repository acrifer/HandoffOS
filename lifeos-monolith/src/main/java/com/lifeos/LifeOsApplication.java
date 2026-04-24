package com.lifeos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * LifeOS Monolith Application
 * AI-Native Knowledge Management System with RAG capabilities
 */
@SpringBootApplication
@EnableAsync
public class LifeOsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LifeOsApplication.class, args);
    }
}
