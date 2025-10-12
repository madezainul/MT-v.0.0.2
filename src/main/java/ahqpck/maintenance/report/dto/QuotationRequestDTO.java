package ahqpck.maintenance.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ahqpck.maintenance.report.entity.QuotationRequest.QRStatus;
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
public class QuotationRequestDTO {

    private String id;

    @NotBlank(message = "Quotation Number is required")
    private String quotationNumber;

    @NotBlank(message = "Supplier name is required")
    private String supplierName;

    private String supplierContact;
    private BigDecimal totalAmount;

    @Builder.Default
    private String currency = "USD";

    @NotNull(message = "Request date is required")
    private LocalDate requestDate;

    private LocalDate expectedDeliveryDate;
    private LocalDate actualDeliveryDate;

    @Builder.Default
    private QRStatus status = QRStatus.CREATED;

    private String notes;

    private String createdById;
    private String createdByName;
    private String createdByEmail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<QuotationRequestPartDTO> parts = new ArrayList<>();

    // Computed fields
    private Integer totalParts;
    private Long totalQuantity;
    private Long totalReceivedQuantity;
    private Boolean canBeSent;
    private Boolean canBeReceived;
    private Boolean canBeCompleted;
    private Boolean isFullyReceived;

    // Helper methods for display
    public String getFormattedRequestDate() {
        return requestDate != null ? requestDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "";
    }

    public String getFormattedExpectedDeliveryDate() {
        return expectedDeliveryDate != null ? expectedDeliveryDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "";
    }

    public String getFormattedActualDeliveryDate() {
        return actualDeliveryDate != null ? actualDeliveryDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "";
    }

    public String getFormattedCreatedAt() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "";
    }

    public String getStatusDisplay() {
        return status != null ? status.getDisplayName() : "Unknown";
    }

    public String getStatusBadgeClass() {
        if (status == null) return "badge-secondary";
        return switch (status) {
            case CREATED -> "badge-warning";
            case SENT -> "badge-info";
            case CONFIRMED -> "badge-primary";
            case DELIVERED -> "badge-success";
            case COMPLETED -> "badge-dark";
        };
    }

    public String getFormattedTotalAmount() {
        if (totalAmount == null) return "N/A";
        return String.format("%s %.2f", currency, totalAmount);
    }

    public Double getReceivePercentage() {
        if (totalQuantity == null || totalQuantity == 0) {
            return 0.0;
        }
        return ((double) (totalReceivedQuantity == null ? 0 : totalReceivedQuantity) / totalQuantity) * 100;
    }

    // Add part helper
    public void addPart(QuotationRequestPartDTO part) {
        if (this.parts == null) {
            this.parts = new ArrayList<>();
        }
        this.parts.add(part);
    }
}