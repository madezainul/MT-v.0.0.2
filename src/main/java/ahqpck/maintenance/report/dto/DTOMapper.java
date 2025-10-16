package ahqpck.maintenance.report.dto;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ahqpck.maintenance.report.entity.Category;
import ahqpck.maintenance.report.entity.MachineType;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.entity.QuotationRequest;
import ahqpck.maintenance.report.entity.QuotationRequestPart;
import ahqpck.maintenance.report.entity.PurchaseRequisition;
import ahqpck.maintenance.report.entity.PurchaseRequisitionPart;
import ahqpck.maintenance.report.entity.Section;
import ahqpck.maintenance.report.entity.Capacity;
import ahqpck.maintenance.report.entity.Subcategory;
import ahqpck.maintenance.report.entity.Supplier;
import ahqpck.maintenance.report.entity.User;

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

    public CapacityDTO mapToCapacityDTO(Capacity cap) {
        CapacityDTO dto = new CapacityDTO();
        dto.setId(cap.getId());
        dto.setCode(cap.getCode());
        dto.setName(cap.getName());
        // dto.setSubcategory(mapToSubcategoryDTO(cap.getSubcategory()));

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
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                // Map audit fields with safe access
                .createdById(safeGetUserId(pr.getCreatedBy()))
                .createdByName(safeGetUserName(pr.getCreatedBy()))
                .createdByEmail(safeGetUserEmail(pr.getCreatedBy()))
                .createdByEmployeeId(safeGetUserEmployeeId(pr.getCreatedBy()))
                .updatedById(safeGetUserId(pr.getUpdatedBy()))
                .updatedByName(safeGetUserName(pr.getUpdatedBy()))
                .updatedByEmail(safeGetUserEmail(pr.getUpdatedBy()))
                .updatedByEmployeeId(safeGetUserEmployeeId(pr.getUpdatedBy()))
                .build();

        // Map parts
        if (pr.getRequisitionParts() != null) {
            dto.setParts(pr.getRequisitionParts().stream()
                    .map(this::mapToPurchaseRequisitionPartDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public PurchaseRequisitionPartDTO mapToPurchaseRequisitionPartDTO(PurchaseRequisitionPart prPart) {
        PurchaseRequisitionPartDTO dto = PurchaseRequisitionPartDTO.builder()
                .id(prPart.getId())
                .purchaseRequisitionId(prPart.getPurchaseRequisition().getId())
                .prCode(prPart.getPurchaseRequisition().getCode()) // Add PR code
                .partId(prPart.getPart().getId())
                .partCode(prPart.getPartCode())
                .partName(prPart.getPartName())
                .partSupplier(prPart.getPartSupplier())
                .partCategory(prPart.getPartCategory())
                .quantityRequested(prPart.getQuantityRequested())
                .criticalityLevel(prPart.getCriticalityLevel())
                .justification(prPart.getJustification())
                .notes(prPart.getNotes())
                .quotationNumber(prPart.getQuotationNumber())
                .quantityOrdered(prPart.getQuantityOrdered())
                .quantityReceived(prPart.getQuantityReceived())
                .receivedAt(prPart.getReceivedAt())
                .status(prPart.getStatus())
                .createdAt(prPart.getCreatedAt())
                .updatedAt(prPart.getUpdatedAt())
                .build();

        // Map part details
        if (prPart.getPart() != null) {
            dto.setPart(mapToPartDTO(prPart.getPart()));
        }

        return dto;
    }

    // Quotation Request Mappings
    public QuotationRequestDTO mapToQuotationRequestDTO(QuotationRequest qr) {
        QuotationRequestDTO dto = QuotationRequestDTO.builder()
                .id(qr.getId())
                .quotationNumber(qr.getQuotationNumber())
                .supplierName(qr.getSupplierName())
                .supplierContact(qr.getSupplierContact())
                .totalAmount(qr.getTotalAmount())
                .currency(qr.getCurrency())
                .requestDate(qr.getRequestDate())
                .expectedDeliveryDate(qr.getExpectedDeliveryDate())
                .actualDeliveryDate(qr.getActualDeliveryDate())
                .status(qr.getStatus())
                .notes(qr.getNotes())
                .createdById(qr.getCreatedBy() != null ? qr.getCreatedBy().getId() : null)
                .createdByName(qr.getCreatedByName())
                .createdByEmail(qr.getCreatedByEmail())
                .createdAt(qr.getCreatedAt())
                .updatedAt(qr.getUpdatedAt())
                .build();

        // Map parts
        if (qr.getRequestParts() != null) {
            dto.setParts(qr.getRequestParts().stream()
                    .map(this::mapToQuotationRequestPartDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public QuotationRequestPartDTO mapToQuotationRequestPartDTO(QuotationRequestPart qrPart) {
        QuotationRequestPartDTO dto = QuotationRequestPartDTO.builder()
                .id(qrPart.getId())
                .quotationRequestId(qrPart.getQuotationRequest().getId())
                .partId(qrPart.getPart().getId())
                .partCode(qrPart.getPartCode())
                .partName(qrPart.getPartName())
                .partSupplier(qrPart.getPartSupplier())
                .partCategory(qrPart.getPartCategory())
                .quantityRequested(qrPart.getQuantityRequested())
                .unitPrice(qrPart.getUnitPrice())
                .totalPrice(qrPart.getTotalPrice())
                .quantityReceived(qrPart.getQuantityReceived())
                .notes(qrPart.getNotes())
                .createdAt(qrPart.getCreatedAt())
                .build();

        // Map part details
        if (qrPart.getPart() != null) {
            dto.setPart(mapToPartDTO(qrPart.getPart()));
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
        dto.setSpecification(part.getSpecification());
        dto.setModel(part.getModel());
        dto.setImage(part.getImage());
        dto.setStockQuantity(part.getStockQuantity());

        return dto;
    }

    // Helper methods for safe User entity access
    private String safeGetUserId(User user) {
        try {
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String safeGetUserName(User user) {
        try {
            return user != null ? user.getName() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String safeGetUserEmail(User user) {
        try {
            return user != null ? user.getEmail() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String safeGetUserEmployeeId(User user) {
        try {
            return user != null ? user.getEmployeeId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}