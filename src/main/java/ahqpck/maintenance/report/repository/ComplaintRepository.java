package ahqpck.maintenance.report.repository;

import ahqpck.maintenance.report.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, String>, JpaSpecificationExecutor<Complaint> {
    List<Complaint> findByStatus(Complaint.Status status);
    boolean existsByCodeIgnoreCase(String code);
    // List<Complaint> findByEquipment(String equipment);
    // All-time count grouped by status
    
}