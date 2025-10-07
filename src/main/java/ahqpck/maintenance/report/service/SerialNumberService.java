package ahqpck.maintenance.report.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ahqpck.maintenance.report.dto.SerialNumberDTO;
import ahqpck.maintenance.report.entity.SerialNumber;
import ahqpck.maintenance.report.repository.SerialNumberRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SerialNumberService {
    private final SerialNumberRepository serialNumberRepo;

    public List<SerialNumber> getAll() {
        return serialNumberRepo.findAll();
    }

    public SerialNumber getByCode(String code) {
        return serialNumberRepo.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Serial number not found with code: " + code));
    }

    public SerialNumber createSerialNumber(SerialNumberDTO dto) {
        String code = dto.getCode().trim().toUpperCase();
        String name = dto.getName().trim();
        if (serialNumberRepo.findByCode(code).isPresent())
            throw new IllegalArgumentException("Duplicate code.");
        SerialNumber sn = new SerialNumber();
        sn.setCode(code);
        sn.setName(name.trim());
        return serialNumberRepo.save(sn);
    }
}
