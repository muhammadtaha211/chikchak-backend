// Data Initialization
package com.chikchak.config;

import com.chikchak.model.ServiceCategory;
import com.chikchak.model.User;
import com.chikchak.model.UserRole;
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
            }
        };
    }

    private void createDefaultServices(ServiceCategoryRepository repository) {
        ServiceCategory[] services = {
                ServiceCategory.builder()
                        .name("Plumbing")
                        .nameHe("שרברבות")
                        .description("Plumbing services including pipe repair, installation, and maintenance")
                        .iconUrl("/icons/plumbing.svg")
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .name("Electrical")
                        .nameHe("חשמל")
                        .description("Electrical services including wiring, installation, and repair")
                        .iconUrl("/icons/electrical.svg")
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .name("AC Repair")
                        .nameHe("תיקון מזגנים")
                        .description("Air conditioning repair, maintenance, and installation")
                        .iconUrl("/icons/ac-repair.svg")
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .name("Appliance Repair")
                        .nameHe("תיקון מכשירי חשמל")
                        .description("Home appliance repair and maintenance services")
                        .iconUrl("/icons/appliance-repair.svg")
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .name("Cleaning")
                        .nameHe("ניקיון")
                        .description("Professional cleaning services for homes and offices")
                        .iconUrl("/icons/cleaning.svg")
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .name("Carpentry")
                        .nameHe("נגרות")
                        .description("Carpentry and woodworking services")
                        .iconUrl("/icons/carpentry.svg")
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
                        .name("Painting")
                        .nameHe("צביעה")
                        .description("Interior and exterior painting services")
                        .iconUrl("/icons/painting.svg")
                        .isActive(true)
                        .build(),
                ServiceCategory.builder()
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
}