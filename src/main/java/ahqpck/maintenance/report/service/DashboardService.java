package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.AssigneeDailyStatusDTO;
import ahqpck.maintenance.report.dto.AssigneeDailyStatusDetailDTO;
import ahqpck.maintenance.report.dto.AssigneeTotalStatusDTO;
import ahqpck.maintenance.report.dto.DailyBreakdownDTO;
import ahqpck.maintenance.report.dto.DailyComplaintDTO;
import ahqpck.maintenance.report.dto.DailyWorkReportDTO;
import ahqpck.maintenance.report.dto.DailyWorkReportEquipmentDTO;
import ahqpck.maintenance.report.dto.EquipmentCountDTO;
import ahqpck.maintenance.report.dto.EquipmentStatusDTO;
import ahqpck.maintenance.report.dto.EquipmentWorkReportDTO;
import ahqpck.maintenance.report.dto.MonthlyBreakdownDTO;
import ahqpck.maintenance.report.dto.MonthlyComplaintDTO;
import ahqpck.maintenance.report.dto.MonthlyWorkReportDTO;
import ahqpck.maintenance.report.dto.MonthlyWorkReportEquipmentDTO;
import ahqpck.maintenance.report.dto.StatusCountDTO;
import ahqpck.maintenance.report.repository.DashboardRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    public StatusCountDTO getStatusCount(LocalDateTime from, LocalDateTime to) {
        return dashboardRepository.getStatusCount(from, to);
    }

    public List<DailyComplaintDTO> getDailyComplaint(LocalDateTime from, LocalDateTime to) {
        DateRange dateRange = getDefaultDateTimeRange(from, to, 6);
        return dashboardRepository.getDailyComplaint(dateRange.from(), dateRange.to());
    }

    public List<MonthlyComplaintDTO> getMonthlyComplaint(Integer year) {
        Integer effectiveYear = getEffectiveYear(year);
        return dashboardRepository.getMonthlyComplaint(effectiveYear);
    }

    public AssigneeDailyStatusDTO getAssigneeDailyStatus(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            DateRange dateRange = getDefaultDateTimeRange(null, null, 6);
            from = dateRange.from();
            to = dateRange.to();
        }

        LocalDate fromDate = from.toLocalDate();
        LocalDate toDate = to.toLocalDate();

        // Validate: from <= to
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Invalid date range: 'from' must be before or equal to 'to'");
        }

        List<Object[]> results = dashboardRepository.getAssigneeDailyStatus(fromDate, toDate);

        // Generate all dates in range
        List<LocalDate> dateList = Stream.iterate(fromDate, d -> d.plusDays(1))
                .takeWhile(d -> !d.isAfter(toDate))
                .collect(Collectors.toList());

        Map<String, AssigneeDailyStatusDetailDTO> assigneeMap = initializeAssigneeMap(results, dateList.size());
        populateAssigneeData(results, assigneeMap, dateList);

        AssigneeDailyStatusDTO response = new AssigneeDailyStatusDTO();
        response.setDates(dateList.stream().map(LocalDate::toString).collect(Collectors.toList()));
        response.setData(new ArrayList<>(assigneeMap.values()));

        return response;
    }

    public List<AssigneeTotalStatusDTO> getAssigneeTotalStatus() {
        List<Object[]> results = dashboardRepository.getAssigneeTotalStatus();

        Map<String, AssigneeTotalStatusDTO> assigneeMap = new LinkedHashMap<>();

        // Initialize all assignees
        for (Object[] row : results) {
            String assigneeName = (String) row[0];
            String assigneeEmpId = (String) row[1];
            String key = assigneeName + "|" + assigneeEmpId;

            assigneeMap.computeIfAbsent(key, k -> new AssigneeTotalStatusDTO(assigneeName, assigneeEmpId, 0, 0, 0));
        }

        // Populate counts
        for (Object[] row : results) {
            String assigneeName = (String) row[0];
            String assigneeEmpId = (String) row[1];
            String status = (String) row[2];
            int count = Math.toIntExact(((Number) row[3]).longValue());

            String key = assigneeName + "|" + assigneeEmpId;
            AssigneeTotalStatusDTO dto = assigneeMap.get(key);

            if (dto != null) {
                switch (status) {
                    case "OPEN" -> dto.setTotalOpen(count);
                    case "PENDING" -> dto.setTotalPending(count);
                    case "CLOSED" -> dto.setTotalClosed(count);
                }
            }
        }

        return new ArrayList<>(assigneeMap.values());
    }

    public List<DailyBreakdownDTO> getDailyBreakdownTime(LocalDate from, LocalDate to) {
        DateRangeLocal dateRange = getDefaultDateRange(from, to, 6);
        return dashboardRepository.getDailyBreakdownTime(dateRange.from(), dateRange.to());
    }

    public List<MonthlyBreakdownDTO> getMonthlyBreakdownTime(Integer year) {
        Integer effectiveYear = getEffectiveYear(year);
        return dashboardRepository.getMonthlyBreakdownTime(effectiveYear);
    }

    public List<EquipmentWorkReportDTO> getEquipmentWorkReport() {
        return dashboardRepository.getEquipmentWorkReport();
    }

    public List<DailyWorkReportDTO> getDailyWorkReport(LocalDate from, LocalDate to) {
        DateRangeLocal dateRange = getDefaultDateRange(from, to, 6);
        return dashboardRepository.getDailyWorkReport(dateRange.from(), dateRange.to());
    }

    // === Monthly Work Report Count ===
    public List<MonthlyWorkReportDTO> getMonthlyWorkReport(Integer year) {
        Integer effectiveYear = getEffectiveYear(year);
        return dashboardRepository.getMonthlyWorkReport(effectiveYear);
    }

    public List<DailyWorkReportEquipmentDTO> getDailyWorkReportEquipment(LocalDate from, LocalDate to,
            String equipmentCode) {
        DateRangeLocal dateRange = getDefaultDateRange(from, to, 6);

        String effectiveEquipmentCode = (equipmentCode != null && !equipmentCode.trim().isEmpty())
                ? equipmentCode.trim()
                : null;

        return dashboardRepository.getDailyWorkReportEquipment(dateRange.from(), dateRange.to(), effectiveEquipmentCode);
    }

    public List<MonthlyWorkReportEquipmentDTO> getMonthlyWorkReportEquipment(Integer year, String equipmentCode) {
        Integer effectiveYear = getEffectiveYear(year);
        return dashboardRepository.getMonthlyWorkReportEquipment(effectiveYear, equipmentCode);
    }

    public List<EquipmentCountDTO> getEquipmentCount() {
        return dashboardRepository.getEquipmentCount();
    }

    public List<EquipmentStatusDTO> getEquipmentStatus() {
        return dashboardRepository.getEquipmentStatus();
    }

    // Helper record for date range handling
    private record DateRange(LocalDateTime from, LocalDateTime to) {}
    
    private record DateRangeLocal(LocalDate from, LocalDate to) {}

    // Helper method for DateTime range with default lookback days
    private DateRange getDefaultDateTimeRange(LocalDateTime from, LocalDateTime to, int defaultLookbackDays) {
        LocalDateTime defaultTo = LocalDateTime.now().with(LocalTime.MAX);
        LocalDateTime defaultFrom = defaultTo.minusDays(defaultLookbackDays).with(LocalTime.MIN);
        
        LocalDateTime effectiveFrom = from != null ? from : defaultFrom;
        LocalDateTime effectiveTo = to != null ? to : defaultTo;
        
        return new DateRange(effectiveFrom, effectiveTo);
    }
    
    // Helper method for LocalDate range with default lookback days
    private DateRangeLocal getDefaultDateRange(LocalDate from, LocalDate to, int defaultLookbackDays) {
        LocalDate defaultTo = LocalDate.now();
        LocalDate defaultFrom = defaultTo.minusDays(defaultLookbackDays);
        
        LocalDate effectiveFrom = from != null ? from : defaultFrom;
        LocalDate effectiveTo = to != null ? to : defaultTo;
        
        return new DateRangeLocal(effectiveFrom, effectiveTo);
    }
    
    // Helper method for year validation
    private Integer getEffectiveYear(Integer year) {
        return (year != null && year > 1900) ? year : LocalDate.now().getYear();
    }

    // Helper methods for getAssigneeDailyStatus
    private Map<String, AssigneeDailyStatusDetailDTO> initializeAssigneeMap(List<Object[]> results, int numDays) {
        Map<String, AssigneeDailyStatusDetailDTO> assigneeMap = new LinkedHashMap<>();
        
        // Pre-initialize all assignees with empty lists of zeros
        for (Object[] row : results) {
            String assigneeName = (String) row[0]; // u.name
            String assigneeEmpId = (String) row[1]; // u.employee_id
            String assigneeKey = createAssigneeKey(assigneeName, assigneeEmpId);

            assigneeMap.computeIfAbsent(assigneeKey, k -> {
                AssigneeDailyStatusDetailDTO dto = new AssigneeDailyStatusDetailDTO();
                dto.setAssigneeName(assigneeName);
                dto.setAssigneeEmpId(assigneeEmpId);
                List<Integer> zeros = Collections.nCopies(numDays, 0);
                dto.setOpen(new ArrayList<>(zeros));
                dto.setPending(new ArrayList<>(zeros));
                dto.setClosed(new ArrayList<>(zeros));
                return dto;
            });
        }
        
        return assigneeMap;
    }

    private void populateAssigneeData(List<Object[]> results, Map<String, AssigneeDailyStatusDetailDTO> assigneeMap, List<LocalDate> dateList) {
        for (Object[] row : results) {
            String assigneeName = (String) row[0];
            String assigneeEmpId = (String) row[1];
            String status = (String) row[2];
            LocalDate reportDate = ((java.sql.Date) row[3]).toLocalDate();
            Long count = ((Number) row[4]).longValue();

            if (!dateList.contains(reportDate)) {
                continue;
            }

            int dayIndex = dateList.indexOf(reportDate);
            String assigneeKey = createAssigneeKey(assigneeName, assigneeEmpId);
            AssigneeDailyStatusDetailDTO dto = assigneeMap.get(assigneeKey);

            if (dto != null) {
                updateStatusCount(dto, status, dayIndex, Math.toIntExact(count));
            }
        }
    }

    private String createAssigneeKey(String assigneeName, String assigneeEmpId) {
        return assigneeName + "|" + assigneeEmpId;
    }

    private void updateStatusCount(AssigneeDailyStatusDetailDTO dto, String status, int dayIndex, int count) {
        switch (status) {
            case "OPEN" -> dto.getOpen().set(dayIndex, count);
            case "PENDING" -> dto.getPending().set(dayIndex, count);
            case "CLOSED" -> dto.getClosed().set(dayIndex, count);
        }
    }
}