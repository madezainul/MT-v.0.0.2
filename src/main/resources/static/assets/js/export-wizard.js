class ExportWizard {
    constructor(tableId) {
        this.table = document.getElementById(tableId);
        if (!this.table) {
            console.error(`Table with ID "${tableId}" not found.`);
            return;
        }
        this.thead = this.table.querySelector('thead');
        this.tbody = this.table.querySelector('tbody');
    }

    // Get visible columns by matching th index to td's data-field
    getVisibleColumns() {
        const headers = [];
        const visibleFields = [];
        const ths = this.thead.querySelectorAll('th');

        // Get sample row to map th index → td data-field
        const sampleRow = this.tbody.querySelector('tr');
        if (!sampleRow) {
            console.warn("No sample row found to map columns.");
            return { headers, visibleFields };
        }

        const sampleTds = sampleRow.querySelectorAll('td');
        if (sampleTds.length === 0) {
            console.warn("No sample tds found to map columns.");
            return { headers, visibleFields };
        }

        // Map each th index to td's data-field
        ths.forEach((th, index) => {
            const style = window.getComputedStyle(th);
            if (style.display === 'none' || parseFloat(style.minWidth || style.width) <= 0) return;

            // Get data-field from corresponding td in sample row
            const td = sampleTds[index];
            // Look for data-field on td OR any child inside it
            const field = td?.dataset?.field || td?.querySelector('[data-field]')?.dataset?.field;
            if (!field) {
                console.warn(`No data-field found for column index ${index}`);
                return; // Skip if no field
            }

            headers.push(th.textContent.trim());
            visibleFields.push(field);
        });

        return { headers, visibleFields };
    }

    // Extract row data for Excel (can include links, IDs if visible)
    extractVisibleData(visibleFields) {
        const data = [];
        const rows = this.tbody.querySelectorAll('tr');

        rows.forEach(tr => {
            if (tr.style.display === 'none') return;

            const tds = tr.querySelectorAll('td');
            if (tds.length === 0) return;

            // Build row by matching data-field
            const row = visibleFields.map(field => {
                const td = Array.from(tds).find(td => td.dataset.field === field);
                if (!td) return '';

                // Handle badge text for priority/category/status
                // Handle multiple badges (e.g., roles, technicians)
                const badges = td.querySelectorAll('.badge');
                if (badges.length > 0) {
                    return Array.from(badges)
                        .map(b => b.textContent.trim())
                        .filter(t => t)
                        .join(", ") || '';
                }

                // Handle link in Code column
                if (field === 'code' && td.querySelector('a')) {
                    return td.querySelector('a').textContent.trim() || '';
                }

                return td.textContent.trim() || '';
            });

            // Add ID columns for Excel (only if exporting Excel)
            if (this.includeIds) {
                const idRow = this.extractIdColumns(tds);
                row.push(...idRow);
            }

            data.push(row);
        });

        return data;
    }

    // Extract row data for PDF — HUMAN READABLE ONLY
    extractVisibleDataForPdf(visibleFields) {
        const data = [];
        const rows = this.tbody.querySelectorAll('tr');

        rows.forEach(tr => {
            if (tr.style.display === 'none') return;

            const tds = tr.querySelectorAll('td');
            if (tds.length === 0) return;

            const row = visibleFields.map(field => {
                const td = Array.from(tds).find(td => td.dataset.field === field);
                if (!td) return '-';

                // For PDF, we always show clean text — no links, no IDs
                // Handle badge
                // Handle multiple badges
                const badges = td.querySelectorAll('.badge');
                if (badges.length > 0) {
                    return Array.from(badges)
                        .map(b => b.textContent.trim())
                        .filter(t => t)
                        .join(", ") || '-';
                }

                // For code, extract link text if exists
                if (field === 'code' && td.querySelector('a')) {
                    return td.querySelector('a').textContent.trim() || '-';
                }

                // For reporter, assignee, area, equipment — ONLY show text, ignore underlying IDs
                if (['reporter', 'assignee', 'area', 'equipment'].includes(field)) {
                    return td.textContent.trim() || '-';
                }

                // Default: clean text
                return td.textContent.trim() || '-';
            });

            data.push(row);
        });

        return data;
    }

    // Extract hidden ID data — but only for fields that are VISIBLE
    extractIdColumns(tds, visibleFields) {
        const idColumns = [];

        // Define mappings: which field has which ID attribute
        const idMappings = [
            { field: 'reporter', attr: 'employeeId', label: 'Reporter ID' },
            { field: 'assignee', attr: 'employeeId', label: 'Assignee ID' },
            { field: 'area', attr: 'areaCode', label: 'Area Code' },
            { field: 'equipment', attr: 'equipmentCode', label: 'Equipment Code' }
        ];

        // Only include ID if the field is in visibleFields
        idMappings.forEach(mapping => {
            if (!visibleFields.includes(mapping.field)) return; // Skip if not visible

            const td = Array.from(tds).find(td => td.dataset.field === mapping.field);
            const idValue = td?.dataset?.[mapping.attr] || '-';
            idColumns.push(idValue);
        });

        return idColumns;
    }

    // Export to Excel — DYNAMIC ID COLUMNS
    exportToExcel(filenamePrefix = 'Export') {
        const { headers, visibleFields } = this.getVisibleColumns();
        const data = this.extractVisibleData(visibleFields);

        // Define which fields should export IDs — and only if visible
        const idMappings = [
            { field: 'reporter', attr: 'employeeId', label: 'Reporter ID' },
            { field: 'assignee', attr: 'employeeId', label: 'Assignee ID' },
            { field: 'area', attr: 'areaCode', label: 'Area Code' },
            { field: 'equipment', attr: 'equipmentCode', label: 'Equipment Code' }
        ];

        // Build dynamic ID headers & remember order
        const idHeaders = [];
        const activeIdMappings = [];

        idMappings.forEach(mapping => {
            if (visibleFields.includes(mapping.field)) {
                idHeaders.push(mapping.label);
                activeIdMappings.push(mapping);
            }
        });

        // Append ID columns to each row (only for visible semantic fields)
        if (idHeaders.length > 0) {
            const rows = this.tbody.querySelectorAll('tr');
            const updatedData = data.map((row, rowIndex) => {
                const tr = rows[rowIndex];
                if (tr.style.display === 'none') return row; // Shouldn't happen, but safe

                const tds = tr.querySelectorAll('td');
                const idRow = [];

                activeIdMappings.forEach(mapping => {
                    const td = Array.from(tds).find(td => td.dataset.field === mapping.field);
                    const idValue = td?.dataset?.[mapping.attr] || '-';
                    idRow.push(idValue);
                });

                return [...row, ...idRow];
            });

            // Use updated data
            const ws = XLSX.utils.aoa_to_sheet([[...headers, ...idHeaders], ...updatedData]);
            const wb = XLSX.utils.book_new();
            XLSX.utils.book_append_sheet(wb, ws, "Data");

            const dateStr = new Date().toISOString().slice(0, 10);
            XLSX.writeFile(wb, `${filenamePrefix}_${dateStr}.xlsx`);
        } else {
            // No ID columns to add
            const ws = XLSX.utils.aoa_to_sheet([headers, ...data]);
            const wb = XLSX.utils.book_new();
            XLSX.utils.book_append_sheet(wb, ws, "Data");

            const dateStr = new Date().toISOString().slice(0, 10);
            XLSX.writeFile(wb, `${filenamePrefix}_${dateStr}.xlsx`);
        }
    }

    exportToPdf(filenamePrefix = 'Export') {
        const { jsPDF } = window.jspdf;
        const doc = new jsPDF({
            orientation: 'landscape',
            unit: 'mm',
            format: 'a4'
        });

        const pageWidth = doc.internal.pageSize.getWidth();
        const margin = 10; // Left/right margin
        const usableWidth = pageWidth - margin * 2;

        const { headers, visibleFields } = this.getVisibleColumns();
        const data = this.extractVisibleDataForPdf(visibleFields);

        const reportDateIndex = visibleFields.indexOf('reportDate');


        // Title
        doc.setFontSize(16);
        doc.text(`${filenamePrefix}`, margin, 15);

        // AutoTable with full-width fitting
        doc.autoTable({
            head: [headers],
            body: data,
            startY: 25,
            theme: 'grid',
            styles: {
                fontSize: 7,
                cellPadding: 1,
                overflow: 'linebreak', // Allow text wrapping
                halign: 'left',
                valign: 'middle'
            },
            headStyles: {
                fillColor: [33, 150, 243],
                fontSize: 8
            },
            columnStyles: {
                ...(reportDateIndex >= 0 && {
                    [reportDateIndex]: {
                        cellWidth: 18,
                        overflow: 'hidden',      // Prevent wrapping — keep date on one line
                        fontStyle: 'normal'
                    }
                })
            }, // We'll auto-fit below
            margin: { top: 25, left: margin, right: margin },
            tableWidth: 'auto', // or 'wrap' — try both

            // 👇 This is KEY: auto-scale columns to fit available width
            didParseCell: (data) => {
                // Optional: you can adjust specific cells here
            },

            didDrawPage: (data) => {
                const pageCount = doc.internal.getNumberOfPages();
                doc.setFontSize(10);
                doc.text(`Page ${pageCount}`, margin, doc.internal.pageSize.height - 10);
            }
        });

        // ✅ AFTER TABLE IS RENDERED, CHECK IF IT OVERFLOWS AND SCALE IF NEEDED
        const finalTable = doc.lastAutoTable;
        if (finalTable && finalTable.finalWidth > usableWidth) {
            // Table is too wide — let's scale it down to fit
            const scaleFactor = usableWidth / finalTable.finalWidth;

            // Start new doc or redraw? Unfortunately, jspdf-autotable doesn’t support post-scale.
            // So instead, we REDRAW with smaller font or column adjustments.

            // ⚠️ Workaround: Reduce font size and rerender
            doc.deletePage(doc.internal.getNumberOfPages()); // Remove last drawn table

            doc.autoTable({
                head: [headers],
                body: data,
                startY: 25,
                theme: 'grid',
                styles: {
                    fontSize: Math.max(5, Math.floor(7 * scaleFactor)), // Scale down font
                    cellPadding: 0.75,
                    overflow: 'linebreak',
                    halign: 'left',
                    valign: 'middle'
                },
                headStyles: {
                    fillColor: [33, 150, 243],
                    fontSize: Math.max(6, Math.floor(8 * scaleFactor))
                },
                margin: { top: 25, left: margin, right: margin },
                tableWidth: 'auto',
                didDrawPage: (data) => {
                    const pageCount = doc.internal.getNumberOfPages();
                    doc.setFontSize(10);
                    doc.text(`Page ${pageCount}`, margin, doc.internal.pageSize.height - 10);
                }
            });
        }

        const dateStr = new Date().toISOString().slice(0, 10);
        doc.save(`${filenamePrefix}_${dateStr}.pdf`);
    }
}

// Make globally accessible
window.ExportWizard = ExportWizard;