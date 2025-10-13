package ahqpck.maintenance.report.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackupHistoryDTO {

    private Long id;
    private String backupDateTime; // Formatted as "yyyy-MM-dd HH:mm"
    private String backupTypes;    // Comma-separated string
    private String status;         // "Success", "Failed"
    private String method;         // "Automatic", "Manual"
    private String fileSize;
    private String location;
}