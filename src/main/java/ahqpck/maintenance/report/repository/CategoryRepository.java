package ahqpck.maintenance.report.repository;

import ahqpck.maintenance.report.entity.Category;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, String> {
    Optional<Category> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndNameNot(String code, String name);
}