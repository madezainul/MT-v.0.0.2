package ahqpck.maintenance.report.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import ahqpck.maintenance.report.util.Base62;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Part {

    @Id
    @Column(length = 22, updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "supplier_name", nullable = false)
    private String supplierName;

    @Column(name = "section_code", nullable = false)
    private String sectionCode;

    private String description;

    private String image;

    @Builder.Default
    private Integer stockQuantity = 0;

    @OneToMany(mappedBy = "part", fetch = FetchType.LAZY)
    private Set<EquipmentPartBOM> equipmentBOMs = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = Base62.encode(UUID.randomUUID());
        }
    }

    public void useParts(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to use must be positive");
        }
        if (this.stockQuantity < quantity) {
            throw new IllegalStateException("Not enough stock for part: " + name +
                    " (Available: " + this.stockQuantity + ", Requested: " + quantity + ")");
        }
        this.stockQuantity -= quantity;
    }

    public void addStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive");
        }
        this.stockQuantity += quantity;
    }

    public void setStockQuantity(Integer quantity) {
        this.stockQuantity = (quantity == null || quantity < 0) ? 0 : quantity;
    }

    // Helper method
    public int getEquipmentCount() {
        return equipmentBOMs != null ? equipmentBOMs.size() : 0;
    }
}