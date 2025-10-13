package ahqpck.maintenance.report.dto;

public interface MonthlyWorkReportDTO {
    Integer getYear();
    Integer getMonth();
    Integer getCorrectiveMaintenance();
    Integer getPreventiveMaintenance();
    Integer getBreakdown();
    Integer getOther();
}