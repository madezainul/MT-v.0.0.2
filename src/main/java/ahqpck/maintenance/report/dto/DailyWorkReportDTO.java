package ahqpck.maintenance.report.dto;

public interface DailyWorkReportDTO {
    String getDate();
    Integer getCorrectiveMaintenance();
    Integer getPreventiveMaintenance();
    Integer getBreakdown();
    Integer getOther();
}