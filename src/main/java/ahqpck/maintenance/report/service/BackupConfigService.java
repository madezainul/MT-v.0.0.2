package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.AreaDTO;
import ahqpck.maintenance.report.dto.BackupConfigDTO;
import ahqpck.maintenance.report.dto.BackupHistoryDTO;
import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.dto.PartDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.Area;
import ahqpck.maintenance.report.entity.BackupConfig;
import ahqpck.maintenance.report.entity.BackupHistory;
import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.entity.Role;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.entity.WorkReport;
import ahqpck.maintenance.report.repository.*;
import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BackupConfigService {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;
    private final WorkReportRepository workReportRepository;
    private final AreaRepository areaRepository;
    private final EquipmentRepository equipmentRepository;
    private final PartRepository partRepository;
    private final BackupConfigRepository backupConfigRepository;
    private final BackupHistoryRepository backupHistoryRepository;

    @Transactional(readOnly = true)
    public BackupConfigDTO getCurrentConfig() {
        BackupConfig config = backupConfigRepository.findTopByOrderByIdDesc()
                .orElse(new BackupConfig());
        return mapToDTO(config);
    }

    @Transactional
    public void saveBackupConfig(BackupConfigDTO dto) {
        // Always get the current (or only) config – if none exists, we'll create one
        BackupConfig existing = backupConfigRepository.findTopByOrderByIdDesc().orElse(null);

        BackupConfig configToSave = existing != null
                ? existing // update existing
                : new BackupConfig(); // create new

        // Map DTO fields to entity
        configToSave.setIntervalDays(dto.getIntervalDays());
        configToSave.setBackupTime(LocalTime.parse(dto.getBackupTime()));
        configToSave.setStartDate(LocalDate.parse(dto.getStartDate()));
        configToSave.setBackupFolder(dto.getBackupFolder());
        configToSave.setBackupTypes(dto.getBackupTypes());
        configToSave.setUpdatedAt(LocalDateTime.now());

        backupConfigRepository.save(configToSave);
    }

    // --- BACKUP NOW ---
    @Transactional
    public void backupNow(String backupFolder, Set<String> backupTypes, String method) throws IOException {
        String status = "SUCCESS";
        String fileSize = null;
        String errorMessage = null;
        Path excelFilePath = null;
        LocalDateTime backupTime = LocalDateTime.now(); // Capture time once

        try {
            System.out.println("Performing backup to folder: " + backupFolder + " with types: " + backupTypes);
            Path folderPath = Paths.get(backupFolder);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }

            try (Workbook workbook = new XSSFWorkbook()) {
                if (backupTypes.contains("USER")) {
                Map<String, ColumnType> userTypes = new HashMap<>();
                userTypes.put("Join Date", ColumnType.DATE);
                userTypes.put("Created At", ColumnType.DATETIME);
                userTypes.put("Activated At", ColumnType.DATETIME);
                createSheet(workbook, "Users", userRepository.findAll(), this::mapUser, userTypes);
            }

            if (backupTypes.contains("AREA")) {
                createSheet(workbook, "Areas", areaRepository.findAll(), this::mapArea); // No dates
            }

            if (backupTypes.contains("EQUIPMENT")) {
                Map<String, ColumnType> equipmentTypes = new HashMap<>();
                equipmentTypes.put("Manufactured Date", ColumnType.DATE);
                equipmentTypes.put("Commissioned Date", ColumnType.DATE);
                createSheet(workbook, "Equipments", equipmentRepository.findAll(), this::mapEquipment, equipmentTypes);
            }

            if (backupTypes.contains("PART")) {
                createSheet(workbook, "Parts", partRepository.findAll(), this::mapPart); // No dates
            }

            if (backupTypes.contains("COMPLAINT")) {
                Map<String, ColumnType> complaintTypes = new HashMap<>();
                complaintTypes.put("Report Date", ColumnType.DATETIME);
                complaintTypes.put("Updated At", ColumnType.DATETIME);
                complaintTypes.put("Close Time", ColumnType.DATETIME);
                createSheet(workbook, "Complaints", complaintRepository.findAll(), this::mapComplaint, complaintTypes);
            }

            if (backupTypes.contains("WORK_REPORT")) {
                Map<String, ColumnType> workReportTypes = new HashMap<>();
                workReportTypes.put("Report Date", ColumnType.DATE);
                workReportTypes.put("Start Time", ColumnType.DATETIME);
                workReportTypes.put("Stop Time", ColumnType.DATETIME);
                createSheet(workbook, "WorkReports", workReportRepository.findAll(), this::mapWorkReport,
                        workReportTypes);
            }

                // Save Excel file
                String timestamp = backupTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String excelFilename = "maintenance_backup_" + timestamp + ".xlsx";
                excelFilePath = folderPath.resolve(excelFilename);
                try (FileOutputStream out = new FileOutputStream(excelFilePath.toFile())) {
                    workbook.write(out);
                }

                // Perform SQL backup
                String sqlFilename = "maintenance_backup_" + timestamp + ".sql";
                Path sqlFilePath = folderPath.resolve(sqlFilename);
                performSqlBackup(sqlFilePath.toString());

                // Get file size
                if (Files.exists(excelFilePath)) {
                    fileSize = formatFileSize(Files.size(excelFilePath));
                }
            }

        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            throw e;
        } finally {
            // Always save history (even on failure)
            BackupHistory history = new BackupHistory();
            history.setBackupDateTime(backupTime);
            history.setBackupTypes(String.join(",", backupTypes));
            history.setStatus(status);
            history.setMethod(method);
            history.setFileSize(fileSize);
            history.setLocation(backupFolder);
            history.setErrorMessage(errorMessage);
            backupHistoryRepository.save(history);
        }
    }
    // --- PERFORM SQL BACKUP ---
    private void performSqlBackup(String backupFile) {

        String url = dbUrl.replace("jdbc:mysql://", "");
        String[] hostPortDb = url.split("[/?]", 3); // Split by '/' or '?' (max 3 parts)

        String hostPort = hostPortDb[0]; // e.g., "localhost:3306"
        String dbName = hostPortDb[1]; // e.g., "maintenance_db_dev"

        String[] hostAndPort = hostPort.split(":");
        String dbHost = hostAndPort[0];
        String dbPort = hostAndPort.length > 1 ? hostAndPort[1] : "3306";

        ProcessBuilder processBuilder = new ProcessBuilder(
                "mysqldump.exe",
                "--host=" + dbHost,
                "--port=" + dbPort,
                "--user=" + dbUser,
                "--password=" + dbPassword,
                "--single-transaction",
                "--routines",
                "--triggers",
                "--events",
                "--hex-blob",
                dbName);

        processBuilder.redirectOutput(new File(backupFile));
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("mysqldump failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to execute SQL backup", e);
        }
    }

    // --- GENERIC SHEET CREATOR (with optional format hints) ---
    private <T> void createSheet(Workbook workbook, String sheetName, List<T> data,
            Function<T, Map<String, Object>> mapper) {
        createSheet(workbook, sheetName, data, mapper, null);
    }

    private <T> void createSheet(Workbook workbook, String sheetName, List<T> data,
            Function<T, Map<String, Object>> mapper, Map<String, ColumnType> columnTypes) {
        if (data.isEmpty())
            return;

        Sheet sheet = workbook.createSheet(sheetName);
        Row headerRow = sheet.createRow(0);
        List<String> headers = new ArrayList<>(mapper.apply(data.get(0)).keySet());

        for (int i = 0; i < headers.size(); i++) {
            headerRow.createCell(i).setCellValue(headers.get(i));
        }

        // Determine column types
        Set<Integer> dateColumns = new HashSet<>();
        Set<Integer> datetimeColumns = new HashSet<>();

        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            ColumnType type = (columnTypes != null) ? columnTypes.get(header) : null;

            if (type != null) {
                if (type == ColumnType.DATE)
                    dateColumns.add(i);
                else if (type == ColumnType.DATETIME)
                    datetimeColumns.add(i);
            } else {
                // Fallback to auto-detection
                String lower = header.toLowerCase();
                if (lower.contains("date") && !lower.contains("time")) {
                    dateColumns.add(i);
                } else if (lower.contains("time")) {
                    datetimeColumns.add(i);
                }
            }
        }

        // Create styles
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd-mm-yyyy"));

        CellStyle datetimeStyle = workbook.createCellStyle();
        datetimeStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd-mm-yyyy hh:mm:ss"));

        // Data rows
        for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            Map<String, Object> values = mapper.apply(data.get(rowIndex));
            int colIndex = 0;

            for (Object value : values.values()) {
                Cell cell = row.createCell(colIndex);
                if (value != null) {
                    if (value instanceof LocalDate) {
                        cell.setCellValue(
                                Date.from(((LocalDate) value).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                        if (dateColumns.contains(colIndex))
                            cell.setCellStyle(dateStyle);
                    } else if (value instanceof LocalDateTime) {
                        cell.setCellValue(
                                Date.from(((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant()));
                        if (datetimeColumns.contains(colIndex))
                            cell.setCellStyle(datetimeStyle);
                    } else if (value instanceof LocalTime) {
                        cell.setCellValue(((LocalTime) value).toSecondOfDay() / (24.0 * 3600));
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value);
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
                colIndex++;
            }
        }
    }

    // Enum for column type hints
    private enum ColumnType {
        DATE,
        DATETIME
    }

    public BackupHistoryDTO mapToHistoryDTO(BackupHistory entity) {
        if (entity == null) {
            return null;
        }

        return new BackupHistoryDTO(
                entity.getId(),
                entity.getBackupDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                entity.getBackupTypes(),
                mapStatusForDisplay(entity.getStatus()),
                mapMethodForDisplay(entity.getMethod()),
                entity.getFileSize(),
                entity.getLocation());
    }

    /**
     * Converts list of BackupHistory entities to DTOs
     */
    public List<BackupHistoryDTO> mapToHistoryDTOList(List<BackupHistory> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(this::mapToHistoryDTO)
                .collect(Collectors.toList());
    }

    // ======================
    // HELPER METHODS
    // ======================

    private String mapStatusForDisplay(String status) {
        return "SUCCESS".equals(status) ? "SUCCESS" : "FAILED";
    }

    private String mapMethodForDisplay(String method) {
        return "AUTOMATIC".equals(method) ? "AUTOMATIC" : "MANUAL";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    // ===================================================================
    // MAPPING FUNCTIONS (readable, at the bottom)
    // ===================================================================

    private BackupConfigDTO mapToDTO(BackupConfig entity) {
        BackupConfigDTO dto = new BackupConfigDTO();
        dto.setIntervalDays(entity.getIntervalDays() != null ? entity.getIntervalDays() : 1);
        dto.setBackupTime(entity.getBackupTime() != null
                ? entity.getBackupTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                : "");
        dto.setStartDate(entity.getStartDate() != null
                ? entity.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : "");
        dto.setBackupFolder(entity.getBackupFolder() != null ? entity.getBackupFolder() : "");
        dto.setBackupTypes(entity.getBackupTypes() != null ? entity.getBackupTypes() : "");
        return dto;
    }

    private Map<String, Object> mapUser(Object userObj) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (userObj instanceof User) {
            User user = (User) userObj;
            map.put("ID", user.getId());
            map.put("Name", user.getName());
            map.put("Employee ID", user.getEmployeeId());
            map.put("Email", user.getEmail());
            map.put("Status", user.getStatus() != null ? user.getStatus().name() : "");
            map.put("Designation", user.getDesignation());
            map.put("Nationality", user.getNationality());
            map.put("Phone Number", user.getPhoneNumber());
            map.put("Join Date", user.getJoinDate());
            map.put("Created At", user.getCreatedAt());
            map.put("Activated At", user.getActivatedAt());

            String roleNames = user.getRoles() != null
                    ? user.getRoles().stream()
                            .map(role -> role.getName().name()) // Assuming Role.getName() returns enum, then .name()
                                                                // gives string
                            .collect(Collectors.joining(", "))
                    : "";
            map.put("Roles", roleNames);

        } else if (userObj instanceof UserDTO) {
            UserDTO user = (UserDTO) userObj;
            map.put("ID", user.getId());
            map.put("Name", user.getName());
            map.put("Employee ID", user.getEmployeeId());
            map.put("Email", user.getEmail());
            map.put("Status", user.getStatus() != null ? user.getStatus().name() : "");
            map.put("Designation", user.getDesignation());
            map.put("Nationality", user.getNationality());
            map.put("Phone Number", user.getPhoneNumber());
            map.put("Join Date", user.getJoinDate());
            map.put("Created At", user.getCreatedAt());
            map.put("Activated At", user.getActivatedAt());
            map.put("Roles", user.getRoleNames() != null ? String.join(", ", user.getRoleNames()) : "");

        } else {
            throw new IllegalArgumentException("Expected User or UserDTO instance");
        }

        return map;
    }

    private Map<String, Object> mapArea(Object areaObj) {
        Area area;
        if (areaObj instanceof Area) {
            area = (Area) areaObj;
        } else if (areaObj instanceof AreaDTO) {
            AreaDTO dto = (AreaDTO) areaObj;
            area = new Area();
            area.setId(dto.getId());
            area.setCode(dto.getCode());
            area.setName(dto.getName());
            area.setStatus(dto.getStatus());
            area.setDescription(dto.getDescription());
            if (dto.getResponsiblePerson() != null) {
                User rp = new User();
                rp.setName(dto.getResponsiblePerson().getName());
                rp.setEmployeeId(dto.getResponsiblePerson().getEmployeeId());
                area.setResponsiblePerson(rp);
            }
        } else {
            throw new IllegalArgumentException("Expected Area or AreaDTO instance");
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ID", area.getId());
        map.put("Code", area.getCode());
        map.put("Name", area.getName());
        map.put("Status", area.getStatus() != null ? area.getStatus().name() : "");
        map.put("Description", area.getDescription());
        map.put("Responsible Person Name",
                (area.getResponsiblePerson() != null) ? area.getResponsiblePerson().getName() : "");
        map.put("Responsible Person ID",
                (area.getResponsiblePerson() != null) ? area.getResponsiblePerson().getEmployeeId() : "");

        return map;
    }

    private Map<String, Object> mapEquipment(Object equipmentObj) {
        Equipment equipment;
        if (equipmentObj instanceof Equipment) {
            equipment = (Equipment) equipmentObj;
        } else if (equipmentObj instanceof EquipmentDTO) {
            EquipmentDTO dto = (EquipmentDTO) equipmentObj;
            equipment = new Equipment();
            equipment.setId(dto.getId());
            equipment.setCode(dto.getCode());
            equipment.setName(dto.getName());
            equipment.setModel(dto.getModel());
            equipment.setUnit(dto.getUnit());
            equipment.setQty(dto.getQty());
            equipment.setManufacturer(dto.getManufacturer());
            equipment.setSerialNo(dto.getSerialNo());
            equipment.setManufacturedDate(dto.getManufacturedDate());
            equipment.setCommissionedDate(dto.getCommissionedDate());
            equipment.setCapacity(dto.getCapacity());
            equipment.setRemarks(dto.getRemarks());
            equipment.setImage(dto.getImage());
        } else {
            throw new IllegalArgumentException("Expected Equipment or EquipmentDTO instance");
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ID", equipment.getId());
        map.put("Code", equipment.getCode());
        map.put("Name", equipment.getName());
        map.put("Model", equipment.getModel());
        map.put("Unit", equipment.getUnit());
        map.put("Quantity", equipment.getQty());
        map.put("Manufacturer", equipment.getManufacturer());
        map.put("Serial No", equipment.getSerialNo());
        map.put("Manufactured Date", equipment.getManufacturedDate());
        map.put("Commissioned Date", equipment.getCommissionedDate());
        map.put("Capacity", equipment.getCapacity());
        map.put("Remarks", equipment.getRemarks());
        map.put("Image", equipment.getImage());

        return map;
    }

    private Map<String, Object> mapPart(Object partObj) {
        Part part;
        if (partObj instanceof Part) {
            part = (Part) partObj;
        } else if (partObj instanceof PartDTO) {
            PartDTO dto = (PartDTO) partObj;
            part = new Part();
            part.setId(dto.getId());
            part.setCode(dto.getCode());
            part.setName(dto.getName());
            part.setDescription(dto.getDescription());
            part.setCategory(dto.getCategory());
            part.setSupplier(dto.getSupplier());
            part.setImage(dto.getImage());
            part.setStockQuantity(dto.getStockQuantity());
        } else {
            throw new IllegalArgumentException("Expected Part or PartDTO instance");
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ID", part.getId());
        map.put("Code", part.getCode());
        map.put("Name", part.getName());
        map.put("Description", part.getDescription());
        map.put("Category", part.getCategory());
        map.put("Supplier", part.getSupplier());
        map.put("Image", part.getImage());
        map.put("Stock Quantity", part.getStockQuantity());

        return map;
    }

    private Map<String, Object> mapComplaint(Object complaintObj) {
        if (!(complaintObj instanceof Complaint)) {
            throw new IllegalArgumentException("Expected Complaint instance");
        }
        Complaint complaint = (Complaint) complaintObj;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ID", complaint.getId());
        map.put("Code", complaint.getCode());
        map.put("Subject", complaint.getSubject());
        map.put("Description", complaint.getDescription());
        map.put("Category", complaint.getCategory());
        map.put("Priority", complaint.getPriority());

        // ✅ Keep as LocalDate (not string)
        map.put("Report Date", complaint.getReportDate()); // LocalDate

        // ✅ Keep as LocalDateTime (not string)
        map.put("Updated At", complaint.getUpdatedAt()); // LocalDateTime
        map.put("Status", complaint.getStatus());
        map.put("Action Taken", complaint.getActionTaken());
        map.put("Image Before", complaint.getImageBefore());
        map.put("Image After", complaint.getImageAfter());

        // ✅ Keep as LocalDateTime
        map.put("Close Time", complaint.getCloseTime()); // LocalDateTime
        map.put("Total Time Minutes", complaint.getTotalTimeMinutes());

        // Area
        map.put("Area Name", (complaint.getArea() != null) ? complaint.getArea().getName() : "");
        map.put("Area Code", (complaint.getArea() != null) ? complaint.getArea().getCode() : "");

        // Equipment
        map.put("Equipment Name", (complaint.getEquipment() != null) ? complaint.getEquipment().getName() : "");
        map.put("Equipment Code", (complaint.getEquipment() != null) ? complaint.getEquipment().getCode() : "");

        // Reporter
        map.put("Reporter Name", (complaint.getReporter() != null) ? complaint.getReporter().getName() : "");
        map.put("Reporter ID", (complaint.getReporter() != null) ? complaint.getReporter().getEmployeeId() : "");

        // Assignee
        map.put("Assignee Name", (complaint.getAssignee() != null) ? complaint.getAssignee().getName() : "");
        map.put("Assignee ID", (complaint.getAssignee() != null) ? complaint.getAssignee().getEmployeeId() : "");

        // Parts
        String partsNames = complaint.getPartsUsed().stream()
                .map(cp -> cp.getPart().getName())
                .sorted()
                .collect(Collectors.joining("; "));
        String partsCodes = complaint.getPartsUsed().stream()
                .map(cp -> cp.getPart().getCode())
                .sorted()
                .collect(Collectors.joining(", "));
        String partsQuantities = complaint.getPartsUsed().stream()
                .map(cp -> String.valueOf(cp.getQuantity()))
                .collect(Collectors.joining(", "));

        map.put("Parts Used", partsNames);
        map.put("Part Codes", partsCodes);
        map.put("Part Quantities", partsQuantities);

        return map;
    }

    private Map<String, Object> mapWorkReport(Object workReportObj) {
        if (!(workReportObj instanceof WorkReport)) {
            throw new IllegalArgumentException("Expected WorkReport instance");
        }
        WorkReport workReport = (WorkReport) workReportObj;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ID", workReport.getId());
        map.put("Code", workReport.getCode());

        // ✅ Keep as LocalDate
        map.put("Report Date", workReport.getReportDate()); // LocalDate

        map.put("Shift", workReport.getShift());

        // Area & Equipment
        map.put("Area Name", (workReport.getArea() != null) ? workReport.getArea().getName() : "");
        map.put("Area Code", (workReport.getArea() != null) ? workReport.getArea().getCode() : "");
        map.put("Equipment Name", (workReport.getEquipment() != null) ? workReport.getEquipment().getName() : "");
        map.put("Equipment Code", (workReport.getEquipment() != null) ? workReport.getEquipment().getCode() : "");

        map.put("Category", workReport.getCategory());
        map.put("Problem", workReport.getProblem());
        map.put("Solution", workReport.getSolution());

        // ✅ Keep as LocalTime or LocalDateTime (depending on your entity)
        map.put("Start Time", workReport.getStartTime()); // LocalTime or LocalDateTime
        map.put("Stop Time", workReport.getStopTime()); // LocalTime or LocalDateTime

        map.put("Total Time Minutes", workReport.getTotalTimeMinutes());

        // Technicians
        String techniciansNames = workReport.getTechnicians().stream()
                .map(User::getName)
                .sorted()
                .collect(Collectors.joining(","));
        String techniciansIds = workReport.getTechnicians().stream()
                .map(User::getEmployeeId)
                .sorted()
                .collect(Collectors.joining(","));
        map.put("Technicians", techniciansNames);
        map.put("Technician IDs", techniciansIds);

        // Supervisor
        map.put("Supervisor Name", (workReport.getSupervisor() != null) ? workReport.getSupervisor().getName() : "");
        map.put("Supervisor ID",
                (workReport.getSupervisor() != null) ? workReport.getSupervisor().getEmployeeId() : "");

        map.put("Status", workReport.getStatus());
        map.put("Scope", workReport.getScope());
        map.put("Work Type", workReport.getWorkType());
        map.put("Remark", workReport.getRemark());

        // Parts
        String partsNames = workReport.getPartsUsed().stream()
                .map(cp -> cp.getPart().getName())
                .sorted()
                .collect(Collectors.joining("; "));
        String partsCodes = workReport.getPartsUsed().stream()
                .map(cp -> cp.getPart().getCode())
                .sorted()
                .collect(Collectors.joining(", "));
        String partsQuantities = workReport.getPartsUsed().stream()
                .map(cp -> String.valueOf(cp.getQuantity()))
                .collect(Collectors.joining(", "));

        map.put("Parts Used", partsNames);
        map.put("Part Codes", partsCodes);
        map.put("Part Quantities", partsQuantities);

        return map;
    }

    // ===================================================================
    // REFLECTION HELPERS (safe and simple)
    // ===================================================================

    private Object getField(Object entity, String fieldName) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(entity);
            return value != null ? value : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getNestedField(Object entity, String parentField, String childField) {
        try {
            java.lang.reflect.Field parent = entity.getClass().getDeclaredField(parentField);
            parent.setAccessible(true);
            Object parentObj = parent.get(entity);
            if (parentObj == null)
                return "";
            java.lang.reflect.Field child = parentObj.getClass().getDeclaredField(childField);
            child.setAccessible(true);
            Object value = child.get(parentObj);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getRoles(Object user) {
        try {
            java.lang.reflect.Field rolesField = user.getClass().getDeclaredField("roles");
            rolesField.setAccessible(true);
            Set<?> roles = (Set<?>) rolesField.get(user);
            if (roles == null || roles.isEmpty())
                return "";
            return roles.stream()
                    .map(role -> {
                        try {
                            java.lang.reflect.Field nameField = role.getClass().getDeclaredField("name");
                            nameField.setAccessible(true);
                            return nameField.get(role).toString();
                        } catch (Exception e) {
                            return role.toString();
                        }
                    })
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            return "";
        }
    }

    private String getTechnicians(Object workReport) {
        try {
            java.lang.reflect.Field techsField = workReport.getClass().getDeclaredField("technicians");
            techsField.setAccessible(true);
            List<?> techs = (List<?>) techsField.get(workReport);
            if (techs == null || techs.isEmpty())
                return "";
            return techs.stream()
                    .map(tech -> {
                        try {
                            String name = getField(tech, "name").toString();
                            String empId = getField(tech, "employeeId").toString();
                            return name + " (" + empId + ")";
                        } catch (Exception e) {
                            return tech.toString();
                        }
                    })
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            return "";
        }
    }
}