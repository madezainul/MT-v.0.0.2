package ahqpck.maintenance.report.dto;

public interface EquipmentCountDTO {

    String getEquipmentName();
    String getEquipmentCode();
    Integer getTotalTime();
    Long getTotalWorkReports();
    Long getTotalComplaints();
    Long getTotalOccurrences();
}