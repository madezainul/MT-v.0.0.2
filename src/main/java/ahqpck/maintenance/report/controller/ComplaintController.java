package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.dto.*;
import ahqpck.maintenance.report.entity.Area;
import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.repository.AreaRepository;
import ahqpck.maintenance.report.repository.EquipmentRepository;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.service.AreaService;
import ahqpck.maintenance.report.service.ComplaintService;
import ahqpck.maintenance.report.service.EquipmentService;
import ahqpck.maintenance.report.service.PartService;
import ahqpck.maintenance.report.service.UserService;
import ahqpck.maintenance.report.util.ImportUtil;
import ahqpck.maintenance.report.util.WebUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final EquipmentService equipmentService;
    private final AreaService areaService;
    private final UserService userService;

    @GetMapping
    public String listComplaints(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDateTo,
            @RequestParam(required = false) String assigneeEmpId,
            @RequestParam(required = false) Complaint.Status status,
            @RequestParam(required = false) String equipmentCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") String size,
            @RequestParam(defaultValue = "reportDate") String sortBy,
            @RequestParam(defaultValue = "false") boolean asc,
            Model model) {

        try {
            int zeroBasedPage = page - 1;
            int parsedSize = "All".equalsIgnoreCase(size) ? Integer.MAX_VALUE : Integer.parseInt(size);

            LocalDateTime from = reportDateFrom != null ? reportDateFrom.atStartOfDay() : null;
            LocalDateTime to = reportDateTo != null ? reportDateTo.atTime(LocalTime.MAX) : null;

            Page<ComplaintDTO> complaintPage = complaintService.getAllComplaints(keyword, from, to, assigneeEmpId,
                    status, equipmentCode, zeroBasedPage, parsedSize, sortBy, asc);

            model.addAttribute("complaints", complaintPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("reportDateFrom", reportDateFrom);
            model.addAttribute("reportDateTo", reportDateTo);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);

            model.addAttribute("title", "Complaint List");

            model.addAttribute("users", getAllUsersForDropdown());
            model.addAttribute("areas", getAllAreasForDropdown());
            model.addAttribute("equipments", getAllEquipmentsForDropdown());

            model.addAttribute("complaintDTO", new ComplaintDTO());

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load complaints: " + e.getMessage());
            return "error/500";
        }

        return "complaint/index";
    }

    @GetMapping("/{id}")
    public String getComplaintDetail(@PathVariable String id, Model model) {
        try {
            ComplaintDTO complaintDTO = complaintService.getComplaintById(id);
            model.addAttribute("complaint", complaintDTO);
            model.addAttribute("title", "Complaint Detail");

            model.addAttribute("users", getAllUsersForDropdown());
            model.addAttribute("areas", getAllAreasForDropdown());
            model.addAttribute("equipments", getAllEquipmentsForDropdown());

            return "complaint/detail";

        } catch (NotFoundException e) {
            model.addAttribute("error", "Complaint not found: " + e.getMessage());
            return "error/404";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load complaint: " + e.getMessage());
            return "error/500";
        }
    }

    @PostMapping
    public String createComplaint(
            @Valid @ModelAttribute ComplaintDTO complaintDTO,
            BindingResult bindingResult,
            @RequestParam(value = "imageBeforeFile", required = false) MultipartFile imageBefore,
            RedirectAttributes ra) {

        if (WebUtil.hasErrors(bindingResult)) {
            ra.addFlashAttribute("error", WebUtil.getErrorMessage(bindingResult));
            return "redirect:/complaints";
        }

        try {
            complaintService.createComplaint(complaintDTO, imageBefore);
            ra.addFlashAttribute("success", "Complaint created successfully.");
            return "redirect:/complaints";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("complaintDTO", complaintDTO);
            return "redirect:/complaints";
        }
    }

    @PostMapping("/update")
    public String updateComplaint(
            @Valid @ModelAttribute ComplaintDTO complaintDTO,
            BindingResult bindingResult,
            @RequestParam(value = "imageBeforeFile", required = false) MultipartFile imageBeforeFile,
            @RequestParam(value = "imageAfterFile", required = false) MultipartFile imageAfterFile,
            @RequestParam(value = "deleteImageBefore", required = false, defaultValue = "false") Boolean deleteImageBefore,
            @RequestParam(value = "deleteImageAfter", required = false, defaultValue = "false") Boolean deleteImageAfter,
            RedirectAttributes ra) {

        if (WebUtil.hasErrors(bindingResult)) {
            ra.addFlashAttribute("error", WebUtil.getErrorMessage(bindingResult));
            return "redirect:/complaints";
        }

        try {
            complaintService.updateComplaint(complaintDTO, imageBeforeFile, imageAfterFile, deleteImageBefore, deleteImageAfter);
            ra.addFlashAttribute("success", "Complaint updated successfully.");
            return "redirect:/complaints";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("complaintDTO", complaintDTO);
            return "redirect:/complaints";
        }
    }

    // === DELETE COMPLAINT ===
    @GetMapping("/delete/{id}")
    public String deleteComplaint(@PathVariable String id, RedirectAttributes ra) {
        try {
            complaintService.deleteComplaint(id);
            ra.addFlashAttribute("success", "Complaint deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/complaints";
    }

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

            ImportUtil.ImportResult result = complaintService.importComplaintsFromExcel(data);

            if (result.getImportedCount() > 0 && !result.hasErrors()) {
                ra.addFlashAttribute("success",
                        "Successfully imported " + result.getImportedCount() + " complaint record(s).");
            } else if (result.getImportedCount() > 0) {
                StringBuilder msg = new StringBuilder("Imported ").append(result.getImportedCount())
                        .append(" record(s), but ").append(result.getErrorMessages().size()).append(" error(s):");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            } else {
                StringBuilder msg = new StringBuilder("Failed to import any complaint:");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            }

            return "redirect:/complaints";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Bulk import failed: " + e.getMessage());
            return "redirect:/complaints";
        }
    }

    private List<UserDTO> getAllUsersForDropdown() {
        return userService.getAllUsers(null, 0, Integer.MAX_VALUE, "name", true)
                .getContent().stream()
                .collect(Collectors.toList());
    }

    private List<AreaDTO> getAllAreasForDropdown() {
        return areaService.getAllAreas(null, 0, Integer.MAX_VALUE, "name", true)
                .getContent().stream()
                .collect(Collectors.toList());
    }

    private List<EquipmentDTO> getAllEquipmentsForDropdown() {
        return equipmentService.getAllEquipments(null, 0, Integer.MAX_VALUE, "name", true)
                .getContent().stream()
                .collect(Collectors.toList());
    }
}
