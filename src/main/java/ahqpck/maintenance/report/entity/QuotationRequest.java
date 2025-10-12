package ahqpck.maintenance.report.entity;

import java.math.BigDecimal;
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
@Table(name = "quotation_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationRequest {

    @Id
    @Column(length = 22, updatable = false, nullable = false)
    private String id;

    @Column(name = "quotation_number", nullable = false, unique = true, length = 100)
    private String quotationNumber;

    @Column(name = "supplier_name", nullable = false, length = 100)
    private String supplierName;

    @Column(name = "supplier_contact", columnDefinition = "TEXT")
    private String supplierContact;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private QRStatus status = QRStatus.CREATED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "quotationRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<QuotationRequestPart> requestParts = new ArrayList<>();

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
        if (this.requestDate == null) {
            this.requestDate = LocalDate.now();
        }
    }

    // Helper methods
    public void addPart(QuotationRequestPart requestPart) {
        requestParts.add(requestPart);
        requestPart.setQuotationRequest(this);
    }

    public void removePart(QuotationRequestPart requestPart) {
        requestParts.remove(requestPart);
        requestPart.setQuotationRequest(null);
    }

    public int getTotalParts() {
        return requestParts.size();
    }

    public long getTotalQuantity() {
        return requestParts.stream()
                .mapToLong(QuotationRequestPart::getQuantityRequested)
                .sum();
    }

    public long getTotalReceivedQuantity() {
        return requestParts.stream()
                .mapToLong(QuotationRequestPart::getQuantityReceived)
                .sum();
    }

    public BigDecimal calculateTotalAmount() {
        return requestParts.stream()
                .map(QuotationRequestPart::getTotalPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Business logic methods
    public boolean canBeSent() {
        return status == QRStatus.CREATED && !requestParts.isEmpty();
    }

    public boolean canBeReceived() {
        return status == QRStatus.SENT || status == QRStatus.CONFIRMED;
    }

    public boolean canBeCompleted() {
        return (status == QRStatus.DELIVERED || status == QRStatus.CONFIRMED) &&
               requestParts.stream().allMatch(part -> part.getQuantityReceived() >= part.getQuantityRequested());
    }

    public boolean isFullyReceived() {
        return requestParts.stream().allMatch(part -> part.getQuantityReceived() >= part.getQuantityRequested());
    }

    // Creator helper methods
    public String getCreatedByName() {
        return createdBy != null ? createdBy.getName() : null;
    }

    public String getCreatedByEmail() {
        return createdBy != null ? createdBy.getEmail() : null;
    }

    public enum QRStatus {
        CREATED("Created"),
        SENT("Sent"),
        CONFIRMED("Confirmed"),
        DELIVERED("Delivered"),
        COMPLETED("Completed");

        private final String displayName;

        QRStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}