package ahqpck.maintenance.report.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import ahqpck.maintenance.report.util.Base62;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
    // PR quantity (total quantity requested in PRs)
    @Column(name = "pr_quantity")
    @Builder.Default
    private Integer prQuantity = 0;

    // Safety quantity minimum
    @Column(name = "safety_min_qty")
    @Builder.Default
    private Integer safetyMinQty = 0;


    @Id
    @Column(length = 22, updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String model;

    @Column(nullable = true)
    private String manufacturer;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "supplier_name", nullable = false)
    private String supplierName;

    @Column(name = "section_code", nullable = false)
    private String sectionCode;

    private String specification;
    private String image;

    @Builder.Default
    private Integer stockQuantity = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", nullable = true)
    @CreatedBy
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id", nullable = true)
    @LastModifiedBy
    private User updatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "part", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<EquipmentPartBOM> equipmentBOMs = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = Base62.encode(UUID.randomUUID());
        }

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
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