package ahqpck.maintenance.report.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackupConfigDTO {

    private Long id;

    @NotNull(message = "Interval days is required")
    @Min(value = 1, message = "Minimum 1 day")
    @Max(value = 30, message = "Maximum 30 days")
    private Integer intervalDays;

    @NotBlank(message = "Backup time is required")
    private String backupTime;

    @NotBlank(message = "Start date is required")
    private String startDate;

    @NotBlank(message = "Backup folder is required")
    private String backupFolder;

    private String backupTypes;
}