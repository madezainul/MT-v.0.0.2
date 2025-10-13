package ahqpck.maintenance.report.controller;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ahqpck.maintenance.report.config.UserDetailsImpl;
import ahqpck.maintenance.report.dto.BackupConfigDTO;
import ahqpck.maintenance.report.dto.BackupHistoryDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.BackupConfig;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.service.BackupConfigService;
import ahqpck.maintenance.report.service.BackupHistoryService;
import ahqpck.maintenance.report.service.ComplaintService;
import ahqpck.maintenance.report.service.UserService;
import ahqpck.maintenance.report.repository.BackupConfigRepository;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupConfigService backupConfigService;
    private final BackupHistoryService backupHistoryService;
    private final UserService userService;

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @GetMapping
    public String showBackup(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") String size,
            @RequestParam(defaultValue = "backupDateTime") String sortBy,
            @RequestParam(defaultValue = "false") boolean asc,
            Authentication authentication,
            Model model) {

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

            // Handle backup config
            BackupConfigDTO dto = backupConfigService.getCurrentConfig();
            model.addAttribute("backupConfig", dto);
            model.addAttribute("title", "Backup Configuration");

            // Handle backup history pagination
            int zeroBasedPage = page - 1;
            int parsedSize = "All".equalsIgnoreCase(size) ? Integer.MAX_VALUE : Integer.parseInt(size);
            
            Page<BackupHistoryDTO> backupPage = backupHistoryService.getAllBackups(zeroBasedPage, parsedSize, sortBy, asc);
            
            model.addAttribute("backups", backupPage);
            model.addAttribute("currentPage", page); // 1-based for Thymeleaf
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load backup configuration: " + e.getMessage());
            return "error/500";
        }

        return "backup/config";
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @PostMapping("/save-config")
    public String saveConfig(@ModelAttribute BackupConfigDTO dto, RedirectAttributes ra) {
        try {
            System.out.println("Saving backup configuration: " + dto);
            backupConfigService.saveBackupConfig(dto);
            ra.addFlashAttribute("success", "Backup configuration saved successfully.");
            return "redirect:/backup";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to save configuration: " + e.getMessage());
            ra.addFlashAttribute("backupConfig", dto);
            return "redirect:/backup";
        }
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @PostMapping("/backup-now")
    public String backupNow(@ModelAttribute BackupConfigDTO dto, RedirectAttributes ra) {
        try {
            Set<String> types = Arrays.stream(dto.getBackupTypes().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
                    
            backupConfigService.backupNow(dto.getBackupFolder(), types, "MANUAL");
            ra.addFlashAttribute("success", "Manual backup executed successfully.");
            return "redirect:/backup";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Backup failed: " + e.getMessage());
            ra.addFlashAttribute("backupConfig", dto); // Preserve form data
            return "redirect:/backup";
        }
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @GetMapping("/delete/{id}")
    public String deleteBackup(@PathVariable Long id, RedirectAttributes ra) {
        try {
            backupHistoryService.deleteBackup(id);
            ra.addFlashAttribute("success", "Backup record deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete backup: " + e.getMessage());
        }
        return "redirect:/backup";
    }
}