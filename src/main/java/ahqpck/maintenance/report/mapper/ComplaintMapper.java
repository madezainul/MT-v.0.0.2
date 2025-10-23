package ahqpck.maintenance.report.mapper;

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
import ahqpck.maintenance.report.repository.AreaRepository;
import ahqpck.maintenance.report.repository.EquipmentRepository;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ComplaintMapper {

    public ComplaintDTO toDTO(Complaint complaint) {
        ComplaintDTO dto = new ComplaintDTO();

        dto.setId(complaint.getId());
        dto.setCode(complaint.getCode());
        dto.setReportDate(complaint.getReportDate());
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

        dto.setUpdatedAt(complaint.getUpdatedAt());

        // Format total time display
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

        // Area
        if (complaint.getArea() != null) {
            AreaDTO areaDTO = new AreaDTO();
            areaDTO.setId(complaint.getArea().getId());
            areaDTO.setCode(complaint.getArea().getCode());
            areaDTO.setName(complaint.getArea().getName());
            dto.setArea(areaDTO);
        }

        // Equipment
        if (complaint.getEquipment() != null) {
            EquipmentDTO equipmentDTO = new EquipmentDTO();
            equipmentDTO.setId(complaint.getEquipment().getId());
            equipmentDTO.setName(complaint.getEquipment().getName());
            equipmentDTO.setCode(complaint.getEquipment().getCode());
            dto.setEquipment(equipmentDTO);
        }

        // Reporter
        if (complaint.getReporter() != null) {
            dto.setReporter(mapToUserDTO(complaint.getReporter()));
        }

        // Assignee
        if (complaint.getAssignee() != null) {
            dto.setAssignee(mapToUserDTO(complaint.getAssignee()));
        }

        // Parts Used
        if (complaint.getPartsUsed() != null && !complaint.getPartsUsed().isEmpty()) {
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

    public void mapToEntity(Complaint complaint, ComplaintDTO dto,
                            AreaRepository areaRepository,
                            EquipmentRepository equipmentRepository,
                            UserRepository userRepository,
                            PartRepository partRepository) {
        // Basic fields
        complaint.setSubject(dto.getSubject());
        complaint.setDescription(dto.getDescription());
        complaint.setPriority(dto.getPriority());
        complaint.setCategory(dto.getCategory());
        complaint.setReportDate(dto.getReportDate());
        complaint.setActionTaken(dto.getActionTaken());

        // Status — handle transition
        Complaint.Status oldStatus = complaint.getStatus();
        Complaint.Status newStatus = dto.getStatus();
        complaint.setStatus(newStatus); // Set first

        // Handle side effects AFTER setting new status
        if (oldStatus != newStatus) {
            // Note: handleStatusTransition is in service, not mapper
            // Mapper only sets data; service handles business logic
        }

        // Area
        if (dto.getArea() != null && dto.getArea().getCode() != null && !dto.getArea().getCode().trim().isEmpty()) {
            String areaCode = dto.getArea().getCode().trim();
            Area area = areaRepository.findByCode(areaCode)
                    .orElseThrow(() -> new IllegalArgumentException("Area not found with code: " + areaCode));
            complaint.setArea(area);
        } else {
            complaint.setArea(null);
        }

        // Equipment
        if (dto.getEquipment() != null && dto.getEquipment().getCode() != null
                && !dto.getEquipment().getCode().trim().isEmpty()) {
            String equipmentCode = dto.getEquipment().getCode().trim();
            Equipment equipment = equipmentRepository.findByCode(equipmentCode)
                    .orElseThrow(() -> new IllegalArgumentException("Equipment not found with code: " + equipmentCode));
            complaint.setEquipment(equipment);
        } else {
            complaint.setEquipment(null);
        }

        // Reporter (mandatory)
        if (dto.getReporter() == null || dto.getReporter().getEmployeeId() == null) {
            throw new IllegalArgumentException("Reporter is mandatory");
        }
        String reporterEmpId = dto.getReporter().getEmployeeId().trim();
        User reporter = userRepository.findByEmployeeId4Roles(reporterEmpId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Reporter not found with employeeId: " + reporterEmpId));
        complaint.setReporter(reporter);

        // Assignee (optional)
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

        // Parts — Merge/Update pattern
        if (dto.getPartsUsed() != null) {
            List<ComplaintPart> existingParts = new ArrayList<>(complaint.getPartsUsed());
            complaint.getPartsUsed().clear(); // Triggers orphan removal

            for (ComplaintPartDTO partDto : dto.getPartsUsed()) {
                Part part = partRepository.findById(partDto.getPart().getId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Part not found with ID: " + partDto.getPart().getId()));

                ComplaintPart existing = existingParts.stream()
                        .filter(cp -> cp.getPart().getId().equals(part.getId()))
                        .findFirst()
                        .orElse(null);

                ComplaintPart cp;
                if (existing != null) {
                    existing.setQuantity(partDto.getQuantity());
                    cp = existing;
                } else {
                    cp = new ComplaintPart();
                    cp.setComplaint(complaint);
                    cp.setPart(part);
                    cp.setQuantity(partDto.getQuantity());
                    cp.setId(new ComplaintPartId(complaint.getId(), part.getId()));
                }

                complaint.getPartsUsed().add(cp);
            }
        } else {
            complaint.getPartsUsed().clear();
        }
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
        dto.setSpecification(part.getSpecification());
        return dto;
    }
}