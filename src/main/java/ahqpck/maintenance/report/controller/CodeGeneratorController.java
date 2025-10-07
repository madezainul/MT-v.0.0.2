package ahqpck.maintenance.report.controller;

import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ahqpck.maintenance.report.dto.AreaDTO;
import ahqpck.maintenance.report.dto.CategoryDTO;
import ahqpck.maintenance.report.dto.DTOMapper;
import ahqpck.maintenance.report.dto.MachineTypeDTO;
import ahqpck.maintenance.report.dto.SectionDTO;
import ahqpck.maintenance.report.dto.SerialNumberDTO;
import ahqpck.maintenance.report.dto.SubcategoryDTO;
import ahqpck.maintenance.report.dto.SupplierDTO;
import ahqpck.maintenance.report.service.CategoryService;
import ahqpck.maintenance.report.service.MachineTypeService;
import ahqpck.maintenance.report.service.SectionService;
import ahqpck.maintenance.report.service.SerialNumberService;
import ahqpck.maintenance.report.service.SubcategoryService;
import ahqpck.maintenance.report.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/code-generator")
public class CodeGeneratorController {

    private final MachineTypeService machineTypeService;
    private final CategoryService categoryService;
    private final SubcategoryService subcategoryService;
    private final SerialNumberService serialNumberService;
    private final SupplierService supplierService;
    private final SectionService sectionService;
    private final DTOMapper dtoMapper;


    @GetMapping
    public String showGenerator(Model model) {
        model.addAttribute("title", "Code Generator");

        // Load all master data as DTOs for Thymeleaf rendering
        model.addAttribute("machineTypes", machineTypeService.getAll().stream()
                .map(dtoMapper::mapToMachineTypeDTO).collect(Collectors.toList()));

        model.addAttribute("categories", categoryService.getAll().stream()
                .map(dtoMapper::mapToCategoryDTO).collect(Collectors.toList()));

        model.addAttribute("subcategories", subcategoryService.getAll().stream()
                .map(dtoMapper::mapToSubcategoryDTO).collect(Collectors.toList()));
        
        model.addAttribute("serialNumbers", serialNumberService.getAll().stream()
                .map(dtoMapper::mapToSerialNumberDTO).collect(Collectors.toList()));

        model.addAttribute("suppliers", supplierService.getAll().stream()
                .map(dtoMapper::mapToSupplierDTO).collect(Collectors.toList()));

        model.addAttribute("sections", sectionService.getAll().stream()
                .map(dtoMapper::mapToSectionDTO).collect(Collectors.toList()));

        // modal category
        model.addAttribute("categoryDTO", new CategoryDTO());

        // modal subcategory
        model.addAttribute("subcategoryDTO", new SubcategoryDTO());

        // modal serial number
        model.addAttribute("serialNumberDTO", new SerialNumberDTO());

        // modal supplier
        model.addAttribute("supplierDTO", new SupplierDTO());

        return "code-generator/index";
    }

    // Machine Type Creation
    @PostMapping("/save-machine-type")
    public String createMachineType(
            @Valid @ModelAttribute MachineTypeDTO machineTypeDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            ra.addFlashAttribute("machineTypeDTO", machineTypeDTO);
            return "redirect:/code-generator";
        }

        try {
            machineTypeService.createMachineType(machineTypeDTO);
            ra.addFlashAttribute("success", "Machine Type created successfully.");
            return "redirect:/code-generator";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("machineTypeDTO", machineTypeDTO);
            return "redirect:/code-generator";
        }
    }
    
    // Category Creation
    @PostMapping("/save-category")
    public String createCategory(
            @Valid @ModelAttribute CategoryDTO categoryDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            ra.addFlashAttribute("categoryDTO", categoryDTO);
            return "redirect:/code-generator";
        }

        try {
            categoryService.createCategory(categoryDTO);
            ra.addFlashAttribute("success", "Category created successfully.");
            return "redirect:/code-generator";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("categoryDTO", categoryDTO);
            return "redirect:/code-generator";
        }
    }
    
    // Subcategory Creation
    @PostMapping("/save-subcategory")
    public String createSubcategory(
            @Valid @ModelAttribute SubcategoryDTO subcategoryDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            ra.addFlashAttribute("subcategoryDTO", subcategoryDTO);
            return "redirect:/code-generator";
        }

        try {
            subcategoryService.createSubcategory(subcategoryDTO);
            ra.addFlashAttribute("success", "Subcategory created successfully.");
            return "redirect:/code-generator";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("subcategoryDTO", subcategoryDTO);
            return "redirect:/code-generator";
        }
    }

    // Serial Number Creation
    @PostMapping("/save-serial-number")
    public String createSerialNumber(
            @Valid @ModelAttribute SerialNumberDTO serialNumberDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            ra.addFlashAttribute("serialNumberDTO", serialNumberDTO);
            return "redirect:/code-generator";
        }

        try {
            serialNumberService.createSerialNumber(serialNumberDTO);
            ra.addFlashAttribute("success", "Serial number created successfully.");
            return "redirect:/code-generator";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("serialNumberDTO", serialNumberDTO);
            return "redirect:/code-generator";
        }
    }

    // Supplier Creation
    @PostMapping("/save-supplier")
    public String createSupplier(
            @Valid @ModelAttribute SupplierDTO supplierDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            ra.addFlashAttribute("supplierDTO", supplierDTO);
            return "redirect:/code-generator";
        }

        try {
            supplierService.createSupplier(supplierDTO);
            ra.addFlashAttribute("success", "Supplier created successfully.");
            return "redirect:/code-generator";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("supplierDTO", supplierDTO);
            return "redirect:/code-generator";
        }
    }
    
    // Section Creation
    @PostMapping("/save-section")
    public String createSection(
            @Valid @ModelAttribute SectionDTO sectionDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            ra.addFlashAttribute("sectionDTO", sectionDTO);
            return "redirect:/code-generator";
        }

        try {
            sectionService.createSection(sectionDTO);
            ra.addFlashAttribute("success", "Section created successfully.");
            return "redirect:/code-generator";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("sectionDTO", sectionDTO);
            return "redirect:/code-generator";
        }
    }
}
