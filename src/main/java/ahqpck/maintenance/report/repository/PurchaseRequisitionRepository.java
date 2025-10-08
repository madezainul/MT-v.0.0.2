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

    // Find by status
    List<PurchaseRequisition> findByStatus(PRStatus status);
    Page<PurchaseRequisition> findByStatus(PRStatus status, Pageable pageable);

    // Find by requestor
    @Query("SELECT pr FROM PurchaseRequisition pr JOIN pr.requestor u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :requestorName, '%'))")
    List<PurchaseRequisition> findByRequestorNameContainingIgnoreCase(@Param("requestorName") String requestorName);
    
    @Query("SELECT pr FROM PurchaseRequisition pr JOIN pr.requestor u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :requestorName, '%'))")
    Page<PurchaseRequisition> findByRequestorNameContainingIgnoreCase(@Param("requestorName") String requestorName, Pageable pageable);

    // Find by approval status
    List<PurchaseRequisition> findByIsApproved(Boolean isApproved);
    Page<PurchaseRequisition> findByIsApproved(Boolean isApproved, Pageable pageable);

    // Find pending approval
    @Query("SELECT pr FROM PurchaseRequisition pr WHERE pr.status = 'SUBMITTED' AND pr.isApproved IS NULL")
    List<PurchaseRequisition> findPendingApproval();

    @Query("SELECT pr FROM PurchaseRequisition pr WHERE pr.status = 'SUBMITTED' AND pr.isApproved IS NULL")
    Page<PurchaseRequisition> findPendingApproval(Pageable pageable);

    // Find by date range
    @Query("SELECT pr FROM PurchaseRequisition pr WHERE pr.dateNeeded BETWEEN :startDate AND :endDate")
    List<PurchaseRequisition> findByDateNeededBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Find by target equipment
    List<PurchaseRequisition> findByTargetEquipmentId(String targetEquipmentId);

    // Count by status
    long countByStatus(PRStatus status);

    // Count pending approval
    @Query("SELECT COUNT(pr) FROM PurchaseRequisition pr WHERE pr.status = 'SUBMITTED' AND pr.isApproved IS NULL")
    long countPendingApproval();

    // Count requiring PO number
    @Query("SELECT COUNT(pr) FROM PurchaseRequisition pr WHERE pr.status = 'SENT_TO_PURCHASE' AND (pr.poNumber IS NULL OR pr.poNumber = '')")
    long countRequiringPONumber();

    // Count ready for completion
    @Query("SELECT COUNT(pr) FROM PurchaseRequisition pr WHERE pr.status = 'SENT_TO_PURCHASE' AND pr.poNumber IS NOT NULL AND pr.poNumber != ''")
    long countReadyForCompletion();

    // Find recent submissions
    @Query("SELECT pr FROM PurchaseRequisition pr WHERE pr.createdAt >= :since ORDER BY pr.createdAt DESC")
    List<PurchaseRequisition> findRecentSubmissions(@Param("since") LocalDateTime since);

    // Search across multiple fields
    @Query("SELECT pr FROM PurchaseRequisition pr JOIN pr.requestor u WHERE " +
           "LOWER(pr.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(pr.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.employeeId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(pr.targetEquipmentName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<PurchaseRequisition> searchPurchaseRequisitions(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Find by title containing
    Page<PurchaseRequisition> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // Find all ordered by created date desc
    Page<PurchaseRequisition> findAllByOrderByCreatedAtDesc(Pageable pageable);
}