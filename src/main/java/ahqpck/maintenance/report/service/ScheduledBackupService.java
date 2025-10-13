package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.entity.BackupConfig;
import ahqpck.maintenance.report.repository.BackupConfigRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ScheduledBackupService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledBackupService.class);
    private final BackupConfigRepository backupConfigRepository;
    private final BackupConfigService backupConfigService;

    // Run every minute to check for due backups
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void checkAndExecuteBackups() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();
            LocalTime currentTime = now.toLocalTime();

            // Find active backup configs due today
            Iterable<BackupConfig> configs = backupConfigRepository.findAll();
            for (BackupConfig config : configs) {
                // Check if today is a backup day
                long daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(config.getStartDate(), today);
                boolean isBackupDay = daysSinceStart >= 0 && daysSinceStart % config.getIntervalDays() == 0;

                // Check if current time matches backup time (within 1-minute window)
                boolean isBackupTime = currentTime.getHour() == config.getBackupTime().getHour() &&
                        currentTime.getMinute() == config.getBackupTime().getMinute();

                // Check if not already executed today (simplified - in real app use execution
                // log)
                if (isBackupDay && isBackupTime) {
                    try {
                        Set<String> backupTypes = Stream.of(config.getBackupTypes().split(","))
                                .map(String::trim)
                                .filter(type -> !type.isEmpty())
                                .collect(Collectors.toSet());

                        log.info("Executing scheduled backup: folder={}, types={}",
                                config.getBackupFolder(), backupTypes);

                        backupConfigService.backupNow(config.getBackupFolder(), backupTypes, "AUTOMATIC");
                        log.info("Scheduled backup completed successfully");
                    } catch (Exception e) {
                        log.error("Failed to execute scheduled backup", e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in scheduled backup check", e);
        }
    }
}