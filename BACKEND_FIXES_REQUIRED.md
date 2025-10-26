# Controller, Service, Repository and DTO Fixes

## Issues Identified

### 1. Controller - receiveParts() method
**File:** `QuotationRequestController.java` (lines 249-329)

**Issue:** 
- Method receives `@RequestParam` arrays but the debug shows the FormData might not be parsing correctly
- Need to fix the parameter binding for array inputs from FormData

**Fix:**
```java
@PostMapping("/{id}/receive")
public String receiveParts(
    @PathVariable String id,
    @RequestParam(value = "partIds", required = false) String[] partIds,
    @RequestParam(value = "receivedQuantities", required = false) Integer[] receivedQuantities,
    @RequestParam(value = "inspectorIds", required = false) String[] inspectorIds,
    @RequestParam(value = "models", required = false) String[] models,
    @RequestParam(value = "newSuppliers", required = false) String newSuppliers,
    @RequestParam(required = false) String receivingNotes,
    RedirectAttributes ra) {
    
    try {
        // Validation
        if (partIds == null || partIds.length == 0) {
            ra.addFlashAttribute("error", "No parts selected. Please select at least one part to receive.");
            return "redirect:/quotation-request/" + id;
        }
        
        if (receivedQuantities == null || receivedQuantities.length == 0) {
            ra.addFlashAttribute("error", "No quantities received. Please enter a quantity for at least one part.");
            return "redirect:/quotation-request/" + id;
        }
        
        // Process each part
        int processedCount = 0;
        for (int i = 0; i < partIds.length; i++) {
            String partId = partIds[i];
            Integer quantity = (i < receivedQuantities.length) ? receivedQuantities[i] : 0;
            String inspectorId = (inspectorIds != null && i < inspectorIds.length && !inspectorIds[i].isEmpty()) ? inspectorIds[i] : null;
            String model = (models != null && i < models.length && !models[i].isEmpty()) ? models[i] : null;
            
            if (quantity != null && quantity > 0) {
                qrService.receivePart(id, partId, quantity, inspectorId, model, newSuppliers);
                processedCount++;
            }
        }
        
        if (processedCount > 0) {
            ra.addFlashAttribute("success", processedCount + " part(s) received successfully!");
        } else {
            ra.addFlashAttribute("error", "No valid quantities found. Please enter quantity > 0 for at least one part.");
        }
        
    } catch (Exception e) {
        e.printStackTrace();
        ra.addFlashAttribute("error", "Failed to receive parts: " + e.getMessage());
    }
    
    return "redirect:/quotation-request/" + id;
}
```

### 2. Service - receivePart() method
**File:** `QuotationRequestService.java` (lines 240-320)

**Issue:**
- The method correctly handles model/supplier changes
- But `updateQuotationRequestStatus()` might not be setting COMPLETED status correctly
- Need to ensure all parts are fully received before marking as COMPLETED

**Fix:** Add this helper method if not present:
```java
private void updateQuotationRequestStatus(QuotationRequest qr) {
    // Check if all parts are fully received
    boolean allPartsReceived = qr.getRequestParts().stream()
        .allMatch(part -> part.getQuantityReceived() >= part.getQuantityRequested());
    
    if (allPartsReceived && !qr.getRequestParts().isEmpty()) {
        qr.setStatus(QRStatus.COMPLETED);
        qr.setActualDeliveryDate(LocalDate.now());
        qr.setUpdatedAt(LocalDateTime.now());
    }
}
```

### 3. DTO - QuotationRequestPartDTO
**File:** `QuotationRequestPartDTO.java`

**Required fields:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationRequestPartDTO {
    private String id;
    private String partCode;
    private String partName;
    private String partCategory;
    private String partSupplier;
    private Integer quantityRequested;
    private Integer quantityReceived;
    private String receiveStatus;  // "Pending", "Partially Received", "Fully Received"
    private String newModel;
    private String newSupplier;
    private String partModel;
    private Double receivePercentage;
    private String inspectedByName;
    private LocalDateTime inspectedAt;
    private String inspectedById;
    
    // Helper methods
    public boolean canBeReceived() {
        return quantityReceived < quantityRequested;
    }
    
    public String getReceiveStatus() {
        if (quantityReceived == null || quantityReceived == 0) {
            return "Pending";
        } else if (quantityReceived >= quantityRequested) {
            return "Fully Received";
        } else {
            return "Partially Received";
        }
    }
    
    public Double getReceivePercentage() {
        if (quantityRequested == null || quantityRequested == 0) return 0.0;
        return (double) (quantityReceived != null ? quantityReceived : 0) / quantityRequested * 100;
    }
    
    public String getReceiveStatusBadgeClass() {
        String status = getReceiveStatus();
        if ("Pending".equals(status)) return "badge-warning";
        if ("Partially Received".equals(status)) return "badge-info";
        if ("Fully Received".equals(status)) return "badge-success";
        return "badge-secondary";
    }
}
```

### 4. Repository - QuotationRequestPartRepository
**File:** `QuotationRequestPartRepository.java`

**Required methods:**
```java
public interface QuotationRequestPartRepository extends JpaRepository<QuotationRequestPart, String> {
    List<QuotationRequestPart> findByQuotationRequestId(String qrId);
    
