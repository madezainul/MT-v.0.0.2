package ahqpck.maintenance.report.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import ahqpck.maintenance.report.dto.MachineTypeDTO;
import ahqpck.maintenance.report.entity.MachineType;
import ahqpck.maintenance.report.repository.MachineTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MachineTypeService {
    
    private final MachineTypeRepository machineTyperepo;

    public MachineType createMachineType(MachineTypeDTO dto) {
        String code = dto.getCode().trim().toUpperCase();
        String name = dto.getName().trim();
        if (machineTyperepo.findByCode(code).isPresent())
            throw new IllegalArgumentException("Duplicate code.");
        MachineType mt = new MachineType();
        mt.setCode(code);
        mt.setName(name.trim());
        return machineTyperepo.save(mt);
    }

    public MachineType getMachineTypeByCode(String code) {
        return machineTyperepo.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Machine Type not found: " + code));
    }

    public List<MachineType> getAll() {
        return machineTyperepo.findAll();
    }

    public Page<MachineType> getAllWithPagination(String keyword, int page, int size, String sortBy, boolean asc) {
        Sort sort = asc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            String searchTerm = "%" + keyword.trim().toLowerCase() + "%";
            Specification<MachineType> spec = (root, query, cb) -> 
                cb.or(
                    cb.like(cb.lower(root.get("code")), searchTerm),
                    cb.like(cb.lower(root.get("name")), searchTerm)
                );
            return machineTyperepo.findAll(spec, pageable);
        }
        
        return machineTyperepo.findAll(pageable);
    }

    public Optional<MachineType> searchByName(String term) {
        if (term == null || term.trim().isEmpty())
            return Optional.empty();
        term = "%" + term.trim().toLowerCase() + "%";
        return machineTyperepo.findByName(term);
    }
}