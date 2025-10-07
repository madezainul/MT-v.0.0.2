package ahqpck.maintenance.report.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import ahqpck.maintenance.report.entity.SerialNumber;

public interface SerialNumberRepository extends JpaRepository<SerialNumber, String> {
    Optional<SerialNumber> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndNameNot(String code, String name);
}