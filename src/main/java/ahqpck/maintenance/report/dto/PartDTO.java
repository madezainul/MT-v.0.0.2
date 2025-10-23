package ahqpck.maintenance.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartDTO {
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    // PR quantity (total quantity requested in PRs)
    private Integer prQuantity = 0;

    // Safety quantity minimum
    private Integer safetyMinQty = 0;
    private String id;

    @NotBlank(message = "Code is mandatory")
    private String code;

    @NotBlank(message = "Name is mandatory")
    private String name;
    
    private String model;

    private String manufacturer;
    
    @NotNull(message = "Category is mandatory")
    private String categoryName;

    @NotBlank(message = "Supplier is mandatory")
    private String supplierName;

    @NotBlank(message = "Section is mandatory")
    private String sectionCode;
    private String specification;
    private String image;

    @NotNull(message = "Stock quantity is required")
    private Integer stockQuantity = 0;

    // BOM related fields
    private Integer equipmentCount = 0; // Number of equipment using this part

    // Helper method: Check if stock is below safety minimum
    public boolean isBelowSafetyMinimum() {
        if (safetyMinQty == null || stockQuantity == null) {
            return false;
        }
        return stockQuantity < safetyMinQty;
    }

    // Helper method: Check if total (stock + PR quantity) meets safety minimum
    public boolean isStockWithPRBelowSafetyMinimum() {
        if (safetyMinQty == null || stockQuantity == null || prQuantity == null) {
            return false;
        }
        return (stockQuantity + prQuantity) < safetyMinQty;
    }
}