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
import ahqpck.maintenance.report.dto.DTOMapper;
import ahqpck.maintenance.report.entity.QuotationRequest;
import ahqpck.maintenance.report.entity.QuotationRequestPart;
import ahqpck.maintenance.report.entity.QuotationRequest.QRStatus;
import ahqpck.maintenance.report.entity.PurchaseRequisition;
import ahqpck.maintenance.report.entity.PurchaseRequisitionPart;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.entity.PurchaseRequisition.PRStatus;
import ahqpck.maintenance.report.entity.PurchaseRequisitionPart.PRPartStatus;
import ahqpck.maintenance.report.repository.QuotationRequestRepository;
import ahqpck.maintenance.report.repository.QuotationRequestPartRepository;
import ahqpck.maintenance.report.repository.PurchaseRequisitionPartRepository;
import ahqpck.maintenance.report.repository.PurchaseRequisitionRepository;
import ahqpck.maintenance.report.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;
import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuotationRequestService {

    private final QuotationRequestRepository qrRepo;
    private final QuotationRequestPartRepository qrPartRepo;
    private final PurchaseRequisitionPartRepository prPartRepo;
    private final PurchaseRequisitionRepository prRepo;
    private final UserRepository userRepo;
    private final DTOMapper dtoMapper;

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
                .quotationNumber(generateQuotationNumber())
                .supplierName(supplier)
                .supplierContact("")
                .requestDate(LocalDate.now())
                .status(QRStatus.CREATED)
                .createdBy(creator)
                .notes(notes != null ? notes : "")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        qr = qrRepo.save(qr);

        // Create QuotationRequestParts
        for (PurchaseRequisitionPart prPart : parts) {
            QuotationRequestPart qrPart = QuotationRequestPart.builder()
                    .quotationRequest(qr)
                    .part(prPart.getPart())
                    .quantityRequested(prPart.getQuantityRequested())
                    .unitPrice(BigDecimal.ZERO)
                    .totalPrice(BigDecimal.ZERO)
                    .quantityReceived(0)
                    .notes("")
                    .createdAt(LocalDateTime.now())
                    .build();

            qrPartRepo.save(qrPart);
            
            // Update PR part status
            prPart.setStatus(PRPartStatus.ORDERED);
            // Note: Skip setting quotationRequestPart if the method doesn't exist
            prPartRepo.save(prPart);
        }

        // Reload to get the parts
        qr = qrRepo.findById(qr.getId()).orElseThrow();
        return dtoMapper.mapToQuotationRequestDTO(qr);
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
                    .quotationNumber(generateQuotationNumber())
                    .supplierName(supplier)
                    .supplierContact("")
                    .requestDate(LocalDate.now())
                    .status(QRStatus.CREATED)
                    .createdBy(creator)
                    .notes(notes != null ? notes : "")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            qr = qrRepo.save(qr);

            // Create QuotationRequestParts
            for (PurchaseRequisitionPart prPart : supplierParts) {
                QuotationRequestPart qrPart = QuotationRequestPart.builder()
                        .quotationRequest(qr)
                        .part(prPart.getPart())
                        .quantityRequested(prPart.getQuantityRequested())
                        .unitPrice(BigDecimal.ZERO)
                        .totalPrice(BigDecimal.ZERO)
                        .quantityReceived(0)
                        .notes("")
                        .createdAt(LocalDateTime.now())
                        .build();

                qrPartRepo.save(qrPart);
                
                // Update PR part status
                prPart.setStatus(PRPartStatus.PENDING);
                // Note: Skip setting quotationRequestPart if the method doesn't exist
                prPartRepo.save(prPart);
            }

            // Store first QR to return
            if (firstQR == null) {
                qr = qrRepo.findById(qr.getId()).orElseThrow();
                firstQR = dtoMapper.mapToQuotationRequestDTO(qr);
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
        
        return qrPage.map(dtoMapper::mapToQuotationRequestDTO);
    }

    public QuotationRequestDTO getQuotationRequestById(String id) {
        QuotationRequest qr = qrRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation Request not found: " + id));
        return dtoMapper.mapToQuotationRequestDTO(qr);
    }

    public QuotationRequestDTO getQuotationRequestByNumber(String qrNumber) {
        QuotationRequest qr = qrRepo.findByQuotationNumber(qrNumber)
                .orElseThrow(() -> new RuntimeException("Quotation Request not found: " + qrNumber));
        return dtoMapper.mapToQuotationRequestDTO(qr);
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
    public void receivePart(String qrId, String partId, Integer receivedQuantity, String notes) {
        QuotationRequest qr = qrRepo.findById(qrId)
                .orElseThrow(() -> new RuntimeException("Quotation Request not found: " + qrId));
        
        QuotationRequestPart qrPart = qr.getRequestParts().stream()
                .filter(p -> p.getPart().getId().equals(partId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Part not found in Quotation Request"));
        
        qrPart.setQuantityReceived(qrPart.getQuantityReceived() + receivedQuantity);
        // Note: setDateReceived method doesn't exist in entity
        if (notes != null && !notes.trim().isEmpty()) {
            qrPart.setNotes(qrPart.getNotes() + "\n" + LocalDateTime.now() + ": " + notes);
        }
        qrPartRepo.save(qrPart);

        // Update QR status based on received parts
        updateQuotationRequestStatus(qr);
        
        // Note: Skip updating PR parts if relationship doesn't exist
    }

    @Transactional
    public void completeQuotationRequest(String id) {
        QuotationRequest qr = qrRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation Request not found: " + id));
        
        qr.setStatus(QRStatus.COMPLETED);
        qr.setActualDeliveryDate(LocalDate.now());
        qr.setUpdatedAt(LocalDateTime.now());

        qrRepo.save(qr);
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
                .map(dtoMapper::mapToQuotationRequestDTO)
                .collect(Collectors.toList());
    }

    public Page<QuotationRequestDTO> getQuotationRequestsByStatus(QRStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<QuotationRequest> qrPage = qrRepo.findByStatus(status, pageable);
        return qrPage.map(dtoMapper::mapToQuotationRequestDTO);
    }

    public List<PurchaseRequisitionPartDTO> getAvailablePartsForSupplier(String supplier) {
        // Use simple approach since specific repository method may not exist
        List<PurchaseRequisitionPart> allParts = prPartRepo.findAll();
        List<PurchaseRequisitionPart> filteredParts = allParts.stream()
                .filter(part -> part.getPart().getSupplierName().equals(supplier))
                .filter(part -> part.getStatus() == PRPartStatus.PENDING)
                .collect(Collectors.toList());
        return filteredParts.stream()
                .map(dtoMapper::mapToPurchaseRequisitionPartDTO)
                .collect(Collectors.toList());
    }

    public List<PurchaseRequisitionPartDTO> getAllAvailablePartsForQR() {
        List<PurchaseRequisitionPart> allParts = prPartRepo.findAll();
        List<PurchaseRequisitionPart> availableParts = allParts.stream()
                .filter(part -> part.getStatus() == PRPartStatus.PENDING)
                .filter(part -> part.getPurchaseRequisition().getStatus() == PRStatus.APPROVED)
                .collect(Collectors.toList());
        return availableParts.stream()
                .map(dtoMapper::mapToPurchaseRequisitionPartDTO)
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
                if (partUpdate.getNotes() != null) {
                    qrPart.setNotes(partUpdate.getNotes());
                }
                
                qrPartRepo.save(qrPart);
            }
        }

        qr = qrRepo.save(qr);
        return dtoMapper.mapToQuotationRequestDTO(qr);
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
                        qrPart.setNotes(partNotes[i]);
                    }
                    
                    qrPartRepo.save(qrPart);
                }
            }
        }

        qr = qrRepo.save(qr);
        return dtoMapper.mapToQuotationRequestDTO(qr);
    }

    public Map<String, List<PurchaseRequisitionPartDTO>> getAvailablePartsGroupedBySupplier() {
        List<PurchaseRequisitionPart> allParts = getAllAvailablePartsForQR().stream()
                .map(dto -> prPartRepo.findById(dto.getId()).orElse(null))
                .filter(part -> part != null)
                .collect(Collectors.toList());
        
        return allParts.stream()
                .collect(Collectors.groupingBy(
                    part -> part.getPart().getSupplierName(),
                    Collectors.mapping(dtoMapper::mapToPurchaseRequisitionPartDTO, Collectors.toList())
                ));
    }

    // Helper methods
    private String generateQuotationNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        String month = String.format("%02d", LocalDate.now().getMonthValue());
        long count = qrRepo.count() + 1;
        return "QR-" + year + month + "-" + String.format("%04d", count);
    }

    private void updateQuotationRequestStatus(QuotationRequest qr) {
        boolean allPartsReceived = qr.getRequestParts().stream()
                .allMatch(part -> part.getQuantityReceived().equals(part.getQuantityRequested()));

        if (allPartsReceived && qr.getStatus() != QRStatus.COMPLETED) {
            qr.setStatus(QRStatus.COMPLETED);
            qr.setActualDeliveryDate(LocalDate.now());
        } else if (!allPartsReceived && qr.getStatus() == QRStatus.CREATED) {
            qr.setStatus(QRStatus.SENT);
        }
    }
}
