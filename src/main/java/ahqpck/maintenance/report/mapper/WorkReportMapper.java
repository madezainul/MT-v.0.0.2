package ahqpck.maintenance.report.mapper;

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
import ahqpck.maintenance.report.repository.AreaRepository;
import ahqpck.maintenance.report.repository.EquipmentRepository;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WorkReportMapper {

    @Lazy
    private final DTOMapper dtoMapper;

    public WorkReportDTO toDTO(WorkReport workReport) {
        WorkReportDTO dto = new WorkReportDTO();
        dto.setId(workReport.getId());
        dto.setCode(workReport.getCode());
        dto.setShift(workReport.getShift());
        dto.setReportDate(workReport.getReportDate());
        dto.setProblem(workReport.getProblem());
        dto.setSolution(workReport.getSolution());
        dto.setCategory(workReport.getCategory());
        dto.setStartTime(workReport.getStartTime());
        dto.setStopTime(workReport.getStopTime());
        dto.setWorkType(workReport.getWorkType());
        dto.setRemark(workReport.getRemark());
        dto.setStatus(workReport.getStatus());
        dto.setScope(workReport.getScope());
        dto.setTotalTimeMinutes(workReport.getTotalTimeMinutes());

        dto.setUpdatedAt(workReport.getUpdatedAt());

        // Format resolution time
        if (workReport.getTotalTimeMinutes() != null) {
            int total = workReport.getTotalTimeMinutes();
            int days = total / (24 * 60), hours = (total % (24 * 60)) / 60, mins = total % 60;
            StringBuilder sb = new StringBuilder();
            if (days > 0)
                sb.append(days).append("d ");
            if (hours > 0)
                sb.append(hours).append("h ");
            if (mins > 0 || sb.length() == 0)
                sb.append(mins).append("m");
            dto.setTotalTimeDisplay(sb.toString().trim());
        } else {
            dto.setTotalTimeDisplay("-");
        }

        if (workReport.getArea() != null) {
            AreaDTO areaDTO = new AreaDTO();
            areaDTO.setId(workReport.getArea().getId());
            areaDTO.setCode(workReport.getArea().getCode());
            areaDTO.setName(workReport.getArea().getName());
            dto.setArea(areaDTO);
        }

        if (workReport.getEquipment() != null) {
            EquipmentDTO equipmentDTO = new EquipmentDTO();
            equipmentDTO.setId(workReport.getEquipment().getId());
            equipmentDTO.setName(workReport.getEquipment().getName());
            equipmentDTO.setCode(workReport.getEquipment().getCode());
            dto.setEquipment(equipmentDTO);
        }

        dto.setSupervisor(mapToUserDTO(workReport.getSupervisor()));
        dto.setTechnicians(workReport.getTechnicians().stream()
                .map(this::mapToUserDTO)
                .collect(Collectors.toSet()));
        dto.setTechnicianEmpIds(workReport.getTechnicians().stream()
                .map(User::getEmployeeId)
                .collect(Collectors.toSet()));

        if (workReport.getPartsUsed() != null) {
            dto.setPartsUsed(workReport.getPartsUsed().stream()
                    .map(cp -> {
                        WorkReportPartDTO partDto = new WorkReportPartDTO();
                        partDto.setPart(dtoMapper.mapToPartDTO(cp.getPart()));
                        partDto.setQuantity(cp.getQuantity());
                        return partDto;
                    })
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public void mapToEntity(WorkReport workReport, WorkReportDTO dto,
                            AreaRepository areaRepository,
                            EquipmentRepository equipmentRepository,
                            UserRepository userRepository,
                            PartRepository partRepository) {
        workReport.setShift(dto.getShift());
        workReport.setReportDate(dto.getReportDate());
        workReport.setProblem(dto.getProblem());
        workReport.setSolution(dto.getSolution());
        workReport.setCategory(dto.getCategory());
        workReport.setStartTime(dto.getStartTime());
        workReport.setStopTime(dto.getStopTime());
        workReport.setWorkType(dto.getWorkType());
        workReport.setRemark(dto.getRemark());
        workReport.setStatus(dto.getStatus());
        workReport.setScope(dto.getScope());
        workReport.setTotalTimeMinutes(dto.getTotalTimeMinutes());

        // === Optional: Area ===
        if (dto.getArea() != null && dto.getArea().getCode() != null && !dto.getArea().getCode().trim().isEmpty()) {
            String areaCode = dto.getArea().getCode().trim();
            Area area = areaRepository.findByCode(areaCode)
                    .orElseThrow(() -> new IllegalArgumentException("Area not found with code: " + areaCode));
            workReport.setArea(area);
        } else {
            workReport.setArea(null);
        }

        // === Optional: Equipment ===
        if (dto.getEquipment() != null && dto.getEquipment().getCode() != null
                && !dto.getEquipment().getCode().trim().isEmpty()) {
            String equipmentCode = dto.getEquipment().getCode().trim();
            Equipment equipment = equipmentRepository.findByCode(equipmentCode)
                    .orElseThrow(() -> new IllegalArgumentException("Equipment not found with code: " + equipmentCode));
            workReport.setEquipment(equipment);
        } else {
            workReport.setEquipment(null);
        }

        // Supervisor (optional)
        if (dto.getSupervisor() != null && dto.getSupervisor().getEmployeeId() != null
                && !dto.getSupervisor().getEmployeeId().trim().isEmpty()) {
            String supervisorEmpId = dto.getSupervisor().getEmployeeId();
            User supervisor = userRepository.findByEmployeeId4Roles(supervisorEmpId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Supervisor not found with employeeId: " + supervisorEmpId));
            workReport.setSupervisor(supervisor);
        } else {
            workReport.setSupervisor(null);
        }

        // PARTS HANDLING: Use merge/update pattern
        if (dto.getPartsUsed() != null) {
            // Create a copy of current parts to allow safe iteration
            List<WorkReportPart> existingParts = new ArrayList<>(workReport.getPartsUsed());

            // Clear the list — thanks to orphanRemoval, old entries will be deleted
            workReport.getPartsUsed().clear();

            for (WorkReportPartDTO partDto : dto.getPartsUsed()) {
                Part part = partRepository.findById(partDto.getPart().getId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Part not found with ID: " + partDto.getPart().getId()));

                // Try to reuse an existing workReportPart if possible
                WorkReportPart existing = existingParts.stream()
                        .filter(cp -> cp.getPart().getId().equals(part.getId()))
                        .findFirst()
                        .orElse(null);

                WorkReportPart cp;
                if (existing != null) {
                    // Reuse and update quantity
                    existing.setQuantity(partDto.getQuantity());
                    cp = existing;
                } else {
                    // Create new
                    cp = new WorkReportPart();
                    cp.setWorkReport(workReport);
                    cp.setPart(part);
                    cp.setQuantity(partDto.getQuantity());
                    cp.setId(new WorkReportPartId(workReport.getId(), part.getId()));
                }

                workReport.getPartsUsed().add(cp);
            }
        } else {
            // If DTO has no parts, just clear
            workReport.getPartsUsed().clear();
        }
    }

    private UserDTO mapToUserDTO(User user) {
        if (user == null)
            return null;
        var dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setEmail(user.getEmail());
        return dto;
    }
}