package ahqpck.maintenance.report.repository;

import ahqpck.maintenance.report.entity.EquipmentPartBOM;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentPartBOMRepository extends JpaRepository<EquipmentPartBOM, String> {

    // Find all BOM entries for a specific equipment
    List<EquipmentPartBOM> findByEquipmentIdAndIsActiveTrue(String equipmentId);
    
    // Find all BOM entries for a specific part
    List<EquipmentPartBOM> findByPartIdAndIsActiveTrue(String partId);
    
    // Find specific equipment-part relationship (active or inactive)
    Optional<EquipmentPartBOM> findByEquipmentIdAndPartId(String equipmentId, String partId);
    
    // Find specific equipment-part relationship (active only)
    Optional<EquipmentPartBOM> findByEquipmentIdAndPartIdAndIsActiveTrue(String equipmentId, String partId);
    
    // Count parts for an equipment
    @Query("SELECT COUNT(b) FROM EquipmentPartBOM b WHERE b.equipment.id = :equipmentId AND b.isActive = true")
    long countPartsByEquipmentId(@Param("equipmentId") String equipmentId);
    
    // Count equipment using a specific part
    @Query("SELECT COUNT(b) FROM EquipmentPartBOM b WHERE b.part.id = :partId AND b.isActive = true")
    long countEquipmentByPartId(@Param("partId") String partId);
    
    // Check if equipment-part relationship exists
    boolean existsByEquipmentIdAndPartIdAndIsActiveTrue(String equipmentId, String partId);
    
    // Find all active BOM entries
    List<EquipmentPartBOM> findByIsActiveTrue();
    
    // Count active BOM entries
    long countByIsActiveTrue();
    
    // Count critical parts in active BOMs
    long countByCriticalityLevelAndIsActiveTrue(String criticalityLevel);
    
    // Delete relationship (set inactive)
    @Query("UPDATE EquipmentPartBOM b SET b.isActive = false WHERE b.equipment.id = :equipmentId AND b.part.id = :partId")
    void deactivateByEquipmentIdAndPartId(@Param("equipmentId") String equipmentId, @Param("partId") String partId);
}