package ahqpck.maintenance.report.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ahqpck.maintenance.report.dto.QuotationRequestDTO;
import ahqpck.maintenance.report.dto.QuotationRequestPartDTO;
import ahqpck.maintenance.report.dto.PurchaseRequisitionPartDTO;
import ahqpck.maintenance.report.mapper.PurchaseRequisitionMapper;
import ahqpck.maintenance.report.mapper.QuotationRequestMapper;
import ahqpck.maintenance.report.entity.QuotationRequest;
import ahqpck.maintenance.report.entity.QuotationRequestPart;
import ahqpck.maintenance.report.entity.QuotationRequest.QRStatus;
import ahqpck.maintenance.report.entity.PurchaseRequisitionPart;
import ahqpck.maintenance.report.entity.PurchaseRequisition;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.entity.PurchaseRequisition.PRStatus;
import ahqpck.maintenance.report.entity.PurchaseRequisitionPart.PRPartStatus;
import ahqpck.maintenance.report.repository.QuotationRequestRepository;
import ahqpck.maintenance.report.repository.QuotationRequestPartRepository;
import ahqpck.maintenance.report.repository.PurchaseRequisitionPartRepository;
import ahqpck.maintenance.report.repository.PurchaseRequisitionRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.repository.PartRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ahqpck.maintenance.report.util.Base62;

@Service
@RequiredArgsConstructor
public class QuotationRequestService {

    private final QuotationRequestRepository qrRepo;
    private final QuotationRequestPartRepository qrPartRepo;
    private final PurchaseRequisitionPartRepository prPartRepo;
    private final PurchaseRequisitionRepository prRepo;
    private final UserRepository userRepo;
    private final PartRepository partRepo;
    private final QuotationRequestMapper quotationRequestMapper;
    private final PurchaseRequisitionMapper purchaseRequisitionMapper;

    private static final Logger log = LoggerFactory.getLogger(QuotationRequestService.class);

    @Transactional
    public QuotationRequestDTO createQuotationRequestFromParts(String supplier, List<String> partIds, String creatorId, String notes) {
        // Get creator user
        User creator = userRepo.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User not found: " + creatorId));
        
        // Get the purchase requisition parts
        List<PurchaseRequisitionPart> parts = prPartRepo.findAllById(partIds);
        if (parts.isEmpty()) {
            throw new RuntimeException("No parts found for the given IDs");
        }

        // Validate all parts are from the same supplier and approved
        String partSupplier = parts.get(0).getPart().getSupplierName();
        if (!partSupplier.equals(supplier)) {
            throw new RuntimeException("Supplier mismatch");
        }

        boolean allApproved = parts.stream()
            .allMatch(p -> p.getPurchaseRequisition().getStatus() == 
                PRStatus.APPROVED);
        if (!allApproved) {
            throw new RuntimeException("All parts must be from approved purchase requisitions");
        }

    // Create QuotationRequest
    QuotationRequest qr = QuotationRequest.builder()
        .quotationNumber(null) // set after generation to allow retry
        .supplierName(supplier)
                .supplierContact("")
                .requestDate(LocalDate.now())
                .status(QRStatus.CREATED)
                .createdBy(creator)
                .notes(notes != null ? notes : "")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

    // Try saving with generated unique quotation number, retry on unique constraint violation
    qr = saveWithGeneratedQuotationNumber(qr, 5);

        // Create QuotationRequestParts
        for (PurchaseRequisitionPart prPart : parts) {
            QuotationRequestPart qrPart = QuotationRequestPart.builder()
                    .quotationRequest(qr)
                    .purchaseRequisitionPart(prPart) // Link to source PR part
                    .part(prPart.getPart())
                    .quantityRequested(prPart.getQuantityRequested())
                    .unitPrice(BigDecimal.ZERO)
                    .totalPrice(BigDecimal.ZERO)
                    .quantityReceived(0)
                    .build();
            qrPartRepo.save(qrPart);
            
            // Update PR part status
            prPart.markAsOrdered(qr.getQuotationNumber(), prPart.getQuantityRequested());
            prPartRepo.save(prPart);
        }

        // Reload to get the parts
        qr = qrRepo.findById(qr.getId()).orElseThrow();
        return quotationRequestMapper.toDTO(qr);
    }

