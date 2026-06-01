package com.aryan.project7.config;

import com.aryan.project7.entity.Role;
import com.aryan.project7.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        if (roleRepository.findByName("USER").isEmpty()) {
            roleRepository.save(Role.builder().name("USER").build());
        }
    }
}