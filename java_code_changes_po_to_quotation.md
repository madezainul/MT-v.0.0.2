# Java Code Changes Required for PO → Quotation Request Migration

## 1. Entity Classes to Rename/Update

### Files to Rename:
- `PurchaseOrder.java` → `QuotationRequest.java`
- `PurchaseOrderPart.java` → `QuotationRequestPart.java`
- `PurchaseOrderDTO.java` → `QuotationRequestDTO.java`
- `PurchaseOrderPartDTO.java` → `QuotationRequestPartDTO.java`
- `PurchaseOrderService.java` → `QuotationRequestService.java`
- `PurchaseOrderController.java` → `QuotationRequestController.java`
- `PurchaseOrderRepository.java` → `QuotationRequestRepository.java`
- `PurchaseOrderPartRepository.java` → `QuotationRequestPartRepository.java`

## 2. Entity Annotations to Update

### In QuotationRequest.java (formerly PurchaseOrder.java):
```java
@Entity
@Table(name = "quotation_requests")  // Changed from "purchase_orders"
public class QuotationRequest {

    @Column(name = "quotation_number", nullable = false, unique = true, length = 100)
    private String quotationNumber;  // Changed from poNumber

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;  // Changed from orderDate

    @OneToMany(mappedBy = "quotationRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<QuotationRequestPart> requestParts;  // Changed from orderParts

    // Update enum values
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private QRStatus status = QRStatus.CREATED;  // Renamed from POStatus
}
```

### In QuotationRequestPart.java (formerly PurchaseOrderPart.java):
```java
@Entity
@Table(name = "quotation_request_parts")  // Changed from "purchase_order_parts"
public class QuotationRequestPart {

    @Column(name = "quantity_requested", nullable = false)
    private Integer quantityRequested;  // Changed from quantityOrdered

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)  // Keep column name for DB compatibility
    private QuotationRequest quotationRequest;  // Changed property name
}
```

### In PurchaseRequisitionPart.java:
```java
@Column(name = "quotation_number", length = 100)  // Column renamed in DB
private String quotationNumber;  // Changed from poNumber

public void markAsOrdered(String quotationNumber, Integer orderedQuantity) {
    this.quotationNumber = quotationNumber;  // Updated parameter name
    // ... rest of method
}
```

## 3. Enum Updates

### Create new QRStatus enum (replace POStatus):
```java
public enum QRStatus {
    CREATED("Created"),
    SENT("Sent to Supplier"),
    CONFIRMED("Confirmed by Supplier"),
    QUOTED("Quote Received"),
    ACCEPTED("Quote Accepted"),
    REJECTED("Quote Rejected"),
    CANCELLED("Cancelled");

    private final String displayName;

    QRStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

## 4. Method Name Updates Throughout Codebase

### Search and Replace Patterns:
- `poNumber` → `quotationNumber`
- `orderDate` → `requestDate` 
- `orderParts` → `requestParts`
- `quantityOrdered` → `quantityRequested`
- `purchaseOrder` → `quotationRequest`
- `createPO` → `createQuotationRequest`
- `getPO` → `getQuotationRequest`
- `POStatus` → `QRStatus`

## 5. Controller URL Mapping Updates

### In QuotationRequestController.java:
```java
@RestController
@RequestMapping("/quotation-request")  // Changed from "/purchase-order"
public class QuotationRequestController {
    
    @GetMapping("/create-multi")  // Keep endpoint names similar for compatibility
    public String showCreateMultiForm() {
        // Implementation
    }
}
```

## 6. Template File Updates

### Files to Rename:
- `purchase-order/` folder → `quotation-request/`
- All `.html` files in the folder need URL updates
- Update all form actions and links

### Template Content Updates:
- "Purchase Order" → "Quotation Request"
- "PO" → "QR"
- "Order Date" → "Request Date"
- "PO Number" → "Quotation Number"

## 7. Database Configuration Updates

### In application.properties:
```properties
# If you have any specific PO-related configurations, update them
# Example: custom table naming strategies, etc.
```

## 8. Service Layer Updates

### Key Method Renames in QuotationRequestService:
- `createPurchaseOrder()` → `createQuotationRequest()`
- `updatePurchaseOrder()` → `updateQuotationRequest()`
- `getPurchaseOrderById()` → `getQuotationRequestById()`
- `getAllPurchaseOrders()` → `getAllQuotationRequests()`

## 9. Repository Updates

### Update all repository interfaces and custom queries:
```java
public interface QuotationRequestRepository extends JpaRepository<QuotationRequest, String> {
    
    List<QuotationRequest> findByQuotationNumberContaining(String quotationNumber);
    
    @Query("SELECT qr FROM QuotationRequest qr WHERE qr.status = :status")
    List<QuotationRequest> findByStatus(@Param("status") QRStatus status);
}
```

## 10. DTO Updates

### Update all DTO classes:
- Rename properties to match new entity names
- Update validation annotations if any reference old field names
- Update mapper classes that convert between entities and DTOs

## IMPORTANT NOTES:

1. **Foreign Key Column Names**: Keep the existing foreign key column names in the database (like `purchase_order_id`) to avoid complex FK constraint updates. Only rename the Java property names.

2. **Migration Order**: 
   - Run database migration first
   - Update Java entities to match new table/column names
   - Update all references throughout the codebase
   - Update templates and UI text

3. **Testing**: Thoroughly test all CRUD operations after migration to ensure data integrity.

4. **Backup**: Always backup your database before running the migration.

5. **Gradual Migration**: Consider doing this change in a feature branch and migrate gradually to minimize downtime.