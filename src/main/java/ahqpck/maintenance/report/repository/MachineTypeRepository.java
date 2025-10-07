package ahqpck.maintenance.report.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ahqpck.maintenance.report.entity.MachineType;

public interface MachineTypeRepository extends JpaRepository<MachineType, String>, JpaSpecificationExecutor<MachineType> {
    Optional<MachineType> findByCode(String code);
    Optional<MachineType> findByName(String name);

    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndNameNot(String code, String name);
}

