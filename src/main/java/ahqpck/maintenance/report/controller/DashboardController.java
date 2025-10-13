package ahqpck.maintenance.report.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import ahqpck.maintenance.report.config.UserDetailsImpl;
import ahqpck.maintenance.report.dto.ComplaintDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.service.ComplaintService;
import ahqpck.maintenance.report.service.UserService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final ComplaintService complaintService;

    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENGINEER', 'VIEWER')")
    @GetMapping
    public String dashboard(Authentication authentication, Model model) {
        String currentUserId = null;
        String currentUserEmpId = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            currentUserId = userDetails.getId();
            currentUserEmpId = userDetails.getEmployeeId();
        }

        // Only fetch current user if needed
        if (currentUserId != null) {
            UserDTO currentUser = userService.getUserById(currentUserId);
            model.addAttribute("currentUser", currentUser);
        }

        Page<ComplaintDTO> complaintPage = complaintService.getAllComplaints(
                null, // keyword
                null, // reportDateFrom
                null, // reportDateTo
                null,
                currentUserEmpId, // assigneeEmpId
                Complaint.Status.OPEN, // status
                null, // category
                null, // equipmentCode
                0, // page (was "0" → now 0)
                10, // size
                "status", // sortBy
                true // asc
        );

        model.addAttribute("complaints", complaintPage);
        model.addAttribute("title", "Overview");
        return "dashboard/overview";
    }
}
