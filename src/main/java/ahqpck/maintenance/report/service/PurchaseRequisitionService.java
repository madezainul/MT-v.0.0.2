package ahqpck.maintenance.report.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ahqpck.maintenance.report.dto.DTOMapper;
import ahqpck.maintenance.report.dto.PurchaseRequisitionDTO;
import ahqpck.maintenance.report.dto.PurchaseRequisitionPartDTO;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.entity.PurchaseRequisition;
import ahqpck.maintenance.report.entity.PurchaseRequisition.PRStatus;
import ahqpck.maintenance.report.entity.PurchaseRequisitionPart;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.repository.PurchaseRequisitionPartRepository;
import ahqpck.maintenance.report.repository.PurchaseRequisitionRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.util.ZeroPaddedCodeGenerator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseRequisitionService {

    private final PurchaseRequisitionRepository prRepository;
    private final PurchaseRequisitionPartRepository prPartRepository;
    private final PartRepository partRepository;
    private final UserRepository userRepository;
    private final DTOMapper dtoMapper;
    private final ZeroPaddedCodeGenerator codeGenerator;

    // CRUD Operations
    @Transactional
    public PurchaseRequisitionDTO createPurchaseRequisition(PurchaseRequisitionDTO prDTO) {
        try {
            // Validate requestor
            User requestor = userRepository.findById(prDTO.getRequestorId())
                    .orElseThrow(() -> new RuntimeException("Requestor not found with id: " + prDTO.getRequestorId()));

            // Create PR entity
            PurchaseRequisition pr = PurchaseRequisition.builder()
                    .code(codeGenerator.generate(PurchaseRequisition.class, "code", "PR"))
                    .title(prDTO.getTitle())
                    .description(prDTO.getDescription())
                    .requestor(requestor)
                    .dateNeeded(prDTO.getDateNeeded())
                    .targetEquipmentId(prDTO.getTargetEquipmentId())
                    .targetEquipmentName(prDTO.getTargetEquipmentName())
                    .build();

            // Add parts if provided
            if (prDTO.getParts() != null && !prDTO.getParts().isEmpty()) {
                for (PurchaseRequisitionPartDTO partDTO : prDTO.getParts()) {
                    PurchaseRequisitionPart prPart = createPRPart(pr, partDTO);
                    pr.addPart(prPart);
                }
            }

            pr = prRepository.save(pr);
            return mapToDTO(pr);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create purchase requisition: " + e.getMessage(), e);
        }
    }

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

    public Page<PurchaseRequisitionDTO> getActionRequired(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return prRepository.findActionRequired(pageable).map(this::mapToDTO);
    }

    public Page<PurchaseRequisitionDTO> getReadyForPO(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return prRepository.findReadyForPO(pageable).map(this::mapToDTO);
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
        PurchaseRequisitionDTO dto = dtoMapper.mapToPurchaseRequisitionDTO(pr);
        
        // Set computed fields
        dto.setTotalParts(pr.getTotalParts());
        dto.setTotalQuantity(pr.getTotalQuantity());
        dto.setCanBeApproved(pr.canBeApproved());
        dto.setCanCreatePO(pr.canCreatePO());
        dto.setCanBeCompleted(pr.canBeCompleted());
        dto.setStatusDisplay(pr.getStatus().getDisplayName());
        dto.setSuppliers(pr.getSuppliers());
        dto.setQuotationNumber(pr.getQuotationNumber());

        return dto;
    }
}