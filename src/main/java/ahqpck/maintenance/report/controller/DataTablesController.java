package ahqpck.maintenance.report.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ahqpck.maintenance.report.service.MachineTypeService;
import ahqpck.maintenance.report.service.CategoryService;
import ahqpck.maintenance.report.service.SubcategoryService;
import ahqpck.maintenance.report.service.CapacityService;
import ahqpck.maintenance.report.service.SupplierService;
import ahqpck.maintenance.report.service.SectionService;
import ahqpck.maintenance.report.dto.DTOMapper;

@Controller
@RequiredArgsConstructor
@RequestMapping("/code-generator")
public class DataTablesController {
    private final MachineTypeService machineTypeService;
    private final CategoryService categoryService;
    private final SubcategoryService subcategoryService;
    private final CapacityService capacityService;
    private final SupplierService supplierService;
    private final SectionService sectionService;
    private final DTOMapper dtoMapper;

    @GetMapping("/data-tables")
    public String showDataTables(Model model) {
        
        try {
            model.addAttribute("title", "Data Tables");

            // Get all data for tabs - convert to DTOs
            model.addAttribute("machineTypes", machineTypeService.getAll().stream().map(dtoMapper::mapToMachineTypeDTO).collect(java.util.stream.Collectors.toList()));
            model.addAttribute("categories", categoryService.getAll().stream().map(dtoMapper::mapToCategoryDTO).collect(java.util.stream.Collectors.toList()));
            model.addAttribute("subcategories", subcategoryService.getAll().stream().map(dtoMapper::mapToSubcategoryDTO).collect(java.util.stream.Collectors.toList()));
            model.addAttribute("capacities", capacityService.getAll().stream().map(dtoMapper::mapToCapacityDTO).collect(java.util.stream.Collectors.toList()));
            model.addAttribute("suppliers", supplierService.getAll().stream().map(dtoMapper::mapToSupplierDTO).collect(java.util.stream.Collectors.toList()));
            model.addAttribute("sections", sectionService.getAll().stream().map(dtoMapper::mapToSectionDTO).collect(java.util.stream.Collectors.toList()));
            
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load data: " + e.getMessage());
        }

        return "code-generator/data-tables";
    }
}