    Optional<QuotationRequestPart> findByIdAndQuotationRequestId(String partId, String qrId);
    
    List<QuotationRequestPart> findByQuotationRequestIdAndQuantityReceivedLessThan(String qrId, Integer quantity);
}
```

### 5. Entity - QuotationRequestPart
**File:** `QuotationRequestPart.java`

**Add these helper methods:**
```java
@Entity
@Table(name = "quotation_request_parts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationRequestPart {
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_request_id", nullable = false)
    private QuotationRequest quotationRequest;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id")
    private Part part;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pr_part_id")
    private PurchaseRequisitionPart purchaseRequisitionPart;
    
    @Column(name = "quantity_requested")
    private Integer quantityRequested;
    
    @Column(name = "quantity_received")
    private Integer quantityReceived = 0;
    
    @Column(name = "unit_price")
    private BigDecimal unitPrice;
    
    @Column(name = "total_price")
    private BigDecimal totalPrice;
    
    @Column(name = "new_model")
    private String newModel;
    
    @Column(name = "new_supplier")
    private String newSupplier;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspected_by_user_id")
    private User inspectedBy;
    
    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt;
    
    // Helper methods
    public boolean canBeReceived() {
        return quantityReceived < quantityRequested;
    }
    
    public Integer getRemainingQuantity() {
        return (quantityRequested != null ? quantityRequested : 0) - (quantityReceived != null ? quantityReceived : 0);
    }
    
    public Double getReceivePercentage() {
        if (quantityRequested == null || quantityRequested == 0) return 0.0;
        return (double) (quantityReceived != null ? quantityReceived : 0) / quantityRequested * 100;
    }
    
    public String getReceiveStatus() {
        if (quantityReceived == null || quantityReceived == 0) {
            return "Pending";
        } else if (quantityReceived >= quantityRequested) {
            return "Fully Received";
        } else {
            return "Partially Received";
        }
    }
    
    public String getReceiveStatusBadgeClass() {
        String status = getReceiveStatus();
        if ("Pending".equals(status)) return "badge-warning";
        if ("Partially Received".equals(status)) return "badge-info";
        if ("Fully Received".equals(status)) return "badge-success";
        return "badge-secondary";
    }
    
    public String getPartCode() {
        return part != null ? part.getCode() : null;
    }
    
    public String getPartName() {
        return part != null ? part.getName() : null;
    }
    
    public String getPartCategory() {
        return part != null ? part.getCategoryName() : null;
    }
    
    public String getPartSupplier() {
        return part != null ? part.getSupplierName() : null;
    }
    
    public String getPartModel() {
        return part != null ? part.getModel() : null;
    }
    
    public String getInspectedByName() {
        return inspectedBy != null ? inspectedBy.getName() : null;
    }
}
```

## Implementation Steps

1. Update `QuotationRequestController.java` - receiveParts() method
2. Update `QuotationRequestService.java` - add/fix updateQuotationRequestStatus()
3. Update `QuotationRequestPartDTO.java` - add helper methods
4. Update `QuotationRequestPart.java` - add helper methods
5. Update `QuotationRequestPartRepository.java` - add query methods
6. Rebuild and test receiving workflow

## Testing Checklist

- [ ] Select 1+ parts to receive
- [ ] Enter quantities for selected parts
- [ ] Select inspector (optional)
- [ ] Enter new model (optional)
- [ ] Enter new supplier (optional)
- [ ] Click "Receive Selected Parts"
- [ ] Verify FormData is sent correctly
- [ ] Verify parts are marked as received in database
- [ ] Verify QR status updates to COMPLETED when all parts received
- [ ] Verify redirect back to QR detail page
- [ ] Check console logs for any errors
