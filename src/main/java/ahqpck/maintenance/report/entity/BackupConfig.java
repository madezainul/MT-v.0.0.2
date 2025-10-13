package ahqpck.maintenance.report.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "backup_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackupConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer intervalDays;

    @Column(nullable = false)
    private LocalTime backupTime;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false, length = 500)
    private String backupFolder;

    // Store as comma-separated string: "USER,AREA,EQUIPMENT"
    @Column(name = "backup_types", nullable = false, length = 200)
    private String backupTypes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;
}