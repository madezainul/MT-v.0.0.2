package ahqpck.maintenance.report.mapper;

import ahqpck.maintenance.report.dto.*;
import ahqpck.maintenance.report.entity.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EquipmentMapper {

    MachineTypeDTO toMachineTypeDTO(MachineType machineType);

    CategoryDTO toCategoryDTO(Category category);

    SubcategoryDTO toSubcategoryDTO(Subcategory subcategory);

    CapacityDTO toCapacityDTO(Capacity capacity);

    SupplierDTO toSupplierDTO(Supplier supplier);

    SectionDTO toSectionDTO(Section section);
}