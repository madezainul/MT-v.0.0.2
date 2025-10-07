package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.dto.WorkReportDTO;
import ahqpck.maintenance.report.dto.AreaDTO;
import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.dto.RoleDTO;
import ahqpck.maintenance.report.entity.Role;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.entity.User.Status;
import ahqpck.maintenance.report.entity.WorkReport;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.repository.RoleRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.specification.UserSpecification;
import ahqpck.maintenance.report.util.EmailUtil;
import ahqpck.maintenance.report.util.FileUploadUtil;
import ahqpck.maintenance.report.util.ImportUtil;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${app.upload-user-image.dir:src/main/resources/static/upload/user/image}")
    private String uploadDir;

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final FileUploadUtil fileUploadUtil;
    private final ImportUtil importUtil;
    private final EmailUtil emailUtil;

    public Page<UserDTO> getAllUsers(String keyword, int page, int size, String sortBy, boolean asc) {
        Sort sort = asc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<User> spec = UserSpecification.search(keyword);
        Page<User> userPage = userRepository.findAll(spec, pageable);

        return userPage.map(this::toDTO);
    }

    public List<RoleDTO> getAllRoles() {
        List<RoleDTO> roles = roleRepository.findAll().stream()
                .map(RoleDTO::new)
                .collect(Collectors.toList());
        return roles;
    }

    public UserDTO getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + id));
        return toDTO(user);
    }

    public void createUser(UserDTO dto, MultipartFile imageFile) {
        if (userRepository.existsByEmployeeIdIgnoringCase(dto.getEmployeeId())) {
            throw new IllegalArgumentException("User with this employee ID already exists.");
        }
        if (userRepository.existsByEmailIgnoringCase(dto.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists.");
        }

        User user = new User();
        mapToEntity(user, dto);

        String token = UUID.randomUUID().toString();
        user.setAccountActivationToken(token);

        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String fileName = fileUploadUtil.saveFile(uploadDir, imageFile, "image");
                user.setImage(fileName);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save image: " + e.getMessage());
            }
        }

        Set<Role> roles = dto.getRoles() == null || dto.getRoles().isEmpty()
                ? Set.of(roleRepository.findByName(Role.Name.VIEWER)
                        .orElseThrow(() -> new IllegalStateException("Default role VIEWER not found")))
                : dto.getRoles().stream()
                        .map(roleDTO -> roleRepository.findByName(Role.Name.valueOf(roleDTO.getName().toString()))
                                .orElseThrow(
                                        () -> new IllegalArgumentException("Role not found: " + roleDTO.getName())))
                        .collect(Collectors.toSet());

        user.getRoles().addAll(roles);
        userRepository.save(user);

        // try {
        // emailUtil.sendAccountActivationEmail(user.getEmail(), token);
        // userRepository.save(user);
        // } catch (Exception e) {
        // // Optional: delete user if email fails?
        // // Or mark as "email_failed" and retry later
        // throw new RuntimeException("Failed to send activation email to " +
        // user.getEmail(), e);
        // }
    }

    // === UPDATE USER ===
    public void updateUser(UserDTO dto, MultipartFile imageFile, boolean deleteImage) {
        String id = dto.getId();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + id));
        System.out.println(dto);

        String newEmployeeId = dto.getEmployeeId().trim();
        String newEmail = dto.getEmail().trim();

        if (!newEmployeeId.equals(user.getEmployeeId()) &&
                userRepository.existsByEmployeeIdIgnoringCase(newEmployeeId)) {
            throw new IllegalArgumentException("Another user with this employee ID already exists.");
        }
        if (!newEmail.equals(user.getEmail()) &&
                userRepository.existsByEmailIgnoringCase(newEmail)) {
            throw new IllegalArgumentException("Another user with this email already exists.");
        }

        mapToEntity(user, dto);

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        String oldImage = user.getImage();
        if (deleteImage && oldImage != null) {
            fileUploadUtil.deleteFile(uploadDir, oldImage);
            user.setImage(null);
        } else if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String newImage = fileUploadUtil.saveFile(uploadDir, imageFile, "image");
                if (oldImage != null) {
                    fileUploadUtil.deleteFile(uploadDir, oldImage);
                }
                user.setImage(newImage);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save image: " + e.getMessage());
            }
        }

        if (dto.getRoles() != null) {
            user.getRoles().clear();
            Set<Role> roles = dto.getRoles().isEmpty()
                    ? Set.of(roleRepository.findByName(Role.Name.VIEWER)
                            .orElseThrow(() -> new IllegalStateException("Default role VIEWER not found")))
                    : dto.getRoles().stream()
                            .map(roleDTO -> roleRepository.findByName(Role.Name.valueOf(roleDTO.getName().toString()))
                                    .orElseThrow(
                                            () -> new IllegalArgumentException("Role not found: " + roleDTO.getName())))
                            .collect(Collectors.toSet());
            user.getRoles().addAll(roles);
        }

        userRepository.save(user);
    }

    public void deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + id));

        if (user.getImage() != null) {
            fileUploadUtil.deleteFile(uploadDir, user.getImage());
        }
        userRepository.delete(user);
    }

    public ImportUtil.ImportResult importUsersFromExcel(List<Map<String, Object>> data) {
        List<String> errorMessages = new ArrayList<>();
        int importedCount = 0;

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data to import.");
        }

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            try {
                UserDTO dto = new UserDTO();

                dto.setName(importUtil.toString(row.get("name")));
                if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                    throw new IllegalArgumentException("Name is mandatory");
                }

                dto.setEmployeeId(importUtil.toString(row.get("employeeId")));
                if (dto.getEmployeeId() == null || dto.getEmployeeId().trim().isEmpty()) {
                    throw new IllegalArgumentException("Employee id is mandatory");
                }

                String email = importUtil.toString(row.get("email"));
                if (email == null || email.trim().isEmpty()) {
                    throw new IllegalArgumentException("Email is mandatory");
                }
                if (!isValidEmail(email)) {
                    throw new IllegalArgumentException("Email should be valid");
                }
                dto.setEmail(email.trim());

                dto.setPassword(importUtil.toString(row.get("password")));
                dto.setImage(importUtil.toString(row.get("image")));
                dto.setCreatedAt(importUtil.toLocalDateTime(row.get("createdAt")));
                dto.setActivatedAt(importUtil.toLocalDateTime(row.get("activatedAt")));

                String statusStr = importUtil.toString(row.get("status"));
                if (statusStr != null && !statusStr.trim().isEmpty()) {
                    try {
                        dto.setStatus(User.Status.valueOf(statusStr.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid Status: " + statusStr + ". Use valid enum values.");
                    }
                }

                dto.setDesignation(importUtil.toString(row.get("designation")));
                dto.setNationality(importUtil.toString(row.get("nationality")));
                dto.setJoinDate(importUtil.toLocalDate(row.get("joinDate")));
                dto.setPhoneNumber(importUtil.toString(row.get("phoneNumber")));

                Set<RoleDTO> roles = new HashSet<>();

                String rolesStr = importUtil.toString(row.get("roles"));
                if (rolesStr != null && !rolesStr.trim().isEmpty()) {
                    for (String roleName : rolesStr.split(",")) {
                        String trimmed = roleName.trim();
                        if (trimmed.isEmpty())
                            continue;

                        try {
                            Role.Name nameEnum = Role.Name.valueOf(trimmed.toUpperCase());
                            Role role = roleRepository.findByName(nameEnum)
                                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + trimmed));

                            RoleDTO roleDTO = new RoleDTO();
                            roleDTO.setId(role.getId());
                            roleDTO.setName(role.getName());
                            roles.add(roleDTO);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("Invalid or missing role: " + trimmed);
                        }
                    }
                }

                dto.setRoles(roles);

                createUser(dto, null);
                importedCount++;

            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "Unknown error";
                errorMessages.add("Row " + (i + 1) + ": " + message);
            }
        }

        return new ImportUtil.ImportResult(importedCount, errorMessages);
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        email = email.trim();
        int at = email.indexOf('@');
        int dot = email.lastIndexOf('.');
        return at > 0 && dot > at + 1 && dot < email.length() - 1;
    }

    private void mapToEntity(User user, UserDTO dto) {
        user.setName(dto.getName().trim());
        user.setEmployeeId(dto.getEmployeeId().trim());
        user.setEmail(dto.getEmail().trim());
        user.setStatus(dto.getStatus());
        user.setDesignation(dto.getDesignation());
        user.setNationality(dto.getNationality());
        user.setJoinDate(dto.getJoinDate());
        user.setPhoneNumber(dto.getPhoneNumber());
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setEmail(user.getEmail());
        dto.setImage(user.getImage());
        dto.setStatus(user.getStatus());
        dto.setDesignation(user.getDesignation());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setActivatedAt(user.getActivatedAt());
        dto.setNationality(user.getNationality());
        dto.setJoinDate(user.getJoinDate());
        dto.setPhoneNumber(user.getPhoneNumber());

        dto.setRoles(user.getRoles().stream()
                .map(RoleDTO::new)
                .collect(Collectors.toSet()));

        return dto;
    }
}