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
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationRequestDTO {

    private String id;

    @NotBlank(message = "Quotation Number is required")
    private String quotationNumber;

    private String purchaseRequisitionId;
    private String purchaseRequisitionCode;
    private String purchaseRequisitionTitle;

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
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private QRStatus status = QRStatus.CREATED;

    private String notes;

    private String createdById;
    private String createdByName;
    private String createdByEmail;

    private LocalDateTime createdAt;

    private String updatedById;
    private String updatedByName;
    private String updatedByEmail;

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

    public String getFormattedNotes() {
        if (notes == null || notes.isEmpty()) {
            return notes;
        }
        
        StringBuilder formatted = new StringBuilder();
        String[] lines = notes.split("\n");
        DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_DATE_TIME;
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");
        
        for (String line : lines) {
            if (line.isEmpty()) continue;
            
            // Check if line starts with ISO datetime format (e.g., 2025-10-21T14:03:54)
            if (line.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")) {
                try {
                    // Find the actual content separator (after milliseconds/nanoseconds)
                    int contentStart = line.indexOf(": ");
                    
                    if (contentStart > 0) {
                        String datetimeStr = line.substring(0, contentStart);
                        String content = line.substring(contentStart + 2);
                        
                        try {
                            java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(datetimeStr, inputFormatter);
                            String formattedDateTime = dateTime.format(outputFormatter);
                            formatted.append(formattedDateTime).append(": ").append(content);
                        } catch (Exception e) {
                            // If parsing fails, use original line
                            formatted.append(line);
                        }
                    } else {
                        formatted.append(line);
                    }
                } catch (Exception e) {
                    formatted.append(line);
                }
            } else {
                formatted.append(line);
            }
            formatted.append("\n");
        }
        
        return formatted.toString().trim();
    }

    // Add part helper
    public void addPart(QuotationRequestPartDTO part) {
        if (this.parts == null) {
            this.parts = new ArrayList<>();
        }
        this.parts.add(part);
    }
}