package ahqpck.maintenance.report.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MachineTypeDTO {
    private String id;

    @NotBlank(message = "Code is mandatory")
    private String code;

    @NotBlank(message = "Name is mandatory")
    private String name;
}   