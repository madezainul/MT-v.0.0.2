package ahqpck.maintenance.report.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ahqpck.maintenance.report.dto.CapacityDTO;
import ahqpck.maintenance.report.entity.Capacity;
import ahqpck.maintenance.report.repository.CapacityRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CapacityService {
    private final CapacityRepository capacityRepo;

    public List<Capacity> getAll() {
        return capacityRepo.findAll();
    }

    public Capacity getByCode(String code) {
        return capacityRepo.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Capacity not found with code: " + code));
    }

    public Capacity createCapacity(CapacityDTO dto) {
        String code = dto.getCode().trim().toUpperCase();
        String name = dto.getName().trim();
        if (capacityRepo.findByCode(code).isPresent())
            throw new IllegalArgumentException("Duplicate code.");
        Capacity cap = new Capacity();
        cap.setCode(code);
        cap.setName(name.trim());
        return capacityRepo.save(cap);
    }
}
