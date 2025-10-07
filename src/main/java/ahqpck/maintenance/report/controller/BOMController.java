package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.service.BOMService;
import ahqpck.maintenance.report.service.EquipmentService;
import ahqpck.maintenance.report.service.PartService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/bom")
@RequiredArgsConstructor
public class BOMController {

    private final BOMService bomService;
    private final EquipmentService equipmentService;
    private final PartService partService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("title", "Bill of Materials");
        return "bom/index";
    }

    @GetMapping("/equipment-simple")
    public String equipmentBOMSimple(@RequestParam(required = false) String equipmentId, Model model) {
        model.addAttribute("title", "Equipment BOM Simple");
        model.addAttribute("selectedEquipmentId", equipmentId);
        
        try {
            var equipments = bomService.getAllEquipmentsForBOM();
            model.addAttribute("equipments", equipments);
            
            if (equipmentId != null && !equipmentId.trim().isEmpty()) {
                var bomEntries = bomService.getEquipmentBOM(equipmentId);
                model.addAttribute("bomEntries", bomEntries);
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error: " + e.getMessage());
        }
        
        return "bom/equipment-simple";
    }

    @GetMapping("/equipment")
    public String equipmentBOM(@RequestParam(required = false) String equipmentId, Model model) {
        try {
            // Get all equipment for dropdown
            var equipments = bomService.getAllEquipmentsForBOM();
            model.addAttribute("equipments", equipments);
            model.addAttribute("title", "Equipment BOM");
            
            if (equipmentId != null && !equipmentId.trim().isEmpty()) {
                var selectedEquipment = bomService.getEquipmentWithParts(equipmentId);
                var bomEntries = bomService.getEquipmentBOM(equipmentId);
                var availableParts = bomService.getAvailablePartsForEquipment(equipmentId);
                
                model.addAttribute("selectedEquipment", selectedEquipment);
                model.addAttribute("bomEntries", bomEntries);
                model.addAttribute("availableParts", availableParts);
                model.addAttribute("selectedEquipmentId", equipmentId);
            } else {
                // If no equipment selected, add empty collections to prevent template errors
                model.addAttribute("bomEntries", java.util.Collections.emptyList());
                model.addAttribute("availableParts", java.util.Collections.emptyList());
            }
            
            return "bom/equipment";
            
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load equipment BOM: " + e.getMessage());
            model.addAttribute("equipments", java.util.Collections.emptyList());
            model.addAttribute("bomEntries", java.util.Collections.emptyList());
            model.addAttribute("availableParts", java.util.Collections.emptyList());
            return "bom/equipment";
        }
    }

    @GetMapping("/part-usage")
    public String partUsage(@RequestParam(required = false) String partId, Model model) {
        // Get all parts for dropdown
        var parts = bomService.getAllPartsForBOM();
        model.addAttribute("parts", parts);
        model.addAttribute("title", "Part Usage Analysis");
        
        if (partId != null && !partId.trim().isEmpty()) {
            var selectedPart = bomService.getPartWithEquipments(partId);
            var usageEntries = bomService.getPartUsage(partId);
            
            model.addAttribute("selectedPart", selectedPart);
            model.addAttribute("usageEntries", usageEntries);
            model.addAttribute("selectedPartId", partId);
        }
        
        return "bom/part-usage";
    }

    @PostMapping("/add-part")
    public String addPartToEquipment(@RequestParam String equipmentId,
                                   @RequestParam String partId,
                                   @RequestParam(defaultValue = "1") Integer quantity,
                                   @RequestParam(required = false) String notes,
                                   @RequestParam(defaultValue = "STANDARD") String criticalityLevel,
                                   RedirectAttributes ra) {
        try {
            bomService.addPartToEquipment(equipmentId, partId, quantity, notes, criticalityLevel);
            ra.addFlashAttribute("success", "Part added to equipment BOM successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bom/equipment?equipmentId=" + equipmentId;
    }

    @PostMapping("/remove-part")
    public String removePartFromEquipment(@RequestParam String equipmentId,
                                        @RequestParam String partId,
                                        RedirectAttributes ra) {
        try {
            bomService.removePartFromEquipment(equipmentId, partId);
            ra.addFlashAttribute("success", "Part removed from equipment BOM successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bom/equipment?equipmentId=" + equipmentId;
    }

    @PostMapping("/update-bom")
    public String updateBOMEntry(@RequestParam String bomId,
                               @RequestParam(defaultValue = "1") Integer quantity,
                               @RequestParam(required = false) String notes,
                               @RequestParam(defaultValue = "STANDARD") String criticalityLevel,
                               RedirectAttributes ra) {
        try {
            bomService.updateBOMEntry(bomId, quantity, notes, criticalityLevel);
            ra.addFlashAttribute("success", "BOM entry updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bom/equipment";
    }
}