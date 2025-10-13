package ahqpck.maintenance.report.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ahqpck.maintenance.report.dto.EquipmentStatusDTO;
import ahqpck.maintenance.report.entity.Equipment;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, String>, JpaSpecificationExecutor<Equipment> {

  Optional<Equipment> findByCode(String code);

  boolean existsByCodeIgnoreCase(String code);

  boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);

  @Query(value = """
        SELECT 
            e.id,
            COUNT(DISTINCT CASE WHEN wr.status = 'OPEN' THEN wr.id END) AS openWr,
            COUNT(DISTINCT CASE WHEN wr.status = 'PENDING' THEN wr.id END) AS pendingWr,
            COUNT(DISTINCT CASE WHEN c.status = 'OPEN' THEN c.id END) AS openCp,
            COUNT(DISTINCT CASE WHEN c.status = 'PENDING' THEN c.id END) AS pendingCp
        FROM equipments e
        LEFT JOIN work_reports wr ON e.id = wr.equipment_code AND wr.status IN ('OPEN', 'PENDING')
        LEFT JOIN complaints c ON e.id = c.equipment_code AND c.status IN ('OPEN', 'PENDING')
        WHERE e.id IN :equipmentIds
        GROUP BY e.id
        """, nativeQuery = true)
    List<EquipmentStatusDTO> findStatsByEquipmentIds(@Param("equipmentIds") List<String> equipmentIds);

  // Add this projection interface inside or near EquipmentRepository
  // interface EquipmentStats {
  //   String getId();

  //   Long getOpenWr(); // OPEN work reports

  //   Long getPendingWr(); // PENDING work reports

  //   Long getOpenCp(); // OPEN complaints

  //   Long getPendingCp(); // PENDING complaints
  // }
}