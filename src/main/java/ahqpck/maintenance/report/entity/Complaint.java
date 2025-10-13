package ahqpck.maintenance.report.entity;

import ahqpck.maintenance.report.util.Base62;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "complaints")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @Column(length = 22, updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "report_date", nullable = false)
    private LocalDateTime reportDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = true)
    private String subject;

    // the nullable should be false
    @Column(columnDefinition = "TEXT", nullable = true)
    private String description;

    @Column(name = "action_taken", columnDefinition = "TEXT", nullable = true)
    private String actionTaken;

    @Column(name = "close_time", nullable = true)
    private LocalDateTime closeTime;

    @Column(name = "total_time_minutes", nullable = true)
    private Integer totalTimeMinutes;

    @Column(name = "image_before", nullable = true)
    private String imageBefore;

    @Column(name = "image_after", nullable = true)
    private String imageAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter", referencedColumnName = "id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee", referencedColumnName = "id", nullable = true)
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_code", referencedColumnName = "id", nullable = true)
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_code", referencedColumnName = "id", nullable = true)
    private Equipment equipment;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ComplaintPart> partsUsed = new ArrayList<>();

    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    public enum Category {
        MECHANICAL, ELECTRICAL, IT, OTHER
    }

    public enum Status {
        OPEN, PENDING, CLOSED
    }

    public void addPart(Part part, Integer quantity) {
        ComplaintPart cp = new ComplaintPart();
        cp.setComplaint(this);
        cp.setPart(part);
        cp.setQuantity(quantity);
        cp.setId(new ComplaintPartId(this.id, part.getId()));
        this.partsUsed.add(cp);
    }

    public void removePart(Part part) {
        this.partsUsed.removeIf(cp -> cp.getPart().equals(part));
    }

    @PrePersist
    public void prePersist() {
        this.id = (this.id == null) ? Base62.encode(UUID.randomUUID()) : this.id;
        
        LocalDateTime now = LocalDateTime.now();

        this.reportDate = (this.reportDate == null) ? now : this.reportDate;
        this.updatedAt = now;
        this.status = this.status != null ? this.status : Status.OPEN;
        this.priority = this.priority != null ? this.priority : Priority.MEDIUM;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (Status.CLOSED.equals(this.status) && this.closeTime != null && this.reportDate != null) {
            this.totalTimeMinutes = (int) Duration.between(this.reportDate, this.closeTime).toMinutes();
        }
    }
}
