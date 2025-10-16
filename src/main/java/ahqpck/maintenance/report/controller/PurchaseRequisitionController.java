package ahqpck.maintenance.report.controller;

import java.util.Arrays;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ahqpck.maintenance.report.config.UserDetailsImpl;
import ahqpck.maintenance.report.dto.PurchaseRequisitionDTO;
import ahqpck.maintenance.report.dto.PurchaseRequisitionPartDTO;
import ahqpck.maintenance.report.entity.PurchaseRequisition.PRStatus;
import ahqpck.maintenance.report.entity.PurchaseRequisitionPart.CriticalityLevel;
import ahqpck.maintenance.report.service.EquipmentService;
import ahqpck.maintenance.report.service.PartService;
import ahqpck.maintenance.report.service.QuotationRequestService;
import ahqpck.maintenance.report.service.PurchaseRequisitionService;
import ahqpck.maintenance.report.service.UserService;
import ahqpck.maintenance.report.util.WebUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/purchase-requisition")
public class PurchaseRequisitionController {

    private final PurchaseRequisitionService prService;
    private final EquipmentService equipmentService;
    private final PartService partService;
    private final UserService userService;
    private final QuotationRequestService qrService;

    // Smart Dashboard - Entry point
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'REVIEWER', 'USER')")
    @GetMapping
    public String showDashboard(Authentication authentication, Model model) {
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

            model.addAttribute("title", "Purchase Requisition Dashboard");

            // Statistics for dashboard cards
            model.addAttribute("totalPRs", prService.getTotalPRsCount());
            model.addAttribute("pendingApproval", prService.getPendingApprovalCount());
            model.addAttribute("actionRequired", prService.getActionRequiredCount());

            // Action Required Section (submitted PRs needing approval + rejected PRs)
            Page<PurchaseRequisitionDTO> actionRequiredPRs = prService.getActionRequired(0, 5);
            model.addAttribute("actionRequiredPRs", actionRequiredPRs.getContent());

            // Monitoring Section (approved PRs)
            Page<PurchaseRequisitionDTO> approvedPRs = prService.getPurchaseRequisitionsByStatus(PRStatus.APPROVED, 0, 5);
            model.addAttribute("monitoringPRs", approvedPRs.getContent());

            // Quotation Request Monitoring Section
            try {
                // Get recent QRs for monitoring
                var recentQRs = qrService.getAllQuotationRequests(null, 0, 5, "createdAt", false);
                model.addAttribute("recentQRs", recentQRs.getContent());
            } catch (Exception e) {
                // Fallback in case QR service is not available
                model.addAttribute("recentQRs", java.util.Collections.emptyList());
            }

            return "purchase-requisition/dashboard";

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load dashboard: " + e.getMessage());
            return "error/500";
        }
    }

    // List all PRs with search and pagination
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'REVIEWER', 'USER')")
    @GetMapping("/list")
    public String listPurchaseRequisitions(
            @RequestParam(value = "search", required = false) String searchTerm,
            @RequestParam(value = "actionRequired", required = false, defaultValue = "false") boolean actionRequired,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "ascending", defaultValue = "false") boolean ascending,
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
                var currentUser = userService.getUserById(currentUserId);
                model.addAttribute("currentUser", currentUser);
            }
            Page<PurchaseRequisitionDTO> prPage;
            
            if (actionRequired) {
                // Get action required PRs with pagination
                prPage = prService.getActionRequired(page, size);
                model.addAttribute("title", "Purchase Requisitions - Action Required");
            } else {
                // Get all PRs with search and pagination
                prPage = prService.getAllPurchaseRequisitions(searchTerm, page, size, sortBy, ascending);
                model.addAttribute("title", "Purchase Requisitions List");
            }

            model.addAttribute("prs", prPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", prPage.getTotalPages());
            model.addAttribute("totalElements", prPage.getTotalElements());
            model.addAttribute("searchTerm", searchTerm);
            model.addAttribute("actionRequired", actionRequired);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("ascending", ascending);

            return "purchase-requisition/list";

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load purchase requisitions: " + e.getMessage());
            return "purchase-requisition/list";
        }
    }

    // Show PR details
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'REVIEWER', 'USER')")
    @GetMapping("/{id}")
    public String showPurchaseRequisition(@PathVariable String id, Authentication authentication, Model model) {
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

            PurchaseRequisitionDTO pr = prService.getPurchaseRequisitionById(id);
            model.addAttribute("title", "Purchase Requisition Details - " + pr.getCode());
            model.addAttribute("pr", pr);
            
            // Load users list for reviewer/inspector selection (only active reviewer users)
            var allUsers = userService.getAllUsers(null, 0, Integer.MAX_VALUE, "name", true);
            var reviewerUsers = allUsers.getContent().stream()
                    .filter(user -> user.getStatus() == ahqpck.maintenance.report.entity.User.Status.ACTIVE)
                    .filter(user -> user.getRoleNames() != null && user.getRoleNames().contains("REVIEWER"))
                    .collect(java.util.stream.Collectors.toList());
            
            // Fallback: if no reviewer users found, show all active users
            if (reviewerUsers.isEmpty()) {
                reviewerUsers = allUsers.getContent().stream()
                        .filter(user -> user.getStatus() == ahqpck.maintenance.report.entity.User.Status.ACTIVE)
                        .collect(java.util.stream.Collectors.toList());
            }
            
            model.addAttribute("usersList", reviewerUsers);
            
            return "purchase-requisition/detail";

        } catch (Exception e) {
            model.addAttribute("error", "Purchase Requisition not found: " + e.getMessage());
            return "redirect:/purchase-requisition/list";
        }
    }

    // Create new PR - show form
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'REVIEWER', 'ENGINEER')")
    @GetMapping("/create")
    public String showCreateForm(Authentication authentication, Model model) {
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

            model.addAttribute("title", "Create Purchase Requisition");
            model.addAttribute("prDTO", new PurchaseRequisitionDTO());
            
            // Load dropdown data
            loadFormData(model);
            
            return "purchase-requisition/create";

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load create form: " + e.getMessage());
            return "error/500";
        }
    }

    // Create new PR - handle form submission
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'REVIEWER', 'ENGINEER')")
    @PostMapping("/create")
    public String createPurchaseRequisition(
            @Valid @ModelAttribute("prDTO") PurchaseRequisitionDTO prDTO,
            BindingResult bindingResult,
            @RequestParam(value = "attachmentFile", required = false) MultipartFile attachmentFile,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            Authentication authentication,
            RedirectAttributes ra,
            Model model) {

        if (WebUtil.hasErrors(bindingResult)) {
            ra.addFlashAttribute("error", WebUtil.getErrorMessage(bindingResult));
            model.addAttribute("title", "Create Purchase Requisition");
            loadFormData(model);
            return "purchase-requisition/create";
        }

        String currentUserId = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            currentUserId = userDetails.getId();
        }

        try {
            if (currentUserId != null) {
                PurchaseRequisitionDTO createdPR = prService.createPurchaseRequisition(prDTO, currentUserId);
                ra.addFlashAttribute("success", "Purchase Requisition created successfully: " + createdPR.getCode());
            } else {
                ra.addFlashAttribute("error", "Unable to identify current user");
                model.addAttribute("title", "Create Purchase Requisition");
                loadFormData(model);
                return "purchase-requisition/create";
            }

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("prDTO", prDTO);
            model.addAttribute("title", "Create Purchase Requisition");
            loadFormData(model);
            return "purchase-requisition/create";
        }
        
        return "redirect:" + (redirectUrl != null ? redirectUrl : "/purchase-requisition/list");
    }

    // Edit PR - show form
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'REVIEWER')")
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, Model model) {
        try {
            PurchaseRequisitionDTO pr = prService.getPurchaseRequisitionById(id);
            
            if (pr.getStatus() != PRStatus.SUBMITTED) {
                model.addAttribute("error", "Only submitted purchase requisitions can be edited");
                return "redirect:/purchase-requisition/" + id;
            }

            model.addAttribute("title", "Edit Purchase Requisition - " + pr.getCode());
            model.addAttribute("pr", pr);  // Add the pr object for template access
            model.addAttribute("prDTO", pr);  // Keep prDTO for form binding
            loadFormData(model);
            
            return "purchase-requisition/edit";

        } catch (Exception e) {
            model.addAttribute("error", "Purchase Requisition not found: " + e.getMessage());
            return "redirect:/purchase-requisition/list";
        }
    }

    // Edit PR - handle form submission
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'REVIEWER')")
    @PostMapping("/{id}/edit")
    public String updatePurchaseRequisition(
            @PathVariable String id,
            @Valid @ModelAttribute("prDTO") PurchaseRequisitionDTO prDTO,
            BindingResult bindingResult,
            @RequestParam(value = "attachmentFile", required = false) MultipartFile attachmentFile,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            Authentication authentication,
            RedirectAttributes ra,
            Model model) {

        if (WebUtil.hasErrors(bindingResult)) {
            ra.addFlashAttribute("error", WebUtil.getErrorMessage(bindingResult));
            model.addAttribute("title", "Edit Purchase Requisition");
            model.addAttribute("pr", prDTO);  // Add pr object for template
            loadFormData(model);
            return "purchase-requisition/edit";
        }

        String currentUserId = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            currentUserId = userDetails.getId();
        }

        try {
            if (currentUserId != null) {
                prService.updatePurchaseRequisition(id, prDTO);
                ra.addFlashAttribute("success", "Purchase Requisition updated successfully");
            } else {
                ra.addFlashAttribute("error", "Unable to identify current user");
                model.addAttribute("title", "Edit Purchase Requisition");
                model.addAttribute("pr", prDTO);
                loadFormData(model);
                return "purchase-requisition/edit";
            }

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("prDTO", prDTO);
            model.addAttribute("title", "Edit Purchase Requisition");
            model.addAttribute("pr", prDTO);
            loadFormData(model);
            return "purchase-requisition/edit";
        }
        
        return "redirect:" + (redirectUrl != null ? redirectUrl : "/purchase-requisition/" + id);
    }

    // Approve PR
    @PostMapping("/{id}/approve")
    public String approvePurchaseRequisition(
            @PathVariable String id,
            @RequestParam String reviewerId,
            @RequestParam(required = false) String reviewNotes,
            RedirectAttributes ra) {

        try {
            prService.approvePurchaseRequisition(id, reviewerId, reviewNotes);
            ra.addFlashAttribute("success", "Purchase Requisition approved successfully and ready for purchase");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to approve purchase requisition: " + e.getMessage());
        }

        return "redirect:/purchase-requisition";
    }

    // Reject PR
    @PostMapping("/{id}/reject")
    public String rejectPurchaseRequisition(
            @PathVariable String id,
            @RequestParam String reviewerId,
            @RequestParam String reviewNotes,
            RedirectAttributes ra) {

        try {
            prService.rejectPurchaseRequisition(id, reviewerId, reviewNotes);
            ra.addFlashAttribute("success", "Purchase Requisition rejected successfully");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to reject purchase requisition: " + e.getMessage());
        }

        return "redirect:/purchase-requisition";
    }

    // Complete PR (simplified - no separate inspector)
    @PostMapping("/{id}/complete")
    public String completePurchaseRequisition(
            @PathVariable String id,
            @RequestParam(required = false) String completionNotes,
            RedirectAttributes ra) {

        try {
            prService.completePurchaseRequisition(id);
            ra.addFlashAttribute("success", "Purchase Requisition completed successfully");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to complete purchase requisition: " + e.getMessage());
        }

        return "redirect:/purchase-requisition/" + id;
    }

    // Delete PR
    @PostMapping("/{id}/delete")
    public String deletePurchaseRequisition(@PathVariable String id, RedirectAttributes ra) {
        try {
            prService.deletePurchaseRequisition(id);
            ra.addFlashAttribute("success", "Purchase Requisition deleted successfully");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete purchase requisition: " + e.getMessage());
        }

        return "redirect:/purchase-requisition/list";
    }

    // Part Management Endpoints

    // Add part to PR
    @PostMapping("/{id}/parts/add")
    public String addPartToPR(
            @PathVariable String id,
            @ModelAttribute PurchaseRequisitionPartDTO partDTO,
            RedirectAttributes ra) {

        try {
            prService.addPartToRequisition(id, partDTO);
            ra.addFlashAttribute("success", "Part added to Purchase Requisition successfully");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to add part: " + e.getMessage());
        }

        return "redirect:/purchase-requisition/" + id + "/edit";
    }

    // Remove part from PR
    @PostMapping("/{prId}/parts/{partId}/remove")
    public String removePartFromPR(
            @PathVariable String prId,
            @PathVariable String partId,
            RedirectAttributes ra) {

        try {
            prService.removePartFromRequisition(prId, partId);
            ra.addFlashAttribute("success", "Part removed from Purchase Requisition successfully");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to remove part: " + e.getMessage());
        }

        return "redirect:/purchase-requisition/" + prId + "/edit";
    }

    // Update part quantity in PR (we'll add this method to service)
    @PostMapping("/{prId}/parts/{partId}/update")
    public String updatePartInPR(
            @PathVariable String prId,
            @PathVariable String partId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) CriticalityLevel criticality,
            @RequestParam(required = false) String remarks,
            RedirectAttributes ra) {

        try {
            // For now, we'll remove and re-add the part with new details
            // Later we can implement a proper update method
            PurchaseRequisitionPartDTO updatedPart = new PurchaseRequisitionPartDTO();
            updatedPart.setPartId(partId);
            updatedPart.setQuantityRequested(quantity);
            updatedPart.setCriticalityLevel(criticality != null ? criticality : CriticalityLevel.MEDIUM);
            updatedPart.setJustification(remarks);
            
            prService.removePartFromRequisition(prId, partId);
            prService.addPartToRequisition(prId, updatedPart);
            
            ra.addFlashAttribute("success", "Part details updated successfully");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update part: " + e.getMessage());
        }

        return "redirect:/purchase-requisition/" + prId + "/edit";
    }

    // Helper method to load form data
    private void loadFormData(Model model) {
        // Load equipment list for target equipment dropdown
        var allEquipments = equipmentService.getAllEquipments(null, 0, Integer.MAX_VALUE, "name", true);
        model.addAttribute("equipmentList", allEquipments.getContent());

        // Load parts list for existing part selection
        var allParts = partService.getAllParts(null, 0, Integer.MAX_VALUE, "name", true);
        model.addAttribute("partsList", allParts.getContent());

        // Load users list for requestor selection (only active ENGINEER users)
        var allUsers = userService.getAllUsers(null, 0, Integer.MAX_VALUE, "name", true);
        var engineerUsers = allUsers.getContent().stream()
                .filter(user -> user.getStatus() == ahqpck.maintenance.report.entity.User.Status.ACTIVE)
                .filter(user -> user.getRoleNames() != null && user.getRoleNames().contains("ENGINEER"))
                .collect(java.util.stream.Collectors.toList());
        
        // Fallback: if no engineer users found, show all active users
        if (engineerUsers.isEmpty()) {
            engineerUsers = allUsers.getContent().stream()
                    .filter(user -> user.getStatus() == ahqpck.maintenance.report.entity.User.Status.ACTIVE)
                    .collect(java.util.stream.Collectors.toList());
            model.addAttribute("warning", "No active engineers found. Showing all active users. Please ensure engineer users are created and properly assigned roles.");
        }
        
        model.addAttribute("usersList", engineerUsers);

        // Add criticality levels for part form
        model.addAttribute("criticalityLevels", Arrays.asList(CriticalityLevel.values()));
    }
}