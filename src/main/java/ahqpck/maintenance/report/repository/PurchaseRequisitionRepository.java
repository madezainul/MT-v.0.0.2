package ahqpck.maintenance.report.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ahqpck.maintenance.report.entity.PurchaseRequisition;
import ahqpck.maintenance.report.entity.PurchaseRequisition.PRStatus;

@Repository
public interface PurchaseRequisitionRepository extends JpaRepository<PurchaseRequisition, String>, JpaSpecificationExecutor<PurchaseRequisition> {
    
    // Find by code
    Optional<PurchaseRequisition> findByCode(String code);
    boolean existsByCode(String code);

    // Find by status
    List<PurchaseRequisition> findByStatus(PRStatus status);
    Page<PurchaseRequisition> findByStatus(PRStatus status, Pageable pageable);

    // Find by approval status
    List<PurchaseRequisition> findByIsApproved(Boolean isApproved);
    Page<PurchaseRequisition> findByIsApproved(Boolean isApproved, Pageable pageable);

    // Find pending approval (submitted but not reviewed)
    @Query("SELECT pr FROM PurchaseRequisition pr WHERE pr.status = 'SUBMITTED' AND pr.isApproved IS NULL")
    List<PurchaseRequisition> findPendingApproval();

    @Query("SELECT pr FROM PurchaseRequisition pr WHERE pr.status = 'SUBMITTED' AND pr.isApproved IS NULL")
    Page<PurchaseRequisition> findPendingApproval(Pageable pageable);

    // Find PRs requiring action (pending approval + rejected PRs)
    @Query("SELECT pr FROM PurchaseRequisition pr WHERE (pr.status = 'SUBMITTED' AND pr.isApproved IS NULL) OR (pr.status = 'SUBMITTED' AND pr.isApproved = false)")
    List<PurchaseRequisition> findActionRequired();

    @Query("SELECT pr FROM PurchaseRequisition pr WHERE (pr.status = 'SUBMITTED' AND pr.isApproved IS NULL) OR (pr.status = 'SUBMITTED' AND pr.isApproved = false)")
    Page<PurchaseRequisition> findActionRequired(Pageable pageable);

    // Find approved PRs that can create POs
    @Query("SELECT pr FROM PurchaseRequisition pr WHERE pr.status = 'APPROVED' AND pr.isApproved = true")
    List<PurchaseRequisition> findReadyForPO();

    @Query("SELECT pr FROM PurchaseRequisition pr WHERE pr.status = 'APPROVED' AND pr.isApproved = true")
    Page<PurchaseRequisition> findReadyForPO(Pageable pageable);

    // Find by requestor
    @Query("SELECT pr FROM PurchaseRequisition pr WHERE pr.requestor.id = :requestorId")
    List<PurchaseRequisition> findByRequestorId(@Param("requestorId") String requestorId);

    @Query("SELECT pr FROM PurchaseRequisition pr WHERE pr.requestor.id = :requestorId")
    Page<PurchaseRequisition> findByRequestorId(@Param("requestorId") String requestorId, Pageable pageable);

    // Find by date range
    @Query("SELECT pr FROM PurchaseRequisition pr WHERE pr.dateNeeded BETWEEN :startDate AND :endDate")
    List<PurchaseRequisition> findByDateNeededBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Find by target equipment
    List<PurchaseRequisition> findByTargetEquipmentId(String targetEquipmentId);

    // Find recent submissions
    @Query("SELECT pr FROM PurchaseRequisition pr WHERE pr.createdAt >= :since ORDER BY pr.createdAt DESC")
    List<PurchaseRequisition> findRecentSubmissions(@Param("since") LocalDateTime since);

    // Count queries
    long countByStatus(PRStatus status);

    @Query("SELECT COUNT(pr) FROM PurchaseRequisition pr WHERE pr.status = 'SUBMITTED' AND pr.isApproved IS NULL")
    long countPendingApproval();

    @Query("SELECT COUNT(pr) FROM PurchaseRequisition pr WHERE (pr.status = 'SUBMITTED' AND pr.isApproved IS NULL) OR (pr.status = 'SUBMITTED' AND pr.isApproved = false)")
    long countActionRequired();

    @Query("SELECT COUNT(pr) FROM PurchaseRequisition pr WHERE pr.status = 'APPROVED' AND pr.isApproved = true")
    long countReadyForPO();

    // Search queries
    @Query("SELECT pr FROM PurchaseRequisition pr WHERE " +
           "LOWER(pr.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(pr.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(pr.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(pr.requestor.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<PurchaseRequisition> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}