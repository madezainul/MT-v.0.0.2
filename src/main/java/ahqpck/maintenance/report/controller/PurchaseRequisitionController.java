package ahqpck.maintenance.report.controller;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ahqpck.maintenance.report.dto.PurchaseRequisitionDTO;
import ahqpck.maintenance.report.dto.PurchaseRequisitionPartDTO;
import ahqpck.maintenance.report.entity.PurchaseRequisition.PRStatus;
import ahqpck.maintenance.report.entity.PurchaseRequisitionPart.CriticalityLevel;
import ahqpck.maintenance.report.service.EquipmentService;
import ahqpck.maintenance.report.service.PartService;
import ahqpck.maintenance.report.service.QuotationRequestService;
import ahqpck.maintenance.report.service.PurchaseRequisitionService;
import ahqpck.maintenance.report.service.UserService;
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
    @GetMapping
    public String showDashboard(Model model) {
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

        // PO Monitoring Section (replace recent activity)
        // try {
        //     // Get recent POs from PO service
        //     var recentPOs = poService.getRecentPOs(10);
        //     model.addAttribute("recentPOs", recentPOs);
            
        //     // PO statistics
        //     model.addAttribute("totalPOs", poService.getTotalPOsCount());
        //     model.addAttribute("pendingPOs", poService.getPendingPOsCount());
        //     model.addAttribute("inProgressPOs", poService.getInProgressPOsCount());
        //     model.addAttribute("completedPOs", poService.getCompletedPOsCount());
        // } catch (Exception e) {
        //     // Fallback in case PO service is not available
        //     model.addAttribute("recentPOs", java.util.Collections.emptyList());
        //     model.addAttribute("totalPOs", 0);
        //     model.addAttribute("pendingPOs", 0);
        //     model.addAttribute("inProgressPOs", 0);
        //     model.addAttribute("completedPOs", 0);
        // }

        return "purchase-requisition/dashboard";
    }

    // List all PRs with search and pagination
    @GetMapping("/list")
    public String listPurchaseRequisitions(
            @RequestParam(value = "search", required = false) String searchTerm,
            @RequestParam(value = "actionRequired", required = false, defaultValue = "false") boolean actionRequired,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "ascending", defaultValue = "false") boolean ascending,
            Model model) {

        try {
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
    @GetMapping("/{id}")
    public String showPurchaseRequisition(@PathVariable String id, Model model) {
        try {
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
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("title", "Create Purchase Requisition");
        model.addAttribute("prDTO", new PurchaseRequisitionDTO());
        
        // Load dropdown data
        loadFormData(model);
        
        return "purchase-requisition/create";
    }

    // Create new PR - handle form submission
    @PostMapping("/create")
    public String createPurchaseRequisition(
            @Valid @ModelAttribute("prDTO") PurchaseRequisitionDTO prDTO,
            BindingResult bindingResult,
            RedirectAttributes ra,
            Model model) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            model.addAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            model.addAttribute("title", "Create Purchase Requisition");
            loadFormData(model);
            return "purchase-requisition/create";
        }

        try {
            PurchaseRequisitionDTO createdPR = prService.createPurchaseRequisition(prDTO);
            ra.addFlashAttribute("success", "Purchase Requisition created successfully: " + createdPR.getCode());
            return "redirect:/purchase-requisition/list";

        } catch (Exception e) {
            model.addAttribute("error", "Failed to create purchase requisition: " + e.getMessage());
            model.addAttribute("title", "Create Purchase Requisition");
            loadFormData(model);
            return "purchase-requisition/create";
        }
    }

    // Edit PR - show form
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
    @PostMapping("/{id}/edit")
    public String updatePurchaseRequisition(
            @PathVariable String id,
            @Valid @ModelAttribute("prDTO") PurchaseRequisitionDTO prDTO,
            BindingResult bindingResult,
            RedirectAttributes ra,
            Model model) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            model.addAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            model.addAttribute("title", "Edit Purchase Requisition");
            model.addAttribute("pr", prDTO);  // Add pr object for template
            loadFormData(model);
            return "purchase-requisition/edit";
        }

        try {
            PurchaseRequisitionDTO updatedPR = prService.updatePurchaseRequisition(id, prDTO);
            ra.addFlashAttribute("success", "Purchase Requisition updated successfully");
            return "redirect:/purchase-requisition/" + updatedPR.getId();

        } catch (Exception e) {
            model.addAttribute("error", "Failed to update purchase requisition: " + e.getMessage());
            model.addAttribute("title", "Edit Purchase Requisition");
            model.addAttribute("pr", prDTO);  // Add pr object for template
            loadFormData(model);
            return "purchase-requisition/edit";
        }
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