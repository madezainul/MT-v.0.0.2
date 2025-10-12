package ahqpck.maintenance.report.controller;

import java.util.Arrays;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ahqpck.maintenance.report.dto.QuotationRequestDTO;
import ahqpck.maintenance.report.entity.QuotationRequest.QRStatus;
import ahqpck.maintenance.report.service.QuotationRequestService;
import ahqpck.maintenance.report.service.UserService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/quotation-request")
public class QuotationRequestController {

    private final QuotationRequestService qrService;
    private final UserService userService;

    // Dashboard - Entry point
    @GetMapping
    public String showDashboard(Model model) {
        model.addAttribute("title", "Quotation Request Dashboard");

        // Statistics for dashboard cards
        model.addAttribute("totalPOs", qrService.getTotalQRsCount());
        model.addAttribute("pendingPOs", qrService.getPendingQRsCount());
        model.addAttribute("inProgressPOs", qrService.getInProgressQRsCount());
        model.addAttribute("completedPOs", qrService.getCompletedQRsCount());

        // Pending POs Section
        Page<QuotationRequestDTO> pendingPOs = qrService.getQuotationRequestsByStatus(QRStatus.CREATED, 0, 5);
        model.addAttribute("pendingPOs", pendingPOs.getContent());

        // In Progress POs Section
        Page<QuotationRequestDTO> inProgressPOs = qrService.getQuotationRequestsByStatus(QRStatus.SENT, 0, 5);
        model.addAttribute("inProgressPOs", inProgressPOs.getContent());

        // Recent POs Section
        model.addAttribute("recentPOs", qrService.getRecentQRs(7));

        return "quotation-request/dashboard";
    }

