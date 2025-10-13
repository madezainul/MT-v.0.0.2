package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.config.UserDetailsImpl;
import ahqpck.maintenance.report.dto.AreaDTO;
import ahqpck.maintenance.report.dto.ComplaintDTO;
import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.service.AreaService;
import ahqpck.maintenance.report.service.UserService;
import ahqpck.maintenance.report.util.ImportUtil;
import ahqpck.maintenance.report.util.WebUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;
    private final UserService userService;

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENGINEER', 'VIEWER')")
    @GetMapping
    public String listAreas(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String hiddenColumns,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") String size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "true") boolean asc,
            Authentication authentication,
            Model model) {

        try {
            int zeroBasedPage = page - 1;
            int parsedSize = "All".equalsIgnoreCase(size) ? Integer.MAX_VALUE : Integer.parseInt(size);

            String currentUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                currentUserId = userDetails.getId();
            }

            // Only fetch current user if needed
            if (currentUserId != null) {
                UserDTO currentUser = userService.getUserById(currentUserId);
                model.addAttribute("currentUser", currentUser);
            }

            Page<AreaDTO> areaPage = areaService.getAllAreas(keyword, zeroBasedPage, parsedSize, sortBy, asc);

            model.addAttribute("areas", areaPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("hiddenColumns", hiddenColumns);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);
            model.addAttribute("title", "Areas");
            model.addAttribute("users", getAllUsersForDropdown());

            model.addAttribute("areaDTO", new AreaDTO());

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load areas: " + e.getMessage());
            return "error/500";
        }

        return "area/index";
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    @PostMapping
    public String createArea(
            @Valid @ModelAttribute AreaDTO areaDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {

                System.out.println("Area" + areaDTO);

        if (WebUtil.hasErrors(bindingResult)) {
            ra.addFlashAttribute("error", WebUtil.getErrorMessage(bindingResult));
            return "redirect:/complaints";
        }

        try {
            areaService.createArea(areaDTO);
            ra.addFlashAttribute("success", "Area created successfully.");
            return "redirect:/areas";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("areaDTO", areaDTO);
            return "redirect:/areas";
        }
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    @PostMapping("/update")
    public String updateArea(
            @Valid @ModelAttribute AreaDTO areaDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {
                System.out.println("Area" + areaDTO);

        if (WebUtil.hasErrors(bindingResult)) {
            ra.addFlashAttribute("error", WebUtil.getErrorMessage(bindingResult));
            return "redirect:/complaints";
        }

        try {
            areaService.updateArea(areaDTO);
            ra.addFlashAttribute("success", "Area updated successfully.");
            return "redirect:/areas";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("areaDTO", areaDTO);
            return "redirect:/areas";
        }
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @GetMapping("/delete/{id}")
    public String deleteArea(@PathVariable String id, RedirectAttributes ra) {
        try {
            areaService.deleteArea(id);
            ra.addFlashAttribute("success", "Area deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/areas";
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    @PostMapping("/import")
    public String importComplaints(
            @RequestParam("data") String dataJson,
            @RequestParam(value = "sheet", required = false) String sheet,
            @RequestParam(value = "headerRow", required = false) Integer headerRow,
            RedirectAttributes ra) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> data = mapper.readValue(dataJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            ImportUtil.ImportResult result = areaService.importAreasFromExcel(data);

            if (result.getImportedCount() > 0 && !result.hasErrors()) {
                ra.addFlashAttribute("success",
                        "Successfully imported " + result.getImportedCount() + " area record(s).");
            } else if (result.getImportedCount() > 0) {
                StringBuilder msg = new StringBuilder("Imported ").append(result.getImportedCount())
                        .append(" record(s), but ").append(result.getErrorMessages().size()).append(" error(s):");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            } else {
                StringBuilder msg = new StringBuilder("Failed to import any area:");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            }

            return "redirect:/areas";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Bulk import failed: " + e.getMessage());
            return "redirect:/areas";
        }
    }

    private List<UserDTO> getAllUsersForDropdown() {
        return userService.getAllUsers(null, 0, Integer.MAX_VALUE, "name", true)
                .getContent().stream()
                .collect(Collectors.toList());
    }
}