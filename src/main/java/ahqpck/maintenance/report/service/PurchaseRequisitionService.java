package ahqpck.maintenance.report.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ahqpck.maintenance.report.dto.DTOMapper;
import ahqpck.maintenance.report.dto.PurchaseRequisitionDTO;
import ahqpck.maintenance.report.dto.PurchaseRequisitionItemDTO;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.entity.PurchaseRequisition;
import ahqpck.maintenance.report.entity.PurchaseRequisition.PRStatus;
import ahqpck.maintenance.report.entity.PurchaseRequisitionItem;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.repository.PurchaseRequisitionItemRepository;
import ahqpck.maintenance.report.repository.PurchaseRequisitionRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.util.Base62;
import ahqpck.maintenance.report.util.ZeroPaddedCodeGenerator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseRequisitionService {

    private final PurchaseRequisitionRepository prRepository;
    private final PurchaseRequisitionItemRepository prItemRepository;
    private final PartRepository partRepository;
    private final UserRepository userRepository;
    private final DTOMapper dtoMapper;
    private final ZeroPaddedCodeGenerator codeGenerator;

    // CRUD Operations
    @Transactional
    public PurchaseRequisitionDTO createPurchaseRequisition(PurchaseRequisitionDTO prDTO) {
        try {
            // Validate requestor ID
            if (prDTO.getRequestorId() == null || prDTO.getRequestorId().trim().isEmpty()) {
                throw new RuntimeException("Requestor ID is required");
            }

            // Generate PR code
            String generatedCode = codeGenerator.generate(PurchaseRequisition.class, "code", "PR");

            // Get requestor user with better error message
            User requestor = userRepository.findById(prDTO.getRequestorId())
                    .orElseThrow(() -> new RuntimeException(
                        String.format("User not found with ID: %s. Please select a valid requestor from the dropdown.", 
                        prDTO.getRequestorId())));

            // Verify user is active (if you have status field)
            if (requestor.getStatus() != User.Status.ACTIVE) {
                throw new RuntimeException(
                    String.format("User %s (%s) is not active. Please select an active user.", 
                    requestor.getName(), requestor.getEmployeeId()));
            }

            // Create PR entity
            PurchaseRequisition pr = PurchaseRequisition.builder()
                    .id(Base62.encode(UUID.randomUUID()))
                    .code(generatedCode)
                    .title(prDTO.getTitle())
                    .description(prDTO.getDescription())
                    .requestor(requestor)
                    .dateNeeded(prDTO.getDateNeeded())
                    .targetEquipmentId(prDTO.getTargetEquipmentId())
                    .targetEquipmentName(prDTO.getTargetEquipmentName())
                    .status(PRStatus.SUBMITTED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // Save PR first
            pr = prRepository.save(pr);

            // Create and save PR items
            if (prDTO.getItems() != null && !prDTO.getItems().isEmpty()) {
                for (PurchaseRequisitionItemDTO itemDTO : prDTO.getItems()) {
                    PurchaseRequisitionItem item = createPRItem(pr, itemDTO);
                    pr.addItem(item);
                }
                pr = prRepository.save(pr);
            }

            return mapToDTO(pr);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create purchase requisition: " + e.getMessage(), e);
        }
    }

    public Page<PurchaseRequisitionDTO> getAllPurchaseRequisitions(String searchTerm, int page, int size, String sortBy, boolean ascending) {
        try {
            Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<PurchaseRequisition> prPage;
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                prPage = prRepository.searchPurchaseRequisitions(searchTerm.trim(), pageable);
            } else {
                prPage = prRepository.findAllByOrderByCreatedAtDesc(pageable);
            }

            return prPage.map(this::mapToDTO);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch purchase requisitions: " + e.getMessage(), e);
        }
    }

    public PurchaseRequisitionDTO getPurchaseRequisitionById(String id) {
        PurchaseRequisition pr = prRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + id));
        return mapToDTO(pr);
    }

    public PurchaseRequisitionDTO getPurchaseRequisitionByCode(String code) {
        PurchaseRequisition pr = prRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with code: " + code));
        return mapToDTO(pr);
    }

    @Transactional
    public PurchaseRequisitionDTO updatePurchaseRequisition(String id, PurchaseRequisitionDTO prDTO) {
        try {
            PurchaseRequisition pr = prRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + id));

            // Update requestor if requestorId is provided
            if (prDTO.getRequestorId() != null && !prDTO.getRequestorId().equals(pr.getRequestor().getId())) {
                User newRequestor = userRepository.findById(prDTO.getRequestorId())
                        .orElseThrow(() -> new RuntimeException("User not found with id: " + prDTO.getRequestorId()));
                pr.setRequestor(newRequestor);
            }

            // Update fields
            pr.setTitle(prDTO.getTitle());
            pr.setDescription(prDTO.getDescription());
            pr.setDateNeeded(prDTO.getDateNeeded());
            pr.setTargetEquipmentId(prDTO.getTargetEquipmentId());
            pr.setTargetEquipmentName(prDTO.getTargetEquipmentName());
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

            if (pr.getStatus() != PRStatus.SUBMITTED) {
                throw new RuntimeException("Cannot delete purchase requisition that is not in SUBMITTED status");
            }

            prRepository.delete(pr);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete purchase requisition: " + e.getMessage(), e);
        }
    }

    // Workflow Operations
    @Transactional
    public PurchaseRequisitionDTO approvePurchaseRequisition(String id, String reviewerName, String reviewNotes) {
        try {
            PurchaseRequisition pr = prRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + id));

            if (!pr.canBeApproved()) {
                throw new RuntimeException("Purchase Requisition cannot be approved in current status: " + pr.getStatus());
            }

            pr.setStatus(PRStatus.APPROVED);
            pr.setIsApproved(true);
            pr.setReviewerName(reviewerName);
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
    public PurchaseRequisitionDTO rejectPurchaseRequisition(String id, String reviewerName, String reviewNotes) {
        try {
            PurchaseRequisition pr = prRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + id));

            if (!pr.canBeApproved()) {
                throw new RuntimeException("Purchase Requisition cannot be reviewed in current status: " + pr.getStatus());
            }

            pr.setIsApproved(false);
            pr.setReviewerName(reviewerName);
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
    public PurchaseRequisitionDTO sendToPurchase(String id) {
        try {
            PurchaseRequisition pr = prRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + id));

            if (!pr.canBeSentToPurchase()) {
                throw new RuntimeException("Purchase Requisition cannot be sent to purchase in current status: " + pr.getStatus());
            }

            pr.setStatus(PRStatus.SENT_TO_PURCHASE);
            pr.setUpdatedAt(LocalDateTime.now());

            pr = prRepository.save(pr);
            return mapToDTO(pr);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send purchase requisition to purchase: " + e.getMessage(), e);
        }
    }

    @Transactional
    public PurchaseRequisitionDTO addPONumber(String id, String poNumber) {
        try {
            PurchaseRequisition pr = prRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + id));

            if (pr.getStatus() != PRStatus.SENT_TO_PURCHASE) {
                throw new RuntimeException("PO Number can only be added to purchase requisitions that are sent to purchase");
            }

            pr.setPoNumber(poNumber);
            pr.setUpdatedAt(LocalDateTime.now());

            pr = prRepository.save(pr);
            return mapToDTO(pr);

        } catch (Exception e) {
            throw new RuntimeException("Failed to add PO number: " + e.getMessage(), e);
        }
    }

    @Transactional
    public PurchaseRequisitionDTO completePurchaseRequisition(String id, String inspectorId, String completionNotes) {
        try {
            PurchaseRequisition pr = prRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + id));

            if (!pr.canBeCompleted()) {
                throw new RuntimeException("Purchase Requisition cannot be completed. Ensure PO number is provided.");
            }

            // Set inspector if provided
            if (inspectorId != null && !inspectorId.trim().isEmpty()) {
                User inspector = userRepository.findById(inspectorId)
                        .orElseThrow(() -> new RuntimeException("Inspector not found with id: " + inspectorId));
                
                if (inspector.getStatus() != User.Status.ACTIVE) {
                    throw new RuntimeException("Inspector user is not active: " + inspector.getName() + " (" + inspector.getEmployeeId() + ")");
                }
                
                pr.setInspector(inspector);
            }

            pr.setStatus(PRStatus.COMPLETED);
            pr.setCompletionNotes(completionNotes);
            pr.setReceivedAt(LocalDateTime.now());
            pr.setUpdatedAt(LocalDateTime.now());

            pr = prRepository.save(pr);
            return mapToDTO(pr);

        } catch (Exception e) {
            throw new RuntimeException("Failed to complete purchase requisition: " + e.getMessage(), e);
        }
    }

    // Statistics and Dashboard
    public long getTotalPRsCount() {
        return prRepository.count();
    }

    public long getPendingApprovalCount() {
        return prRepository.countPendingApproval();
    }

    public long getRequiringPONumberCount() {
        return prRepository.countRequiringPONumber();
    }

    public long getReadyForCompletionCount() {
        return prRepository.countReadyForCompletion();
    }

    public List<PurchaseRequisitionDTO> getRecentSubmissions(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return prRepository.findRecentSubmissions(since).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Page<PurchaseRequisitionDTO> getPurchaseRequisitionsByStatus(PRStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return prRepository.findByStatus(status, pageable).map(this::mapToDTO);
    }

    public Page<PurchaseRequisitionDTO> getPendingApproval(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return prRepository.findPendingApproval(pageable).map(this::mapToDTO);
    }

    // Helper methods
    private PurchaseRequisitionItem createPRItem(PurchaseRequisition pr, PurchaseRequisitionItemDTO itemDTO) {
        PurchaseRequisitionItem item = PurchaseRequisitionItem.builder()
                .id(Base62.encode(UUID.randomUUID()))
                .purchaseRequisition(pr)
                .partName(itemDTO.getPartName())
                .partDescription(itemDTO.getPartDescription())
                .partSpecifications(itemDTO.getPartSpecifications())
                .partCategory(itemDTO.getPartCategory())
                .partSupplier(itemDTO.getPartSupplier())
                .quantity(itemDTO.getQuantity())
                .unitMeasure(itemDTO.getUnitMeasure())
                .justification(itemDTO.getJustification())
                .criticalityLevel(itemDTO.getCriticalityLevel())
                .notes(itemDTO.getNotes())
                .isNewPart(itemDTO.getIsNewPart())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Link to existing part if provided
        if (itemDTO.getPartId() != null && !itemDTO.getPartId().trim().isEmpty()) {
            Part existingPart = partRepository.findById(itemDTO.getPartId())
                    .orElseThrow(() -> new RuntimeException("Part not found with id: " + itemDTO.getPartId()));
            item.setPart(existingPart);
            item.setIsNewPart(false);
        } else {
            item.setIsNewPart(true);
        }

        return prItemRepository.save(item);
    }

    private PurchaseRequisitionDTO mapToDTO(PurchaseRequisition pr) {
        PurchaseRequisitionDTO dto = dtoMapper.mapToPurchaseRequisitionDTO(pr);
        
        // Set computed fields
        dto.setTotalItems(pr.getTotalItems());
        dto.setTotalQuantity(pr.getTotalQuantity());
        dto.setCanBeApproved(pr.canBeApproved());
        dto.setCanBeSentToPurchase(pr.canBeSentToPurchase());
        dto.setCanBeReceived(pr.canBeReceived());
        dto.setCanBeCompleted(pr.canBeCompleted());
        dto.setStatusDisplay(pr.getStatus().getDisplayName());

        return dto;
    }
}