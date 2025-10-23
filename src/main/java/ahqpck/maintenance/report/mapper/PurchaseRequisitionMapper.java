package ahqpck.maintenance.report.mapper;

import ahqpck.maintenance.report.dto.PurchaseRequisitionDTO;
import ahqpck.maintenance.report.dto.PurchaseRequisitionPartDTO;
import ahqpck.maintenance.report.entity.PurchaseRequisition;
import ahqpck.maintenance.report.entity.PurchaseRequisitionPart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PurchaseRequisitionMapper {

    @Mapping(target = "requestorId", source = "requestor.id")
    @Mapping(target = "requestorName", source = "requestorName")
    @Mapping(target = "requestorEmail", source = "requestorEmail")
    @Mapping(target = "requestorEmployeeId", source = "requestorEmployeeId")
    @Mapping(target = "statusDisplay", source = "status", qualifiedByName = "statusToDisplayName")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", source = "createdBy.name")
    @Mapping(target = "createdByEmail", source = "createdBy.email")
    @Mapping(target = "createdByEmployeeId", source = "createdBy.employeeId")
    @Mapping(target = "updatedById", source = "updatedBy.id")
    @Mapping(target = "updatedByName", source = "updatedBy.name")
    @Mapping(target = "updatedByEmail", source = "updatedBy.email")
    @Mapping(target = "updatedByEmployeeId", source = "updatedBy.employeeId")
    @Mapping(target = "parts", source = "requisitionParts")
    @Mapping(target = "receivedAt", source = "reviewedAt")
    @Mapping(target = "inspectorName", source = "reviewerName")
    @Mapping(target = "canBeApproved", ignore = true)
    @Mapping(target = "canCreatePO", ignore = true)
    @Mapping(target = "canBeCompleted", ignore = true)
    @Mapping(target = "completionNotes", ignore = true)
    PurchaseRequisitionDTO toDTO(PurchaseRequisition pr);

    @Mapping(target = "purchaseRequisitionId", source = "purchaseRequisition.id")
    @Mapping(target = "prCode", source = "purchaseRequisition.code")
    @Mapping(target = "partId", source = "part.id")
    @Mapping(target = "partCode", source = "partCode")
    @Mapping(target = "partName", source = "partName")
    @Mapping(target = "partSupplier", source = "partSupplier")
    @Mapping(target = "partCategory", source = "partCategory")
    PurchaseRequisitionPartDTO toPartDTO(PurchaseRequisitionPart prPart);

    List<PurchaseRequisitionPartDTO> toPartDTOs(List<PurchaseRequisitionPart> parts);

    @Named("statusToDisplayName")
    default String statusToDisplayName(Enum<?> status) {
        return status != null ? status.toString() : null;
    }
}