package ahqpck.maintenance.report.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ahqpck.maintenance.report.util.Base62;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "purchase_requisitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseRequisition {

    @Id
    @Column(length = 22, updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requestor_id", nullable = false)
    private User requestor;

    @Column(name = "date_needed", nullable = false)
    private LocalDate dateNeeded;

    @Column(name = "target_equipment_id")
    private String targetEquipmentId;

    @Column(name = "target_equipment_name")
    private String targetEquipmentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PRStatus status = PRStatus.SUBMITTED;

    @Column(name = "is_approved")
    private Boolean isApproved;

    @Column(name = "reviewer_name")
    private String reviewerName;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes")
    private String reviewNotes;

    @Column(name = "po_number")
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id")
    private User inspector;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "completion_notes")
    private String completionNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "purchaseRequisition", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseRequisitionItem> items = new ArrayList<>();

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
    public void addItem(PurchaseRequisitionItem item) {
        items.add(item);
        item.setPurchaseRequisition(this);
    }

    public void removeItem(PurchaseRequisitionItem item) {
        items.remove(item);
        item.setPurchaseRequisition(null);
    }

    public int getTotalItems() {
        return items.size();
    }

    public long getTotalQuantity() {
        return items.stream()
                .mapToLong(PurchaseRequisitionItem::getQuantity)
                .sum();
    }

    // Requestor helper methods
    public String getRequestorName() {
        return requestor != null ? requestor.getName() : null;
    }

    public String getRequestorEmail() {
        return requestor != null ? requestor.getEmail() : null;
    }

    public String getRequestorEmployeeId() {
        return requestor != null ? requestor.getEmployeeId() : null;
    }

    // Inspector helper methods
    public String getInspectorName() {
        return inspector != null ? inspector.getName() : null;
    }

    public String getInspectorEmail() {
        return inspector != null ? inspector.getEmail() : null;
    }

    public String getInspectorEmployeeId() {
        return inspector != null ? inspector.getEmployeeId() : null;
    }

    public boolean canBeApproved() {
        return status == PRStatus.SUBMITTED;
    }

    public boolean canBeSentToPurchase() {
        return status == PRStatus.APPROVED && Boolean.TRUE.equals(isApproved);
    }

    public boolean canBeReceived() {
        return status == PRStatus.SENT_TO_PURCHASE;
    }

    public boolean canBeCompleted() {
        return status == PRStatus.SENT_TO_PURCHASE && poNumber != null && !poNumber.trim().isEmpty();
    }

    public enum PRStatus {
        SUBMITTED("Submitted"),
        APPROVED("Approved"), 
        SENT_TO_PURCHASE("Sent to Purchase"),
        COMPLETED("Completed");

        private final String displayName;

        PRStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}