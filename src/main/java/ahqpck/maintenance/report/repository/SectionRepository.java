package ahqpck.maintenance.report.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ahqpck.maintenance.report.entity.Section;

public interface SectionRepository extends JpaRepository<Section, String> {
    Optional<Section> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);
}