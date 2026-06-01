package com.aryan.project7;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// The heart of the application.
// @SpringBootApplication tells Spring to look for configurations,
// enable autoconfiguration, and scan for components in this package.
@SpringBootApplication
@EnableScheduling // FIX: This enables the @Scheduled tasks (like token cleanup)
public class Project7Application {

	public static void main(String[] args) {
		// This line launches the embedded Tomcat server and starts the app
		SpringApplication.run(Project7Application.class, args);
	}

}