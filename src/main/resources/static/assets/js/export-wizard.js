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

        ths.forEach((th, index) => {
            const style = window.getComputedStyle(th);
            if (style.display === 'none' || parseFloat(style.minWidth || style.width) <= 0) return;

            const td = sampleTds[index];
            const field = td?.dataset?.field || td?.querySelector('[data-field]')?.dataset?.field;
            if (!field) {
                console.warn(`No data-field found for column index ${index}`);
                return;
            }

            headers.push(th.textContent.trim());
            visibleFields.push(field);
        });

        return { headers, visibleFields };
    }

    // Extract clean, human-readable data for PDF
    extractVisibleDataForPdf(visibleFields) {
        const data = [];
        const rows = this.tbody.querySelectorAll('tr');

        rows.forEach(tr => {
            if (tr.style.display === 'none') return;

            const tds = tr.querySelectorAll('td');
            if (tds.length === 0) return;

            const fieldToTd = {};
            tds.forEach(td => {
                const f = td.dataset.field;
                if (f) fieldToTd[f] = td;
            });

            const row = visibleFields.map(field => {
                const td = fieldToTd[field];
                if (!td) return '-';

                // Handle badges (e.g., roles, technicians)
                const badges = td.querySelectorAll('.badge');
                if (badges.length > 0) {
                    // For technicians or any badge-based field: remove (ID) from text
                    return Array.from(badges)
                        .map(b => b.textContent.trim().replace(/\s*\([^)]*\)$/, ''))
                        .filter(t => t)
                        .join(', ') || '-';
                }

                // Handle link in 'code' column
                if (field === 'code' && td.querySelector('a')) {
                    return td.querySelector('a').textContent.trim() || '-';
                }

                // For semantic fields like reporter, assignee, area, equipment, supervisor:
                // → Remove (ID) if present in plain text
                if (['reporter', 'assignee', 'area', 'equipment', 'supervisor'].includes(field)) {
                    return td.textContent.trim().replace(/\s*\([^)]*\)$/, '') || '-';
                }

                return td.textContent.trim() || '-';
            });

            data.push(row);
        });

        return data;
    }

    // Export to Excel with clean names + separate ID columns
    exportToExcel(filenamePrefix = 'Export') {
        const { headers, visibleFields } = this.getVisibleColumns();
        const rows = this.tbody.querySelectorAll('tr');
        const idFields = ['reporter', 'assignee', 'supervisor', 'area', 'equipment', 'technicians'];

        const finalData = Array.from(rows)
            .filter(tr => tr.style.display !== 'none')
            .map(tr => {
                const tds = Array.from(tr.querySelectorAll('td'));
                const fieldToTd = {};
                tds.forEach(td => {
                    const f = td.dataset.field;
                    if (f) fieldToTd[f] = td;
                });

                // Main data: clean technician names (remove "(ID)")
                const mainRow = visibleFields.map(field => {
                    const td = fieldToTd[field];
                    if (!td) return '';

                    if (field === 'technicians') {
                        return Array.from(td.querySelectorAll('.badge'))
                            .map(b => b.textContent.trim().replace(/\s*\([^)]*\)$/, ''))
                            .join(', ') || '-';
                    }
                    return td.textContent.trim() || '-';
                });

                // ID columns for visible semantic fields
                const idRow = visibleFields
                    .filter(f => idFields.includes(f))
                    .map(field => {
                        const td = fieldToTd[field];
                        if (!td) return '-';

                        if (field === 'technicians') {
                            return Array.from(td.querySelectorAll('.badge'))
                                .map(b => {
                                    const match = b.textContent.trim().match(/\(([^)]+)\)$/);
                                    return match ? match[1] : 'UNK';
                                })
                                .join(', ') || '-';
                        }

                        return (
                            td.dataset.employeeId ||
                            td.dataset.areaCode ||
                            td.dataset.equipmentCode ||
                            '-'
                        );
                    });

                return [...mainRow, ...idRow];
            });

        // Build headers for ID columns
        const idHeaders = visibleFields
            .filter(f => idFields.includes(f))
            .map(f => f === 'technicians' ? 'Technician IDs' : f.charAt(0).toUpperCase() + f.slice(1) + ' ID');

        const fullHeaders = [...headers, ...idHeaders];
        const ws = XLSX.utils.aoa_to_sheet([fullHeaders, ...finalData]);
        const wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, "Data");

        const dateStr = new Date().toISOString().slice(0, 10);
        XLSX.writeFile(wb, `${filenamePrefix}_${dateStr}.xlsx`);
    }

    // Export to PDF (human-readable only)
    exportToPdf(filenamePrefix = 'Export') {
        const { jsPDF } = window.jspdf;
        const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });

        const margin = 10;
        const pageWidth = doc.internal.pageSize.getWidth();
        const usableWidth = pageWidth - margin * 2;

        const { headers, visibleFields } = this.getVisibleColumns();
        const data = this.extractVisibleDataForPdf(visibleFields);
        const reportDateIndex = visibleFields.indexOf('reportDate');

        // Title
        doc.setFontSize(16);
        doc.text(`${filenamePrefix}`, margin, 15);

        // First render attempt
        doc.autoTable({
            head: [headers],
            body: data,
            startY: 25,
            theme: 'grid',
            styles: { fontSize: 7, cellPadding: 1, overflow: 'linebreak', halign: 'left', valign: 'middle' },
            headStyles: { fillColor: [33, 150, 243], fontSize: 8 },
            columnStyles: reportDateIndex >= 0 ? { [reportDateIndex]: { cellWidth: 18, overflow: 'hidden' } } : {},
            margin: { top: 25, left: margin, right: margin },
            tableWidth: 'auto',
            didDrawPage: (data) => {
                const pageCount = doc.internal.getNumberOfPages();
                doc.setFontSize(10);
                doc.text(`Page ${pageCount}`, margin, doc.internal.pageSize.height - 10);
            }
        });

        // If table overflows, redraw with smaller font
        const finalTable = doc.lastAutoTable;
        if (finalTable && finalTable.finalWidth > usableWidth) {
            doc.deletePage(doc.internal.getNumberOfPages());

            const scaleFactor = usableWidth / finalTable.finalWidth;
            const fontSize = Math.max(5, Math.floor(7 * scaleFactor));
            const headFontSize = Math.max(6, Math.floor(8 * scaleFactor));

            doc.autoTable({
                head: [headers],
                body: data,
                startY: 25,
                theme: 'grid',
                styles: { fontSize, cellPadding: 0.75, overflow: 'linebreak', halign: 'left', valign: 'middle' },
                headStyles: { fillColor: [33, 150, 243], fontSize: headFontSize },
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