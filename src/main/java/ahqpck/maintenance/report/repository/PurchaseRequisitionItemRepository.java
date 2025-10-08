package ahqpck.maintenance.report.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ahqpck.maintenance.report.entity.PurchaseRequisitionItem;

@Repository
public interface PurchaseRequisitionItemRepository extends JpaRepository<PurchaseRequisitionItem, String> {

    // Find by purchase requisition
    List<PurchaseRequisitionItem> findByPurchaseRequisitionId(String purchaseRequisitionId);

    // Find by part
    List<PurchaseRequisitionItem> findByPartId(String partId);

    // Find new parts (not linked to existing Part entity)
    List<PurchaseRequisitionItem> findByIsNewPartTrue();

    // Find received items
    List<PurchaseRequisitionItem> findByIsReceivedTrue();

    // Find pending receipt
    List<PurchaseRequisitionItem> findByIsReceivedFalse();

    // Find by part code (for new parts)
    List<PurchaseRequisitionItem> findByPartCode(String partCode);

    // Find by part name containing
    List<PurchaseRequisitionItem> findByPartNameContainingIgnoreCase(String partName);

    // Find by criticality level
    List<PurchaseRequisitionItem> findByCriticalityLevel(String criticalityLevel);

    // Count by purchase requisition
    long countByPurchaseRequisitionId(String purchaseRequisitionId);

    // Count new parts
    long countByIsNewPartTrue();

    // Count received items
    long countByIsReceivedTrue();

    // Count pending receipt
    long countByIsReceivedFalse();

    // Sum quantities by purchase requisition
    @Query("SELECT COALESCE(SUM(pri.quantity), 0) FROM PurchaseRequisitionItem pri WHERE pri.purchaseRequisition.id = :prId")
    long sumQuantitiesByPurchaseRequisitionId(@Param("prId") String purchaseRequisitionId);

    // Find items needing part code generation (new parts without codes)
    @Query("SELECT pri FROM PurchaseRequisitionItem pri WHERE pri.isNewPart = true AND (pri.partCode IS NULL OR pri.partCode = '')")
    List<PurchaseRequisitionItem> findNewPartsWithoutCodes();

    // Find items by PR status and receipt status
    @Query("SELECT pri FROM PurchaseRequisitionItem pri WHERE pri.purchaseRequisition.status = :prStatus AND pri.isReceived = :isReceived")
    List<PurchaseRequisitionItem> findByPRStatusAndReceiptStatus(@Param("prStatus") String prStatus, @Param("isReceived") Boolean isReceived);

    // Find items ready for receiving (PR approved, not yet received)
    @Query("SELECT pri FROM PurchaseRequisitionItem pri WHERE pri.purchaseRequisition.status IN ('APPROVED', 'SENT_TO_PURCHASE') AND pri.isReceived = false")
    List<PurchaseRequisitionItem> findItemsReadyForReceiving();

    // Count critical items pending receipt
    @Query("SELECT COUNT(pri) FROM PurchaseRequisitionItem pri WHERE pri.criticalityLevel = 'CRITICAL' AND pri.isReceived = false")
    long countCriticalItemsPendingReceipt();
}