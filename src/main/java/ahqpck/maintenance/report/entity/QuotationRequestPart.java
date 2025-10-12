package ahqpck.maintenance.report.entity;

import java.math.BigDecimal;
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
@Table(name = "quotation_request_parts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationRequestPart {

    @Id
    @Column(length = 22, updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private QuotationRequest quotationRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @Column(name = "quantity_requested", nullable = false)
    private Integer quantityRequested;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "quantity_received")
    @Builder.Default
    private Integer quantityReceived = 0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = Base62.encode(UUID.randomUUID());
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        // Calculate total price if unit price is provided
        if (this.unitPrice != null && this.quantityRequested != null) {
            this.totalPrice = this.unitPrice.multiply(BigDecimal.valueOf(this.quantityRequested));
        }
    }

    // Business logic methods
    public boolean canBeReceived() {
        return quantityReceived < quantityRequested;
    }

    public boolean isFullyReceived() {
        return quantityReceived != null && quantityReceived >= quantityRequested;
    }

    public void receiveQuantity(Integer receivedQty) {
        this.quantityReceived = (this.quantityReceived == null ? 0 : this.quantityReceived) + receivedQty;
    }

    public Integer getRemainingQuantity() {
        return quantityRequested - (quantityReceived == null ? 0 : quantityReceived);
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

    public String getReceiveStatus() {
        if (quantityReceived == null || quantityReceived == 0) {
            return "Not Received";
        } else if (quantityReceived >= quantityRequested) {
            return "Fully Received";
        } else {
            return "Partially Received";
        }
    }
}