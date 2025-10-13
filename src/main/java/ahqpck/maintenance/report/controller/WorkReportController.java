package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.config.UserDetailsImpl;
import ahqpck.maintenance.report.dto.AreaDTO;
import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.dto.WorkReportDTO;
import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.WorkReport;
import ahqpck.maintenance.report.service.AreaService;
import ahqpck.maintenance.report.service.EquipmentService;
import ahqpck.maintenance.report.service.UserService;
import ahqpck.maintenance.report.service.WorkReportService;
import ahqpck.maintenance.report.util.ImportUtil;
import ahqpck.maintenance.report.util.WebUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/work-reports")
@RequiredArgsConstructor
public class WorkReportController {

    private final WorkReportService workReportService;
    private final AreaService areaService;
    private final EquipmentService equipmentService;
    private final UserService userService;

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENGINEER', 'VIEWER')")
    @GetMapping
    public String listWorkReports(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDateTo,
            @RequestParam(required = false) WorkReport.Status state,
            @RequestParam(required = false) WorkReport.Category group,
            @RequestParam(required = false) WorkReport.Scope field,
            @RequestParam(required = false) String equipmentCode,
            @RequestParam(required = false) String hiddenColumns,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") String size,
            @RequestParam(defaultValue = "reportDate") String sortBy,
            @RequestParam(defaultValue = "false") boolean asc,
            Authentication authentication,
            Model model) {

        try {
            int zeroBasedPage = page - 1;
            int parsedSize = "All".equalsIgnoreCase(size) ? Integer.MAX_VALUE : Integer.parseInt(size);

            LocalDateTime from = reportDateFrom != null ? reportDateFrom.atStartOfDay() : null;
            LocalDateTime to = reportDateTo != null ? reportDateTo.atTime(LocalTime.MAX) : null;

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

            Page<WorkReportDTO> reportPage = workReportService.getAllWorkReports(keyword, from, to, state,
                    group, field, equipmentCode, zeroBasedPage, parsedSize, sortBy, asc);
            
            model.addAttribute("workReports", reportPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("reportDateFrom", reportDateFrom);
            model.addAttribute("hiddenColumns", hiddenColumns);
            model.addAttribute("reportDateTo", reportDateTo);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);

            model.addAttribute("equipmentCode", equipmentCode);
            model.addAttribute("group", group);
            model.addAttribute("field", field);
            model.addAttribute("state", state);

            model.addAttribute("title", "Work Report");

            model.addAttribute("users", getAllUsersForDropdown());
            model.addAttribute("areas", getAllAreasForDropdown());
            model.addAttribute("equipments", getAllEquipmentsForDropdown());

            model.addAttribute("workReportDTO", new WorkReportDTO());

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load work reports: " + e.getMessage());
            return "error/500";
            // e.printStackTrace();
        }

        return "work-report/index";
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    @PostMapping
    public String createWorkReport(
            @Valid @ModelAttribute WorkReportDTO workReportDTO,
            BindingResult bindingResult,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            RedirectAttributes ra) {

        if (workReportDTO.getTechnicianEmpIds() != null && !workReportDTO.getTechnicianEmpIds().isEmpty()) {
            Set<UserDTO> technicianDTOs = workReportDTO.getTechnicianEmpIds().stream()
                    .map(empId -> {
                        UserDTO userDTO = new UserDTO();
                        userDTO.setEmployeeId(empId);
                        return userDTO;
                    })
                    .collect(Collectors.toSet());
            workReportDTO.setTechnicians(technicianDTOs);
        }

        if (WebUtil.hasErrors(bindingResult)) {
            ra.addFlashAttribute("error", WebUtil.getErrorMessage(bindingResult));
        }

        try {
            workReportService.createWorkReport(workReportDTO);
            ra.addFlashAttribute("success", "Work report created successfully.");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create work report: " + e.getMessage());
            ra.addFlashAttribute("workReportDTO", workReportDTO);
        }

        return "redirect:" + (redirectUrl != null ? redirectUrl : "/work-reports");
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENGINEER')")
    @PostMapping("/update")
    public String updateWorkReport(
            @Valid @ModelAttribute WorkReportDTO workReportDTO,
            BindingResult bindingResult,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            RedirectAttributes ra) {

        if (workReportDTO.getTechnicianEmpIds() != null && !workReportDTO.getTechnicianEmpIds().isEmpty()) {
            Set<UserDTO> technicianDTOs = workReportDTO.getTechnicianEmpIds().stream()
                    .map(empId -> {
                        UserDTO userDTO = new UserDTO();
                        userDTO.setEmployeeId(empId);
                        return userDTO;
                    })
                    .collect(Collectors.toSet());
            workReportDTO.setTechnicians(technicianDTOs);
        }

        if (WebUtil.hasErrors(bindingResult)) {
            ra.addFlashAttribute("error", WebUtil.getErrorMessage(bindingResult));
        }

        try {
            workReportService.updateWorkReport(workReportDTO);
            ra.addFlashAttribute("success", "Work report updated successfully.");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update work report: " + e.getMessage());
            ra.addFlashAttribute("workReportDTO", workReportDTO);
        }

        return "redirect:" + (redirectUrl != null ? redirectUrl : "/work-reports");
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @GetMapping("/delete/{id}")
    public String deleteWorkReport(@PathVariable String id, @RequestParam(required = false) String redirectUrl, RedirectAttributes ra) {
        try {
            workReportService.deleteWorkReport(id);
            ra.addFlashAttribute("success", "Work report deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete work report: " + e.getMessage());
        }
        return "redirect:" + (redirectUrl != null ? redirectUrl : "/work-reports");
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    @PostMapping("/import")
    public String importWorkReports(
            @RequestParam("data") String dataJson,
            @RequestParam(value = "sheet", required = false) String sheet,
            @RequestParam(value = "headerRow", required = false) Integer headerRow,
            RedirectAttributes ra) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> data = mapper.readValue(dataJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            ImportUtil.ImportResult result = workReportService.importWorkReportsFromExcel(data);

            if (result.getImportedCount() > 0 && !result.hasErrors()) {
                ra.addFlashAttribute("success",
                        "Successfully imported " + result.getImportedCount() + " work report record(s).");
            } else if (result.getImportedCount() > 0) {
                StringBuilder msg = new StringBuilder("Imported ").append(result.getImportedCount())
                        .append(" record(s), but ").append(result.getErrorMessages().size()).append(" error(s):");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            } else {
                StringBuilder msg = new StringBuilder("Failed to import any work report:");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            }

            return "redirect:/work-reports";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Bulk import failed: " + e.getMessage());
            return "redirect:/work-reports";
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