// Purchase Requisition Add Item Modal JavaScript
// This script handles the modal functionality for adding items to purchase requisitions

let itemCounter = 0;

// Wait for document to be ready
$(document).ready(function() {
    console.log('Purchase Requisition Modal JS loaded');
    console.log('Modal exists:', $('#itemModal').length > 0);
    initializeModalEventHandlers();
});

function initializeModalEventHandlers() {
    // Toggle between existing and new part sections
    $('input[name="itemType"]').change(function() {
        if ($(this).val() === 'existing') {
            $('#existingPartSection').show();
            $('#newPartSection').hide();
        } else {
            $('#existingPartSection').hide();
            $('#newPartSection').show();
            $('#partDetailsPreview').hide();
        }
    });

    // Handle part selection and show preview
    $('#existingPartSelect').change(function() {
        let selectedOption = $(this).find('option:selected');
        if ($(this).val()) {
            $('#previewCode').text(selectedOption.data('code'));
            $('#previewName').text(selectedOption.data('name'));
            $('#previewCategory').text(selectedOption.data('category') || 'N/A');
            $('#previewSupplier').text(selectedOption.data('supplier') || 'N/A');
            $('#previewStock').text(selectedOption.data('stock'));
            
            // Show stock warning if stock is low (less than 5)
            if (selectedOption.data('stock') < 5) {
                $('#stockWarning').show();
            } else {
                $('#stockWarning').hide();
            }
            
            $('#partDetailsPreview').show();
            // Trigger quantity validation
            validateQuantity();
        } else {
            $('#partDetailsPreview').hide();
            $('#quantityWarning').hide();
        }
    });

    // Validate quantity against available stock
    $('#itemQuantity').on('input', function() {
        validateQuantity();
    });

    // Auto-populate equipment name when equipment is selected
    $('#targetEquipmentId').change(function() {
        let selectedText = $(this).find('option:selected').text();
        if (selectedText && selectedText !== 'Select target equipment') {
            $('input[name="targetEquipmentName"]').remove();
            $('form').append(`<input type="hidden" name="targetEquipmentName" value="${selectedText}">`);
        }
    });
}

function validateQuantity() {
    let selectedPart = $('#existingPartSelect').find('option:selected');
    let quantity = parseInt($('#itemQuantity').val()) || 0;
    let availableStock = selectedPart.data('stock') || 0;
    
    if ($('#existingPartSelect').val() && quantity > availableStock) {
        $('#quantityWarning').show();
    } else {
        $('#quantityWarning').hide();
    }
}

function addPRItem() {
    console.log('addPRItem function called');
    
    // Check if modal exists
    const modal = document.getElementById('itemModal');
    const testModal = document.getElementById('testModal');
    
    if (!modal) {
        console.error('Item modal not found in DOM');
        
        // Try test modal as fallback
        if (testModal) {
            console.log('Opening test modal as fallback');
            $('#testModal').modal('show');
            return;
        }
        
        alert('Modal not found. Please refresh the page and try again.');
        return;
    }
    
    console.log('Modal found, opening...');
    $('#itemModal').modal('show');
    resetItemForm();
    
    // Check if parts are available, if not, switch to new part mode
    let hasExistingParts = $('#existingPartSelect option').length > 2; // More than empty option and "no parts" option
    console.log('Parts available:', hasExistingParts);
    
    if (!hasExistingParts) {
        $('#newPart').prop('checked', true);
        $('#existingPartSection').hide();
        $('#newPartSection').show();
        $('#partDetailsPreview').hide();
        
        // Show a notice
        if ($('#noPartsNotice').length === 0) {
            $('#itemForm').prepend(`
                <div id="noPartsNotice" class="alert alert-info">
                    <i class="fas fa-info-circle mr-2"></i>
                    <strong>No parts available in inventory.</strong> Please add the part details manually.
                </div>
            `);
        }
    }
}

function resetItemForm() {
    // Safely reset the form
    let itemForm = document.getElementById('itemForm');
    if (itemForm) {
        itemForm.reset();
    }
    
    $('#existingPart').prop('checked', true);
    $('#existingPartSection').show();
    $('#newPartSection').hide();
    $('#partDetailsPreview').hide();
    $('#quantityWarning').hide();
    $('#noPartsNotice').remove(); // Remove the no parts notice
}

