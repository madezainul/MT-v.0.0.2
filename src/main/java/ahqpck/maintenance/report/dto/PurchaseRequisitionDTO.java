package ahqpck.maintenance.report.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ahqpck.maintenance.report.entity.PurchaseRequisition.PRStatus;
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
public class PurchaseRequisitionDTO {

    private String id;
    private String code;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Requestor is required")
    private String requestorId;

    private String requestorName;
    private String requestorEmail;
    private String requestorEmployeeId;

    @NotNull(message = "Date needed is required")
    private LocalDate dateNeeded;

    private String targetEquipmentId;
    private String targetEquipmentName;

    private PRStatus status;
    private String statusDisplay;

    private Boolean isApproved;
    private String reviewerName;
    private LocalDateTime reviewedAt;
    private String reviewNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<PurchaseRequisitionPartDTO> parts = new ArrayList<>();

    // Computed fields
    private Integer totalParts;
    private Long totalQuantity;
    private Boolean canBeApproved;
    private Boolean canCreatePO;
    private Boolean canBeCompleted;

    // Suppliers for PO creation
    private List<String> suppliers;

    // QR Number if created
    private String quotationNumber;

    // Helper methods for display
    public String getFormattedDateNeeded() {
        return dateNeeded != null ? dateNeeded.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "";
    }

    public String getApprovalStatusDisplay() {
        if (isApproved == null) {
            return "Pending Approval";
        }
        return Boolean.TRUE.equals(isApproved) ? "Approved" : "Rejected";
    }

    public String getFormattedCreatedAt() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "";
    }

    public String getStatusBadgeClass() {
        if (status == null) return "badge-secondary";
        return switch (status) {
            case SUBMITTED -> Boolean.TRUE.equals(isApproved) ? "badge-success" : 
                           (Boolean.FALSE.equals(isApproved) ? "badge-danger" : "badge-warning");
            case APPROVED -> "badge-success";
            case COMPLETED -> "badge-primary";
        };
    }

    // Add part helper
    public void addPart(PurchaseRequisitionPartDTO part) {
        if (this.parts == null) {
            this.parts = new ArrayList<>();
        }
        this.parts.add(part);
    }

    // Template compatibility methods
    public List<PurchaseRequisitionPartDTO> getRequisitionParts() {
        return this.parts;
    }

    public String getStatusDisplay() {
        return status != null ? status.getDisplayName() : "Unknown";
    }
}