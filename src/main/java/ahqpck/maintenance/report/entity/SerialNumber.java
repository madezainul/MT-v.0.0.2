package ahqpck.maintenance.report.entity;

import java.util.UUID;

import ahqpck.maintenance.report.util.Base62;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "serial_number")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SerialNumber {

    @Id
    @Column(length = 22, updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, length = 2, unique = true)
    @Size(max = 2)
    private String code; // e.g., A1, ZZ

    @Column(nullable = false)
    private String name;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = Base62.encode(UUID.randomUUID());
        }
    }
}