    @Transactional
    public QuotationRequestDTO createQuotationRequestsFromSelectedParts(List<String> partIds, String creatorId, String notes) {
        // Get creator user
        User creator = userRepo.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User not found: " + creatorId));

        // Get the purchase requisition parts
        List<PurchaseRequisitionPart> parts = prPartRepo.findAllById(partIds);
        if (parts.isEmpty()) {
            throw new RuntimeException("No parts found for the given IDs");
        }

        // Group parts by supplier
        Map<String, List<PurchaseRequisitionPart>> partsBySupplier = parts.stream()
                .collect(Collectors.groupingBy(p -> p.getPart().getSupplierName()));

        QuotationRequestDTO firstQR = null;
        
        // Create QR for each supplier
        for (Map.Entry<String, List<PurchaseRequisitionPart>> entry : partsBySupplier.entrySet()) {
            String supplier = entry.getKey();
            List<PurchaseRequisitionPart> supplierParts = entry.getValue();

            // Validate all parts are approved
            boolean allApproved = supplierParts.stream()
                    .allMatch(p -> p.getPurchaseRequisition().getStatus() == 
                        PRStatus.APPROVED);
            if (!allApproved) {
                throw new RuntimeException("All parts must be from approved purchase requisitions");
            }

            // Create QuotationRequest
            QuotationRequest qr = QuotationRequest.builder()
            .quotationNumber(null)
                    .supplierName(supplier)
                    .supplierContact("")
                    .requestDate(LocalDate.now())
                    .status(QRStatus.CREATED)
                    .createdBy(creator)
                    .notes(notes != null ? notes : "")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

        qr = saveWithGeneratedQuotationNumber(qr, 5);

            // Create QuotationRequestParts
            for (PurchaseRequisitionPart prPart : supplierParts) {
                QuotationRequestPart qrPart = QuotationRequestPart.builder()
                        .quotationRequest(qr)
                        .purchaseRequisitionPart(prPart) // Link to source PR part
                        .part(prPart.getPart())
                        .quantityRequested(prPart.getQuantityRequested())
                        .unitPrice(BigDecimal.ZERO)
                        .totalPrice(BigDecimal.ZERO)
                        .quantityReceived(0)
                        .build();

                qrPartRepo.save(qrPart);
                
                // Update PR part status
                prPart.markAsOrdered(qr.getQuotationNumber(), prPart.getQuantityRequested());
                prPartRepo.save(prPart);
            }

            // Store first QR to return
            if (firstQR == null) {
                qr = qrRepo.findById(qr.getId()).orElseThrow();
                firstQR = quotationRequestMapper.toDTO(qr);
            }
        }

        return firstQR;
    }

    public Page<QuotationRequestDTO> getAllQuotationRequests(String searchTerm, int page, int size, String sortBy, boolean ascending) {
        Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<QuotationRequest> qrPage;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            qrPage = qrRepo.findByQuotationNumberContainingIgnoreCaseOrSupplierNameContainingIgnoreCase(
                searchTerm, searchTerm, pageable);
        } else {
            qrPage = qrRepo.findAll(pageable);
        }
        
