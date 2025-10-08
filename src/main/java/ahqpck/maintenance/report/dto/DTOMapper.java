package ahqpck.maintenance.report.dto;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ahqpck.maintenance.report.entity.Category;
import ahqpck.maintenance.report.entity.MachineType;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.entity.PurchaseRequisition;
import ahqpck.maintenance.report.entity.PurchaseRequisitionItem;
import ahqpck.maintenance.report.entity.Section;
import ahqpck.maintenance.report.entity.SerialNumber;
import ahqpck.maintenance.report.entity.Subcategory;
import ahqpck.maintenance.report.entity.Supplier;

@Component
public class DTOMapper {

    public MachineTypeDTO mapToMachineTypeDTO(MachineType mt) {
        MachineTypeDTO dto = new MachineTypeDTO();
        dto.setId(mt.getId());
        dto.setName(mt.getName());
        dto.setCode(mt.getCode());

        return dto;
    }

    public CategoryDTO mapToCategoryDTO(Category cat) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(cat.getId());
        dto.setName(cat.getName());
        dto.setCode(cat.getCode());
        // dto.setMachineType(mapToMachineTypeDTO(cat.getMachineType()));

        return dto;
    }

    public SubcategoryDTO mapToSubcategoryDTO(Subcategory subcat) {
        SubcategoryDTO dto = new SubcategoryDTO();
        dto.setId(subcat.getId());
        dto.setName(subcat.getName());
        dto.setCode(subcat.getCode());
        // dto.setCategory(mapToCategoryDTO(subcat.getCategory()));

        return dto;
    }

    public SerialNumberDTO mapToSerialNumberDTO(SerialNumber sn) {
        SerialNumberDTO dto = new SerialNumberDTO();
        dto.setId(sn.getId());
        dto.setCode(sn.getCode());
        dto.setName(sn.getName());
        // dto.setSubcategory(mapToSubcategoryDTO(sn.getSubcategory()));

        return dto;
    }

    public SupplierDTO mapToSupplierDTO(Supplier sup) {
        SupplierDTO dto = new SupplierDTO();
        dto.setId(sup.getId());
        dto.setName(sup.getName());
        dto.setCode(sup.getCode());
        // dto.setSerialNumber(mapToSerialNumberDTO(sup.getSerialNumber()));

        return dto;
    }

    public SectionDTO mapToSectionDTO(Section sec) {
        SectionDTO dto = new SectionDTO();
        dto.setId(sec.getId());
        dto.setCode(sec.getCode());

        return dto;
    }

    // Purchase Requisition Mappings
    public PurchaseRequisitionDTO mapToPurchaseRequisitionDTO(PurchaseRequisition pr) {
        PurchaseRequisitionDTO dto = PurchaseRequisitionDTO.builder()
                .id(pr.getId())
                .code(pr.getCode())
                .title(pr.getTitle())
                .description(pr.getDescription())
                .requestorId(pr.getRequestor() != null ? pr.getRequestor().getId() : null)
                .requestorName(pr.getRequestorName())
                .requestorEmail(pr.getRequestorEmail())
                .requestorEmployeeId(pr.getRequestorEmployeeId())
                .dateNeeded(pr.getDateNeeded())
                .targetEquipmentId(pr.getTargetEquipmentId())
                .targetEquipmentName(pr.getTargetEquipmentName())
                .status(pr.getStatus())
                .statusDisplay(pr.getStatus().getDisplayName())
                .isApproved(pr.getIsApproved())
                .reviewerName(pr.getReviewerName())
                .reviewedAt(pr.getReviewedAt())
                .reviewNotes(pr.getReviewNotes())
                .poNumber(pr.getPoNumber())
                .inspectorId(pr.getInspector() != null ? pr.getInspector().getId() : null)
                .inspectorName(pr.getInspectorName())
                .inspectorEmail(pr.getInspectorEmail())
                .inspectorEmployeeId(pr.getInspectorEmployeeId())
                .receivedAt(pr.getReceivedAt())
                .completionNotes(pr.getCompletionNotes())
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                .build();

        // Map items
        if (pr.getItems() != null) {
            dto.setItems(pr.getItems().stream()
                    .map(this::mapToPurchaseRequisitionItemDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public PurchaseRequisitionItemDTO mapToPurchaseRequisitionItemDTO(PurchaseRequisitionItem item) {
        PurchaseRequisitionItemDTO dto = PurchaseRequisitionItemDTO.builder()
                .id(item.getId())
                .purchaseRequisitionId(item.getPurchaseRequisition().getId())
                .partId(item.getPart() != null ? item.getPart().getId() : null)
                .partCode(item.getPartCode())
                .partName(item.getPartName())
                .partDescription(item.getPartDescription())
                .partSpecifications(item.getPartSpecifications())
                .partCategory(item.getPartCategory())
                .partSupplier(item.getPartSupplier())
                .quantity(item.getQuantity())
                .unitMeasure(item.getUnitMeasure())
                .justification(item.getJustification())
                .criticalityLevel(item.getCriticalityLevel())
                .notes(item.getNotes())
                .isNewPart(item.getIsNewPart())
                .isReceived(item.getIsReceived())
                .receivedQuantity(item.getReceivedQuantity())
                .receivedAt(item.getReceivedAt())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();

        // Set display fields
        dto.setDisplayPartCode(item.getDisplayPartCode());
        dto.setDisplayPartName(item.getDisplayPartName());
        dto.setDisplaySupplier(item.getDisplaySupplier());
        dto.setDisplayCategory(item.getDisplayCategory());
        dto.setCanBeReceived(item.canBeReceived());

        // Map existing part if available
        if (item.getPart() != null) {
            dto.setExistingPart(mapToPartDTO(item.getPart()));
            dto.setCurrentStock(item.getPart().getStockQuantity());
        }

        return dto;
    }

    public PartDTO mapToPartDTO(Part part) {
        PartDTO dto = new PartDTO();
        dto.setId(part.getId());
        dto.setCode(part.getCode());
        dto.setName(part.getName());
        dto.setCategoryName(part.getCategoryName());
        dto.setSupplierName(part.getSupplierName());
        dto.setSectionCode(part.getSectionCode());
        dto.setDescription(part.getDescription());
        dto.setImage(part.getImage());
        dto.setStockQuantity(part.getStockQuantity());
        dto.setEquipmentCount(part.getEquipmentCount());

        return dto;
    }
}