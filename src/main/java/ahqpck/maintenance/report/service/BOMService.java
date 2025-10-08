package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.dto.PartDTO;
import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.entity.EquipmentPartBOM;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.repository.EquipmentPartBOMRepository;
import ahqpck.maintenance.report.repository.EquipmentRepository;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.util.Base62;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BOMService {

    private final EquipmentPartBOMRepository bomRepository;
    private final EquipmentRepository equipmentRepository;
    private final PartRepository partRepository;
    private final EquipmentService equipmentService;
    private final PartService partService;

    public List<EquipmentDTO> getAllEquipmentsForBOM() {
        // Get all equipments without pagination - using same pattern as ComplaintController
        var allEquipments = equipmentService.getAllEquipments(null, 0, Integer.MAX_VALUE, "name", true);
        return allEquipments.getContent().stream().collect(Collectors.toList());
    }

    public List<PartDTO> getAllPartsForBOM() {
        // Get all parts without pagination - using same pattern as ComplaintController  
        var allParts = partService.getAllParts(null, 0, Integer.MAX_VALUE, "name", true);
        return allParts.getContent().stream().collect(Collectors.toList());
    }

    public EquipmentDTO getEquipmentWithParts(String equipmentId) {
        var allEquipments = equipmentService.getAllEquipments(null, 0, Integer.MAX_VALUE, "name", true);
        return allEquipments.getContent().stream()
                .filter(eq -> eq.getId().equals(equipmentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Equipment not found"));
    }

    public PartDTO getPartWithEquipments(String partId) {
        var allParts = partService.getAllParts(null, 0, Integer.MAX_VALUE, "name", true);
        return allParts.getContent().stream()
                .filter(part -> part.getId().equals(partId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Part not found"));
    }

    public List<EquipmentPartBOM> getEquipmentBOM(String equipmentId) {
        return bomRepository.findByEquipmentIdAndIsActiveTrue(equipmentId);
    }

    public List<EquipmentPartBOM> getPartUsage(String partId) {
        return bomRepository.findByPartIdAndIsActiveTrue(partId);
    }

    public List<PartDTO> getAvailablePartsForEquipment(String equipmentId) {
        // Get all parts that are not yet in this equipment's BOM
        var existingPartIds = bomRepository.findByEquipmentIdAndIsActiveTrue(equipmentId)
                .stream()
                .map(bom -> bom.getPart().getId())
                .collect(Collectors.toList());
        
        var allParts = partService.getAllParts(null, 0, Integer.MAX_VALUE, "name", true);
        return allParts.getContent().stream()
                .filter(part -> !existingPartIds.contains(part.getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addPartToEquipment(String equipmentId, String partId, Integer quantity, 
                                 String notes, String criticalityLevel) {
        // Check if relationship already exists
        Optional<EquipmentPartBOM> existing = bomRepository.findByEquipmentIdAndPartId(equipmentId, partId);
        if (existing.isPresent()) {
            if (existing.get().getIsActive()) {
                throw new RuntimeException("Part is already added to this equipment");
            } else {
                // Reactivate existing relationship
                EquipmentPartBOM bom = existing.get();
                bom.setIsActive(true);
                bom.setQuantityPerUnit(quantity);
                bom.setNotes(notes);
                bom.setCriticalityLevel(criticalityLevel);
                bom.setUpdatedAt(LocalDateTime.now());
                bomRepository.save(bom);
                return;
            }
        }

        // Get entities
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found"));
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new RuntimeException("Part not found"));

        // Create new BOM entry
        EquipmentPartBOM bom = new EquipmentPartBOM();
        bom.setId(Base62.encode(UUID.randomUUID()));
        bom.setEquipment(equipment);
        bom.setPart(part);
        bom.setQuantityPerUnit(quantity);
        bom.setNotes(notes);
        bom.setCriticalityLevel(criticalityLevel);
        bom.setIsActive(true);
        bom.setCreatedAt(LocalDateTime.now());
        bom.setUpdatedAt(LocalDateTime.now());

        bomRepository.save(bom);
    }

    @Transactional
    public void removePartFromEquipment(String equipmentId, String partId) {
        EquipmentPartBOM bom = bomRepository.findByEquipmentIdAndPartIdAndIsActiveTrue(equipmentId, partId)
                .orElseThrow(() -> new RuntimeException("BOM entry not found"));
        
        bom.setIsActive(false);
        bom.setUpdatedAt(LocalDateTime.now());
        bomRepository.save(bom);
    }

    @Transactional
    public void updateBOMEntry(String bomId, Integer quantity, String notes, String criticalityLevel) {
        EquipmentPartBOM bom = bomRepository.findById(bomId)
                .orElseThrow(() -> new RuntimeException("BOM entry not found"));
        
        bom.setQuantityPerUnit(quantity);
        bom.setNotes(notes);
        bom.setCriticalityLevel(criticalityLevel);
        bom.setUpdatedAt(LocalDateTime.now());
        
        bomRepository.save(bom);
    }

    // Statistics methods for BOM dashboard
    public String getTotalEquipmentCount() {
        return String.valueOf(equipmentRepository.count());
    }

    public String getTotalPartCount() {
        return String.valueOf(partRepository.count());
    }

    public String getTotalBOMEntriesCount() {
        return String.valueOf(bomRepository.countByIsActiveTrue());
    }

    public String getCriticalPartsCount() {
        return String.valueOf(bomRepository.countByCriticalityLevelAndIsActiveTrue("CRITICAL"));
    }
}