package ahqpck.maintenance.report.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import ahqpck.maintenance.report.util.Base62;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "areas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Area {

    @Id
    @Column(length = 22, updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_person", referencedColumnName = "id", nullable = false)
    private User responsiblePerson;

    @OneToMany(mappedBy = "area", fetch = FetchType.LAZY)
    private final Set<Complaint> complaints = new HashSet<>();

    private String description;

    public enum Status {
        ACTIVE,
        INACTIVE,
        UNDER_MAINTENANCE
    }

    @PrePersist
    public void prePersist() {
        this.id = (this.id == null) ? Base62.encode(UUID.randomUUID()) : this.id;
        this.status = this.status != null ? this.status : Status.INACTIVE;
    }
}