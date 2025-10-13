package ahqpck.maintenance.report.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

@Component
public class ImportUtil {

    public String toString(Object obj) {
        return obj != null ? obj.toString().trim() : null;
    }

    public Integer toInteger(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) return null;
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + obj);
        }
    }

    public Integer toDurationInMinutes(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) return null;

        if (obj instanceof Number) {
            double value = ((Number) obj).doubleValue();
            return value < 1.0 ? (int) Math.round(value * 24 * 60) : ((Number) obj).intValue();
        }

        String str = obj.toString().trim();
        if (str.matches("^\\d+(\\.\\d+)?$")) {
            double serial = Double.parseDouble(str);
            return serial < 1.0 ? (int) Math.round(serial * 24 * 60) : (int) serial;
        }

        if (str.matches("^\\d{1,2}:\\d{2}(:\\d{2})?$")) {
            String[] parts = str.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return (int) Math.round(hours * 60 + minutes + seconds / 60.0);
        }

        throw new IllegalArgumentException("Invalid duration: " + str);
    }

    public LocalDate toLocalDate(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) return null;

        if (obj instanceof Date) {
            return ((Date) obj).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }

        String str = obj.toString().trim();
        if (str.matches("\\d+(\\.\\d+)?")) {
            return convertExcelDate(Double.parseDouble(str));
        }

        return Stream.of(
                "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy", "MM-dd-yyyy",
                "MMM yyyy", "MMMM yyyy", "MM/yyyy", "M/yyyy", "yyyy-MM", "yyyy/MM")
                .map(pattern -> {
                    try {
                        DateTimeFormatter f = DateTimeFormatter.ofPattern(pattern);
                        var parsed = f.parse(str);
                        if (parsed.isSupported(ChronoField.YEAR) && parsed.isSupported(ChronoField.MONTH_OF_YEAR)) {
                            int year = parsed.get(ChronoField.YEAR);
                            int month = parsed.get(ChronoField.MONTH_OF_YEAR);
                            int day = parsed.isSupported(ChronoField.DAY_OF_MONTH) ? parsed.get(ChronoField.DAY_OF_MONTH) : 1;
                            return LocalDate.of(year, month, day);
                        }
                        return null;
                    } catch (Exception ignored) { return null; }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid date: " + str));
    }

    private static LocalDate convertExcelDate(double serial) {
        int n = (int) serial;
        if (n >= 60) n--;
        return LocalDate.of(1899, 12, 31).plusDays(n);
    }

    public LocalDateTime toLocalDateTime(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) return null;

        if (obj instanceof Date) {
            return ((Date) obj).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        }

        String str = obj.toString().trim();
        if (str.matches("\\d+(\\.\\d+)?")) {
            return convertExcelDateTime(Double.parseDouble(str));
        }

        return Stream.of(
                "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss",
                "dd/MM/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm:ss",
                "dd-MM-yyyy HH:mm:ss", "MM-dd-yyyy HH:mm:ss",
                "yyyy-MM-dd HH:mm", "dd/MM/yyyy HH:mm",
                "MM/dd/yyyy HH:mm", "dd-MM-yyyy HH:mm",
                "MM-dd-yyyy HH:mm", "yyyy-MM-dd h:mm a",
                "dd/MM/yyyy h:mm a", "MM/dd/yyyy h:mm a")
                .map(pattern -> {
                    try {
                        return LocalDateTime.parse(str, DateTimeFormatter.ofPattern(pattern));
                    } catch (DateTimeParseException ignored) { return null; }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid datetime: " + str));
    }

    private static LocalDateTime convertExcelDateTime(double serial) {
        int days = (int) serial;
        double fractionalDay = serial - days;
        if (days >= 60) days--;
        return LocalDateTime.of(1899, 12, 31, 0, 0).plusDays(days)
                .plusNanos((long) (fractionalDay * 86400_000_000_000L));
    }

    public static class ImportResult {
        private final int importedCount;
        private final List<String> errorMessages;

        public ImportResult(int importedCount, List<String> errorMessages) {
            this.importedCount = importedCount;
            this.errorMessages = List.copyOf(errorMessages);
        }

        public int getImportedCount() { return importedCount; }
        public List<String> getErrorMessages() { return errorMessages; }
        public boolean hasErrors() { return !errorMessages.isEmpty(); }
    }
}