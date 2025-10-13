package ahqpck.maintenance.report.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "backup_histories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackupHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "backup_datetime", nullable = false)
    private LocalDateTime backupDateTime;

    @Column(name = "backup_types", nullable = false, length = 200)
    private String backupTypes; // Comma-separated: "USER,WORK_REPORT"

    @Column(nullable = false, length = 20)
    private String status; // "SUCCESS", "FAILED"

    @Column(nullable = false, length = 20)
    private String method; // "AUTOMATIC", "MANUAL"

    @Column(name = "file_size")
    private String fileSize; // "1.25 KB", "842 B", or null for failed

    @Column(nullable = false, length = 500)
    private String location;

    @Column
    private String errorMessage; // For failed backups

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}