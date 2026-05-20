package com.aryan.project7.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// This class is just our general setup for various tools used across the app
@Configuration
public class ProjectConfig {

    // We're setting up ModelMapper as a Bean so we can easily swap
    // Data Transfer Objects (DTOs) with our Entities throughout the project
    @Bean
    public ModelMapper modelMapper(){
        return new ModelMapper();
    }
}