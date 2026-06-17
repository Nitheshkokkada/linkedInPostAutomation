package com.linkedinagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LinkedinAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkedinAgentApplication.class, args);
    }
}
