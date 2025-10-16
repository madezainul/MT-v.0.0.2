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
import jakarta.persistence.PreUpdate;
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

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requestor_id", nullable = false)
    private User requestor;

    @Column(name = "date_needed", nullable = false)
    private LocalDate dateNeeded;

    @Column(name = "target_equipment_id", length = 50)
    private String targetEquipmentId;

    @Column(name = "target_equipment_name")
    private String targetEquipmentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PRStatus status = PRStatus.SUBMITTED;

    @Column(name = "is_approved")
    private Boolean isApproved;

    @Column(name = "reviewer_name")
    private String reviewerName;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", nullable = true)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id", nullable = true)
    private User updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Many-to-Many relationship with Parts through bridge table
    @OneToMany(mappedBy = "purchaseRequisition", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseRequisitionPart> requisitionParts = new ArrayList<>();

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

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public void addPart(PurchaseRequisitionPart prPart) {
        requisitionParts.add(prPart);
        prPart.setPurchaseRequisition(this);
    }

    public void removePart(PurchaseRequisitionPart prPart) {
        requisitionParts.remove(prPart);
        prPart.setPurchaseRequisition(null);
    }

    public int getTotalParts() {
        return requisitionParts.size();
    }

    public long getTotalQuantity() {
        return requisitionParts.stream()
                .mapToLong(PurchaseRequisitionPart::getQuantityRequested)
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

    // Business logic methods
    public boolean canBeApproved() {
        return status == PRStatus.SUBMITTED && !requisitionParts.isEmpty();
    }

    public boolean canCreatePO() {
        return status == PRStatus.APPROVED && Boolean.TRUE.equals(isApproved);
    }

    public boolean canBeCompleted() {
        return status == PRStatus.APPROVED && 
               requisitionParts.stream().allMatch(rp -> rp.getStatus() == PurchaseRequisitionPart.PRPartStatus.RECEIVED);
    }

    // Get parts grouped by supplier for PO creation
    public List<String> getSuppliers() {
        return requisitionParts.stream()
                .map(rp -> rp.getPart().getSupplierName())
                .distinct()
                .sorted()
                .toList();
    }

    // Get QR number if all parts have the same QR number
    public String getQuotationNumber() {
        if (requisitionParts == null || requisitionParts.isEmpty()) {
            return null;
        }
        
        String firstQrNumber = requisitionParts.get(0).getQuotationNumber();
        if (firstQrNumber == null) {
            return null;
        }
        
        // Check if all parts have the same QR number
        boolean allSameQr = requisitionParts.stream()
                .allMatch(part -> firstQrNumber.equals(part.getQuotationNumber()));
        
        return allSameQr ? firstQrNumber : null;
    }

    public enum PRStatus {
        SUBMITTED("Submitted"),
        APPROVED("Approved"), 
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