    // List all QRs with search, filtering and pagination
    @GetMapping("/list")
    public String listQuotationRequests(
            @RequestParam(value = "search", required = false) String searchTerm,
            @RequestParam(value = "status", required = false) String statusFilter,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "ascending", defaultValue = "false") boolean ascending,
            Model model) {

        try {
            Page<QuotationRequestDTO> qrPage;
            
            // Apply status filter if provided
            if (statusFilter != null && !statusFilter.isEmpty()) {
                try {
                    QRStatus status = QRStatus.valueOf(statusFilter);
                    // For now, use the existing method without search and sorting
                    // TODO: Enhance service to support search with status filtering
                    qrPage = qrService.getQuotationRequestsByStatus(status, page, size);
                } catch (IllegalArgumentException e) {
                    // Invalid status, fallback to all QRs
                    qrPage = qrService.getAllQuotationRequests(searchTerm, page, size, sortBy, ascending);
                    statusFilter = null;
                }
            } else {
                qrPage = qrService.getAllQuotationRequests(searchTerm, page, size, sortBy, ascending);
            }

            model.addAttribute("title", "Quotation Requests List");
            model.addAttribute("qrs", qrPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", qrPage.getTotalPages());
            model.addAttribute("totalElements", qrPage.getTotalElements());
            model.addAttribute("searchTerm", searchTerm);
            model.addAttribute("currentFilter", statusFilter);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("ascending", ascending);

            return "quotation-request/list";

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load quotation requests: " + e.getMessage());
            return "quotation-request/list";
        }
    }

    // Show PO details
    @GetMapping("/{id}")
    public String showPurchaseOrder(@PathVariable String id, Model model) {
        try {
            QuotationRequestDTO po = qrService.getQuotationRequestById(id);
            model.addAttribute("title", "Purchase Order Details - " + po.getQuotationNumber());
            model.addAttribute("po", po);
            
            // Load users list for assignment (only active reviewer users)
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

            return "quotation-request/detail";

        } catch (Exception e) {
            model.addAttribute("error", "Quotation Request not found: " + e.getMessage());
            return "redirect:/quotation-request/list";
        }
    }

    // Create QR from approved PR parts
    @GetMapping("/create-from-pr")
    public String showCreateFromPRForm(
            @RequestParam String supplier,
            Model model) {
        
        try {
            model.addAttribute("title", "Create Quotation Request - " + supplier);
            model.addAttribute("supplier", supplier);
            
            // Get available approved parts for this supplier
            var availableParts = qrService.getAvailablePartsForSupplier(supplier);
            model.addAttribute("availableParts", availableParts);
            
            // Load users for creator assignment
            loadFormData(model);
            
            return "quotation-request/create-from-pr";

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load parts for supplier: " + e.getMessage());
            return "redirect:/purchase-requisition";
        }
    }

    // Create PO from multiple approved PRs (new multi-selection page)
    @GetMapping("/create-multi")
    public String showCreateMultiPOForm(Model model) {
        try {
            model.addAttribute("title", "Create Quotation - Select Parts");
            
            // Get all available parts grouped by supplier, sorted by part name
            var partsGroupedBySupplier = qrService.getAvailablePartsGroupedBySupplier();
            model.addAttribute("partsGroupedBySupplier", partsGroupedBySupplier);
            
            // Load users for creator assignment
            loadFormData(model);

            return "quotation-request/create-multi";

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load available parts: " + e.getMessage());
            return "redirect:/quotation-request";
        }
    }

    // Create PO - handle form submission
    @PostMapping("/create")
    public String createPurchaseOrder(
            @RequestParam(required = false) String supplier,
            @RequestParam String[] selectedPartIds,
            @RequestParam String creatorId,
            @RequestParam(required = false) String notes,
            RedirectAttributes ra) {

        try {
            QuotationRequestDTO createdQR;
            if (supplier != null && !supplier.isEmpty()) {
                // Traditional single-supplier creation
                createdQR = qrService.createQuotationRequestFromParts(supplier, Arrays.asList(selectedPartIds), creatorId, notes);
            } else {
                // Multi-supplier creation - create separate QRs per supplier
                createdQR = qrService.createQuotationRequestsFromSelectedParts(Arrays.asList(selectedPartIds), creatorId, notes);
            }

            ra.addFlashAttribute("success", "Quotation Request(s) created successfully");
            return "redirect:/quotation-request/" + createdQR.getId();

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create quotation request: " + e.getMessage());
            if (supplier != null && !supplier.isEmpty()) {
                return "redirect:/quotation-request/create-from-pr?supplier=" + supplier;
            } else {
                return "redirect:/quotation-request/create-multi";
            }
        }
    }

    // Update QR status
    @PostMapping("/{id}/status")
    public String updateQRStatus(
            @PathVariable String id,
            @RequestParam QRStatus status,
            @RequestParam(required = false) String notes,
            RedirectAttributes ra) {

        try {
            qrService.updateQRStatus(id, status, notes);
            ra.addFlashAttribute("success", "Quotation Request status updated successfully");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update status: " + e.getMessage());
        }

        return "redirect:/quotation-request/" + id;
    }

    // Receive parts
    @PostMapping("/{id}/receive")
    public String receiveParts(
            @PathVariable String id,
            @RequestParam String[] partIds,
            @RequestParam Integer[] receivedQuantities,
            @RequestParam(required = false) String receivingNotes,
            RedirectAttributes ra) {

        try {
            for (int i = 0; i < partIds.length; i++) {
                if (receivedQuantities[i] != null && receivedQuantities[i] > 0) {
                    qrService.receivePart(id, partIds[i], receivedQuantities[i], receivingNotes);
                }
            }
            ra.addFlashAttribute("success", "Parts received successfully");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to receive parts: " + e.getMessage());
        }

        return "redirect:/quotation-request/" + id;
    }

    // Complete QR
    @PostMapping("/{id}/complete")
    public String completeQuotationRequest(
            @PathVariable String id,
            @RequestParam(required = false) String completionNotes,
            RedirectAttributes ra) {

        try {
            qrService.completeQuotationRequest(id);
            ra.addFlashAttribute("success", "Quotation Request completed successfully");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to complete quotation request: " + e.getMessage());
        }

        return "redirect:/quotation-request/" + id;
    }

    // Cancel QR
    @PostMapping("/{id}/cancel")
    public String cancelQuotationRequest(
            @PathVariable String id,
            @RequestParam String cancellationReason,
            RedirectAttributes ra) {

        try {
            qrService.cancelQuotationRequest(id, cancellationReason);
            ra.addFlashAttribute("success", "Quotation Request   cancelled");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to cancel purchase order: " + e.getMessage());
        }

        return "redirect:/quotation-request/" + id;
    }

    // Edit QR form
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, Model model) {
        try {
            QuotationRequestDTO qr = qrService.getQuotationRequestById(id);

            // Only allow editing for CREATED status
            if (qr.getStatus() != QRStatus.CREATED) {
                model.addAttribute("error", "Only quotation requests with CREATED status can be edited");
                return "redirect:/quotation-request/" + id;
            }

            model.addAttribute("title", "Edit Quotation Request - " + qr.getQuotationNumber());
            model.addAttribute("qr", qr);

            // Load form data
            loadFormData(model);

            return "quotation-request/edit";

        } catch (Exception e) {
            model.addAttribute("error", "Quotation Request not found: " + e.getMessage());
            return "redirect:/quotation-request/list";
        }
    }

    // Update QR
    @PostMapping("/{id}/edit")
    public String updateQuotationRequest(
            @PathVariable String id,
            @RequestParam String supplierName,
            @RequestParam(required = false) String supplierContact,
            @RequestParam(required = false) String notes,
            @RequestParam String expectedDeliveryDate,
            @RequestParam(required = false) String[] partIds,
            @RequestParam(required = false) Integer[] quantities,
            @RequestParam(required = false) String[] unitPrices,
            @RequestParam(required = false) String[] partNotes,
            RedirectAttributes ra) {

        try {
            qrService.updateQuotationRequest(id, supplierName, supplierContact, notes,
                expectedDeliveryDate, partIds, quantities, unitPrices, partNotes);
            ra.addFlashAttribute("success", "Quotation Request updated successfully");
            return "redirect:/quotation-request/" + id;

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update quotation request: " + e.getMessage());
            return "redirect:/quotation-request/" + id + "/edit";
        }
    }

    // Delete QR
    @PostMapping("/{id}/delete")
    public String deleteQuotationRequest(@PathVariable String id, RedirectAttributes ra) {
        try {
            qrService.deleteQuotationRequest(id);
            ra.addFlashAttribute("success", "Quotation Request deleted successfully");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete quotation request: " + e.getMessage());
        }

        return "redirect:/quotation-request/list";
    }

    // Helper method to load form data
    private void loadFormData(Model model) {
        // Load users list for creator selection (only active reviewer users)
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
        
        // Add warning if no active users available
        if (reviewerUsers.isEmpty()) {
            model.addAttribute("warning", "No active users found. Please ensure users are created and activated before creating quotation requests.");
        }

        // Add QR status options
        model.addAttribute("qrStatuses", Arrays.asList(QRStatus.values()));
    }
}