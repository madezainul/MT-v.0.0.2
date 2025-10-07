package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.AreaDTO;
import ahqpck.maintenance.report.dto.ComplaintDTO;
import ahqpck.maintenance.report.dto.ComplaintPartDTO;
import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.dto.PartDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.Area;
import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.ComplaintPart;
import ahqpck.maintenance.report.entity.ComplaintPartId;
import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.entity.User;
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
import java.util.stream.Collectors;

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

    public Page<ComplaintDTO> getAllComplaints(String keyword, LocalDateTime reportDateFrom, LocalDateTime reportDateTo,
            String assigneeEmpId, Complaint.Status status, String equipmentCode, int page, int size, String sortBy,
            boolean asc) {
        Sort sort = asc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Complaint> spec = ComplaintSpecification.search(keyword)
                .and(ComplaintSpecification.withReportDateRange(reportDateFrom, reportDateTo))
                .and(ComplaintSpecification.withAssignee(assigneeEmpId))
                .and(ComplaintSpecification.withStatus(status))
                .and(ComplaintSpecification.withEquipment(equipmentCode));
        Page<Complaint> complaintPage = complaintRepository.findAll(spec, pageable);

        return complaintPage.map(this::toDTO);
    }

    public ComplaintDTO getComplaintById(String id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found with ID: " + id));
        return toDTO(complaint);
    }

    public void createComplaint(ComplaintDTO dto, MultipartFile imageBefore) {
        Complaint complaint = new Complaint();

        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            String generatedCode = codeGenerator.generate(Complaint.class, "code", "CP");
            complaint.setCode(generatedCode);
        }

        System.out.println("check dto " + dto);

        mapToEntity(complaint, dto);

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

        mapToEntity(complaint, dto);

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

    /**
     * Handle side effects of status transitions:
     * - Closing: set closeTime, deduct inventory
     * - Reopening: clear closeTime, restock parts
     */

    protected void handleStatusTransition(Complaint complaint, Complaint.Status oldStatus, Complaint.Status newStatus) {
        if (newStatus == Complaint.Status.CLOSED && oldStatus != Complaint.Status.CLOSED) {
            // Transitioning TO CLOSED
            LocalDateTime now = LocalDateTime.now();
            complaint.setCloseTime(now);
            // totalResolutionTimeMinutes will be calculated in @PreUpdate or @PrePersist

            log.info("Complaint {} CLOSED: Deducting {} parts from inventory",
                    complaint.getId(), complaint.getPartsUsed().size());
            deductPartsFromInventory(complaint);

        } else if (oldStatus == Complaint.Status.CLOSED && newStatus != Complaint.Status.CLOSED) {
            // Reopening a CLOSED complaint
            log.warn("Reopening CLOSED complaint: {}", complaint.getId());
            restockParts(complaint);

            complaint.setCloseTime(null);
            complaint.setTotalTimeMinutes(null);
            complaint.setStatus(newStatus); // Allow transition to any non-CLOSED
        }
        // For other transitions (e.g. OPEN → PENDING), no side effects
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

    private void mapToEntity(Complaint complaint, ComplaintDTO dto) {
        complaint.setSubject(dto.getSubject());
        complaint.setDescription(dto.getDescription());
        complaint.setPriority(dto.getPriority());
        complaint.setCategory(dto.getCategory());
        complaint.setStatus(dto.getStatus());
        complaint.setReportDate(dto.getReportDate());
        complaint.setCloseTime(dto.getCloseTime());
        complaint.setTotalTimeMinutes(dto.getTotalTimeMinutes());

        // === Optional: Area ===
        if (dto.getArea() != null && dto.getArea().getCode() != null && !dto.getArea().getCode().trim().isEmpty()) {
            String areaCode = dto.getArea().getCode().trim();
            Area area = areaRepository.findByCode(areaCode)
                    .orElseThrow(() -> new IllegalArgumentException("Area not found with code: " + areaCode));
            complaint.setArea(area);
        } else {
            complaint.setArea(null);
        }

        // === Optional: Equipment ===
        if (dto.getEquipment() != null && dto.getEquipment().getCode() != null
                && !dto.getEquipment().getCode().trim().isEmpty()) {
            String equipmentCode = dto.getEquipment().getCode().trim();
            Equipment equipment = equipmentRepository.findByCode(equipmentCode)
                    .orElseThrow(() -> new IllegalArgumentException("Equipment not found with code: " + equipmentCode));
            complaint.setEquipment(equipment);
        } else {
            complaint.setEquipment(null);
        }

        // === Mandatory: Reporter ===
        if (dto.getReporter() == null || dto.getReporter().getEmployeeId() == null) {
            throw new IllegalArgumentException("Reporter is mandatory");
        }
        String reporterEmpId = dto.getReporter().getEmployeeId().trim();
        User reporter = userRepository.findByEmployeeId4Roles(reporterEmpId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Reporter not found with employeeId: " + reporterEmpId));
        complaint.setReporter(reporter);

        // === Optional: Assignee ===
        if (dto.getAssignee() != null && dto.getAssignee().getEmployeeId() != null
                && !dto.getAssignee().getEmployeeId().trim().isEmpty()) {
            String assigneeEmpId = dto.getAssignee().getEmployeeId().trim();
            User assignee = userRepository.findByEmployeeId4Roles(assigneeEmpId)
                    .orElseThrow(
                            () -> new IllegalArgumentException("Assignee not found with employeeId: " + assigneeEmpId));
            complaint.setAssignee(assignee);
        } else {
            complaint.setAssignee(null);
        }

        // PARTS HANDLING: Use merge/update pattern
        if (dto.getPartsUsed() != null) {
            // Create a copy of current parts to allow safe iteration
            List<ComplaintPart> existingParts = new ArrayList<>(complaint.getPartsUsed());

            // Clear the list — thanks to orphanRemoval, old entries will be deleted
            complaint.getPartsUsed().clear();

            for (ComplaintPartDTO partDto : dto.getPartsUsed()) {
                Part part = partRepository.findById(partDto.getPart().getId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Part not found with ID: " + partDto.getPart().getId()));

                // Try to reuse an existing ComplaintPart if possible
                ComplaintPart existing = existingParts.stream()
                        .filter(cp -> cp.getPart().getId().equals(part.getId()))
                        .findFirst()
                        .orElse(null);

                ComplaintPart cp;
                if (existing != null) {
                    // Reuse and update quantity
                    existing.setQuantity(partDto.getQuantity());
                    cp = existing;
                } else {
                    // Create new
                    cp = new ComplaintPart();
                    cp.setComplaint(complaint);
                    cp.setPart(part);
                    cp.setQuantity(partDto.getQuantity());
                    cp.setId(new ComplaintPartId(complaint.getId(), part.getId()));
                }

                complaint.getPartsUsed().add(cp);
            }
        } else {
            // If DTO has no parts, just clear
            complaint.getPartsUsed().clear();
        }
    }

    // HELPER: DTO Conversion
    private ComplaintDTO toDTO(Complaint complaint) {
        ComplaintDTO dto = new ComplaintDTO();

        dto.setId(complaint.getId());
        dto.setCode(complaint.getCode());
        dto.setReportDate(complaint.getReportDate());
        dto.setUpdatedAt(complaint.getUpdatedAt());
        dto.setSubject(complaint.getSubject());
        dto.setDescription(complaint.getDescription());
        dto.setPriority(complaint.getPriority());
        dto.setCategory(complaint.getCategory());
        dto.setStatus(complaint.getStatus());
        dto.setActionTaken(complaint.getActionTaken());
        dto.setImageBefore(complaint.getImageBefore());
        dto.setImageAfter(complaint.getImageAfter());
        dto.setCloseTime(complaint.getCloseTime());
        dto.setTotalTimeMinutes(complaint.getTotalTimeMinutes());

        if (complaint.getTotalTimeMinutes() != null) {
            int total = complaint.getTotalTimeMinutes();
            int days = total / 1440;
            int hours = (total % 1440) / 60;
            int mins = total % 60;

            String display = (days > 0 ? days + "d " : "") +
                    (hours > 0 ? hours + "h " : "") +
                    (mins > 0 || (days == 0 && hours == 0) ? mins + "m" : "");
            dto.setTotalTimeDisplay(display.trim());
        } else {
            dto.setTotalTimeDisplay("-");
        }

        if (complaint.getArea() != null) {
            AreaDTO areaDTO = new AreaDTO();
            areaDTO.setId(complaint.getArea().getId());
            areaDTO.setCode(complaint.getArea().getCode());
            areaDTO.setName(complaint.getArea().getName());
            dto.setArea(areaDTO);
        }

        if (complaint.getEquipment() != null) {
            EquipmentDTO equipmentDTO = new EquipmentDTO();
            equipmentDTO.setId(complaint.getEquipment().getId());
            equipmentDTO.setName(complaint.getEquipment().getName());
            equipmentDTO.setCode(complaint.getEquipment().getCode());
            dto.setEquipment(equipmentDTO);
        }

        dto.setReporter(mapToUserDTO(complaint.getReporter()));
        dto.setAssignee(mapToUserDTO(complaint.getAssignee()));

        if (complaint.getPartsUsed() != null) {
            dto.setPartsUsed(complaint.getPartsUsed().stream()
                    .map(cp -> {
                        ComplaintPartDTO partDto = new ComplaintPartDTO();
                        partDto.setPart(mapToPartDTO(cp.getPart()));
                        partDto.setQuantity(cp.getQuantity());
                        return partDto;
                    })
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private UserDTO mapToUserDTO(User user) {
        if (user == null)
            return null;

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setEmail(user.getEmail());
        return dto;
    }

    private PartDTO mapToPartDTO(Part part) {
        if (part == null)
            return null;

        PartDTO dto = new PartDTO();
        dto.setId(part.getId());
        dto.setName(part.getName());
        dto.setCode(part.getCode());
        dto.setDescription(part.getDescription());
        return dto;
    }
}