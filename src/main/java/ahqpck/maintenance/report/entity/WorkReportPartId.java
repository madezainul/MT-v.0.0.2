package ahqpck.maintenance.report.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkReportPartId implements Serializable {
    private String workReportId;
    private String partId;

    // equals() and hashCode() can be added if needed
}