package com.schoolms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.schoolms")
@EnableJpaRepositories(basePackages = "com.schoolms")
public class SchoolMsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchoolMsApplication.class, args);
    }
}
