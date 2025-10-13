package ahqpck.maintenance.report.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import ahqpck.maintenance.report.util.Base62;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "work_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkReport {

    @Id
    @Column(length = 22, updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String problem;
    
    @Column(name = "solution", columnDefinition = "TEXT", nullable = true)
    private String solution;

    @Column(name = "work_type", nullable = true)
    private String workType;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String remark;
    
    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "stop_time", nullable = true)
    private LocalDateTime stopTime;
    
    @Column(name = "total_time_minutes", nullable = true)
    private Integer totalTimeMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Shift shift;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Scope scope;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_code", referencedColumnName = "id", nullable = true)
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_code", referencedColumnName = "id", nullable = true)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor", referencedColumnName = "id", nullable = true)
    private User supervisor;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "work_report_technicians", joinColumns = @JoinColumn(name = "work_report_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private final Set<User> technicians = new HashSet<>();

    @OneToMany(mappedBy = "workReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkReportPart> partsUsed = new ArrayList<>();
    
    public enum Shift {
        DAY, NIGHT
    }

    public enum Category {
        CORRECTIVE_MAINTENANCE,
        PREVENTIVE_MAINTENANCE,
        BREAKDOWN,
        OTHER
    }

    public enum Status {
        OPEN, PENDING, CLOSED
    }
    
    public enum Scope {
        MECHANICAL,
        ELECTRICAL,
        IT,
        OTHER
    }

    public void addPart(Part part, Integer quantity) {
        WorkReportPart wrp = new WorkReportPart();
        wrp.setWorkReport(this);
        wrp.setPart(part);
        wrp.setQuantity(quantity);
        wrp.setId(new WorkReportPartId(this.id, part.getId()));
        this.partsUsed.add(wrp);
    }
    
    public void removePart(Part part) {
        this.partsUsed.removeIf(wrp -> wrp.getPart().equals(part));
    }
    
    @PrePersist
    public void prePersist() {
        this.id = (this.id == null) ? Base62.encode(UUID.randomUUID()) : this.id;
        
        LocalDateTime now = LocalDateTime.now();

        this.reportDate = (this.reportDate == null) ? now.toLocalDate() : this.reportDate;
        this.updatedAt = now;
        this.status = this.status != null ? this.status : Status.OPEN;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setTechnicians(Set<User> technicians) {
        this.technicians.clear();
        if (technicians != null) {
            this.technicians.addAll(technicians);
        }
    }
}