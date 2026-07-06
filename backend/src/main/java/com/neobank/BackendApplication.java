//package com.neobank;
//
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.cache.annotation.EnableCaching;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.cache.CacheManager;
//import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
//
//@SpringBootApplication
//@EnableAsync
//@EnableCaching
//public class BackendApplication {
//
//	public static void main(String[] args) {
//		SpringApplication.run(BackendApplication.class, args);
//	}
//
//	@Bean
//	public CacheManager cacheManager() {
//		return new ConcurrentMapCacheManager("loanProducts");
//	}
//}

package com.neobank;

import com.neobank.entity.User;
import com.neobank.enums.Role;
import com.neobank.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableAsync
@EnableCaching
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("loanProducts");
    }

    // Run once to create the default admin account
    @Bean
    CommandLineRunner seedAdmin(UserRepository userRepository,
                                PasswordEncoder passwordEncoder) {

        return args -> {

            if (!userRepository.existsByEmail("admin@neobank.in")) {

                User admin = new User();

                admin.setFullName("NeoBank Admin");
                admin.setEmail("admin@neobank.in");
                admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setRole(Role.ADMIN);
                admin.setIsActive(true);

                // Required fields in your entity
                admin.setAadhaarNumber("123456789012");
                admin.setPanNumber("ABCDE1234F");

                userRepository.save(admin);

                System.out.println("✅ Admin user created successfully.");

            } else {

                System.out.println("ℹ️ Admin already exists.");

            }
        };
    }
}
