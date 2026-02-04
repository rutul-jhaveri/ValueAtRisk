package com.var.calculation.config;

import com.var.calculation.model.entity.User;
import com.var.calculation.model.enums.UserRole;
import com.var.calculation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User user = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user123"))
                    .role(UserRole.USER)
                    .enabled(true)
                    .build();
            userRepository.save(user);
            
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role(UserRole.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            
            System.out.println("✓ Default users created:");
            System.out.println("  USER: username=user");
            System.out.println("  ADMIN: username=admin");
        }
    }
}
