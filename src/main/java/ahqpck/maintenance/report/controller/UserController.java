package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.config.UserDetailsImpl;
import ahqpck.maintenance.report.dto.ComplaintDTO;
import ahqpck.maintenance.report.dto.RoleDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.Role;
import ahqpck.maintenance.report.exception.ImportException;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.exception.ValidationException;
import ahqpck.maintenance.report.repository.RoleRepository;
import ahqpck.maintenance.report.service.UserService;
import ahqpck.maintenance.report.util.WebUtil;
import ahqpck.maintenance.report.util.ImportUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Value("${app.upload-user-image.dir:src/main/resources/static/upload/user/image}")
    private String uploadDir;

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    @GetMapping
    public String listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String hiddenColumns,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") String size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "true") boolean asc,
            Authentication authentication,
            Model model) {

        try {
            int zeroBasedPage = page - 1;
            int parsedSize = "All".equalsIgnoreCase(size) ? Integer.MAX_VALUE : Integer.parseInt(size);
            
            String currentUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                currentUserId = userDetails.getId();
            }
            
            // Only fetch current user if needed
            if (currentUserId != null) {
                UserDTO currentUser = userService.getUserById(currentUserId);
                model.addAttribute("currentUser", currentUser);
            }
            
            Page<UserDTO> userPage = userService.getAllUsers(keyword, zeroBasedPage, parsedSize, sortBy, asc);

            model.addAttribute("users", userPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("hiddenColumns", hiddenColumns);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);
            model.addAttribute("title", "User Management");
            model.addAttribute("roles", userService.getAllRoles());

            // Empty DTO for create form
            model.addAttribute("userDTO", new UserDTO());

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load users: " + e.getMessage());
            return "error/500";
        }

        return "user/index";
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENGINEER', 'VIEWER')")
    @GetMapping("/profile")
    public String getUserDetail(Authentication authentication, Model model) {
        try {
            String currentUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                currentUserId = userDetails.getId();
            }

            // Only fetch current user if needed
            if (currentUserId != null) {
                UserDTO currentUser = userService.getUserById(currentUserId);
                model.addAttribute("currentUser", currentUser);
            }
            
            model.addAttribute("title", "User Detail");

            return "user/profile";

        } catch (NotFoundException e) {
            model.addAttribute("error", "User not found: " + e.getMessage());
            return "error/404";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load user: " + e.getMessage());
            return "error/500";
        }
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    @PostMapping
    public String createUser(
            @Valid @ModelAttribute UserDTO userDTO,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes ra) {

        System.out.println("User DTO: " + userDTO);

        if (userDTO.getRoleNames() != null && !userDTO.getRoleNames().isEmpty()) {
            Set<RoleDTO> roleDTOs = userDTO.getRoleNames().stream()
                    .map(name -> {
                        Role.Name roleName = Role.Name.valueOf(name);
                        RoleDTO dto = new RoleDTO();
                        dto.setName(roleName);
                        return dto;
                    })
                    .collect(Collectors.toSet());
            userDTO.setRoles(roleDTOs);
        }

        if (WebUtil.hasErrors(bindingResult)) {
            ra.addFlashAttribute("error", WebUtil.getErrorMessage(bindingResult));
            return "redirect:/users";
        }

        try {
            userService.createUser(userDTO, imageFile);
            ra.addFlashAttribute("success", "User created successfully.");
            return "redirect:/users";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("userDTO", userDTO);
            return "redirect:/users";
        }
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENGINEER')")
    @PostMapping("/update")
    public String updateUser(
            @Valid @ModelAttribute UserDTO userDTO,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "deleteImage", required = false, defaultValue = "false") boolean deleteImage,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            RedirectAttributes ra) {

        System.out.println("User DTO: " + userDTO);
        System.out.println("Current URL: " + redirectUrl);

        if (userDTO.getRoleNames() != null && !userDTO.getRoleNames().isEmpty()) {
            Set<RoleDTO> roleDTOs = userDTO.getRoleNames().stream()
                    .map(name -> {
                        Role.Name roleName = Role.Name.valueOf(name);
                        RoleDTO dto = new RoleDTO();
                        dto.setName(roleName);
                        return dto;
                    })
                    .collect(Collectors.toSet());
            userDTO.setRoles(roleDTOs);
        }

        if (WebUtil.hasErrors(bindingResult)) {
            ra.addFlashAttribute("error", WebUtil.getErrorMessage(bindingResult));
        }

        try {
            userService.updateUser(userDTO, imageFile, deleteImage);
            ra.addFlashAttribute("success", "User updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("userDTO", userDTO);
        }
        return "redirect:" + (redirectUrl != null ? redirectUrl : "/users");
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable String id, RedirectAttributes ra) {
        try {
            userService.deleteUser(id);
            ra.addFlashAttribute("success", "User deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users";
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    @PostMapping("/import")
    public String importWorkReports(
            @RequestParam("data") String dataJson,
            @RequestParam(value = "sheet", required = false) String sheet,
            @RequestParam(value = "headerRow", required = false) Integer headerRow,
            RedirectAttributes ra) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> data = mapper.readValue(dataJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            ImportUtil.ImportResult result = userService.importUsersFromExcel(data);

            if (result.getImportedCount() > 0 && !result.hasErrors()) {
                ra.addFlashAttribute("success",
                        "Successfully imported " + result.getImportedCount() + " work report record(s).");
            } else if (result.getImportedCount() > 0) {
                StringBuilder msg = new StringBuilder("Imported ").append(result.getImportedCount())
                        .append(" record(s), but ").append(result.getErrorMessages().size()).append(" error(s):");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            } else {
                StringBuilder msg = new StringBuilder("Failed to import any work report:");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            }

            return "redirect:/users";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Bulk import failed: " + e.getMessage());
            return "redirect:/users";
        }
    }
}