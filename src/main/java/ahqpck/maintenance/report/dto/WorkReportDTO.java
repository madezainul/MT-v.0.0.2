package ahqpck.maintenance.report.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;

import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.WorkReport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkReportDTO {

    private String id;
    private String code;

    @NotNull(message = "Problem is mandatory")
    private String problem;
    private String solution;
    private String workType;
    private String remark;
    
    @NotNull(message = "Report date is mandatory")
    private LocalDate reportDate;
    private LocalDateTime updatedAt;
    
    @NotNull(message = "Start time is mandatory")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startTime;
    

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime stopTime;
    
    private Integer totalTimeMinutes;
    private String totalTimeDisplay;

    @NotNull(message = "Shift is mandatory")
    private WorkReport.Shift shift;

    @NotNull(message = "Category is mandatory")
    private WorkReport.Category category;

    private WorkReport.Status status;

    @NotNull(message = "Scope is mandatory")
    private WorkReport.Scope scope;

    private AreaDTO area;
    private EquipmentDTO equipment;
    private UserDTO supervisor;

    private Set<String> technicianEmpIds = new HashSet<>();
    private Set<UserDTO> technicians = new HashSet<>();    

    @Valid
    private List<WorkReportPartDTO> partsUsed = new ArrayList<>();
}