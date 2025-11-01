package ahqpck.maintenance.report.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import ahqpck.maintenance.report.mapper.PurchaseRequisitionMapper;
import ahqpck.maintenance.report.dto.PurchaseRequisitionDTO;
import ahqpck.maintenance.report.dto.PurchaseRequisitionPartDTO;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.entity.PurchaseRequisition;
import ahqpck.maintenance.report.entity.PurchaseRequisition.PRStatus;
import ahqpck.maintenance.report.entity.PurchaseRequisitionPart;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.repository.PurchaseRequisitionRepository;
import ahqpck.maintenance.report.repository.PurchaseRequisitionPartRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.util.ZeroPaddedCodeGenerator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseRequisitionService {

    private final PurchaseRequisitionRepository prRepository;
    private final PartRepository partRepository;
    private final PurchaseRequisitionPartRepository purchaseRequisitionPartRepository;
    private final UserRepository userRepository;
    private final PurchaseRequisitionMapper purchaseRequisitionMapper;
    private final ZeroPaddedCodeGenerator codeGenerator;

    // CRUD Operations
    @Transactional
    public PurchaseRequisitionDTO createPurchaseRequisition(PurchaseRequisitionDTO prDTO, String currentUserId) {
        try {
            PurchaseRequisition pr = new PurchaseRequisition();

            // Validate business rules
            validatePurchaseRequisition(prDTO);

            // Handle code generation
            if (prDTO.getCode() == null || prDTO.getCode().trim().isEmpty()) {
                String generatedCode = codeGenerator.generate(PurchaseRequisition.class, "code", "PR");
                pr.setCode(generatedCode);
            } else {
                pr.setCode(prDTO.getCode());
            }

            // Get current user for audit fields
            User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalStateException("User not found with ID: " + currentUserId));

            // Set audit fields
            pr.setCreatedBy(currentUser);
            pr.setUpdatedBy(currentUser);

            // Use mapper for complex entity mapping
            mapPRFromDTO(pr, prDTO);

            pr = prRepository.save(pr);
            return mapToDTO(pr);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create purchase requisition: " + e.getMessage(), e);
        }
    }

    private void validatePurchaseRequisition(PurchaseRequisitionDTO prDTO) {
        // Validate requestor exists
        if (prDTO.getRequestorId() == null) {
            throw new IllegalArgumentException("Requestor ID is required");
        }
        
        userRepository.findById(prDTO.getRequestorId())
                .orElseThrow(() -> new IllegalArgumentException("Requestor not found with id: " + prDTO.getRequestorId()));

        // Validate title and description
        if (prDTO.getTitle() == null || prDTO.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }

        // Validate parts
        if (prDTO.getParts() == null || prDTO.getParts().isEmpty()) {
            throw new IllegalArgumentException("At least one part must be specified");
        }

        // Validate each part
        for (PurchaseRequisitionPartDTO partDTO : prDTO.getParts()) {
            if (partDTO.getPartId() == null) {
                throw new IllegalArgumentException("Part ID is required for all parts");
            }
            if (partDTO.getQuantityRequested() == null || partDTO.getQuantityRequested() <= 0) {
                throw new IllegalArgumentException("Valid quantity is required for all parts");
            }
            // Verify part exists
            partRepository.findById(partDTO.getPartId())
                .orElseThrow(() -> new IllegalArgumentException("Part not found with id: " + partDTO.getPartId()));
        }
    }

    private void mapPRFromDTO(PurchaseRequisition pr, PurchaseRequisitionDTO prDTO) {
        // Get requestor
        User requestor = userRepository.findById(prDTO.getRequestorId())
                .orElseThrow(() -> new RuntimeException("Requestor not found"));

        // Map basic fields
        pr.setTitle(prDTO.getTitle());
        pr.setDescription(prDTO.getDescription());
        pr.setRequestor(requestor);
        pr.setDateNeeded(prDTO.getDateNeeded());
        pr.setTargetEquipmentId(prDTO.getTargetEquipmentId());
        pr.setTargetEquipmentName(prDTO.getTargetEquipmentName());

        // Add parts
        if (prDTO.getParts() != null && !prDTO.getParts().isEmpty()) {
            for (PurchaseRequisitionPartDTO partDTO : prDTO.getParts()) {
                PurchaseRequisitionPart prPart = createPRPart(pr, partDTO);
                pr.addPart(prPart);
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<PurchaseRequisitionDTO> getAllPurchaseRequisitions(String searchTerm, int page, int size, String sortBy, boolean ascending) {
        Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<PurchaseRequisition> prPage;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            prPage = prRepository.searchByKeyword(searchTerm.trim(), pageable);
        } else {
            prPage = prRepository.findAll(pageable);
        }
        
        return prPage.map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public PurchaseRequisitionDTO getPurchaseRequisitionById(String id) {
        PurchaseRequisition pr = prRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + id));
        return mapToDTO(pr);
    }

    @Transactional(readOnly = true)
    public PurchaseRequisitionDTO getPurchaseRequisitionByCode(String code) {
        PurchaseRequisition pr = prRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with code: " + code));
        return mapToDTO(pr);
    }

    @Transactional
    public PurchaseRequisitionDTO updatePurchaseRequisition(String id, PurchaseRequisitionDTO prDTO, String userId) {
        try {
            PurchaseRequisition pr = prRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + id));

            // Only allow updates for SUBMITTED status
            if (pr.getStatus() != PRStatus.SUBMITTED) {
                throw new RuntimeException("Cannot update Purchase Requisition that is not in SUBMITTED status");
            }

            // Update requestor if changed
            if (prDTO.getRequestorId() != null && !prDTO.getRequestorId().equals(pr.getRequestor().getId())) {
                User newRequestor = userRepository.findById(prDTO.getRequestorId())
                        .orElseThrow(() -> new RuntimeException("User not found with id: " + prDTO.getRequestorId()));
                pr.setRequestor(newRequestor);
            }

            // Update basic fields
            pr.setTitle(prDTO.getTitle());
            pr.setDescription(prDTO.getDescription());
            pr.setDateNeeded(prDTO.getDateNeeded());
            pr.setTargetEquipmentId(prDTO.getTargetEquipmentId());
            pr.setTargetEquipmentName(prDTO.getTargetEquipmentName());
            
            // Reset approval fields when editing a previously reviewed PR
            if (pr.getIsApproved() != null) {
                pr.setIsApproved(null);
                pr.setReviewerName(null);
                pr.setReviewNotes(null);
                pr.setReviewedAt(null);
            }
            
            // Update parts if provided
            if (prDTO.getParts() != null && !prDTO.getParts().isEmpty()) {
                // Create a map of existing parts by ID for quick lookup
                java.util.Map<String, PurchaseRequisitionPart> existingPartsMap = new java.util.HashMap<>();
                for (PurchaseRequisitionPart prp : pr.getRequisitionParts()) {
                    existingPartsMap.put(prp.getId(), prp);
                }
                
                // Track which parts to keep
                java.util.Set<String> partsToKeep = new java.util.HashSet<>();
                
                // Update existing parts or create new ones
                for (PurchaseRequisitionPartDTO partDTO : prDTO.getParts()) {
                    if (partDTO.getId() != null && existingPartsMap.containsKey(partDTO.getId())) {
                        // Update existing part
                        PurchaseRequisitionPart existingPart = existingPartsMap.get(partDTO.getId());
                        existingPart.setQuantityRequested(partDTO.getQuantityRequested());
                        existingPart.setCriticalityLevel(partDTO.getCriticalityLevel());
                        existingPart.setJustification(partDTO.getJustification());
                        existingPart.setNotes(partDTO.getNotes());
                        
                        // Update part if part ID changed
                        if (!existingPart.getPart().getId().equals(partDTO.getPartId())) {
                            Part newPart = partRepository.findById(partDTO.getPartId())
                                    .orElseThrow(() -> new RuntimeException("Part not found with id: " + partDTO.getPartId()));
                            existingPart.setPart(newPart);
                        }
                        
                        partsToKeep.add(partDTO.getId());
                    } else {
                        // Create new part
                        PurchaseRequisitionPart newPrPart = createPRPart(pr, partDTO);
                        pr.addPart(newPrPart);
                    }
                }
                
                // Remove parts that are no longer in the DTO (deleted parts)
                java.util.List<PurchaseRequisitionPart> partsToRemove = pr.getRequisitionParts().stream()
                        .filter(prp -> !partsToKeep.contains(prp.getId()))
                        .collect(java.util.stream.Collectors.toList());
                
                for (PurchaseRequisitionPart prp : partsToRemove) {
                    pr.removePart(prp);
                }
            }
            
            // Set updatedBy user
            if (userId != null) {
                User updatedByUser = userRepository.findById(userId).orElse(null);
                pr.setUpdatedBy(updatedByUser);
            }
            
            pr.setUpdatedAt(LocalDateTime.now());

            pr = prRepository.save(pr);
            return mapToDTO(pr);

        } catch (Exception e) {
            throw new RuntimeException("Failed to update purchase requisition: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deletePurchaseRequisition(String id) {
        try {
            PurchaseRequisition pr = prRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + id));

            prRepository.delete(pr);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete purchase requisition: " + e.getMessage(), e);
        }
    }

    // Part management
    @Transactional
    public PurchaseRequisitionDTO addPartToRequisition(String prId, PurchaseRequisitionPartDTO partDTO) {
        try {
            PurchaseRequisition pr = prRepository.findById(prId)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + prId));

            if (pr.getStatus() != PRStatus.SUBMITTED) {
                throw new RuntimeException("Cannot add parts to Purchase Requisition that is not in SUBMITTED status");
            }

            PurchaseRequisitionPart prPart = createPRPart(pr, partDTO);
            pr.addPart(prPart);
            
            pr = prRepository.save(pr);
            return mapToDTO(pr);

        } catch (Exception e) {
            throw new RuntimeException("Failed to add part to purchase requisition: " + e.getMessage(), e);
        }
    }

    @Transactional
    public PurchaseRequisitionDTO removePartFromRequisition(String prId, String partId) {
        try {
            PurchaseRequisition pr = prRepository.findById(prId)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + prId));

            if (pr.getStatus() != PRStatus.SUBMITTED) {
                throw new RuntimeException("Cannot remove parts from Purchase Requisition that is not in SUBMITTED status");
            }

            // Find and remove the part
            PurchaseRequisitionPart prPartToRemove = pr.getRequisitionParts().stream()
                    .filter(prp -> prp.getPart().getId().equals(partId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Part not found in this Purchase Requisition"));

            pr.removePart(prPartToRemove);
            
            pr = prRepository.save(pr);
            return mapToDTO(pr);

        } catch (Exception e) {
            throw new RuntimeException("Failed to remove part from purchase requisition: " + e.getMessage(), e);
        }
    }

    // Workflow Operations
    @Transactional
    public PurchaseRequisitionDTO approvePurchaseRequisition(String id, String reviewerId, String reviewNotes) {
        try {
            PurchaseRequisition pr = prRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + id));

            if (!pr.canBeApproved()) {
                throw new RuntimeException("Purchase Requisition cannot be approved in current status: " + pr.getStatus());
            }

            // Get reviewer by ID
            User reviewer = userRepository.findById(reviewerId)
                    .orElseThrow(() -> new RuntimeException("Reviewer not found with id: " + reviewerId));

            pr.setStatus(PRStatus.APPROVED);
            pr.setIsApproved(true);
            pr.setReviewerName(reviewer.getName());
            pr.setReviewNotes(reviewNotes);
            pr.setReviewedAt(LocalDateTime.now());
            pr.setUpdatedAt(LocalDateTime.now());

            pr = prRepository.save(pr);
            return mapToDTO(pr);

        } catch (Exception e) {
            throw new RuntimeException("Failed to approve purchase requisition: " + e.getMessage(), e);
        }
    }

    @Transactional
    public PurchaseRequisitionDTO rejectPurchaseRequisition(String id, String reviewerId, String reviewNotes) {
        try {
            PurchaseRequisition pr = prRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + id));

            if (!pr.canBeApproved()) {
                throw new RuntimeException("Purchase Requisition cannot be reviewed in current status: " + pr.getStatus());
            }

            // Get reviewer by ID
            User reviewer = userRepository.findById(reviewerId)
                    .orElseThrow(() -> new RuntimeException("Reviewer not found with id: " + reviewerId));

            pr.setIsApproved(false);
            pr.setReviewerName(reviewer.getName());
            pr.setReviewNotes(reviewNotes);
            pr.setReviewedAt(LocalDateTime.now());
            pr.setUpdatedAt(LocalDateTime.now());

            pr = prRepository.save(pr);
            return mapToDTO(pr);

        } catch (Exception e) {
            throw new RuntimeException("Failed to reject purchase requisition: " + e.getMessage(), e);
        }
    }

    @Transactional
    public PurchaseRequisitionDTO completePurchaseRequisition(String id) {
        try {
            PurchaseRequisition pr = prRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + id));

            if (!pr.canBeCompleted()) {
                throw new RuntimeException("Purchase Requisition cannot be completed. Not all parts have been received.");
            }

            pr.setStatus(PRStatus.COMPLETED);
            pr.setUpdatedAt(LocalDateTime.now());

            pr = prRepository.save(pr);
            return mapToDTO(pr);

        } catch (Exception e) {
            throw new RuntimeException("Failed to complete purchase requisition: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void approvePartInRequisition(String prId, String partId, Boolean isApproved, String partApprovalNotes, String reviewerId, String reviewNotes) {
        try {
            PurchaseRequisition pr = prRepository.findById(prId)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + prId));

            PurchaseRequisitionPart part = pr.getRequisitionParts().stream()
                    .filter(p -> p.getId().equals(partId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Part not found in Purchase Requisition"));

            // Set part approval status
            part.setIsPartApproved(isApproved);
            part.setPartApprovalNotes(partApprovalNotes);
            part.setUpdatedAt(LocalDateTime.now());

            // Update overall PR status if all parts are reviewed
            long reviewedPartsCount = pr.getRequisitionParts().stream()
                    .filter(p -> p.getIsPartApproved() != null)
                    .count();

            if (reviewedPartsCount == pr.getRequisitionParts().size()) {
                // All parts have been reviewed
                boolean allApproved = pr.getRequisitionParts().stream()
                        .allMatch(p -> Boolean.TRUE.equals(p.getIsPartApproved()));

                if (allApproved) {
                    pr.setStatus(PRStatus.APPROVED);
                    pr.setIsApproved(true);
                } else {
                    pr.setIsApproved(false);
                }

                pr.setReviewerName(reviewerId);
                pr.setReviewNotes(reviewNotes);
                pr.setReviewedAt(LocalDateTime.now());
            }

            pr.setUpdatedAt(LocalDateTime.now());
            prRepository.save(pr);

        } catch (Exception e) {
            throw new RuntimeException("Failed to approve part in requisition: " + e.getMessage(), e);
        }
    }

    /**
     * Update status for a single part in a purchase requisition.
     * Used for bulk updates from the parts status list page.
     *
     * @param prId Purchase Requisition ID
     * @param partId Part ID
     * @param status New status (PENDING, ORDERED, PARTIALLY_RECEIVED, RECEIVED)
     * @param supplier Supplier name (optional)
     * @param poNumber Purchase Order number (optional)
     * @throws RuntimeException if part not found or invalid status
     */
    @Transactional
    public void updatePartStatus(String prId, String partId, String status, String supplier, String poNumber) {
        try {
            System.out.println("updatePartStatus called: prId=" + prId + ", partId=" + partId + ", status=" + status);
            
            // Find the part by both prId and partId
            java.util.List<PurchaseRequisitionPart> parts = 
                purchaseRequisitionPartRepository.findByPurchaseRequisitionIdAndPartId(prId, partId);
            
            if (parts == null || parts.isEmpty()) {
                throw new RuntimeException("Part not found in this Purchase Requisition. PR ID: " + prId + ", Part ID: " + partId);
            }
            
            PurchaseRequisitionPart part = parts.get(0);
            System.out.println("Found part: " + part.getId() + ", current status: " + part.getStatus());
            
            // Validate and convert status string to enum
            PurchaseRequisitionPart.PRPartStatus newStatus;
            try {
                newStatus = PurchaseRequisitionPart.PRPartStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid status: " + status + ". Valid statuses: PENDING, ORDERED, PARTIALLY_RECEIVED, RECEIVED");
            }
            
            // Update the part status
            part.setStatus(newStatus);
            System.out.println("Part status set to: " + newStatus);
            
            // Update supplier name if provided (from the Part entity)
            if (supplier != null && !supplier.trim().isEmpty()) {
                Part partEntity = part.getPart();
                if (partEntity != null) {
                    partEntity.setSupplierName(supplier);
                    partRepository.save(partEntity);
                    System.out.println("Supplier updated to: " + supplier);
                }
            }
            
            // Store PO number if provided
            if (poNumber != null && !poNumber.trim().isEmpty()) {
                part.setPoNumber(poNumber);
                System.out.println("PO number set to: " + poNumber);
            }
            
            // Update timestamp
            part.setUpdatedAt(LocalDateTime.now());
            
            // Save the updated part
            purchaseRequisitionPartRepository.saveAndFlush(part);
            System.out.println("Part saved and flushed");
            
            // Clear the persistence context to avoid stale data
            // This ensures fresh data is fetched from DB in the next method
            
        } catch (RuntimeException e) {
            System.err.println("RuntimeException in updatePartStatus: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Exception in updatePartStatus: " + e.getMessage());
            throw new RuntimeException("Failed to update part status: " + e.getMessage(), e);
        }
    }
    
    /**
     * Separate method to update PR status - runs in its own transaction
     * This avoids the issue of querying the same table that was just updated
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePRStatusAfterPartUpdate(String prId) {
        try {
            System.out.println("updatePRStatusAfterPartUpdate called for prId: " + prId);
            
            // Small delay to ensure database write is committed
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            updatePRStatusBasedOnPOStatus(prId);
            System.out.println("updatePRStatusAfterPartUpdate completed for prId: " + prId);
        } catch (Exception e) {
            System.err.println("Error in updatePRStatusAfterPartUpdate: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Update PR status based on the PO status of its parts
     * - If any part has status ORDERED, set PR status to SENT_TO_PURCHASE
     * - If all parts have status RECEIVED, set PR status to COMPLETED
     */
    @Transactional
    private void updatePRStatusBasedOnPOStatus(String prId) {
        try {
            // Flush pending changes to database before querying
            purchaseRequisitionPartRepository.flush();
            
            PurchaseRequisition pr = prRepository.findById(prId).orElse(null);
            if (pr == null) {
                System.out.println("PR not found: " + prId);
                return;
            }
            
            // Get all parts for this PR - this will get the fresh data from DB
            java.util.List<PurchaseRequisitionPart> allParts = purchaseRequisitionPartRepository.findByPurchaseRequisitionId(prId);
            
            if (allParts == null || allParts.isEmpty()) {
                System.out.println("No parts found for PR: " + prId);
                return;
            }
            
            System.out.println("Checking PR status update for " + prId);
            System.out.println("Current PR status: " + pr.getStatus());
            System.out.println("Number of parts: " + allParts.size());
            
            // Log part statuses
            for (PurchaseRequisitionPart part : allParts) {
                System.out.println("  Part: " + part.getId() + " - Status: " + part.getStatus());
            }
            
            // Check if any part has status ORDERED
            boolean hasOrderedParts = allParts.stream()
                .anyMatch(part -> part.getStatus() == PurchaseRequisitionPart.PRPartStatus.ORDERED);
            
            // Check if all parts have status RECEIVED
            boolean allPartsReceived = allParts.stream()
                .allMatch(part -> part.getStatus() == PurchaseRequisitionPart.PRPartStatus.RECEIVED);
            
            System.out.println("hasOrderedParts: " + hasOrderedParts + ", allPartsReceived: " + allPartsReceived);
            
            // Update PR status accordingly
            if (allPartsReceived) {
                // If all parts are received, set PR status to COMPLETED
                if (pr.getStatus() != PurchaseRequisition.PRStatus.COMPLETED) {
                    System.out.println("Setting PR status to COMPLETED");
                    pr.setStatus(PurchaseRequisition.PRStatus.COMPLETED);
                    prRepository.saveAndFlush(pr);
                    System.out.println("PR status updated to COMPLETED and flushed");
                }
            } else if (hasOrderedParts) {
                // If any part is ordered, set to SENT_TO_PURCHASE
                if (pr.getStatus() != PurchaseRequisition.PRStatus.SENT_TO_PURCHASE && 
                    pr.getStatus() != PurchaseRequisition.PRStatus.COMPLETED) {
                    System.out.println("Setting PR status to SENT_TO_PURCHASE");
                    pr.setStatus(PurchaseRequisition.PRStatus.SENT_TO_PURCHASE);
                    prRepository.saveAndFlush(pr);
                    System.out.println("PR status updated to SENT_TO_PURCHASE and flushed");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error updating PR status based on PO status: " + e.getMessage());
            e.printStackTrace();
            // Don't throw exception to avoid blocking the part update
        }
    }

    // Statistics and Dashboard
    public long getTotalPRsCount() {
        return prRepository.count();
    }

    public long getPendingApprovalCount() {
        return prRepository.countPendingApproval();
    }

    public long getActionRequiredCount() {
        return prRepository.countActionRequired();
    }

    public long getReadyForPOCount() {
        return prRepository.countReadyForPO();
    }

    /**
     * Get count of parts by status
     */
    public long getPartsCountByStatus(PurchaseRequisitionPart.PRPartStatus status) {
        return purchaseRequisitionPartRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequisitionDTO> getRecentSubmissions(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return prRepository.findRecentSubmissions(since).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PurchaseRequisitionDTO> getPurchaseRequisitionsByStatus(PRStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return prRepository.findByStatus(status, pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseRequisitionDTO> getPendingApproval(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return prRepository.findPendingApproval(pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseRequisitionDTO> getActionRequired(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return prRepository.findActionRequired(pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseRequisitionDTO> getReadyForPO(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return prRepository.findReadyForPO(pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public long countApprovedPRs() {
        return prRepository.countByStatus(PRStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public long countRejectedPRs() {
        // Count PRs where status is SUBMITTED and isApproved is false
        return prRepository.findByIsApproved(false).size();
    }

    @Transactional(readOnly = true)
    public Page<PurchaseRequisitionDTO> getRejectedPRs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return prRepository.findByIsApproved(false, pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getPartsWithPRApprovalStatus(String searchTerm, String approvalFilter, int page, int size, String sortBy, boolean ascending) {
        // Get ALL PRs (not paginated) to collect all parts first
        List<PurchaseRequisition> allPrs = prRepository.findAll();
        
        List<Map<String, Object>> prParts = new ArrayList<>();
        
        for (PurchaseRequisition pr : allPrs) {
            if (pr.getRequisitionParts() != null) {
                for (PurchaseRequisitionPart prPart : pr.getRequisitionParts()) {
                    // Filter by approval status
                    if ("pending".equals(approvalFilter) && prPart.getIsPartApproved() != null) continue;
                    if ("approved".equals(approvalFilter) && !Boolean.TRUE.equals(prPart.getIsPartApproved())) continue;
                    if ("rejected".equals(approvalFilter) && !Boolean.FALSE.equals(prPart.getIsPartApproved())) continue;
                    
                    // Filter by search term
                    if (searchTerm != null && !searchTerm.isEmpty()) {
                        String search = searchTerm.toLowerCase();
                        if (!pr.getCode().toLowerCase().contains(search) && 
                            !prPart.getPart().getCode().toLowerCase().contains(search) &&
                            !prPart.getPart().getName().toLowerCase().contains(search)) {
                            continue;
                        }
                    }
                    
                    Map<String, Object> partMap = new HashMap<>();
                    partMap.put("partId", prPart.getPart().getId());
                    partMap.put("partCode", prPart.getPart().getCode());
                    partMap.put("partName", prPart.getPart().getName());
                    partMap.put("quantityRequested", prPart.getQuantityRequested());
                    partMap.put("prId", pr.getId());
                    partMap.put("prCode", pr.getCode());
                    partMap.put("requestorName", pr.getRequestor() != null ? pr.getRequestor().getName() : "Unknown");
                    partMap.put("prStatus", pr.getStatus().name());
                    partMap.put("prStatusDisplay", pr.getStatus().getDisplayName());
                    partMap.put("statusPriority", getStatusPriority(pr.getStatus())); // Add for sorting
                    partMap.put("isApproved", prPart.getIsPartApproved());
                    partMap.put("status", prPart.getStatus().name()); // Add PRPartStatus
                    partMap.put("statusDisplay", prPart.getStatus().getDisplayName()); // Add status display
                    partMap.put("criticalityLevel", prPart.getCriticalityLevel() != null ? 
                                prPart.getCriticalityLevel().getDisplayName() : "Medium");
                    partMap.put("criticalityDisplay", prPart.getCriticalityLevel() != null ? 
                                "badge-" + prPart.getCriticalityLevel().name().toLowerCase() : "badge-warning");
                    partMap.put("poNumber", prPart.getPoNumber()); // Add PO Number
                    
                    // Add criticality CSS class for color coding
                    String criticalityClass = "criticality-medium";
                    if (prPart.getCriticalityLevel() != null) {
                        switch (prPart.getCriticalityLevel().name()) {
                            case "CRITICAL":
                                criticalityClass = "criticality-critical";
                                break;
                            case "HIGH":
                                criticalityClass = "criticality-high";
                                break;
                            case "MEDIUM":
                                criticalityClass = "criticality-medium";
                                break;
                            case "LOW":
                                criticalityClass = "criticality-low";
                                break;
                        }
                    }
                    partMap.put("criticalityClass", criticalityClass);
                    
                    prParts.add(partMap);
                }
            }
        }
        
        // Sort by PR status priority (SUBMITTED first, then APPROVED, then COMPLETED)
        prParts.sort((a, b) -> {
            Integer priorityA = (Integer) a.get("statusPriority");
            Integer priorityB = (Integer) b.get("statusPriority");
            return priorityB.compareTo(priorityA); // Descending order (highest priority first)
        });
        
        // Apply sorting
        if (sortBy != null && !sortBy.isEmpty()) {
            prParts.sort((a, b) -> {
                Object valueA = a.get(sortBy);
                Object valueB = b.get(sortBy);
                int comparison = 0;
                
                // Special handling for prStatus to maintain custom order
                if ("prStatus".equals(sortBy)) {
                    int priorityA = getPRStatusSortPriority((String) valueA);
                    int priorityB = getPRStatusSortPriority((String) valueB);
                    comparison = Integer.compare(priorityA, priorityB);
                } else if (valueA instanceof Comparable<?> && valueB instanceof Comparable<?>) {
                    comparison = ((Comparable<Object>) valueA).compareTo(valueB);
                } else if (valueA != null && valueB != null) {
                    comparison = valueA.toString().compareTo(valueB.toString());
                }
                
                return ascending ? comparison : -comparison;
            });
        }
        
        // Convert to Page
        int fromIndex = Math.min(page * size, prParts.size());
        int toIndex = Math.min(fromIndex + size, prParts.size());
        List<Map<String, Object>> pageContent = prParts.isEmpty() ? new ArrayList<>() : prParts.subList(fromIndex, toIndex);
        
        Pageable pageable = PageRequest.of(page, size);
        
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, prParts.size());
    }
    
    /**
     * Get priority value for PR status sorting
     * Higher priority = shown first
     * SUBMITTED = 3 (highest)
     * APPROVED = 2
     * COMPLETED = 1 (lowest)
     */
    private int getStatusPriority(PurchaseRequisition.PRStatus status) {
        if (status == null) return 0;
        switch (status) {
            case SUBMITTED:
                return 3;
            case APPROVED:
                return 2;
            case COMPLETED:
                return 1;
            default:
                return 0;
        }
    }

    /**
     * Get priority value for PR Status string sorting in order:
     * SUBMITTED = 1, APPROVED = 2, SENT_TO_PURCHASE = 3, COMPLETED = 4
     */
    private int getPRStatusSortPriority(String status) {
        if (status == null) return 5;
        switch (status) {
            case "SUBMITTED":
                return 1;
            case "APPROVED":
                return 2;
            case "SENT_TO_PURCHASE":
                return 3;
            case "COMPLETED":
                return 4;
            default:
                return 5;
        }
    }

    @Transactional(readOnly = true)
    public long countPartsPendingApproval() {
        List<PurchaseRequisition> allPrs = prRepository.findAll();
        return allPrs.stream()
                .flatMap(pr -> pr.getRequisitionParts() != null ? pr.getRequisitionParts().stream() : java.util.stream.Stream.empty())
                .filter(prPart -> prPart.getIsPartApproved() == null)
                .count();
    }

    @Transactional(readOnly = true)
    public long countPartsApproved() {
        List<PurchaseRequisition> allPrs = prRepository.findAll();
        return allPrs.stream()
                .flatMap(pr -> pr.getRequisitionParts() != null ? pr.getRequisitionParts().stream() : java.util.stream.Stream.empty())
                .filter(prPart -> Boolean.TRUE.equals(prPart.getIsPartApproved()))
                .count();
    }

    @Transactional(readOnly = true)
    public long countPartsRejected() {
        List<PurchaseRequisition> allPrs = prRepository.findAll();
        return allPrs.stream()
                .flatMap(pr -> pr.getRequisitionParts() != null ? pr.getRequisitionParts().stream() : java.util.stream.Stream.empty())
                .filter(prPart -> Boolean.FALSE.equals(prPart.getIsPartApproved()))
                .count();
    }

    // Helper methods
    private PurchaseRequisitionPart createPRPart(PurchaseRequisition pr, PurchaseRequisitionPartDTO partDTO) {
        Part part = partRepository.findById(partDTO.getPartId())
                .orElseThrow(() -> new RuntimeException("Part not found with id: " + partDTO.getPartId()));

        return PurchaseRequisitionPart.builder()
                .purchaseRequisition(pr)
                .part(part)
                .quantityRequested(partDTO.getQuantityRequested())
                .criticalityLevel(partDTO.getCriticalityLevel())
                .justification(partDTO.getJustification())
                .notes(partDTO.getNotes())
                .build();
    }

    private PurchaseRequisitionDTO mapToDTO(PurchaseRequisition pr) {
        PurchaseRequisitionDTO dto = purchaseRequisitionMapper.toDTO(pr);
        
        // Set computed fields
        dto.setTotalParts(pr.getTotalParts());
        dto.setTotalQuantity(pr.getTotalQuantity());
        dto.setCanBeApproved(pr.canBeApproved());
        dto.setCanCreatePO(pr.canCreatePO());
        dto.setCanBeCompleted(pr.canBeCompleted());
        dto.setStatusDisplay(pr.getStatus().getDisplayName());
        dto.setSuppliers(pr.getSuppliers());

        return dto;
    }
}