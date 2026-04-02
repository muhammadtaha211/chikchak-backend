// Data Initialization
package com.chikchak.config;

import com.chikchak.model.JobType;
import com.chikchak.model.ServiceCategory;
import com.chikchak.model.ServiceType;
import com.chikchak.model.User;
import com.chikchak.model.UserRole;
import com.chikchak.repository.JobTypeRepository;
import com.chikchak.repository.ServiceCategoryRepository;
import com.chikchak.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               ServiceCategoryRepository serviceCategoryRepository,
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

            // Create default service categories
            if (serviceCategoryRepository.count() == 0) {
                createDefaultServices(serviceCategoryRepository);
                log.info("Default service categories created");
            } else {
                patchExistingServiceTypes(serviceCategoryRepository);
            }

            // Seed default job types
            if (jobTypeRepository.count() == 0) {
                seedJobTypes(serviceCategoryRepository, jobTypeRepository);
                log.info("Default job types seeded");
            }
        };
    }

    private void createDefaultServices(ServiceCategoryRepository repository) {
        ServiceCategory[] services = {
                ServiceCategory.builder()
                        .serviceType(ServiceType.PLUMBING)
                        .name("Plumbing")
                        .nameHe("שרברבות")
                        .description("Plumbing services including pipe repair, installation, and maintenance")
                        .iconUrl("/icons/plumbing.svg")
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .serviceType(ServiceType.ELECTRICAL)
                        .name("Electrical")
                        .nameHe("חשמל")
                        .description("Electrical services including wiring, installation, and repair")
                        .iconUrl("/icons/electrical.svg")
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .serviceType(ServiceType.AC_REPAIR)
                        .name("AC Repair")
                        .nameHe("תיקון מזגנים")
                        .description("Air conditioning repair, maintenance, and installation")
                        .iconUrl("/icons/ac-repair.svg")
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .serviceType(ServiceType.MOVERS)
                        .name("Movers")
                        .nameHe("הובלות")
                        .description("Moving and relocation services")
                        .iconUrl("/icons/movers.svg")
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .serviceType(ServiceType.CLEANING)
                        .name("Cleaning")
                        .nameHe("ניקיון")
                        .description("Professional cleaning services for homes and offices")
                        .iconUrl("/icons/cleaning.svg")
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .serviceType(ServiceType.CARPENTRY)
                        .name("Carpentry")
                        .nameHe("נגרות")
                        .description("Carpentry and woodworking services")
                        .iconUrl("/icons/carpentry.svg")
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .serviceType(ServiceType.PAINTING)
                        .name("Painting")
                        .nameHe("צביעה")
                        .description("Interior and exterior painting services")
                        .iconUrl("/icons/painting.svg")
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .serviceType(ServiceType.GARDENING)
                        .name("Gardening")
                        .nameHe("גינון")
                        .description("Gardening and landscaping services")
                        .iconUrl("/icons/gardening.svg")
                        .isActive(true)
                        .build()
        };

        for (ServiceCategory service : services) {
            repository.save(service);
        }
    }

    private void seedJobTypes(ServiceCategoryRepository serviceCategoryRepository,
                              JobTypeRepository jobTypeRepository) {
        java.util.Map<ServiceType, String[]> jobTypesByCategory = java.util.Map.of(
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

        for (var entry : jobTypesByCategory.entrySet()) {
            ServiceCategory category = serviceCategoryRepository.findByServiceType(entry.getKey())
                    .orElse(null);
            if (category == null) {
                log.warn("ServiceCategory not found for type: {}", entry.getKey());
                continue;
            }
            for (String jobName : entry.getValue()) {
                JobType jobType = JobType.builder()
                        .name(jobName)
                        .nameHe(jobName) // Placeholder — will be replaced with Hebrew
                        .serviceCategory(category)
                        .build();
                jobTypeRepository.save(jobType);
            }
        }
    }

    private void patchExistingServiceTypes(ServiceCategoryRepository repository) {
        // Patch existing rows that don't have serviceType set yet
        java.util.Map<String, ServiceType> nameToType = java.util.Map.of(
                "Plumbing", ServiceType.PLUMBING,
                "Electrical", ServiceType.ELECTRICAL,
                "AC Repair", ServiceType.AC_REPAIR,
                "Movers", ServiceType.MOVERS,
                "Cleaning", ServiceType.CLEANING,
                "Carpentry", ServiceType.CARPENTRY,
                "Painting", ServiceType.PAINTING,
                "Gardening", ServiceType.GARDENING
        );

        for (ServiceCategory category : repository.findAll()) {
            if (category.getServiceType() == null) {
                ServiceType type = nameToType.get(category.getName());
                if (type != null) {
                    category.setServiceType(type);
                    repository.save(category);
                    log.info("Patched serviceType for: {} -> {}", category.getName(), type);
                } else {
                    log.warn("No matching ServiceType for category: {}", category.getName());
                }
            }
        }
    }
}