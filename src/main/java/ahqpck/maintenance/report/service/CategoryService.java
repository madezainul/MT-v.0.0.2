package ahqpck.maintenance.report.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ahqpck.maintenance.report.dto.CategoryDTO;
import ahqpck.maintenance.report.entity.Category;
import ahqpck.maintenance.report.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepo;

    public List<Category> getAll() {
        return categoryRepo.findAll();
    }

    public Category getByCode(String code) {
        return categoryRepo.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + code));
    }

    public Category createCategory(CategoryDTO dto) {
        String code = dto.getCode().trim().toUpperCase();
        String name = dto.getName().trim();
        if (categoryRepo.findByCode(code).isPresent())
            throw new IllegalArgumentException("Duplicate code.");
        Category cat = new Category();
        cat.setCode(code);
        cat.setName(name.trim());
        return categoryRepo.save(cat);
    }
}
