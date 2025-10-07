package ahqpck.maintenance.report.entity;

// import javax.persistence.*;

import ahqpck.maintenance.report.util.Base62;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "equipment_part_bom")
@Getter
@Setter
public class EquipmentPartBOM {
    @Id
    @Column(length = 22, updatable = false, nullable = false)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;      // String ID
    
    @ManyToOne
    @JoinColumn(name = "part_id")
    private Part part;               // String ID
    
    private Integer quantityPerUnit = 1;
    private String notes;
    private String criticalityLevel = "STANDARD"; // CRITICAL, IMPORTANT, STANDARD
    private Boolean isActive = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = Base62.encode(UUID.randomUUID());
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }
}
