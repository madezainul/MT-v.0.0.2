package ahqpck.maintenance.report.repository;

import ahqpck.maintenance.report.entity.BackupHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackupHistoryRepository extends JpaRepository<BackupHistory, Long> {
}