        return qrPage.map(quotationRequestMapper::toDTO);
    }

    public QuotationRequestDTO getQuotationRequestById(String id) {
        QuotationRequest qr = qrRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation Request not found: " + id));
        return quotationRequestMapper.toDTO(qr);
    }

    public QuotationRequestDTO getQuotationRequestByNumber(String qrNumber) {
        QuotationRequest qr = qrRepo.findByQuotationNumber(qrNumber)
                .orElseThrow(() -> new RuntimeException("Quotation Request not found: " + qrNumber));
        return quotationRequestMapper.toDTO(qr);
    }

    @Transactional
    public void updateQRStatus(String id, QRStatus status, String notes) {
        QuotationRequest qr = qrRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation Request not found: " + id));
        
        qr.setStatus(status);
        if (notes != null && !notes.trim().isEmpty()) {
            qr.setNotes(qr.getNotes() + "\n" + LocalDateTime.now() + ": " + notes);
        }
        qr.setUpdatedAt(LocalDateTime.now());

        qrRepo.save(qr);
    }

    @Transactional
    public void receivePart(String qrId, String partId, Integer receivedQuantity, String inspectorId, String model, String newSupplier) {
        QuotationRequest qr = qrRepo.findById(qrId)
                .orElseThrow(() -> new RuntimeException("Quotation Request not found: " + qrId));

        QuotationRequestPart qrPart = qr.getRequestParts().stream()
                .filter(p -> p.getId().equals(partId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Part not found in Quotation Request"));

        // Check if model or supplier differs from original
        boolean modelChanged = model != null && !model.trim().isEmpty() && !model.equals(qrPart.getPartModel());
        boolean supplierChanged = newSupplier != null && !newSupplier.trim().isEmpty() && !newSupplier.equals(qrPart.getPartSupplier());

        log.info("Receiving part: {} - Original Model: '{}', New Model: '{}', ModelChanged: {}", 
                qrPart.getPartCode(), qrPart.getPartModel(), model, modelChanged);
        log.info("Receiving part: {} - Original Supplier: '{}', New Supplier: '{}', SupplierChanged: {}", 
                qrPart.getPartCode(), qrPart.getPartSupplier(), newSupplier, supplierChanged);

        // If model or supplier changed, create a new part
        if (modelChanged || supplierChanged) {
            log.info("Creating NEW part due to model/supplier change for: {}", qrPart.getPartCode());
            createNewPartForReceiving(qr, qrPart, receivedQuantity, inspectorId, model, newSupplier);
        } else {
            // Standard receiving - update existing QR part and inventory
            log.info("Updating EXISTING part for: {} with received quantity: {}", qrPart.getPartCode(), receivedQuantity);
            updateExistingPartReceiving(qr, qrPart, receivedQuantity, inspectorId);
        }

        // Update QR status based on received parts
        updateQuotationRequestStatus(qr);
        qrRepo.save(qr);
    }

    private void updateExistingPartReceiving(QuotationRequest qr, QuotationRequestPart qrPart, Integer receivedQuantity, String inspectorId) {
        qrPart.setQuantityReceived(qrPart.getQuantityReceived() + receivedQuantity);

        // Set inspector information if provided
        if (inspectorId != null && !inspectorId.trim().isEmpty()) {
            User inspector = userRepo.findById(inspectorId)
                    .orElseThrow(() -> new RuntimeException("Inspector not found: " + inspectorId));
            qrPart.setInspectedBy(inspector);
            qrPart.setInspectedAt(LocalDateTime.now());
        }

        qrPartRepo.save(qrPart);

        // Update the Part inventory - increment stock quantity
        if (qrPart.getPart() != null) {
            Part part = qrPart.getPart();
            part.setStockQuantity((part.getStockQuantity() != null ? part.getStockQuantity() : 0) + receivedQuantity);
            partRepo.save(part);
            log.info("Updated part inventory: {} - Added {} to stock (new total: {})", 
                    part.getCode(), receivedQuantity, part.getStockQuantity());
        }

        // Update corresponding PR part received quantity
        if (qrPart.getPurchaseRequisitionPart() != null) {
            PurchaseRequisitionPart prPart = qrPart.getPurchaseRequisitionPart();
            prPart.markAsReceived(receivedQuantity);
            prPartRepo.save(prPart);
        }
    }

    private void createNewPartForReceiving(QuotationRequest qr, QuotationRequestPart originalQrPart, 
            Integer receivedQuantity, String inspectorId, String model, String newSupplier) {
        
        log.info("Creating new part for receiving due to model/supplier change");
        
        // Get the original part to use as template
        Part originalPart = originalQrPart.getPart();
        
        // Create new part code (original code + "-ALT-" + timestamp suffix)
        String newPartCode = generateNewPartCode(originalPart.getCode());
        
        // Create new Part entity
        Part newPart = Part.builder()
                .id(Base62.encode(UUID.randomUUID()))
                .code(newPartCode)
                .name(originalPart.getName())
                .model(model != null && !model.trim().isEmpty() ? model : originalPart.getModel())
                .manufacturer(originalPart.getManufacturer())
                .categoryName(originalPart.getCategoryName())
                .supplierName(newSupplier != null && !newSupplier.trim().isEmpty() ? newSupplier : originalPart.getSupplierName())
                .sectionCode(originalPart.getSectionCode())
                .specification(originalPart.getSpecification())
                .image(originalPart.getImage())
                .stockQuantity(receivedQuantity)  // Set to received quantity
                .prQuantity(0)
                .safetyMinQty(0)
                .build();
        
        newPart = partRepo.save(newPart);
        log.info("New part created with code: {}", newPartCode);
        
        // Create new QuotationRequestPart for the new part
        QuotationRequestPart newQrPart = QuotationRequestPart.builder()
                .quotationRequest(qr)
                .part(newPart)
                .quantityRequested(receivedQuantity)
                .quantityReceived(receivedQuantity)
                .newModel(model)
                .newSupplier(newSupplier)
                .build();
        
        // Set inspector if provided
        if (inspectorId != null && !inspectorId.trim().isEmpty()) {
            User inspector = userRepo.findById(inspectorId)
                    .orElseThrow(() -> new RuntimeException("Inspector not found: " + inspectorId));
            newQrPart.setInspectedBy(inspector);
            newQrPart.setInspectedAt(LocalDateTime.now());
        }
        
        newQrPart = qrPartRepo.save(newQrPart);
        qr.getRequestParts().add(newQrPart);
        
        // Note: Do NOT update the PR part quantity since this is a different part
        // The new part will be tracked separately in inventory
        log.info("New QR part created for part code: {} with quantity: {}", newPart.getCode(), receivedQuantity);
    }

    private String generateNewPartCode(String originalCode) {
        // Generate new part code: originalCode-ALT-timestamp
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(6);  // Last 7 digits
        String newCode = originalCode + "-ALT-" + timestamp;
        
        // Check for uniqueness, if exists, append a counter
        int counter = 1;
        String candidate = newCode;
        while (partRepo.existsByCodeIgnoreCase(candidate)) {
            candidate = newCode + "-" + counter;
            counter++;
        }
        
        return candidate;
    }

    // Overloaded method for backward compatibility
    @Transactional
    public void receivePart(String qrId, String partId, Integer receivedQuantity, String inspectorId) {
        receivePart(qrId, partId, receivedQuantity, inspectorId, null, null);
    }

    @Transactional
    public void completeQuotationRequest(String id) {
        QuotationRequest qr = qrRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation Request not found: " + id));

        qr.setStatus(QRStatus.COMPLETED);
        qr.setActualDeliveryDate(LocalDate.now());
        qr.setUpdatedAt(LocalDateTime.now());

        qr = qrRepo.save(qr);

        // Check if PR can be completed when QR is completed
        checkAndUpdatePRCompletion(qr);
    }

    @Transactional
    public void cancelQuotationRequest(String id, String reason) {
        QuotationRequest qr = qrRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation Request not found: " + id));
        
        // Since CANCELLED doesn't exist, we'll use CREATED to indicate it's back to initial state
        qr.setStatus(QRStatus.CREATED);
        qr.setNotes(qr.getNotes() + "\n" + LocalDateTime.now() + ": CANCELLED - " + reason);
        qr.setUpdatedAt(LocalDateTime.now());

        qrRepo.save(qr);

        // Note: Skip updating PR parts since relationship may not exist
    }

    @Transactional
    public void deleteQuotationRequest(String id) {
        QuotationRequest qr = qrRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation Request not found: " + id));
        
        // Note: Skip updating PR parts since relationship may not exist
        qrRepo.delete(qr);
    }

    // Dashboard and statistics methods
    public long getTotalQRsCount() {
        return qrRepo.count();
    }

    public long getPendingQRsCount() {
        return qrRepo.countByStatus(QRStatus.CREATED);
    }

    public long getInProgressQRsCount() {
        return qrRepo.countByStatus(QRStatus.SENT);
    }

    public long getCompletedQRsCount() {
        return qrRepo.countByStatus(QRStatus.COMPLETED);
    }

    public List<QuotationRequestDTO> getRecentQRs(int days) {
        // Use a simpler approach since specific repository method may not exist
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<QuotationRequest> recentQRsPage = qrRepo.findAll(pageable);
        return recentQRsPage.getContent().stream()
                .map(quotationRequestMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<QuotationRequestDTO> getQuotationRequestsByStatus(QRStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<QuotationRequest> qrPage = qrRepo.findByStatus(status, pageable);
        return qrPage.map(quotationRequestMapper::toDTO);
    }

    public List<PurchaseRequisitionPartDTO> getAvailablePartsForSupplier(String supplier) {
        // TODO: Implement repository method for better performance
        // For now, use simple approach since specific repository method may not exist
        List<PurchaseRequisitionPart> allParts = prPartRepo.findAll();
        List<PurchaseRequisitionPart> filteredParts = allParts.stream()
                .filter(part -> part.getPart().getSupplierName().equals(supplier))
                .filter(part -> part.getStatus() == PRPartStatus.PENDING)
                .collect(Collectors.toList());
        return filteredParts.stream()
                .map(purchaseRequisitionMapper::toPartDTO)
                .collect(Collectors.toList());
    }

    public List<PurchaseRequisitionPartDTO> getAllAvailablePartsForQR() {
        // TODO: Implement repository method for better performance
        List<PurchaseRequisitionPart> allParts = prPartRepo.findAll();
        List<PurchaseRequisitionPart> availableParts = allParts.stream()
                .filter(part -> part.getStatus() == PRPartStatus.PENDING)
                .filter(part -> part.getPurchaseRequisition().getStatus() == PRStatus.APPROVED)
                .collect(Collectors.toList());
        return availableParts.stream()
                .map(purchaseRequisitionMapper::toPartDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public QuotationRequestDTO updateQuotationRequest(String id, String supplierName, String supplierContact, 
            String notes, LocalDate expectedDeliveryDate, List<QuotationRequestPartDTO> partUpdates) {

        QuotationRequest qr = qrRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation Request not found: " + id));
        
        // Update basic information
        if (supplierName != null) qr.setSupplierName(supplierName);
        if (supplierContact != null) qr.setSupplierContact(supplierContact);
        if (notes != null) qr.setNotes(notes);
        if (expectedDeliveryDate != null) qr.setExpectedDeliveryDate(expectedDeliveryDate);
        qr.setUpdatedAt(LocalDateTime.now());
        
        // Update parts if provided
        if (partUpdates != null) {
            for (QuotationRequestPartDTO partUpdate : partUpdates) {
                QuotationRequestPart qrPart = qr.getRequestParts().stream()
                        .filter(p -> p.getId().equals(partUpdate.getId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Part not found: " + partUpdate.getId()));
                
                if (partUpdate.getUnitPrice() != null) {
                    qrPart.setUnitPrice(partUpdate.getUnitPrice());
                    qrPart.setTotalPrice(partUpdate.getUnitPrice().multiply(new BigDecimal(qrPart.getQuantityRequested())));
                }
                
                qrPartRepo.save(qrPart);
            }
        }

        qr = qrRepo.save(qr);
        return quotationRequestMapper.toDTO(qr);
    }

    // Overloaded method to handle form parameters from controller
    @Transactional
    public QuotationRequestDTO updateQuotationRequest(String id, String supplierName, String supplierContact, 
            String notes, String expectedDeliveryDate, String[] partIds, Integer[] quantities, 
            String[] unitPrices, String[] partNotes) {

        QuotationRequest qr = qrRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation Request not found: " + id));
        
        // Update basic information
        if (supplierName != null && !supplierName.trim().isEmpty()) {
            qr.setSupplierName(supplierName);
        }
        if (supplierContact != null && !supplierContact.trim().isEmpty()) {
            qr.setSupplierContact(supplierContact);
        }
        if (notes != null && !notes.trim().isEmpty()) {
            qr.setNotes(notes);
        }
        if (expectedDeliveryDate != null && !expectedDeliveryDate.trim().isEmpty()) {
            try {
                qr.setExpectedDeliveryDate(LocalDate.parse(expectedDeliveryDate));
            } catch (Exception e) {
                // Invalid date format, skip update
            }
        }
        qr.setUpdatedAt(LocalDateTime.now());
        
        // Update parts if provided
        if (partIds != null && partIds.length > 0) {
            for (int i = 0; i < partIds.length; i++) {
                String partId = partIds[i];
                
                QuotationRequestPart qrPart = qr.getRequestParts().stream()
                        .filter(p -> p.getId().equals(partId))
                        .findFirst()
                        .orElse(null);
                        
                if (qrPart != null) {
                    // Update quantity if provided
                    if (quantities != null && i < quantities.length && quantities[i] != null) {
                        qrPart.setQuantityRequested(quantities[i]);
                    }
                    
                    // Update unit price if provided
                    if (unitPrices != null && i < unitPrices.length && unitPrices[i] != null && !unitPrices[i].trim().isEmpty()) {
                        try {
                            BigDecimal unitPrice = new BigDecimal(unitPrices[i]);
                            qrPart.setUnitPrice(unitPrice);
                            qrPart.setTotalPrice(unitPrice.multiply(new BigDecimal(qrPart.getQuantityRequested())));
                        } catch (NumberFormatException e) {
                            // Invalid price format, skip update
                        }
                    }
                    
                    // Update notes if provided
                    if (partNotes != null && i < partNotes.length && partNotes[i] != null && !partNotes[i].trim().isEmpty()) {
                        // Note: QuotationRequestPart entity doesn't have notes field, skip this update
                    }
                    
                    qrPartRepo.save(qrPart);
                }
            }
        }

        qr = qrRepo.save(qr);
        return quotationRequestMapper.toDTO(qr);
    }

    public Map<String, List<PurchaseRequisitionPartDTO>> getAvailablePartsGroupedBySupplier() {
        List<PurchaseRequisitionPartDTO> allPartsDTOs = getAllAvailablePartsForQR();
        List<PurchaseRequisitionPart> allParts = allPartsDTOs.stream()
                .map(dto -> prPartRepo.findById(dto.getId()).orElse(null))
                .filter(part -> part != null)
                .collect(Collectors.toList());
        
        return allParts.stream()
                .collect(Collectors.groupingBy(
                    part -> part.getPart().getSupplierName(),
                    Collectors.mapping(purchaseRequisitionMapper::toPartDTO, Collectors.toList())
                ));
    }

    // Helper methods
    private String generateQuotationNumber() {
        // Build prefix based on current year and month
        String year = String.valueOf(LocalDate.now().getYear());
        String month = String.format("%02d", LocalDate.now().getMonthValue());
        String prefix = "QR-" + year + month + "-";

        // Find latest QR that starts with this prefix
        java.util.Optional<QuotationRequest> latestOpt = qrRepo.findTopByQuotationNumberStartingWithOrderByQuotationNumberDesc(prefix);
        int nextSeq = 1;
        if (latestOpt.isPresent()) {
            String latest = latestOpt.get().getQuotationNumber();
            try {
                String[] parts = latest.split("-");
                String seqPart = parts[2];
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (Exception e) {
                // fallback to count-based approach if parsing fails
                nextSeq = (int) (qrRepo.count() + 1);
            }
        } else {
            nextSeq = 1;
        }

        return prefix + String.format("%04d", nextSeq);
    }

    /**
     * Attempts to generate a unique quotation number, set it on the entity and save.
     * Retries a few times if DataIntegrityViolationException occurs (unique constraint)
     */
    private QuotationRequest saveWithGeneratedQuotationNumber(QuotationRequest qr, int maxRetries) {
        int attempts = 0;
        while (attempts < maxRetries) {
            attempts++;
            String generated = generateQuotationNumber();
            qr.setQuotationNumber(generated);
            try {
                QuotationRequest saved = qrRepo.save(qr);
                return saved;
            } catch (DataIntegrityViolationException dive) {
                log.warn("Attempt {}: Quotation number {} caused DataIntegrityViolation (possible duplicate). Retrying...", attempts, generated);
                // small backoff to reduce tight loop in high-concurrency scenarios
                try { Thread.sleep(50L * attempts); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        throw new RuntimeException("Failed to generate unique quotation number after " + maxRetries + " attempts");
    }

    private void updateQuotationRequestStatus(QuotationRequest qr) {
        QRStatus originalStatus = qr.getStatus();
        boolean allPartsReceived = qr.getRequestParts().stream()
                .allMatch(part -> part.getQuantityReceived() != null &&
                        part.getQuantityReceived().equals(part.getQuantityRequested()));

        boolean somePartsReceived = qr.getRequestParts().stream()
                .anyMatch(part -> part.getQuantityReceived() != null && part.getQuantityReceived() > 0);

        if (allPartsReceived && qr.getStatus() != QRStatus.COMPLETED) {
            qr.setStatus(QRStatus.COMPLETED);
            qr.setActualDeliveryDate(LocalDate.now());
        } else if (somePartsReceived && !allPartsReceived) {
            // Keep status as CONFIRMED if partial receiving (not all parts fully received)
            // Do not transition to DELIVERED until all parts are fully received
            if (qr.getStatus() == QRStatus.CREATED || qr.getStatus() == QRStatus.SENT) {
                qr.setStatus(QRStatus.CONFIRMED);
            }
        } else if (!somePartsReceived && qr.getStatus() == QRStatus.CREATED) {
            // Keep as CREATED if no parts received yet
            qr.setStatus(QRStatus.CREATED);
        }
        // Note: Other status transitions should be handled explicitly through status update methods

        // Check PR completion if QR status changed to COMPLETED
        if (originalStatus != QRStatus.COMPLETED && qr.getStatus() == QRStatus.COMPLETED) {
            checkAndUpdatePRCompletion(qr);
        }
    }

    private void checkAndUpdatePRCompletion(QuotationRequest qr) {
        // Find all unique PRs that have parts in this QR
        Set<String> prIds = qr.getRequestParts().stream()
                .filter(part -> part.getPurchaseRequisitionPart() != null)
                .map(part -> part.getPurchaseRequisitionPart().getPurchaseRequisition().getId())
                .collect(Collectors.toSet());

        // Check each PR for completion
        for (String prId : prIds) {
            PurchaseRequisition pr = prRepo.findById(prId)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found: " + prId));

            log.info("Checking PR {} for completion. Status: {}", pr.getCode(), pr.getStatus());
            for (PurchaseRequisitionPart prPart : pr.getRequisitionParts()) {
                log.info("  PR Part {}: status={}, qtyOrdered={}, qtyReceived={}", prPart.getPartCode(), prPart.getStatus(), prPart.getQuantityOrdered(), prPart.getQuantityReceived());
            }

            if (pr.canBeCompleted() && pr.getStatus() != PRStatus.COMPLETED) {
                log.info("PR {} is now completed!", pr.getCode());
                pr.setStatus(PRStatus.COMPLETED);
                pr.setUpdatedAt(LocalDateTime.now());
                prRepo.save(pr);
            }
        }
    }

    public String formatNotes(String notes) {
        if (notes == null || notes.isEmpty()) {
            return notes;
        }
        
        StringBuilder formatted = new StringBuilder();
        String[] lines = notes.split("\n");
        DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_DATE_TIME;
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");
        
        for (String line : lines) {
            if (line.isEmpty()) continue;
            
            // Check if line starts with ISO datetime format (e.g., 2025-10-21T14:03:54)
            if (line.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")) {
                try {
                    // Extract the datetime part (before the colon that separates timestamp from content)
                    // Find the actual content separator (after milliseconds/nanoseconds)
                    int contentStart = line.indexOf(": ");
                    
                    if (contentStart > 0) {
                        String datetimeStr = line.substring(0, contentStart);
                        String content = line.substring(contentStart + 2);
                        
                        // Try to parse the datetime
                        try {
                            LocalDateTime dateTime = LocalDateTime.parse(datetimeStr, inputFormatter);
                            String formattedDateTime = dateTime.format(outputFormatter);
                            formatted.append(formattedDateTime).append(": ").append(content);
                        } catch (Exception e) {
                            // If parsing fails, use original line
                            formatted.append(line);
                        }
                    } else {
                        formatted.append(line);
                    }
                } catch (Exception e) {
                    formatted.append(line);
                }
            } else {
                formatted.append(line);
            }
            formatted.append("\n");
        }
        
        return formatted.toString().trim();
    }
}
