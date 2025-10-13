package ahqpck.maintenance.report.dto;

public interface EquipmentStatusDTO {
    String getId();
    String getCode();
    String getName();
    Long getOpenWr();
    Long getPendingWr();
    Long getOpenCp();
    Long getPendingCp();
}
