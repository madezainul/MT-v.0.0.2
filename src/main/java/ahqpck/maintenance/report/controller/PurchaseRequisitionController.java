package ahqpck.maintenance.report.controller;

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
import ahqpck.maintenance.report.entity.PurchaseRequisition.PRStatus;
import ahqpck.maintenance.report.service.EquipmentService;
import ahqpck.maintenance.report.service.PartService;
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

    // Smart Dashboard - Entry point
    @GetMapping
    public String showDashboard(Model model) {
        model.addAttribute("title", "Purchase Requisition Dashboard");

        // Statistics for dashboard cards
        model.addAttribute("totalPRs", prService.getTotalPRsCount());
        model.addAttribute("pendingApproval", prService.getPendingApprovalCount());
        model.addAttribute("requirePONumber", prService.getRequiringPONumberCount());
        model.addAttribute("readyForCompletion", prService.getReadyForCompletionCount());

        // Action Required Section
        Page<PurchaseRequisitionDTO> pendingApprovalPRs = prService.getPendingApproval(0, 5);
        model.addAttribute("actionRequiredPRs", pendingApprovalPRs.getContent());

        // Monitoring Section
        Page<PurchaseRequisitionDTO> sentToPurchasePRs = prService.getPurchaseRequisitionsByStatus(PRStatus.SENT_TO_PURCHASE, 0, 5);
        model.addAttribute("monitoringPRs", sentToPurchasePRs.getContent());

        // Recent Activity Section
        model.addAttribute("recentPRs", prService.getRecentSubmissions(7));

        return "purchase-requisition/dashboard";
    }

    // List all PRs with search and pagination
    @GetMapping("/list")
    public String listPurchaseRequisitions(
            @RequestParam(value = "search", required = false) String searchTerm,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "ascending", defaultValue = "false") boolean ascending,
            Model model) {

        try {
            Page<PurchaseRequisitionDTO> prPage = prService.getAllPurchaseRequisitions(searchTerm, page, size, sortBy, ascending);

            model.addAttribute("title", "Purchase Requisitions List");
            model.addAttribute("prs", prPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", prPage.getTotalPages());
            model.addAttribute("totalElements", prPage.getTotalElements());
            model.addAttribute("searchTerm", searchTerm);
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
            
            // Load users list for inspector selection (only active users)
            var allUsers = userService.getAllUsers(null, 0, Integer.MAX_VALUE, "name", true);
            var activeUsers = allUsers.getContent().stream()
                    .filter(user -> user.getStatus() == ahqpck.maintenance.report.entity.User.Status.ACTIVE)
                    .collect(java.util.stream.Collectors.toList());
            model.addAttribute("usersList", activeUsers);
            
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
            return "redirect:/purchase-requisition/" + createdPR.getId();

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
            model.addAttribute("prDTO", pr);
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
            loadFormData(model);
            return "purchase-requisition/edit";
        }
    }

    // Approve PR
    @PostMapping("/{id}/approve")
    public String approvePurchaseRequisition(
            @PathVariable String id,
            @RequestParam String reviewerName,
            @RequestParam(required = false) String reviewNotes,
            RedirectAttributes ra) {

        try {
            prService.approvePurchaseRequisition(id, reviewerName, reviewNotes);
            ra.addFlashAttribute("success", "Purchase Requisition approved successfully");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to approve purchase requisition: " + e.getMessage());
        }

        return "redirect:/purchase-requisition/" + id;
    }

    // Reject PR
    @PostMapping("/{id}/reject")
    public String rejectPurchaseRequisition(
            @PathVariable String id,
            @RequestParam String reviewerName,
            @RequestParam String reviewNotes,
            RedirectAttributes ra) {

        try {
            prService.rejectPurchaseRequisition(id, reviewerName, reviewNotes);
            ra.addFlashAttribute("success", "Purchase Requisition rejected");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to reject purchase requisition: " + e.getMessage());
        }

        return "redirect:/purchase-requisition/" + id;
    }

    // Send to Purchase
    @PostMapping("/{id}/send-to-purchase")
    public String sendToPurchase(@PathVariable String id, RedirectAttributes ra) {
        try {
            prService.sendToPurchase(id);
            ra.addFlashAttribute("success", "Purchase Requisition sent to purchase department");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to send to purchase: " + e.getMessage());
        }

        return "redirect:/purchase-requisition/" + id;
    }

    // Add PO Number
    @PostMapping("/{id}/add-po")
    public String addPONumber(
            @PathVariable String id,
            @RequestParam String poNumber,
            RedirectAttributes ra) {

        try {
            prService.addPONumber(id, poNumber);
            ra.addFlashAttribute("success", "PO Number added successfully");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to add PO number: " + e.getMessage());
        }

        return "redirect:/purchase-requisition/" + id;
    }

    // Complete PR
    @PostMapping("/{id}/complete")
    public String completePurchaseRequisition(
            @PathVariable String id,
            @RequestParam String inspectorId,
            @RequestParam(required = false) String completionNotes,
            RedirectAttributes ra) {

        try {
            prService.completePurchaseRequisition(id, inspectorId, completionNotes);
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

    // Helper method to load form data
    private void loadFormData(Model model) {
        // Load equipment list for target equipment dropdown
        var allEquipments = equipmentService.getAllEquipments(null, 0, Integer.MAX_VALUE, "name", true);
        model.addAttribute("equipmentList", allEquipments.getContent());

        // Load parts list for existing part selection
        var allParts = partService.getAllParts(null, 0, Integer.MAX_VALUE, "name", true);
        model.addAttribute("partsList", allParts.getContent());

        // Load users list for requestor selection (only active users)
        var allUsers = userService.getAllUsers(null, 0, Integer.MAX_VALUE, "name", true);
        // Filter only active users for the dropdown
        var activeUsers = allUsers.getContent().stream()
                .filter(user -> user.getStatus() == ahqpck.maintenance.report.entity.User.Status.ACTIVE)
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("usersList", activeUsers);
        
        // Add warning if no active users available
        if (activeUsers.isEmpty()) {
            model.addAttribute("warning", "No active users found. Please ensure users are created and activated before creating purchase requisitions.");
        }

        // Load categories and suppliers for new part creation
        // These should be loaded from your master data services
        // model.addAttribute("categories", categoryService.getAll());
        // model.addAttribute("suppliers", supplierService.getAll());
    }
}