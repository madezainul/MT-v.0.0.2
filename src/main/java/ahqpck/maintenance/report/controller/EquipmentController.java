package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.service.EquipmentService;
import ahqpck.maintenance.report.util.ImportUtil;
import ahqpck.maintenance.report.util.WebUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/equipments")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @Value("${app.upload-equipment-image.dir:src/main/resources/static/upload/equipment/image}")
    private String uploadDir;

    @GetMapping
    public String listEquipments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") String size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "true") boolean asc,
            Model model) {

        try {
            int zeroBasedPage = page - 1;
            int parsedSize = "All".equalsIgnoreCase(size) ? Integer.MAX_VALUE : Integer.parseInt(size);

            Page<EquipmentDTO> equipmentPage = equipmentService.getAllEquipments(keyword, zeroBasedPage, parsedSize, sortBy, asc);

            model.addAttribute("equipments", equipmentPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentPage", page); // Store 1-based for Thymeleaf
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);
            model.addAttribute("title", "Equipments");
            
            // Empty DTO for create form
            model.addAttribute("equipmentDTO", new EquipmentDTO());

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load equipment: " + e.getMessage());
            return "error/500";
        }

        return "equipment/index";
    }

    @PostMapping
    public String createEquipment(
            @Valid @ModelAttribute EquipmentDTO equipmentDTO,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes ra) {

        if (WebUtil.hasErrors(bindingResult)) {
            ra.addFlashAttribute("error", WebUtil.getErrorMessage(bindingResult));
            return "redirect:/equipments";
        }

        try {
            equipmentService.createEquipment(equipmentDTO, imageFile);
            ra.addFlashAttribute("success", "Equipment created successfully.");
            return "redirect:/equipments";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("equipmentDTO", equipmentDTO);
            return "redirect:/equipments";
        }
    }

    @PostMapping("/update")
    public String updateEquipment(
            @Valid @ModelAttribute EquipmentDTO equipmentDTO,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "deleteImage", required = false, defaultValue = "false") boolean deleteImage,
            RedirectAttributes ra) {

        if (WebUtil.hasErrors(bindingResult)) {
            ra.addFlashAttribute("error", WebUtil.getErrorMessage(bindingResult));
            return "redirect:/equipments";
        }

        try {
            equipmentService.updateEquipment(equipmentDTO, imageFile, deleteImage);
            ra.addFlashAttribute("success", "Equipment updated successfully.");
            return "redirect:/equipments";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("equipmentDTO", equipmentDTO);
            return "redirect:/equipments";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteEquipment(@PathVariable String id, RedirectAttributes ra) {
        try {
            equipmentService.deleteEquipment(id);
            ra.addFlashAttribute("success", "Equipment deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/equipments";
    }

    @PostMapping("/import")
    public String importEquipments(
            @RequestParam("data") String dataJson,
            @RequestParam(value = "sheet", required = false) String sheet,
            @RequestParam(value = "headerRow", required = false) Integer headerRow,
            RedirectAttributes ra) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> data = mapper.readValue(dataJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            ImportUtil.ImportResult result = equipmentService.importEquipmentsFromExcel(data);

            if (result.getImportedCount() > 0 && !result.hasErrors()) {
                ra.addFlashAttribute("success",
                        "Successfully imported " + result.getImportedCount() + " equipment record(s).");
            } else if (result.getImportedCount() > 0) {
                StringBuilder msg = new StringBuilder("Imported ").append(result.getImportedCount())
                        .append(" record(s), but ").append(result.getErrorMessages().size()).append(" error(s):");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            } else {
                StringBuilder msg = new StringBuilder("Failed to import any equipment:");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            }

            return "redirect:/equipments";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Bulk import failed: " + e.getMessage());
            return "redirect:/equipments";
        }
    }
}