package ahqpck.maintenance.report.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ahqpck.maintenance.report.entity.PurchaseRequisitionPart.CriticalityLevel;
import ahqpck.maintenance.report.entity.PurchaseRequisitionPart.PRPartStatus;
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
public class PurchaseRequisitionPartDTO {

    private String id;
    private String purchaseRequisitionId;
    private String prCode; // Add PR code for display

    @NotNull(message = "Part is required")
    private String partId;

    // Part information (from Part entity)
    private PartDTO part;
    private String partCode;
    private String partName;
    private String partSupplier;
    private String partCategory;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantityRequested;

    @Builder.Default
    private CriticalityLevel criticalityLevel = CriticalityLevel.MEDIUM;

    private String justification;
    private String notes;

    // QR and Status tracking
    private String quotationNumber;
    @Builder.Default
    private Integer quantityOrdered = 0;
    @Builder.Default
    private Integer quantityReceived = 0;
    private LocalDateTime receivedAt;

    @Builder.Default
    private PRPartStatus status = PRPartStatus.PENDING;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Display helper methods
    public String getStatusDisplay() {
        return status != null ? status.getDisplayName() : "Unknown";
    }

    public String getCriticalityDisplay() {
        return criticalityLevel != null ? criticalityLevel.getDisplayName() : "Medium";
    }

    public String getStatusBadgeClass() {
        if (status == null) return "badge-secondary";
        return switch (status) {
            case PENDING -> "badge-warning";
            case ORDERED -> "badge-info";
            case PARTIALLY_RECEIVED -> "badge-primary";
            case RECEIVED -> "badge-success";
        };
    }

    public String getCriticalityBadgeClass() {
        if (criticalityLevel == null) return "badge-secondary";
        return switch (criticalityLevel) {
            case LOW -> "badge-light";
            case MEDIUM -> "badge-info";
            case HIGH -> "badge-warning";
            case CRITICAL -> "badge-danger";
        };
    }

    public String getFormattedReceivedAt() {
        return receivedAt != null ? receivedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "";
    }

    public boolean canBeOrdered() {
        return status == PRPartStatus.PENDING;
    }

    public boolean canBeReceived() {
        return status == PRPartStatus.ORDERED && quantityOrdered > quantityReceived;
    }

    public boolean isFullyReceived() {
        return quantityReceived != null && quantityReceived.equals(quantityOrdered);
    }

    public Integer getRemainingQuantity() {
        if (quantityOrdered == null || quantityReceived == null) {
            return quantityRequested;
        }
        return quantityOrdered - quantityReceived;
    }

    public Double getReceivePercentage() {
        if (quantityOrdered == null || quantityOrdered == 0) {
            return 0.0;
        }
        return ((double) (quantityReceived == null ? 0 : quantityReceived) / quantityOrdered) * 100;
    }
}