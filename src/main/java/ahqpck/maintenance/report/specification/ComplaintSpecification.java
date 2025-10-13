package ahqpck.maintenance.report.specification;

import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.Area;
import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.entity.WorkReport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;

public class ComplaintSpecification {

    public static Specification<Complaint> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            // Use LEFT JOIN
            Join<Complaint, User> reporter = root.join("reporter", JoinType.LEFT);
            Join<Complaint, User> assignee = root.join("assignee", JoinType.LEFT);
            Join<Complaint, Area> area = root.join("area", JoinType.LEFT);
            Join<Complaint, Equipment> equipment = root.join("equipment", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("subject")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("actionTaken")), pattern),
                    cb.like(cb.lower(root.get("priority").as(String.class)), pattern),
                    cb.like(cb.lower(root.get("category").as(String.class)), pattern),
                    cb.like(cb.lower(root.get("status").as(String.class)), pattern),

                    // Use coalesce to handle nulls in joined fields
                    cb.like(cb.lower(cb.coalesce(reporter.<String>get("name"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(reporter.<String>get("employeeId"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(reporter.<String>get("email"), "")), pattern),

                    cb.like(cb.lower(cb.coalesce(assignee.<String>get("name"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(assignee.<String>get("employeeId"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(assignee.<String>get("email"), "")), pattern),

                    cb.like(cb.lower(cb.coalesce(area.<String>get("name"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(area.<String>get("code"), "")), pattern),

                    cb.like(cb.lower(cb.coalesce(equipment.<String>get("name"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(equipment.<String>get("code"), "")), pattern));
        };
    }

    public static Specification<Complaint> withReportDateRange(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("reportDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("reportDate"), to));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Complaint> withCloseTime(LocalDateTime closeTime) {
        return (root, query, cb) -> {
            if (closeTime == null) {
                return cb.conjunction(); // no filter
            }

            // Extract the date part
            LocalDate date = closeTime.toLocalDate();
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

            return cb.between(root.get("closeTime"), startOfDay, endOfDay);
        };
    }

    public static Specification<Complaint> withAssignee(String assigneeId) {
        return (root, query, cb) -> {
            if (assigneeId == null || assigneeId.trim().isEmpty()) {
                return cb.conjunction();
            }
            Join<Complaint, User> assignee = root.join("assignee", JoinType.LEFT);
            return cb.equal(assignee.get("employeeId"), assigneeId);
        };
    }

    public static Specification<Complaint> withStatus(Complaint.Status status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Complaint> withCategory(Complaint.Category category) {
        return (root, query, cb) -> {
            if (category == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("category"), category);
        };
    }

    public static Specification<Complaint> withEquipment(String equipmentCode) {
        return (root, query, cb) -> {
            if (equipmentCode == null || equipmentCode.trim().isEmpty()) {
                return cb.conjunction();
            }

            // Split, trim, and filter
            List<String> codes = Arrays.stream(equipmentCode.split(","))
                    .map(String::trim)
                    .filter(code -> !code.isEmpty())
                    .collect(Collectors.toList());

            if (codes.isEmpty()) {
                return cb.conjunction();
            }

            Join<Complaint, Equipment> equipment = root.join("equipment", JoinType.LEFT);
            return equipment.get("code").in(codes);
        };
    }
}