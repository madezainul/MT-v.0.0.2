package ahqpck.maintenance.report.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import ahqpck.maintenance.report.util.Base62;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "purchase_requisition_parts", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"purchase_requisition_id", "part_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseRequisitionPart {

    @Id
    @Column(length = 22, updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_requisition_id", nullable = false)
    private PurchaseRequisition purchaseRequisition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @Column(name = "quantity_requested", nullable = false)
    private Integer quantityRequested;

    @Enumerated(EnumType.STRING)
    @Column(name = "criticality_level", length = 20)
    @Builder.Default
    private CriticalityLevel criticalityLevel = CriticalityLevel.MEDIUM;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // QR and Status tracking
    @Column(name = "quotation_number", length = 100)
    private String quotationNumber;

    @Column(name = "quantity_ordered")
    @Builder.Default
    private Integer quantityOrdered = 0;

    @Column(name = "quantity_received")
    @Builder.Default
    private Integer quantityReceived = 0;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PRPartStatus status = PRPartStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = Base62.encode(UUID.randomUUID());
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    // Business logic methods
    public boolean canBeOrdered() {
        return status == PRPartStatus.PENDING;
    }

    public boolean canBeReceived() {
        return status == PRPartStatus.ORDERED && quantityOrdered > quantityReceived;
    }

    public boolean isFullyReceived() {
        return quantityReceived != null && quantityReceived.equals(quantityOrdered);
    }

    public void markAsOrdered(String quotationNumber, Integer orderedQuantity) {
        this.quotationNumber = quotationNumber;
        this.quantityOrdered = orderedQuantity;
        this.status = PRPartStatus.ORDERED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsReceived(Integer receivedQuantity) {
        this.quantityReceived = (this.quantityReceived == null ? 0 : this.quantityReceived) + receivedQuantity;
        this.receivedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // Update status based on received quantity
        if (isFullyReceived()) {
            this.status = PRPartStatus.RECEIVED;
        } else {
            this.status = PRPartStatus.PARTIALLY_RECEIVED;
        }
    }

    // Helper methods for display
    public String getPartCode() {
        return part != null ? part.getCode() : null;
    }

    public String getPartName() {
        return part != null ? part.getName() : null;
    }

    public String getPartSupplier() {
        return part != null ? part.getSupplierName() : null;
    }

    public String getPartCategory() {
        return part != null ? part.getCategoryName() : null;
    }

    public String getStatusDisplay() {
        return status != null ? status.getDisplayName() : "Unknown";
    }

    public String getCriticalityDisplay() {
        return criticalityLevel != null ? criticalityLevel.getDisplayName() : "Medium";
    }

    public enum PRPartStatus {
        PENDING("Pending"),
        ORDERED("Ordered"),
        PARTIALLY_RECEIVED("Partially Received"),
        RECEIVED("Received");

        private final String displayName;

        PRPartStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum CriticalityLevel {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High"),
        CRITICAL("Critical");

        private final String displayName;

        CriticalityLevel(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}