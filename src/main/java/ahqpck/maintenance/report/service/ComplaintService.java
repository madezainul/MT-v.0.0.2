package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.AreaDTO;
import ahqpck.maintenance.report.dto.ComplaintDTO;
import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.ComplaintPart;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.repository.AreaRepository;
import ahqpck.maintenance.report.repository.ComplaintRepository;
import ahqpck.maintenance.report.repository.EquipmentRepository;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.specification.ComplaintSpecification;
import ahqpck.maintenance.report.util.FileUploadUtil;
import ahqpck.maintenance.report.util.ImportUtil;
import ahqpck.maintenance.report.util.ZeroPaddedCodeGenerator;
import ahqpck.maintenance.report.mapper.ComplaintMapper;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    @Value("${app.upload-complaint-image-before.dir:src/main/resources/static/upload/complaint/image/before}")
    private String uploadBeforeDir;

    @Value("${app.upload-complaint-image-after.dir:src/main/resources/static/upload/complaint/image/after}")
    private String uploadAfterDir;

    private static final Logger log = LoggerFactory.getLogger(ComplaintService.class);

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final AreaRepository areaRepository;
    private final PartRepository partRepository;

    private final FileUploadUtil fileUploadUtil;
    private final ImportUtil importUtil;
    private final ZeroPaddedCodeGenerator codeGenerator;
    private final ComplaintMapper complaintMapper;

    public Page<ComplaintDTO> getAllComplaints(String keyword, LocalDateTime reportDateFrom, LocalDateTime reportDateTo,
            LocalDateTime closeTime, String assigneeEmpId, Complaint.Status status, Complaint.Category category,
            String equipmentCode, int page, int size, String sortBy, boolean asc) {
        Sort sort = asc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Complaint> spec = ComplaintSpecification.search(keyword)
                .and(ComplaintSpecification.withReportDateRange(reportDateFrom, reportDateTo))
                .and(ComplaintSpecification.withCloseTime(closeTime))
                .and(ComplaintSpecification.withAssignee(assigneeEmpId))
                .and(ComplaintSpecification.withStatus(status))
                .and(ComplaintSpecification.withCategory(category))
                .and(ComplaintSpecification.withEquipment(equipmentCode));
        Page<Complaint> complaintPage = complaintRepository.findAll(spec, pageable);

        return complaintPage.map(complaintMapper::toDTO);
    }

    public ComplaintDTO getComplaintById(String id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found with ID: " + id));
        return complaintMapper.toDTO(complaint);
    }

    public void createComplaint(ComplaintDTO dto, MultipartFile imageBefore) {

        Complaint complaint = new Complaint();

        validateAreaOrEquipment(
                dto.getArea() != null ? dto.getArea().getCode() : null,
                dto.getEquipment() != null ? dto.getEquipment().getCode() : null);

        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            String generatedCode = codeGenerator.generate(Complaint.class, "code", "CP");
            complaint.setCode(generatedCode);
        }

        complaintMapper.mapToEntity(complaint, dto, areaRepository, equipmentRepository, userRepository, partRepository);

        if (imageBefore != null && !imageBefore.isEmpty()) {
            try {
                String fileName = fileUploadUtil.saveFile(uploadBeforeDir, imageBefore, "image");
                complaint.setImageBefore(fileName);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save image: " + e.getMessage());
            }
        }

        complaintRepository.save(complaint);
    }

    public void updateComplaint(ComplaintDTO dto, MultipartFile imageBefore, MultipartFile imageAfter,
            Boolean deleteImageBefore,
            Boolean deleteImageAfter) {

        Complaint complaint = complaintRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("Complaint not found with ID: " + dto.getId()));

        validateAreaOrEquipment(
                dto.getArea() != null ? dto.getArea().getCode() : null,
                dto.getEquipment() != null ? dto.getEquipment().getCode() : null);

        if (dto.getStatus() == Complaint.Status.CLOSED) {
            if (dto.getActionTaken() == null || dto.getActionTaken().trim().isEmpty()) {
                throw new IllegalArgumentException("Action taken is required before closing the complaint.");
            }
        }

        complaintMapper.mapToEntity(complaint, dto, areaRepository, equipmentRepository, userRepository, partRepository);

        Complaint.Status oldStatus = complaint.getStatus();
        Complaint.Status newStatus = dto.getStatus();

        if (newStatus != null && newStatus != oldStatus) {
            handleStatusTransition(complaint, oldStatus, newStatus);
        }

        String oldBeforeImage = complaint.getImageBefore();
        if (deleteImageBefore && oldBeforeImage != null) {
            fileUploadUtil.deleteFile(uploadBeforeDir, oldBeforeImage);
            complaint.setImageBefore(null);
        } else if (imageBefore != null && !imageBefore.isEmpty()) {
            try {
                String newImage = fileUploadUtil.saveFile(uploadBeforeDir, imageBefore, "image");
                if (oldBeforeImage != null) {
                    fileUploadUtil.deleteFile(uploadBeforeDir, oldBeforeImage);
                }
                complaint.setImageBefore(newImage);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save image: " + e.getMessage());
            }
        }

        String oldAfterImage = complaint.getImageAfter();
        if (deleteImageAfter && oldAfterImage != null) {
            fileUploadUtil.deleteFile(uploadAfterDir, oldAfterImage);
            complaint.setImageAfter(null);
        } else if (imageAfter != null && !imageAfter.isEmpty()) {
            try {
                String newImage = fileUploadUtil.saveFile(uploadAfterDir, imageAfter, "image");
                if (oldAfterImage != null) {
                    fileUploadUtil.deleteFile(uploadAfterDir, oldAfterImage);
                }
                complaint.setImageAfter(newImage);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save image: " + e.getMessage());
            }
        }

        complaintRepository.save(complaint);
    }

    public void deleteComplaint(String id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found with ID: " + id));

        complaintRepository.delete(complaint);
    }

    public ImportUtil.ImportResult importComplaintsFromExcel(List<Map<String, Object>> data) {
        List<String> errorMessages = new ArrayList<>();
        int importedCount = 0;

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data to import.");
        }

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            try {
                ComplaintDTO dto = new ComplaintDTO();

                // === REQUIRED FIELDS (match @NotNull in DTO) ===

                dto.setReportDate(importUtil.toLocalDateTime(row.get("reportDate")));
                if (dto.getReportDate() == null) {
                    throw new IllegalArgumentException("Report date is mandatory");
                }

                String priorityStr = importUtil.toString(row.get("priority"));
                if (priorityStr == null || priorityStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Priority is mandatory");
                }
                try {
                    dto.setPriority(Complaint.Priority.valueOf(priorityStr.trim().toUpperCase()));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid Priority: " + priorityStr + ". Use: LOW, MEDIUM, HIGH");
                }

                String categoryStr = importUtil.toString(row.get("category"));
                if (categoryStr == null || categoryStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Category is mandatory");
                }
                try {
                    dto.setCategory(Complaint.Category.valueOf(categoryStr.trim().toUpperCase()));
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Invalid Category: " + categoryStr + ". Use: MECHANICAL, ELECTRICAL, IT");
                }

                String reporterEmpId = importUtil.toString(row.get("reporter"));
                if (reporterEmpId == null || reporterEmpId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Reporter is mandatory");
                }
                UserDTO reporterDTO = new UserDTO();
                reporterDTO.setEmployeeId(reporterEmpId.trim());
                dto.setReporter(reporterDTO);

                // === OPTIONAL FIELDS ===

                String code = importUtil.toString(row.get("code"));
                if (code != null && !code.trim().isEmpty()) {
                    dto.setCode(code.trim());
                    if (complaintRepository.existsByCodeIgnoreCase(dto.getCode())) {
                        throw new IllegalArgumentException("Duplicate complaint code: " + dto.getCode());
                    }
                }
                dto.setSubject(importUtil.toString(row.get("subject")));
                dto.setDescription(importUtil.toString(row.get("description")));
                dto.setActionTaken(importUtil.toString(row.get("actionTaken")));
                dto.setCloseTime(importUtil.toLocalDateTime(row.get("closeTime")));
                dto.setTotalTimeMinutes(importUtil.toDurationInMinutes(row.get("totalTimeMinutes")));

                String assigneeEmpId = importUtil.toString(row.get("assignee"));
                if (assigneeEmpId != null && !assigneeEmpId.trim().isEmpty()) {
                    UserDTO assigneeDTO = new UserDTO();
                    assigneeDTO.setEmployeeId(assigneeEmpId.trim());
                    dto.setAssignee(assigneeDTO);
                }

                String areaCode = importUtil.toString(row.get("area"));
                if (areaCode != null && !areaCode.trim().isEmpty()) {
                    AreaDTO areaDTO = new AreaDTO();
                    areaDTO.setCode(areaCode.trim());
                    dto.setArea(areaDTO);
                }

                String equipmentCode = importUtil.toString(row.get("equipment"));
                if (equipmentCode != null && !equipmentCode.trim().isEmpty()) {
                    EquipmentDTO equipmentDTO = new EquipmentDTO();
                    equipmentDTO.setCode(equipmentCode.trim());
                    dto.setEquipment(equipmentDTO);
                }

                String statusStr = importUtil.toString(row.get("status"));
                if (statusStr != null && !statusStr.trim().isEmpty()) {
                    try {
                        dto.setStatus(Complaint.Status.valueOf(statusStr.trim().toUpperCase()));
                    } catch (Exception ignored) {
                        throw new IllegalArgumentException(
                                "Invalid Status: " + statusStr + ". Use: OPEN, PENDING, CLOSED");
                    }
                }

                createComplaint(dto, null);
                importedCount++;

            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "Unknown error";
                errorMessages.add("Row " + (i + 1) + ": " + message);
            }
        }

        return new ImportUtil.ImportResult(importedCount, errorMessages);
    }

    private void validateAreaOrEquipment(String areaId, String equipmentId) {
        boolean areaExists = areaId != null && areaRepository.existsByCodeIgnoreCase(areaId);
        boolean equipmentExists = equipmentId != null && equipmentRepository.existsByCodeIgnoreCase(equipmentId);

        if (!areaExists && !equipmentExists) {
            throw new IllegalArgumentException("Either Area or Equipment must be specified and must exist");
        }
    }

    /**
     * Handle side effects of status transitions:
     * - Closing: set closeTime, deduct inventory
     * - Reopening: clear closeTime, restock parts
     */

    protected void handleStatusTransition(Complaint complaint, Complaint.Status oldStatus, Complaint.Status newStatus) {
        if (newStatus == null)
            return;

        // Transitioning TO CLOSED
        if (newStatus == Complaint.Status.CLOSED && oldStatus != Complaint.Status.CLOSED) {
            LocalDateTime now = LocalDateTime.now();
            complaint.setCloseTime(now);

            // Calculate total time from reportDate to now
            if (complaint.getReportDate() != null) {
                long minutes = java.time.Duration.between(complaint.getReportDate(), now).toMinutes();
                complaint.setTotalTimeMinutes((int) minutes);
            }

            // Deduct parts from inventory
            log.info("Complaint {} CLOSED: Deducting {} parts from inventory",
                    complaint.getId(), complaint.getPartsUsed().size());
            deductPartsFromInventory(complaint);
        }
        // Reopening a CLOSED complaint
        else if (oldStatus == Complaint.Status.CLOSED && newStatus != Complaint.Status.CLOSED) {
            log.warn("Reopening CLOSED complaint: {}", complaint.getId());

            // Restock parts
            restockParts(complaint);

            // Clear close time & total time
            complaint.setCloseTime(null);
            complaint.setTotalTimeMinutes(null);
        }
        // For other transitions (OPEN ↔ PENDING), do nothing
    }

    /**
     * Deduct all parts used in this complaint from stock
     */
    private void deductPartsFromInventory(Complaint complaint) {
        for (ComplaintPart cp : complaint.getPartsUsed()) {
            Part part = cp.getPart();
            log.info("Deducting {} x '{}' (Part ID: {}) from stock",
                    cp.getQuantity(), part.getName(), part.getId());
            part.useParts(cp.getQuantity());
            partRepository.save(part);
        }
    }

    /**
     * Restock all parts used in this complaint
     */
    private void restockParts(Complaint complaint) {
        for (ComplaintPart cp : complaint.getPartsUsed()) {
            Part part = cp.getPart();
            log.info("Restocking {} x '{}' (Part ID: {}) to inventory",
                    cp.getQuantity(), part.getName(), part.getId());
            part.addStock(cp.getQuantity());
            partRepository.save(part);
        }
    }

    // ================== MAPPING METHODS ==================

    // HELPER: DTO Conversion - DEPRECATED: Use ComplaintMapper instead
    // All mapping logic has been moved to ComplaintMapper for better separation of concerns
}