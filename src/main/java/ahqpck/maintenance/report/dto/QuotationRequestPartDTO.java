package ahqpck.maintenance.report.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationRequestPartDTO {

    private String id;
    private String quotationRequestId;

    @NotNull(message = "Part is required")
    private String partId;

    // Part information (from Part entity)
    private PartDTO part;
    private String partCode;
    private String partName;
    private String partModel;
    private String partSupplier;
    private String partCategory;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantityRequested;

    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    @Builder.Default
    private Integer quantityReceived = 0;

    private String notes;
    private LocalDateTime createdAt;

    // Changed model/supplier tracking
    private String newModel;
    private String newSupplier;

    // Inspector information
    private String inspectedById;
    private String inspectedByName;
    private LocalDateTime inspectedAt;

    // Display helper methods
    public String getFormattedUnitPrice() {
        return unitPrice != null ? String.format("%.2f", unitPrice) : "N/A";
    }

    public String getFormattedTotalPrice() {
        return totalPrice != null ? String.format("%.2f", totalPrice) : "N/A";
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

    public String getReceiveStatusBadgeClass() {
        String status = getReceiveStatus();
        return switch (status) {
            case "Not Received" -> "badge-warning";
            case "Partially Received" -> "badge-info";
            case "Fully Received" -> "badge-success";
            default -> "badge-secondary";
        };
    }

    public boolean canBeReceived() {
        return quantityReceived < quantityRequested;
    }

    public boolean isFullyReceived() {
        return quantityReceived != null && quantityReceived >= quantityRequested;
    }

    public Integer getRemainingQuantity() {
        return quantityRequested - (quantityReceived == null ? 0 : quantityReceived);
    }

    public Double getReceivePercentage() {
        if (quantityRequested == null || quantityRequested == 0) {
            return 0.0;
        }
        return ((double) (quantityReceived == null ? 0 : quantityReceived) / quantityRequested) * 100;
    }

    // Check if part has been changed during receiving
    public boolean isPartChanged() {
        boolean modelChanged = newModel != null && !newModel.trim().isEmpty() && 
                             !newModel.equals(partModel);
        boolean supplierChanged = newSupplier != null && !newSupplier.trim().isEmpty() && 
                                !newSupplier.equals(partSupplier);
        return modelChanged || supplierChanged;
    }
}