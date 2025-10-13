package ahqpck.maintenance.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssigneeTotalStatusDTO {
    private String assigneeName;
    private String assigneeEmpId;
    private int totalOpen;
    private int totalPending;
    private int totalClosed;
}