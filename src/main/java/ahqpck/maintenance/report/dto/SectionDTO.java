package ahqpck.maintenance.report.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionDTO {
    private String id;

    @NotBlank(message = "Code is mandatory")
    private String code;
}
