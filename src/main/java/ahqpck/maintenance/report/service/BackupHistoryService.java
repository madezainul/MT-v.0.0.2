package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.BackupHistoryDTO;
import ahqpck.maintenance.report.entity.BackupHistory;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.repository.BackupHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BackupHistoryService {

    private final BackupHistoryRepository backupHistoryRepository;

    public Page<BackupHistoryDTO> getAllBackups(int page, int size, String sortBy, boolean asc) {
        Sort sort = asc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<BackupHistory> historyPage = backupHistoryRepository.findAll(pageable);
        return historyPage.map(this::toDTO);
    }

    public void deleteBackup(Long id) {
        if (!backupHistoryRepository.existsById(id)) {
            throw new NotFoundException("Backup history not found with ID: " + id);
        }
        backupHistoryRepository.deleteById(id);
    }

    private BackupHistoryDTO toDTO(BackupHistory history) {
        BackupHistoryDTO dto = new BackupHistoryDTO();
        dto.setId(history.getId());
        
        // Format datetime as "yyyy-MM-dd HH:mm"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        dto.setBackupDateTime(history.getBackupDateTime().format(formatter));
        
        dto.setBackupTypes(history.getBackupTypes());
        
        // Convert status to display format
        dto.setStatus("SUCCESS".equals(history.getStatus()) ? "SUCCESS" : "FAILED");
        dto.setMethod("AUTOMATIC".equals(history.getMethod()) ? "AUTOMATIC" : "MANUAL");
        dto.setFileSize(history.getFileSize());
        dto.setLocation(history.getLocation());
        
        return dto;
    }
}