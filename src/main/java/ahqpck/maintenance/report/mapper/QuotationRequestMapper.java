package ahqpck.maintenance.report.mapper;

import ahqpck.maintenance.report.dto.QuotationRequestDTO;
import ahqpck.maintenance.report.dto.QuotationRequestPartDTO;
import ahqpck.maintenance.report.entity.QuotationRequest;
import ahqpck.maintenance.report.entity.QuotationRequestPart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QuotationRequestMapper {

    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", source = "createdByName")
    @Mapping(target = "createdByEmail", source = "createdByEmail")
    @Mapping(target = "updatedById", source = "updatedBy.id")
    @Mapping(target = "updatedByName", source = "updatedByName")
    @Mapping(target = "updatedByEmail", source = "updatedByEmail")
    @Mapping(target = "purchaseRequisitionId", source = "purchaseRequisition.id")
    @Mapping(target = "purchaseRequisitionCode", source = "purchaseRequisition.code")
    @Mapping(target = "purchaseRequisitionTitle", source = "purchaseRequisition.title")
    @Mapping(target = "parts", source = "requestParts")
    @Mapping(target = "canBeSent", ignore = true)
    @Mapping(target = "canBeReceived", ignore = true)
    @Mapping(target = "canBeCompleted", ignore = true)
    @Mapping(target = "isFullyReceived", ignore = true)
    QuotationRequestDTO toDTO(QuotationRequest qr);

    @Mapping(target = "quotationRequestId", source = "quotationRequest.id")
    @Mapping(target = "partId", source = "part.id")
    @Mapping(target = "partCode", source = "partCode")
    @Mapping(target = "partName", source = "partName")
    @Mapping(target = "partModel", source = "partModel")
    @Mapping(target = "partSupplier", source = "partSupplier")
    @Mapping(target = "partCategory", source = "partCategory")
    @Mapping(target = "newModel", source = "newModel")
    @Mapping(target = "newSupplier", source = "newSupplier")
    @Mapping(target = "inspectedById", source = "inspectedBy.id")
    @Mapping(target = "inspectedByName", source = "inspectedBy.name")
    @Mapping(target = "inspectedAt", source = "inspectedAt")
    QuotationRequestPartDTO toPartDTO(QuotationRequestPart qrPart);

    List<QuotationRequestPartDTO> toPartDTOs(List<QuotationRequestPart> parts);
}