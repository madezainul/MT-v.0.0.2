package ahqpck.maintenance.report.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ahqpck.maintenance.report.dto.SectionDTO;
import ahqpck.maintenance.report.entity.Section;
import ahqpck.maintenance.report.repository.SectionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SectionService {
    private final SectionRepository sectionRepo;

    public List<Section> getAll() {
        return sectionRepo.findAll();
    }

    public Section getByCode(String code) {
        return sectionRepo.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Section with code " + code + " not found"));
    }

    public Section createSection(SectionDTO dto) {
        String code = dto.getCode().trim().toUpperCase();
        if (sectionRepo.findByCode(code).isPresent())
            throw new IllegalArgumentException("Duplicate code.");
        Section sec = new Section();
        sec.setCode(code);
        return sectionRepo.save(sec);
    }
}
