package ahqpck.maintenance.report.dto;

import ahqpck.maintenance.report.entity.Complaint;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintDTO {

    private String id;
    private String code;

    @NotNull(message = "Report date is mandatory")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime reportDate;
    private LocalDateTime updatedAt;

    private String subject;
    private String description;
    private String actionTaken;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime closeTime;

    private Integer totalTimeMinutes;

    private String totalTimeDisplay;
    private String imageBefore;
    private String imageAfter;

    @NotNull(message = "Priority is mandatory")
    private Complaint.Priority priority;

    @NotNull(message = "Category is mandatory")
    private Complaint.Category category;

    private Complaint.Status status;

    @NotNull(message = "Reporter is mandatory")
    private UserDTO reporter;

    private UserDTO assignee;
    private AreaDTO area;
    private EquipmentDTO equipment;

    @Valid
    private List<ComplaintPartDTO> partsUsed = new ArrayList<>();
}