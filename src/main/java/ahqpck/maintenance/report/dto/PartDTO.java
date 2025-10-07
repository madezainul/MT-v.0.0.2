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
    private String id;

    @NotBlank(message = "Code is mandatory")
    private String code;

    @NotBlank(message = "Name is mandatory")
    private String name;
    
    @NotNull(message = "Category is mandatory")
    private String categoryName;

    @NotBlank(message = "Supplier is mandatory")
    private String supplierName;

    @NotBlank(message = "Section is mandatory")
    private String sectionCode;

    private String description;
    private String image;

    @NotNull(message = "Stock quantity is required")
    private Integer stockQuantity = 0;
    
    // BOM related fields
    private Integer equipmentCount = 0; // Number of equipment using this part
}