package ahqpck.maintenance.report.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ahqpck.maintenance.report.entity.PurchaseRequisitionPart;
import ahqpck.maintenance.report.entity.PurchaseRequisitionPart.PRPartStatus;

@Repository
public interface PurchaseRequisitionPartRepository extends JpaRepository<PurchaseRequisitionPart, String> {
    
    // Find by purchase requisition
    List<PurchaseRequisitionPart> findByPurchaseRequisitionId(String purchaseRequisitionId);

    // Find by part
    List<PurchaseRequisitionPart> findByPartId(String partId);

    // Find by status
    List<PurchaseRequisitionPart> findByStatus(PRPartStatus status);

    // Find by PO number
    List<PurchaseRequisitionPart> findByQuotationNumber(String quotationNumber);

    // Find parts pending for PO creation (approved PRs, pending parts)
    @Query("SELECT prp FROM PurchaseRequisitionPart prp " +
           "JOIN prp.purchaseRequisition pr " +
           "WHERE pr.status = 'APPROVED' AND pr.isApproved = true AND prp.status = 'PENDING'")
    List<PurchaseRequisitionPart> findPartsReadyForPO();

    // Find parts ready for PO creation grouped by supplier
    @Query("SELECT prp FROM PurchaseRequisitionPart prp " +
           "JOIN prp.purchaseRequisition pr " +
           "JOIN prp.part p " +
           "WHERE pr.status = 'APPROVED' AND pr.isApproved = true AND prp.status = 'PENDING' " +
           "AND p.supplierName = :supplierName " +
           "ORDER BY p.name")
    List<PurchaseRequisitionPart> findPartsReadyForPOBySupplier(@Param("supplierName") String supplierName);

    // Get suppliers with pending parts for PO creation
    @Query("SELECT DISTINCT p.supplierName FROM PurchaseRequisitionPart prp " +
           "JOIN prp.purchaseRequisition pr " +
           "JOIN prp.part p " +
           "WHERE pr.status = 'APPROVED' AND pr.isApproved = true AND prp.status = 'PENDING' " +
           "ORDER BY p.supplierName")
    List<String> findSuppliersWithPendingParts();

    // Find parts that can be received
    @Query("SELECT prp FROM PurchaseRequisitionPart prp " +
           "WHERE prp.status = 'ORDERED' AND prp.quantityOrdered > prp.quantityReceived")
    List<PurchaseRequisitionPart> findPartsCanBeReceived();

    // Find parts by supplier and status
    @Query("SELECT prp FROM PurchaseRequisitionPart prp " +
           "JOIN prp.part p " +
           "WHERE p.supplierName = :supplierName AND prp.status = :status")
    List<PurchaseRequisitionPart> findBySupplierAndStatus(@Param("supplierName") String supplierName, 
                                                           @Param("status") PRPartStatus status);

    // Count by status
    long countByStatus(PRPartStatus status);

    // Count parts ready for PO
    @Query("SELECT COUNT(prp) FROM PurchaseRequisitionPart prp " +
           "JOIN prp.purchaseRequisition pr " +
           "WHERE pr.status = 'APPROVED' AND pr.isApproved = true AND prp.status = 'PENDING'")
    long countPartsReadyForPO();
}