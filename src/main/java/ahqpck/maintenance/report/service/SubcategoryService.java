package ahqpck.maintenance.report.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ahqpck.maintenance.report.dto.SubcategoryDTO;
import ahqpck.maintenance.report.entity.Subcategory;
import ahqpck.maintenance.report.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubcategoryService {
    private final SubcategoryRepository subcategoryRepo;

    public List<Subcategory> getAll() {
        return subcategoryRepo.findAll();
    }

    public Subcategory getByCode(String code) {
        return subcategoryRepo.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Subcategory with code " + code + " not found"));
    }

    public Subcategory createSubcategory(SubcategoryDTO dto) {
        String code = dto.getCode().trim().toUpperCase();
        String name = dto.getName().trim();
        if (subcategoryRepo.findByCode(code).isPresent())
            throw new IllegalArgumentException("Duplicate code.");
        Subcategory subcat = new Subcategory();
        subcat.setCode(code);
        subcat.setName(name.trim());
        return subcategoryRepo.save(subcat);
    }
}
