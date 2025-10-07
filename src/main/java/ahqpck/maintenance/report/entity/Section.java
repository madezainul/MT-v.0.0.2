package ahqpck.maintenance.report.entity;

import java.util.UUID;

import ahqpck.maintenance.report.util.Base62;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "section")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Section {

    @Id
    @Column(length = 22, updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, length = 3, unique = true)
    @Size(max = 3)
    private String code;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = Base62.encode(UUID.randomUUID());
        }
    }
}