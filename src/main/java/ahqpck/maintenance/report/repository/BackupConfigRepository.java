package ahqpck.maintenance.report.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ahqpck.maintenance.report.entity.BackupConfig;

public interface BackupConfigRepository extends JpaRepository<BackupConfig, Long> {
    Optional<BackupConfig> findTopByOrderByIdDesc();
}
