package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.AreaDTO;
import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.dto.PartDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.dto.WorkReportDTO;
import ahqpck.maintenance.report.dto.WorkReportPartDTO;
import ahqpck.maintenance.report.entity.Area;
import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.entity.WorkReport;
import ahqpck.maintenance.report.entity.WorkReportPart;
import ahqpck.maintenance.report.entity.WorkReportPartId;
import ahqpck.maintenance.report.repository.WorkReportRepository;
import ahqpck.maintenance.report.repository.AreaRepository;
import ahqpck.maintenance.report.repository.EquipmentRepository;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.specification.WorkReportSpecification;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.util.ImportUtil;
import ahqpck.maintenance.report.util.ZeroPaddedCodeGenerator;
import ahqpck.maintenance.report.mapper.WorkReportMapper;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkReportService {

    private static final Logger log = LoggerFactory.getLogger(WorkReportService.class);

    private final WorkReportRepository workReportRepository;
    private final AreaRepository areaRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final PartRepository partRepository;

    private final ImportUtil importUtil;
    private final ZeroPaddedCodeGenerator codeGenerator;
    private final WorkReportMapper workReportMapper;

    @Transactional
    public Page<WorkReportDTO> getAllWorkReports(String keyword, LocalDateTime reportDateFrom,
            LocalDateTime reportDateTo, WorkReport.Status status, WorkReport.Category category, WorkReport.Scope scope, String equipmentCode,
            int page, int size,
            String sortBy, boolean asc) {
        Sort sort = asc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<WorkReport> spec = WorkReportSpecification.search(keyword)
                .and(WorkReportSpecification.withReportDateRange(reportDateFrom, reportDateTo))
                .and(WorkReportSpecification.withStatus(status))
                .and(WorkReportSpecification.withCategory(category))
                .and(WorkReportSpecification.withScope(scope))
                .and(WorkReportSpecification.withEquipment(equipmentCode));
        Page<WorkReport> workReportPage = workReportRepository.findAll(spec, pageable);

        return workReportPage.map(workReport -> workReportMapper.toDTO(workReport));
    }

    public WorkReportDTO getWorkReportById(String id) {
        WorkReport workReport = workReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Work report not found with ID: " + id));
        return workReportMapper.toDTO(workReport);
    }

    @Transactional
    public void createWorkReport(WorkReportDTO dto) {
        try {
            WorkReport workReport = new WorkReport();

            validateAreaOrEquipment(
                    dto.getArea() != null ? dto.getArea().getCode() : null,
                    dto.getEquipment() != null ? dto.getEquipment().getCode() : null);

            if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
                String generatedCode = codeGenerator.generate(WorkReport.class, "code", "WR");
                workReport.setCode(generatedCode);
            }

            validateNoDuplicateBreakdown(dto);
            workReportMapper.mapToEntity(workReport, dto, areaRepository, equipmentRepository, userRepository, partRepository);

            Set<User> technicians = resolveTechnicians(dto.getTechnicianEmpIds());
            workReport.setTechnicians(technicians);
            workReportRepository.save(workReport);

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public void updateWorkReport(WorkReportDTO dto) {
        try {
            WorkReport workReport = workReportRepository.findById(dto.getId())
                    .orElseThrow(() -> new NotFoundException("Work report not found with ID: " + dto.getId()));

            validateAreaOrEquipment(
                    dto.getArea() != null ? dto.getArea().getCode() : null,
                    dto.getEquipment() != null ? dto.getEquipment().getCode() : null);

            workReportMapper.mapToEntity(workReport, dto, areaRepository, equipmentRepository, userRepository, partRepository);

            Set<User> technicians = resolveTechnicians(dto.getTechnicianEmpIds());
            workReport.setTechnicians(technicians);
            workReportRepository.save(workReport);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public void deleteWorkReport(String id) {
        WorkReport workReport = workReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Work report not found with ID: " + id));

        workReportRepository.delete(workReport);
    }

    public ImportUtil.ImportResult importWorkReportsFromExcel(List<Map<String, Object>> data) {
        List<String> errorMessages = new ArrayList<>();
        int importedCount = 0;
        System.out.println("data imported " + data);

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data to import.");
        }

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            try {
                WorkReportDTO dto = new WorkReportDTO();

                // === REQUIRED FIELDS (match @NotNull in DTO) ===

                dto.setProblem(importUtil.toString(row.get("problem")));
                if (dto.getProblem() == null || dto.getProblem().isEmpty()) {
                    throw new IllegalArgumentException("Problem is required");
                }

                dto.setReportDate(importUtil.toLocalDate(row.get("reportDate")));
                if (dto.getReportDate() == null) {
                    throw new IllegalArgumentException("Report date is mandatory");
                }

                dto.setStartTime(importUtil.toLocalDateTime(row.get("startTime")));
                if (dto.getStartTime() == null) {
                    throw new IllegalArgumentException("Start time is mandatory");
                }

                String shiftStr = importUtil.toString(row.get("shift"));
                if (shiftStr == null || shiftStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Shift is required");
                }
                try {
                    dto.setShift(WorkReport.Shift.valueOf(shiftStr.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid Shift: " + shiftStr + ". Use: DAY, NIGHT");
                }

                String categoryStr = importUtil.toString(row.get("category"));
                if (categoryStr == null || categoryStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Category is required");
                }
                try {
                    dto.setCategory(WorkReport.Category.valueOf(categoryStr.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid Category: " + categoryStr
                                    + ". Use: CORRECTIVE_MAINTENANCE, PREVENTIVE_MAINTENANCE, BREAKDOWN");
                }

                String scopeStr = importUtil.toString(row.get("scope"));
                if (scopeStr == null || scopeStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Scope is required");
                }
                try {
                    dto.setScope(WorkReport.Scope.valueOf(scopeStr.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid Scope value: '" + scopeStr + ". Use: MECHANICAL, ELECTRICAL, IT");
                }

                String technicianEmpIds = importUtil.toString(row.get("technicians"));
                if (technicianEmpIds == null || technicianEmpIds.trim().isEmpty()) {
                    throw new IllegalArgumentException("At least one technician is required");
                }

                Set<UserDTO> technicianDTOs = new HashSet<>();
                Set<String> empIds = new HashSet<>();

                for (String empId : technicianEmpIds.split(",")) {
                    String trimmed = empId.trim();
                    if (trimmed.isEmpty())
                        continue;

                    User user = userRepository.findByEmployeeId(trimmed)
                            .orElseThrow(() -> new IllegalArgumentException("Technician not found: " + trimmed));

                    UserDTO dtoTechnician = new UserDTO();
                    dtoTechnician.setId(user.getId());
                    dtoTechnician.setName(user.getName());
                    dtoTechnician.setEmployeeId(user.getEmployeeId());
                    dtoTechnician.setEmail(user.getEmail());

                    technicianDTOs.add(dtoTechnician);
                    empIds.add(trimmed);
                }

                dto.setTechnicians(technicianDTOs);
                dto.setTechnicianEmpIds(empIds);

                // === OPTIONAL FIELDS ===

                String code = importUtil.toString(row.get("code"));
                if (code != null && !code.trim().isEmpty()) {
                    dto.setCode(code.trim());
                    if (workReportRepository.existsByCodeIgnoreCase(dto.getCode())) {
                        throw new IllegalArgumentException("Duplicate work report code: " + dto.getCode());
                    }
                }
                dto.setSolution(importUtil.toString(row.get("solution")));
                dto.setWorkType(importUtil.toString(row.get("workType")));
                dto.setRemark(importUtil.toString(row.get("remark")));
                dto.setStopTime(importUtil.toLocalDateTime(row.get("stopTime")));
                dto.setTotalTimeMinutes(importUtil.toDurationInMinutes(row.get("totalTimeMinutes")));

                String statusStr = importUtil.toString(row.get("status"));
                if (statusStr != null && !statusStr.trim().isEmpty()) {
                    try {
                        dto.setStatus(WorkReport.Status.valueOf(statusStr.trim().toUpperCase()));
                    } catch (Exception ignored) {
                        throw new IllegalArgumentException(
                                "Invalid Status: " + statusStr + ". Use: OPEN, PENDING, CLOSED");
                    }
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

                String supervisorEmpId = importUtil.toString(row.get("supervisor"));
                if (supervisorEmpId != null && !supervisorEmpId.trim().isEmpty()) {
                    UserDTO supervisorDTO = new UserDTO();
                    supervisorDTO.setEmployeeId(supervisorEmpId.trim());
                    dto.setSupervisor(supervisorDTO);
                }

                createWorkReport(dto);
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

    protected void handleStatusTransition(WorkReport workReport, WorkReport.Status oldStatus,
            WorkReport.Status newStatus) {
        if (newStatus == WorkReport.Status.CLOSED && oldStatus != WorkReport.Status.CLOSED) {
            // Transitioning TO CLOSED

            log.info("workReport {} CLOSED: Deducting {} parts from inventory",
                    workReport.getId(), workReport.getPartsUsed().size());
            deductPartsFromInventory(workReport);

        } else if (oldStatus == WorkReport.Status.CLOSED && newStatus != WorkReport.Status.CLOSED) {
            // Reopening a CLOSED workReport
            log.warn("Reopening CLOSED workReport: {}", workReport.getId());
            restockParts(workReport);

            workReport.setStatus(newStatus); // Allow transition to any non-CLOSED
        }
        // For other transitions (e.g. OPEN → PENDING), no side effects
    }

    /**
     * Deduct all parts used in this workReport from stock
     */
    private void deductPartsFromInventory(WorkReport workReport) {
        for (WorkReportPart cp : workReport.getPartsUsed()) {
            Part part = cp.getPart();
            log.info("Deducting {} x '{}' (Part ID: {}) from stock",
                    cp.getQuantity(), part.getName(), part.getId());
            part.useParts(cp.getQuantity());
            partRepository.save(part);
        }
    }

    /**
     * Restock all parts used in this workReport
     */
    private void restockParts(WorkReport workReport) {
        for (WorkReportPart cp : workReport.getPartsUsed()) {
            Part part = cp.getPart();
            log.info("Restocking {} x '{}' (Part ID: {}) to inventory",
                    cp.getQuantity(), part.getName(), part.getId());
            part.addStock(cp.getQuantity());
            partRepository.save(part);
        }
    }

    private void validateAreaOrEquipment(String areaId, String equipmentId) {
        boolean areaExists = areaId != null && areaRepository.existsByCodeIgnoreCase(areaId);
        boolean equipmentExists = equipmentId != null && equipmentRepository.existsByCodeIgnoreCase(equipmentId);

        if (!areaExists && !equipmentExists) {
            throw new IllegalArgumentException("Either Area or Equipment must be specified and must exist");
        }
    }

    private void validateNoDuplicateBreakdown(WorkReportDTO dto) {
        // Skip if not BREAKDOWN
        if (dto.getCategory() != WorkReport.Category.BREAKDOWN) {
            return;
        }

        // Equipment is required for breakdown validation
        if (dto.getEquipment() == null || dto.getEquipment().getCode() == null) {
            return;
        }

        // Time fields are required
        if (dto.getStartTime() == null || dto.getStopTime() == null) {
            return;
        }

        String equipmentCode = dto.getEquipment().getCode().trim();
        LocalDateTime start = dto.getStartTime();
        LocalDateTime stop = dto.getStopTime();

        // Ensure start <= stop
        if (start.isAfter(stop)) {
            throw new IllegalArgumentException("Start time cannot be after stop time.");
        }

        // Check for overlapping breakdown reports on same equipment
        boolean exists = workReportRepository.hasOverlappingBreakdownReport(
                equipmentCode, start, stop);

        if (exists) {
            throw new IllegalArgumentException(
                    "A breakdown report already exists for equipment '" + equipmentCode +
                            "' during this time period. Overlapping breakdowns are not allowed.");
        }
    }

    // ================== MAPPING METHODS ==================

    // HELPER: DTO Conversion - DEPRECATED: Use WorkReportMapper instead
    // All mapping logic has been moved to WorkReportMapper for better separation of concerns

    private Set<User> resolveTechnicians(Set<String> technicianEmpIds) {
        Set<User> technicians = new HashSet<>();
        if (technicianEmpIds != null && !technicianEmpIds.isEmpty()) {
            for (String empId : technicianEmpIds) {
                if (empId == null || empId.trim().isEmpty())
                    continue;
                String trimmedEmpId = empId.trim();
                User technician = userRepository.findByEmployeeId(trimmedEmpId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Technician not found with employee ID: " + trimmedEmpId));
                technicians.add(technician);
            }
        } else {
            throw new IllegalArgumentException("At least one technician must be assigned.");
        }
        return technicians;
    }
}