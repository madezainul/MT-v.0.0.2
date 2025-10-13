package ahqpck.maintenance.report.config;

import ahqpck.maintenance.report.dto.RoleDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.Role;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.service.UserService;
import ahqpck.maintenance.report.repository.RoleRepository;
import ahqpck.maintenance.report.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${app.default-user.email}")
    private String defaultUserEmail;

    @Value("${app.default-user.name}")
    private String defaultUserName;

    @Value("${app.default-user.employee-id}")
    private String defaultUserEmployeeId;

    @Value("${app.default-user.password}")
    private String defaultUserPassword;

    @Value("${app.default-user.enabled}")
    private boolean defaultUserEnabled;

    @PostConstruct
    public void init() {
        try {
            initDefaultRoles();
            initDefaultUser();
        } catch (Exception e) {
            log.error("Error during data initialization", e);
        }
    }

    private void initDefaultRoles() {
        log.info("Initializing default roles...");
        Arrays.stream(Role.Name.values()).forEach(roleName -> {
            Optional<Role> existingRole = roleRepository.findByName(roleName);
            if (existingRole.isPresent()) {
                log.debug("Role '{}' already exists.", roleName);
                return;
            }

            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
            log.info("Created role: {}", roleName);
        });
        log.info("Default roles initialization completed.");
    }

    private void initDefaultUser() {
        String email = "ggomugo@gmail.com";
        log.info("Checking if default user with email '{}' exists...", email);

        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Default user with email '{}' already exists. Skipping creation.", email);
            return;
        }

        log.info("Creating default user with email '{}'", email);

        // In your init method
        if (!defaultUserEnabled)
            return;

        if (userRepository.findByEmail(defaultUserEmail).isPresent()) {
            log.info("Default user already exists. Skipping.");
            return;
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setName(defaultUserName);
        userDTO.setEmail(defaultUserEmail);
        userDTO.setEmployeeId(defaultUserEmployeeId);
        userDTO.setPassword(defaultUserPassword);
        userDTO.setStatus(User.Status.ACTIVE);
        userDTO.setCreatedAt(LocalDateTime.now());
        userDTO.setActivatedAt(LocalDateTime.now());

        Set<RoleDTO> roleDTOS = Arrays.stream(new Role.Name[] { Role.Name.SUPERADMIN, Role.Name.ADMIN })
                .map(name -> {
                    RoleDTO roleDTO = new RoleDTO();
                    roleDTO.setName(name);
                    return roleDTO;
                })
                .collect(Collectors.toSet());
        userDTO.setRoles(roleDTOS);

        try {
            userService.createUser(userDTO, null);
            log.info("Default user with email '{}' created successfully.", email);
        } catch (Exception e) {
            log.error("Failed to create default user with email '{}'", email, e);
        }
    }
}