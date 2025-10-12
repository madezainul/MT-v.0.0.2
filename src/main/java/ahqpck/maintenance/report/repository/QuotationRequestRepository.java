package ahqpck.maintenance.report.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ahqpck.maintenance.report.entity.QuotationRequest;
import ahqpck.maintenance.report.entity.QuotationRequest.QRStatus;

@Repository
public interface QuotationRequestRepository
              extends JpaRepository<QuotationRequest, String>, JpaSpecificationExecutor<QuotationRequest> {

       // Find by quotation number
       Optional<QuotationRequest> findByQuotationNumber(String quotationNumber);

       boolean existsByQuotationNumber(String quotationNumber);

       // Find by quotation number containing or supplier name containing
       // (case-insensitive)
       Page<QuotationRequest> findByQuotationNumberContainingIgnoreCaseOrSupplierNameContainingIgnoreCase(
                     String quotationNumber, String supplierName, Pageable pageable);

       // Find by status
       List<QuotationRequest> findByStatus(QRStatus status);

       Page<QuotationRequest> findByStatus(QRStatus status, Pageable pageable);

       // Find by supplier
       List<QuotationRequest> findBySupplierName(String supplierName);

       Page<QuotationRequest> findBySupplierName(String supplierName, Pageable pageable);

       // Find by supplier and status
       List<QuotationRequest> findBySupplierNameAndStatus(String supplierName, QRStatus status);

       // Find by created by
       @Query("SELECT qr FROM QuotationRequest qr WHERE qr.createdBy.id = :createdById")
       List<QuotationRequest> findByCreatedById(@Param("createdById") String createdById);

       @Query("SELECT qr FROM QuotationRequest qr WHERE qr.createdBy.id = :createdById")
       Page<QuotationRequest> findByCreatedById(@Param("createdById") String createdById, Pageable pageable);

       // Find by date range
       List<QuotationRequest> findByRequestDateBetween(LocalDate startDate, LocalDate endDate);

       // Find by expected delivery date
       List<QuotationRequest> findByExpectedDeliveryDate(LocalDate expectedDeliveryDate);

       @Query("SELECT qr FROM QuotationRequest qr WHERE qr.expectedDeliveryDate <= :date AND qr.status NOT IN ('DELIVERED', 'COMPLETED')")
       List<QuotationRequest> findOverdueRequests(@Param("date") LocalDate date);

       // Find requests that can be received
       @Query("SELECT qr FROM QuotationRequest qr WHERE qr.status IN ('SENT', 'CONFIRMED')")
       List<QuotationRequest> findRequestsCanBeReceived();

       @Query("SELECT qr FROM QuotationRequest qr WHERE qr.status IN ('SENT', 'CONFIRMED')")
       Page<QuotationRequest> findRequestsCanBeReceived(Pageable pageable);

       // Find requests ready for completion
       @Query("SELECT qr FROM QuotationRequest qr WHERE qr.status = 'DELIVERED' OR " +
                     "(qr.status = 'CONFIRMED' AND NOT EXISTS (SELECT qrp FROM QuotationRequestPart qrp WHERE qrp.quotationRequest = qr AND qrp.quantityReceived < qrp.quantityRequested))")
       List<QuotationRequest> findRequestsReadyForCompletion();

       // Count queries
       long countByStatus(QRStatus status);

       long countBySupplierName(String supplierName);

       @Query("SELECT COUNT(qr) FROM QuotationRequest qr WHERE qr.status IN ('SENT', 'CONFIRMED')")
       long countRequestsCanBeReceived();

       @Query("SELECT COUNT(qr) FROM QuotationRequest qr WHERE qr.expectedDeliveryDate <= :date AND qr.status NOT IN ('DELIVERED', 'COMPLETED')")
       long countOverdueRequests(@Param("date") LocalDate date);

       // Search queries
       @Query("SELECT qr FROM QuotationRequest qr WHERE " +
                     "LOWER(qr.quotationNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(qr.supplierName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(qr.notes) LIKE LOWER(CONCAT('%', :keyword, '%'))")
       Page<QuotationRequest> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

       // Supplier statistics
       @Query("SELECT qr.supplierName, COUNT(qr), SUM(qr.totalAmount) FROM QuotationRequest qr " +
                     "WHERE qr.requestDate BETWEEN :startDate AND :endDate " +
                     "GROUP BY qr.supplierName ORDER BY COUNT(qr) DESC")
       List<Object[]> getSupplierStatistics(@Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);
}