package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.config.UserDetailsImpl;
import ahqpck.maintenance.report.service.BOMService;
import ahqpck.maintenance.report.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/bom")
@RequiredArgsConstructor
public class BOMController {

    private final BOMService bomService;
    private final UserService userService;

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENGINEER', 'VIEWER')")
    @GetMapping
    public String index(Authentication authentication, Model model) {
        try {
            String currentUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                currentUserId = userDetails.getId();
            }

            // Only fetch current user if needed
            if (currentUserId != null) {
                var currentUser = userService.getUserById(currentUserId);
                model.addAttribute("currentUser", currentUser);
            }

            var equipments = bomService.getAllEquipmentsForBOM();
            
            // Get comprehensive statistics
            String totalEquipments = bomService.getTotalEquipmentCount();
            String totalParts = bomService.getTotalPartCount();
            String totalBOMEntries = bomService.getTotalBOMEntriesCount();
            String criticalParts = bomService.getCriticalPartsCount();
            
            model.addAttribute("equipments", equipments);
            model.addAttribute("totalEquipments", totalEquipments);
            model.addAttribute("totalParts", totalParts);
            model.addAttribute("totalBOMEntries", totalBOMEntries);
            model.addAttribute("criticalParts", criticalParts);
            model.addAttribute("title", "Bill of Materials");
            
            return "bom/index";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load BOM data: " + e.getMessage());
            model.addAttribute("equipments", java.util.Collections.emptyList());
            model.addAttribute("totalEquipments", "0");
            model.addAttribute("totalParts", "0");
            model.addAttribute("totalBOMEntries", "0");
            model.addAttribute("criticalParts", "0");
            return "bom/index";
        }
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENGINEER', 'VIEWER')")
    @GetMapping("/{equipmentId}")
    public String equipmentDetail(@PathVariable String equipmentId, Authentication authentication, Model model) {
        try {
            String currentUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                currentUserId = userDetails.getId();
            }

            // Only fetch current user if needed
            if (currentUserId != null) {
                var currentUser = userService.getUserById(currentUserId);
                model.addAttribute("currentUser", currentUser);
            }

            var equipment = bomService.getEquipmentWithParts(equipmentId);
            var bomEntries = bomService.getEquipmentBOM(equipmentId);
            var availableParts = bomService.getAvailablePartsForEquipment(equipmentId);
            
            model.addAttribute("equipment", equipment);
            model.addAttribute("bomEntries", bomEntries);
            model.addAttribute("availableParts", availableParts);
            model.addAttribute("title", "Equipment BOM Detail");
            
            return "bom/detail";
        } catch (Exception e) {
            model.addAttribute("error", "Equipment not found: " + e.getMessage());
            return "error/404";
        }
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENGINEER', 'VIEWER')")
    @GetMapping("/part-usage")
    public String partUsage(@RequestParam(required = false) String partId, Authentication authentication, Model model) {
        try {
            String currentUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                currentUserId = userDetails.getId();
            }

            // Only fetch current user if needed
            if (currentUserId != null) {
                var currentUser = userService.getUserById(currentUserId);
                model.addAttribute("currentUser", currentUser);
            }

            // Get all parts for dropdown
            var parts = bomService.getAllPartsForBOM();
            model.addAttribute("parts", parts);
            model.addAttribute("title", "Part Usage Analysis");
            
            if (partId != null && !partId.trim().isEmpty()) {
                var selectedPart = bomService.getPartWithEquipments(partId);
                var usageEntries = bomService.getPartUsage(partId);
                
                model.addAttribute("selectedPart", selectedPart);
                model.addAttribute("usageEntries", usageEntries);
                model.addAttribute("selectedPartId", partId);
            }
            
            return "bom/part-usage";

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load part usage: " + e.getMessage());
            return "error/500";
        }
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENGINEER')")
    @PostMapping("/add-part")
    public String addPartToEquipment(@RequestParam String equipmentId,
                                   @RequestParam String partId,
                                   @RequestParam(defaultValue = "1") Integer quantity,
                                   @RequestParam(required = false) String notes,
                                   @RequestParam(defaultValue = "STANDARD") String criticalityLevel,
                                   RedirectAttributes ra) {
        try {
            bomService.addPartToEquipment(equipmentId, partId, quantity, notes, criticalityLevel);
            ra.addFlashAttribute("success", "Part added to equipment BOM successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bom/" + equipmentId;
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENGINEER')")
    @PostMapping("/remove-part")
    public String removePartFromEquipment(@RequestParam String equipmentId,
                                        @RequestParam String partId,
                                        RedirectAttributes ra) {
        try {
            bomService.removePartFromEquipment(equipmentId, partId);
            ra.addFlashAttribute("success", "Part removed from equipment BOM successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bom/" + equipmentId;
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENGINEER')")
    @PostMapping("/update-bom")
    public String updateBOMEntry(@RequestParam String bomId,
                               @RequestParam(defaultValue = "1") Integer quantity,
                               @RequestParam(required = false) String notes,
                               @RequestParam(defaultValue = "STANDARD") String criticalityLevel,
                               RedirectAttributes ra) {
        try {
            bomService.updateBOMEntry(bomId, quantity, notes, criticalityLevel);
            ra.addFlashAttribute("success", "BOM entry updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bom/equipment";
    }
}