package ahqpck.maintenance.report.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseRequisitionItemDTO {

    private String id;
    
    private String purchaseRequisitionId;

    // For existing parts
    private String partId;
    private PartDTO existingPart;

    // For new parts
    private String partCode;

    @NotBlank(message = "Part name is required")
    private String partName;

    private String partDescription;
    private String partSpecifications;
    private String partCategory;
    private String partSupplier;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private String unitMeasure;
    private String justification;
    private String criticalityLevel;
    private String notes;

    @Builder.Default
    private Boolean isNewPart = false;

    @Builder.Default
    private Boolean isReceived = false;

    private Integer receivedQuantity;
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Display helper fields
    private String displayPartCode;
    private String displayPartName;
    private String displaySupplier;
    private String displayCategory;
    private Integer currentStock;
    private Boolean canBeReceived;

    // Helper methods for display
    public String getFormattedReceivedAt() {
        return receivedAt != null ? receivedAt.toString() : "";
    }

    public String getReceiptStatus() {
        if (Boolean.TRUE.equals(isReceived)) {
            return "Received (" + receivedQuantity + "/" + quantity + ")";
        }
        return "Pending";
    }

    public String getPartTypeDisplay() {
        return Boolean.TRUE.equals(isNewPart) ? "New Part" : "Existing Part";
    }

    public String getCriticalityDisplay() {
        if (criticalityLevel == null) return "Standard";
        switch (criticalityLevel.toUpperCase()) {
            case "CRITICAL": return "Critical";
            case "HIGH": return "High";
            case "MEDIUM": return "Medium";
            case "LOW": return "Low";
            default: return "Standard";
        }
    }
}