function saveItem() {
    let isExisting = $('input[name="itemType"]:checked').val() === 'existing';
    let isValid = true;
    let itemData = {};

    if (isExisting) {
        let partSelect = $('#existingPartSelect');
        if (!partSelect.val() || partSelect.val() === '') {
            // Check if there are any valid parts available
            let hasValidParts = partSelect.find('option:not([disabled]):not([value=""])').length > 0;
            if (!hasValidParts) {
                alert('No parts available in inventory. Please switch to "New Part" option.');
                return;
            } else {
                alert('Please select a part from inventory');
                return;
            }
        }
        
        let selectedOption = partSelect.find('option:selected');
        itemData = {
            partId: partSelect.val(),
            partName: selectedOption.data('name'),
            partCode: selectedOption.data('code'),
            partCategory: selectedOption.data('category'),
            partSupplier: selectedOption.data('supplier'),
            currentStock: selectedOption.data('stock'),
            isNewPart: false
        };
    } else {
        let partName = $('#newPartName').val().trim();
        if (!partName) {
            alert('Please enter part name');
            return;
        }
        
        itemData = {
            partName: partName,
            partCategory: $('#newPartCategory').val(),
            partSupplier: $('#newPartSupplier').val(),
            partDescription: $('#newPartDescription').val(),
            partSpecifications: $('#newPartSpecifications').val(),
            isNewPart: true
        };
    }

    let quantity = parseInt($('#itemQuantity').val());
    if (!quantity || quantity < 1) {
        alert('Please enter a valid quantity');
        return;
    }

    itemData.quantity = quantity;
    itemData.unitMeasure = $('#itemUnit').val();
    itemData.criticalityLevel = $('#itemCriticality').val();
    itemData.justification = $('#itemJustification').val();
    itemData.notes = $('#itemNotes').val();

    addItemToTable(itemData);
    $('#itemModal').modal('hide');
}

function addItemToTable(itemData) {
    itemCounter++;
    
    let stockWarning = '';
    if (!itemData.isNewPart && itemData.currentStock < itemData.quantity) {
        stockWarning = '<br><small class="text-danger">⚠ Insufficient stock (Available: ' + itemData.currentStock + ')</small>';
    }

    let partInfo = itemData.isNewPart ? 
        `<strong>${itemData.partName}</strong> <span class="badge badge-info">New Part</span>` :
        `<strong>${itemData.partCode}</strong> - ${itemData.partName}`;

    let row = `
        <tr id="item-${itemCounter}">
            <td>
                ${partInfo}
                ${stockWarning}
                <br><small class="text-muted">${itemData.partCategory || 'N/A'} | ${itemData.partSupplier || 'N/A'}</small>
            </td>
            <td class="text-center">${itemData.quantity} ${itemData.unitMeasure || ''}</td>
            <td class="text-center">
                <span class="badge ${getCriticalityClass(itemData.criticalityLevel)}">
                    ${itemData.criticalityLevel}
                </span>
            </td>
            <td>${itemData.justification || '-'}</td>
            <td class="text-center">
                <button type="button" class="btn btn-danger btn-sm" onclick="removeItem(${itemCounter})">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        </tr>
    `;

    if ($('#itemsTable').length === 0) {
        let tableHtml = `
            <div class="table-responsive">
                <table class="table table-bordered" id="itemsTable">
                    <thead class="thead-light">
                        <tr>
                            <th>Part Details</th>
                            <th class="text-center">Quantity</th>
                            <th class="text-center">Criticality</th>
                            <th>Justification</th>
                            <th class="text-center">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                    </tbody>
                </table>
            </div>
        `;
        $('#prItemsContainer').html(tableHtml);
    }

    $('#itemsTable tbody').append(row);

    // Add hidden form fields
    addHiddenFields(itemCounter - 1, itemData);
}

function addHiddenFields(index, itemData) {
    let hiddenFields = '';
    
    Object.keys(itemData).forEach(key => {
        if (itemData[key] !== null && itemData[key] !== undefined) {
            hiddenFields += `<input type="hidden" name="items[${index}].${key}" value="${itemData[key]}">`;
        }
    });

    $('form').append(`<div id="hidden-fields-${index + 1}">${hiddenFields}</div>`);
}

function removeItem(itemId) {
    $(`#item-${itemId}`).remove();
    $(`#hidden-fields-${itemId}`).remove();
    
    if ($('#itemsTable tbody tr').length === 0) {
        $('#prItemsContainer').html(`
            <div class="alert alert-info">
                <i class="fas fa-info-circle mr-2"></i>
                Click "Add Item" to add parts to this purchase requisition.
            </div>
        `);
    }
}

function getCriticalityClass(level) {
    switch(level) {
        case 'CRITICAL': return 'badge-danger';
        case 'HIGH': return 'badge-warning';
        case 'MEDIUM': return 'badge-info';
        case 'LOW': return 'badge-secondary';
        default: return 'badge-light';
    }
}