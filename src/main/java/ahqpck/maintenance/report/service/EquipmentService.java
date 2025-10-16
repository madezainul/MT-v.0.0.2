package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.repository.EquipmentRepository;
import ahqpck.maintenance.report.repository.EquipmentPartBOMRepository;
import ahqpck.maintenance.report.dto.EquipmentStatusDTO;
import ahqpck.maintenance.report.specification.EquipmentSpecification;
import ahqpck.maintenance.report.util.FileUploadUtil;
import ahqpck.maintenance.report.util.ImportUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    @Value("${app.upload-equipment-image.dir:src/main/resources/static/upload/equipment/image}")
    private String uploadDir;

    private final EquipmentRepository equipmentRepository;
    private final EquipmentPartBOMRepository equipmentPartBOMRepository;

    private final FileUploadUtil fileUploadUtil;
    private final ImportUtil importUtil;
    private final Validator validator;

    public Page<EquipmentDTO> getAllEquipments(String keyword, int page, int size, String sortBy, boolean asc) {
        Sort sort = asc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Equipment> spec = EquipmentSpecification.search(keyword);
        Page<Equipment> equipmentPage = equipmentRepository.findAll(spec, pageable);

        // Fetch stats only for current page IDs
        List<String> ids = equipmentPage.getContent().stream()
                .map(Equipment::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<String, EquipmentStatusDTO> statsMap = ids.isEmpty()
                ? Map.of()
                : equipmentRepository.findStatsByEquipmentIds(ids).stream()
                        .collect(Collectors.toMap(
                                EquipmentStatusDTO::getId,
                                s -> s,
                                (existing, replacement) -> existing // in case of duplicate (shouldn't happen)
                        ));

        // Map to DTO + enrich with stats
        return equipmentPage.map(equipment -> {
            EquipmentDTO dto = toDTO(equipment);
            EquipmentStatusDTO stats = statsMap.get(equipment.getId());
            if (stats != null) {
                dto.setTotalOpenWorkReports(stats.getOpenWr());
                dto.setTotalPendingWorkReports(stats.getPendingWr());
                dto.setTotalOpenComplaints(stats.getOpenCp());
                dto.setTotalPendingComplaints(stats.getPendingCp());
            } else {
                // Set zeros if no stats (e.g., no WR/complaints)
                dto.setTotalOpenWorkReports(0L);
                dto.setTotalPendingWorkReports(0L);
                dto.setTotalOpenComplaints(0L);
                dto.setTotalPendingComplaints(0L);
            }
            return dto;
        });
    }

    public EquipmentDTO getEquipmentById(String id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipment not found with ID: " + id));
        return toDTO(equipment);
    }

    public void createEquipment(EquipmentDTO dto, MultipartFile imageFile) {
        validateEquipmentDTO(dto);
        
        if (equipmentRepository.existsByCodeIgnoreCase(dto.getCode())) {
            throw new IllegalArgumentException("Equipment with this code already exists.");
        }

        Equipment equipment = new Equipment();
        mapToEntity(equipment, dto);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String fileName = fileUploadUtil.saveFile(uploadDir, imageFile, "image");
                equipment.setImage(fileName);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save image: " + e.getMessage());
            }
        }

        equipmentRepository.save(equipment);
    }

    public void updateEquipment(EquipmentDTO dto, MultipartFile imageFile, boolean deleteImage) {
        validateEquipmentDTO(dto);
        
        String id = dto.getId();
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipment not found with ID: " + id));

        mapToEntity(equipment, dto);

        String oldImage = equipment.getImage();
        if (deleteImage && oldImage != null) {
            fileUploadUtil.deleteFile(uploadDir, oldImage);
            equipment.setImage(null);
        } else if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String newImage = fileUploadUtil.saveFile(uploadDir, imageFile, "image");
                if (oldImage != null) {
                    fileUploadUtil.deleteFile(uploadDir, oldImage);
                }
                equipment.setImage(newImage);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save image: " + e.getMessage());
            }
        }

        equipmentRepository.save(equipment);
    }

    public void deleteEquipment(String id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipment not found with ID: " + id));

        if (equipment.getImage() != null) {
            fileUploadUtil.deleteFile(uploadDir, equipment.getImage());
        }
        equipmentRepository.delete(equipment);
    }

    // Add this method to EquipmentService
    public ImportUtil.ImportResult importEquipmentsFromExcel(List<Map<String, Object>> data) {
        List<String> errorMessages = new ArrayList<>();
        int importedCount = 0;

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data to import.");
        }

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            try {
                EquipmentDTO dto = new EquipmentDTO();

                // === REQUIRED FIELDS (match @NotNull in DTO) ===

                dto.setCode(importUtil.toString(row.get("code")));
                if (dto.getCode() == null || dto.getCode().isEmpty()) {
                    throw new IllegalArgumentException("Code is required");
                }

                dto.setName(importUtil.toString(row.get("name")));
                if (dto.getName() == null || dto.getName().isEmpty()) {
                    throw new IllegalArgumentException("Name is required");
                }

                dto.setModel(importUtil.toString(row.get("model")));
                dto.setUnit(importUtil.toString(row.get("unit")));
                dto.setQty(importUtil.toInteger(row.get("qty")));
                dto.setManufacturer(importUtil.toString(row.get("manufacturer")));
                dto.setSerialNo(importUtil.toString(row.get("serialNo")));
                dto.setManufacturedDate(importUtil.toLocalDate(row.get("manufacturedDate")));
                dto.setCommissionedDate(importUtil.toLocalDate(row.get("commissionedDate")));
                dto.setCapacity(importUtil.toString(row.get("capacity")));
                dto.setRemarks(importUtil.toString(row.get("remarks")));

                if (equipmentRepository.existsByCodeIgnoreCase(dto.getCode())) {
                    throw new IllegalArgumentException("Duplicate equipment code: " + dto.getCode());
                }

                createEquipment(dto, null);
                importedCount++;

            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "Unknown error";
                errorMessages.add("Row " + (i + 1) + ": " + message);
            }
        }

        return new ImportUtil.ImportResult(importedCount, errorMessages);
    }

    private void validateEquipmentDTO(EquipmentDTO dto) {
        Set<ConstraintViolation<EquipmentDTO>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            List<String> errorMessages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .toList();
            throw new IllegalArgumentException("Validation errors: " + String.join(", ", errorMessages));
        }
    }

    private void mapToEntity(Equipment equipment, EquipmentDTO dto) {
        equipment.setCode(dto.getCode().trim());
        equipment.setName(dto.getName().trim());
        equipment.setModel(dto.getModel());
        equipment.setUnit(dto.getUnit());
        equipment.setQty(dto.getQty() != null ? dto.getQty() : 0);
        equipment.setManufacturer(dto.getManufacturer());
        equipment.setSerialNo(dto.getSerialNo());
        equipment.setManufacturedDate(dto.getManufacturedDate());
        equipment.setCommissionedDate(dto.getCommissionedDate());
        equipment.setCapacity(dto.getCapacity());
        equipment.setRemarks(dto.getRemarks());
    }

    private EquipmentDTO toDTO(Equipment equipment) {
        EquipmentDTO dto = new EquipmentDTO();
        dto.setId(equipment.getId());
        dto.setCode(equipment.getCode());
        dto.setName(equipment.getName());
        dto.setModel(equipment.getModel());
        dto.setUnit(equipment.getUnit());
        dto.setQty(equipment.getQty());
        dto.setManufacturer(equipment.getManufacturer());
        dto.setSerialNo(equipment.getSerialNo());
        dto.setManufacturedDate(equipment.getManufacturedDate());
        dto.setCommissionedDate(equipment.getCommissionedDate());
        dto.setCapacity(equipment.getCapacity());
        dto.setRemarks(equipment.getRemarks());
        dto.setImage(equipment.getImage());
        
        // Set BOM parts count
        long partsCount = equipmentPartBOMRepository.countPartsByEquipmentId(equipment.getId());
        dto.setPartsCount((int) partsCount);
        
        return dto;
    }
}