package ahqpck.maintenance.report.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ahqpck.maintenance.report.entity.QuotationRequestPart;

@Repository
public interface QuotationRequestPartRepository extends JpaRepository<QuotationRequestPart, String> {
    
    // Find by quotation request
    List<QuotationRequestPart> findByQuotationRequestId(String quotationRequestId);

    // Find by part
    List<QuotationRequestPart> findByPartId(String partId);

    // Find by quotation request and part
    @Query("SELECT qrp FROM QuotationRequestPart qrp WHERE qrp.quotationRequest.id = :quotationRequestId AND qrp.part.id = :partId")
    List<QuotationRequestPart> findByQuotationRequestIdAndPartId(@Param("quotationRequestId") String quotationRequestId, 
                                                               @Param("partId") String partId);

    // Find parts that can be received
    @Query("SELECT qrp FROM QuotationRequestPart qrp WHERE qrp.quantityReceived < qrp.quantityRequested")
    List<QuotationRequestPart> findPartsCanBeReceived();

    // Find parts by quotation request that can be received
    @Query("SELECT qrp FROM QuotationRequestPart qrp WHERE qrp.quotationRequest.id = :quotationRequestId AND qrp.quantityReceived < qrp.quantityRequested")
    List<QuotationRequestPart> findPartsCanBeReceivedByQR(@Param("quotationRequestId") String quotationRequestId);

    // Find fully received parts
    @Query("SELECT qrp FROM QuotationRequestPart qrp WHERE qrp.quantityReceived >= qrp.quantityRequested")
    List<QuotationRequestPart> findFullyReceivedParts();

    // Find partially received parts
    @Query("SELECT qrp FROM QuotationRequestPart qrp WHERE qrp.quantityReceived > 0 AND qrp.quantityReceived < qrp.quantityRequested")
    List<QuotationRequestPart> findPartiallyReceivedParts();

    // Count parts by receive status
    @Query("SELECT COUNT(qrp) FROM QuotationRequestPart qrp WHERE qrp.quantityReceived < qrp.quantityRequested")
    long countPartsCanBeReceived();

    @Query("SELECT COUNT(qrp) FROM QuotationRequestPart qrp WHERE qrp.quantityReceived >= qrp.quantityRequested")
    long countFullyReceivedParts();

    @Query("SELECT COUNT(qrp) FROM QuotationRequestPart qrp WHERE qrp.quantityReceived > 0 AND qrp.quantityReceived < qrp.quantityRequested")
    long countPartiallyReceivedParts();
}