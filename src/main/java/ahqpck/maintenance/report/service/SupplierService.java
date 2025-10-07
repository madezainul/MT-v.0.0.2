package ahqpck.maintenance.report.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ahqpck.maintenance.report.dto.SupplierDTO;
import ahqpck.maintenance.report.entity.Supplier;
import ahqpck.maintenance.report.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierService {
    private final SupplierRepository supplierRepo;

    public List<Supplier> getAll() {
        return supplierRepo.findAll();
    }

    public Supplier getByCode(String code) {
        return supplierRepo.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Supplier not found with code: " + code));
    }

    public Optional<Supplier> searchByName(String term) {
        if (term == null || term.trim().isEmpty())
            return Optional.empty();
        term = "%" + term.trim().toLowerCase() + "%";
        return supplierRepo.findByName(term);
    }

    public Supplier createSupplier(SupplierDTO dto) {
        String code = dto.getCode().trim().toUpperCase();
        String name = dto.getName().trim();
        if (supplierRepo.findByCode(code).isPresent())
            throw new IllegalArgumentException("Duplicate code.");
        Supplier sup = new Supplier();
        sup.setCode(code);
        sup.setName(name.trim());
        return supplierRepo.save(sup);
    }
}
