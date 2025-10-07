package ahqpck.maintenance.report.dto;

import org.springframework.stereotype.Component;

import ahqpck.maintenance.report.entity.Category;
import ahqpck.maintenance.report.entity.MachineType;
import ahqpck.maintenance.report.entity.Section;
import ahqpck.maintenance.report.entity.SerialNumber;
import ahqpck.maintenance.report.entity.Subcategory;
import ahqpck.maintenance.report.entity.Supplier;

@Component
public class DTOMapper {

    public MachineTypeDTO mapToMachineTypeDTO(MachineType mt) {
        MachineTypeDTO dto = new MachineTypeDTO();
        dto.setId(mt.getId());
        dto.setName(mt.getName());
        dto.setCode(mt.getCode());

        return dto;
    }

    public CategoryDTO mapToCategoryDTO(Category cat) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(cat.getId());
        dto.setName(cat.getName());
        dto.setCode(cat.getCode());
        // dto.setMachineType(mapToMachineTypeDTO(cat.getMachineType()));

        return dto;
    }

    public SubcategoryDTO mapToSubcategoryDTO(Subcategory subcat) {
        SubcategoryDTO dto = new SubcategoryDTO();
        dto.setId(subcat.getId());
        dto.setName(subcat.getName());
        dto.setCode(subcat.getCode());
        // dto.setCategory(mapToCategoryDTO(subcat.getCategory()));

        return dto;
    }

    public SerialNumberDTO mapToSerialNumberDTO(SerialNumber sn) {
        SerialNumberDTO dto = new SerialNumberDTO();
        dto.setId(sn.getId());
        dto.setCode(sn.getCode());
        dto.setName(sn.getName());
        // dto.setSubcategory(mapToSubcategoryDTO(sn.getSubcategory()));

        return dto;
    }

    public SupplierDTO mapToSupplierDTO(Supplier sup) {
        SupplierDTO dto = new SupplierDTO();
        dto.setId(sup.getId());
        dto.setName(sup.getName());
        dto.setCode(sup.getCode());
        // dto.setSerialNumber(mapToSerialNumberDTO(sup.getSerialNumber()));

        return dto;
    }

    public SectionDTO mapToSectionDTO(Section sec) {
        SectionDTO dto = new SectionDTO();
        dto.setId(sec.getId());
        dto.setCode(sec.getCode());

        return dto;
    }
}