// Data Initialization
package com.chikchak.config;

import com.chikchak.model.JobType;
import com.chikchak.model.ServiceType;
import com.chikchak.model.User;
import com.chikchak.model.UserRole;
import com.chikchak.repository.JobTypeRepository;
import com.chikchak.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    // Standardized visit fee for all technicians (MOVERS get ₪0)
    public static final BigDecimal STANDARD_DIAGNOSIS_FEE = new BigDecimal("80");
    public static final BigDecimal MOVERS_DIAGNOSIS_FEE = BigDecimal.ZERO;

    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               JobTypeRepository jobTypeRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            // Create default admin user
            if (!userRepository.existsByEmail("admin@chikchak.com")) {
                User admin = User.builder()
                        .email("admin@chikchak.com")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("ChikChak Admin")
                        .role(UserRole.ADMIN)
                        .preferredLanguage("he")
                        .build();
                userRepository.save(admin);
                log.info("Default admin user created");
            }

            // Seed default job types
            if (jobTypeRepository.count() == 0) {
                seedJobTypes(jobTypeRepository);
                log.info("Default job types seeded");
            }
        };
    }

    private void seedJobTypes(JobTypeRepository jobTypeRepository) {
        java.util.Map<ServiceType, String[]> jobTypesByServiceType = java.util.Map.of(
                ServiceType.PLUMBING, new String[]{
                        "Simple Blockage", "Deep Blockage", "Pipe Replacement",
                        "Faucet Installation", "Toilet Repair", "Water Heater Repair"
                },
                ServiceType.ELECTRICAL, new String[]{
                        "Outlet Installation", "Light Fixture Installation", "Breaker Panel Repair",
                        "Wiring Repair", "Ceiling Fan Installation", "Switch Replacement"
                },
                ServiceType.AC_REPAIR, new String[]{
                        "AC Unit Service", "AC Installation", "Refrigerant Recharge",
                        "Thermostat Replacement", "Duct Cleaning", "Compressor Repair"
                },
                ServiceType.MOVERS, new String[]{
                        "Studio Apartment Move", "1-Bedroom Apartment Move", "2-Bedroom Apartment Move",
                        "Single Item Move", "Office Move", "Packing Service"
                }
        );

        for (var entry : jobTypesByServiceType.entrySet()) {
            for (String jobName : entry.getValue()) {
                JobType jobType = JobType.builder()
                        .name(jobName)
                        .nameHe(jobName) // Placeholder — will be replaced with Hebrew
                        .serviceType(entry.getKey())
                        .build();
                jobTypeRepository.save(jobType);
            }
        }
    }
}
