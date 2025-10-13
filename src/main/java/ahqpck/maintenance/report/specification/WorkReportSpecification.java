package ahqpck.maintenance.report.specification;

import ahqpck.maintenance.report.entity.WorkReport;
import ahqpck.maintenance.report.entity.Area;
import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;

public class WorkReportSpecification {

    public static Specification<WorkReport> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            Join<WorkReport, Equipment> equipment = root.join("equipment", JoinType.LEFT);
            Join<WorkReport, Area> area = root.join("area", JoinType.LEFT);
            Join<WorkReport, User> supervisor = root.join("supervisor", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("problem")), pattern),
                    cb.like(cb.lower(root.get("solution")), pattern),
                    cb.like(cb.lower(root.get("workType")), pattern),
                    cb.like(cb.lower(root.get("remark")), pattern),
                    cb.like(cb.lower(root.get("shift").as(String.class)), pattern),
                    cb.like(cb.lower(root.get("category").as(String.class)), pattern),
                    cb.like(cb.lower(root.get("status").as(String.class)), pattern),
                    cb.like(cb.lower(root.get("scope").as(String.class)), pattern),

                    // Search in Equipment (via join)
                    cb.like(cb.lower(cb.coalesce(equipment.<String>get("name"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(equipment.<String>get("code"), "")), pattern),

                    // Search in Area (via join)
                    cb.like(cb.lower(cb.coalesce(area.<String>get("name"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(area.<String>get("code"), "")), pattern),

                    // Search in Supervisor
                    cb.like(cb.lower(cb.coalesce(supervisor.<String>get("name"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(supervisor.<String>get("employeeId"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(supervisor.<String>get("email"), "")), pattern));
        };
    }

    public static Specification<WorkReport> withReportDateRange(LocalDateTime from, LocalDateTime to) {
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

    public static Specification<WorkReport> withStatus(WorkReport.Status status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<WorkReport> withCategory(WorkReport.Category category) {
        return (root, query, cb) -> {
            if (category == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("category"), category);
        };
    }

    public static Specification<WorkReport> withScope(WorkReport.Scope scope) {
        return (root, query, cb) -> {
            if (scope == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("scope"), scope);
        };
    }

    public static Specification<WorkReport> withEquipment(String equipmentCode) {
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

            Join<WorkReport, Equipment> equipment = root.join("equipment", JoinType.LEFT);
            return equipment.get("code").in(codes);
        };
    }
}