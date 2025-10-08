package ahqpck.maintenance.report.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private String poNumber;
    private String inspectorId;
    private String inspectorName;
    private String inspectorEmail;
    private String inspectorEmployeeId;
    private LocalDateTime receivedAt;
    private String completionNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<PurchaseRequisitionItemDTO> items = new ArrayList<>();

    // Computed fields
    private Integer totalItems;
    private Long totalQuantity;
    private Boolean canBeApproved;
    private Boolean canBeSentToPurchase;
    private Boolean canBeReceived;
    private Boolean canBeCompleted;

    // Helper methods for display
    public String getFormattedDateNeeded() {
        return dateNeeded != null ? dateNeeded.toString() : "";
    }

    public String getApprovalStatusDisplay() {
        if (isApproved == null) return "Pending Review";
        return Boolean.TRUE.equals(isApproved) ? "Approved" : "Rejected";
    }

    public String getFormattedCreatedAt() {
        return createdAt != null ? createdAt.toString() : "";
    }

    // Add item helper
    public void addItem(PurchaseRequisitionItemDTO item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
    }
}