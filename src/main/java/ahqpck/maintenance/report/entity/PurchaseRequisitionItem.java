package ahqpck.maintenance.report.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import ahqpck.maintenance.report.util.Base62;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "purchase_requisition_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseRequisitionItem {

    @Id
    @Column(length = 22, updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_requisition_id", nullable = false)
    private PurchaseRequisition purchaseRequisition;

    // For existing parts - reference to Part entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id")
    private Part part;

    // For new parts - manual entry fields
    @Column(name = "part_code")
    private String partCode;

    @Column(name = "part_name", nullable = false)
    private String partName;

    @Column(name = "part_description")
    private String partDescription;

    @Column(name = "part_specifications")
    private String partSpecifications;

    @Column(name = "part_category")
    private String partCategory;

    @Column(name = "part_supplier")
    private String partSupplier;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_measure")
    private String unitMeasure;

    private String justification;

    @Column(name = "criticality_level")
    private String criticalityLevel;

    private String notes;

    @Column(name = "is_new_part")
    @Builder.Default
    private Boolean isNewPart = false;

    @Column(name = "is_received")
    @Builder.Default
    private Boolean isReceived = false;

    @Column(name = "received_quantity")
    private Integer receivedQuantity;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

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

    // Helper methods
    public boolean isExistingPart() {
        return part != null && !Boolean.TRUE.equals(isNewPart);
    }

    public String getDisplayPartCode() {
        if (isExistingPart()) {
            return part.getCode();
        }
        return partCode != null ? partCode : "To be generated";
    }

    public String getDisplayPartName() {
        if (isExistingPart()) {
            return part.getName();
        }
        return partName;
    }

    public String getDisplaySupplier() {
        if (isExistingPart()) {
            return part.getSupplierName();
        }
        return partSupplier;
    }

    public String getDisplayCategory() {
        if (isExistingPart()) {
            return part.getCategoryName();
        }
        return partCategory;
    }

    public boolean canBeReceived() {
        return !Boolean.TRUE.equals(isReceived);
    }

    public void markAsReceived(Integer receivedQty, String generatedPartCode) {
        this.isReceived = true;
        this.receivedQuantity = receivedQty;
        this.receivedAt = LocalDateTime.now();
        if (Boolean.TRUE.equals(isNewPart) && generatedPartCode != null) {
            this.partCode = generatedPartCode;
        }
        this.updatedAt = LocalDateTime.now();
    }
}