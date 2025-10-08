package ahqpck.maintenance.report.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ahqpck.maintenance.report.dto.DTOMapper;
import ahqpck.maintenance.report.dto.PurchaseRequisitionItemDTO;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.entity.PurchaseRequisition;
import ahqpck.maintenance.report.entity.PurchaseRequisitionItem;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.repository.PurchaseRequisitionItemRepository;
import ahqpck.maintenance.report.repository.PurchaseRequisitionRepository;
import ahqpck.maintenance.report.util.Base62;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseRequisitionItemService {

    private final PurchaseRequisitionItemRepository prItemRepository;
    private final PurchaseRequisitionRepository prRepository;
    private final PartRepository partRepository;
    private final DTOMapper dtoMapper;

    // CRUD Operations
    @Transactional
    public PurchaseRequisitionItemDTO addItemToPR(String prId, PurchaseRequisitionItemDTO itemDTO) {
        try {
            PurchaseRequisition pr = prRepository.findById(prId)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition not found with id: " + prId));

            PurchaseRequisitionItem item = PurchaseRequisitionItem.builder()
                    .id(Base62.encode(UUID.randomUUID()))
                    .purchaseRequisition(pr)
                    .partName(itemDTO.getPartName())
                    .partDescription(itemDTO.getPartDescription())
                    .partSpecifications(itemDTO.getPartSpecifications())
                    .partCategory(itemDTO.getPartCategory())
                    .partSupplier(itemDTO.getPartSupplier())
                    .quantity(itemDTO.getQuantity())
                    .unitMeasure(itemDTO.getUnitMeasure())
                    .justification(itemDTO.getJustification())
                    .criticalityLevel(itemDTO.getCriticalityLevel())
                    .notes(itemDTO.getNotes())
                    .isNewPart(itemDTO.getIsNewPart())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // Link to existing part if provided
            if (itemDTO.getPartId() != null && !itemDTO.getPartId().trim().isEmpty()) {
                Part existingPart = partRepository.findById(itemDTO.getPartId())
                        .orElseThrow(() -> new RuntimeException("Part not found with id: " + itemDTO.getPartId()));
                item.setPart(existingPart);
                item.setIsNewPart(false);
            } else {
                item.setIsNewPart(true);
            }

            item = prItemRepository.save(item);
            return dtoMapper.mapToPurchaseRequisitionItemDTO(item);

        } catch (Exception e) {
            throw new RuntimeException("Failed to add item to purchase requisition: " + e.getMessage(), e);
        }
    }

    public List<PurchaseRequisitionItemDTO> getItemsByPRId(String prId) {
        return prItemRepository.findByPurchaseRequisitionId(prId).stream()
                .map(dtoMapper::mapToPurchaseRequisitionItemDTO)
                .collect(Collectors.toList());
    }

    public PurchaseRequisitionItemDTO getItemById(String itemId) {
        PurchaseRequisitionItem item = prItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Purchase Requisition Item not found with id: " + itemId));
        return dtoMapper.mapToPurchaseRequisitionItemDTO(item);
    }

    @Transactional
    public PurchaseRequisitionItemDTO updateItem(String itemId, PurchaseRequisitionItemDTO itemDTO) {
        try {
            PurchaseRequisitionItem item = prItemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition Item not found with id: " + itemId));

            // Update fields
            item.setPartName(itemDTO.getPartName());
            item.setPartDescription(itemDTO.getPartDescription());
            item.setPartSpecifications(itemDTO.getPartSpecifications());
            item.setPartCategory(itemDTO.getPartCategory());
            item.setPartSupplier(itemDTO.getPartSupplier());
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitMeasure(itemDTO.getUnitMeasure());
            item.setJustification(itemDTO.getJustification());
            item.setCriticalityLevel(itemDTO.getCriticalityLevel());
            item.setNotes(itemDTO.getNotes());
            item.setUpdatedAt(LocalDateTime.now());

            item = prItemRepository.save(item);
            return dtoMapper.mapToPurchaseRequisitionItemDTO(item);

        } catch (Exception e) {
            throw new RuntimeException("Failed to update purchase requisition item: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteItem(String itemId) {
        try {
            PurchaseRequisitionItem item = prItemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition Item not found with id: " + itemId));

            prItemRepository.delete(item);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete purchase requisition item: " + e.getMessage(), e);
        }
    }

    // Receiving Operations
    @Transactional
    public PurchaseRequisitionItemDTO receiveItem(String itemId, Integer receivedQuantity, String generatedPartCode) {
        try {
            PurchaseRequisitionItem item = prItemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Purchase Requisition Item not found with id: " + itemId));

            if (!item.canBeReceived()) {
                throw new RuntimeException("Item has already been received");
            }

            // Mark as received
            item.markAsReceived(receivedQuantity, generatedPartCode);

            // If it's a new part and we have a generated code, create the Part entity
            if (Boolean.TRUE.equals(item.getIsNewPart()) && generatedPartCode != null) {
                createPartFromItem(item, generatedPartCode, receivedQuantity);
            } else if (item.getPart() != null) {
                // Update stock for existing part
                item.getPart().addStock(receivedQuantity);
                partRepository.save(item.getPart());
            }

            item = prItemRepository.save(item);
            return dtoMapper.mapToPurchaseRequisitionItemDTO(item);

        } catch (Exception e) {
            throw new RuntimeException("Failed to receive purchase requisition item: " + e.getMessage(), e);
        }
    }

    // Statistics and Queries
    public List<PurchaseRequisitionItemDTO> getItemsReadyForReceiving() {
        return prItemRepository.findItemsReadyForReceiving().stream()
                .map(dtoMapper::mapToPurchaseRequisitionItemDTO)
                .collect(Collectors.toList());
    }

    public List<PurchaseRequisitionItemDTO> getNewPartsWithoutCodes() {
        return prItemRepository.findNewPartsWithoutCodes().stream()
                .map(dtoMapper::mapToPurchaseRequisitionItemDTO)
                .collect(Collectors.toList());
    }

    public long getCriticalItemsPendingReceiptCount() {
        return prItemRepository.countCriticalItemsPendingReceipt();
    }

    public List<PurchaseRequisitionItemDTO> getItemsByPartId(String partId) {
        return prItemRepository.findByPartId(partId).stream()
                .map(dtoMapper::mapToPurchaseRequisitionItemDTO)
                .collect(Collectors.toList());
    }

    public List<PurchaseRequisitionItemDTO> getItemsByCriticality(String criticalityLevel) {
        return prItemRepository.findByCriticalityLevel(criticalityLevel).stream()
                .map(dtoMapper::mapToPurchaseRequisitionItemDTO)
                .collect(Collectors.toList());
    }

    // Helper method to create Part entity from received new part
    @Transactional
    private void createPartFromItem(PurchaseRequisitionItem item, String generatedCode, Integer receivedQuantity) {
        try {
            Part newPart = Part.builder()
                    .code(generatedCode)
                    .name(item.getPartName())
                    .description(item.getPartDescription())
                    .categoryName(item.getPartCategory())
                    .supplierName(item.getPartSupplier())
                    .sectionCode("TBD") // To be determined based on code generator
                    .stockQuantity(receivedQuantity)
                    .build();

            newPart = partRepository.save(newPart);

            // Link the item to the newly created part
            item.setPart(newPart);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create Part entity from PR item: " + e.getMessage(), e);
        }
    }
}