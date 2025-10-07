package ahqpck.maintenance.report.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ahqpck.maintenance.report.entity.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, String> {
    Optional<Supplier> findByCode(String code);
    Optional<Supplier> findByName(String name);

    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndNameNot(String code, String name);
}