package ahqpck.maintenance.report.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ahqpck.maintenance.report.entity.Subcategory;

public interface SubcategoryRepository extends JpaRepository<Subcategory, String> {
    Optional<Subcategory> findByCode(String code);
    
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndNameNot(String code, String name);
}