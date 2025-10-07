// Helper Functions
function formatDate(date) {
    return date.toISOString().split('T')[0]; // YYYY-MM-DD
}

function toApiDateTime(dateStr, isEnd = false) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    if (isEnd) {
        date.setHours(23, 59, 59, 999); // 23:59:59.999
    } else {
        date.setHours(0, 0, 0, 0); // 00:00:00.000
    }

    return date.toISOString().slice(0, 16); // "2025-08-01T00:00"
}

function populateYearSelector(selectId, selectedYear = null) {
    const select = document.getElementById(selectId);
    const currentYear = new Date().getFullYear();
    select.innerHTML = '';
    for (let y = currentYear - 10; y <= currentYear + 1; y++) {
        const opt = new Option(y, y);
        if (y === selectedYear || (selectedYear === null && y === currentYear)) {
            opt.selected = true;
        }
        select.appendChild(opt);
    }
}

function toDateTimeLocal(date) {
    const pad = (n) => n.toString().padStart(2, '0');
    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1); // 0-indexed
    const day = pad(date.getDate());
    const hours = pad(date.getHours());
    const minutes = pad(date.getMinutes());
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

// Overall Complaint
function setComplaintStatsDefaults() {
    // DO NOTHING — leave inputs empty for "all data" default
    // No auto-filling from/to → server gets null → returns all complaints
}

function fetchComplaintStats(from, to) {
    let url = `${baseUrl}/api/dashboards/status-count`;
    const params = new URLSearchParams();

    if (from) params.append('from', from);
    if (to) params.append('to', to);

    if (params.toString()) {
        url += '?' + params.toString();
    }

    fetch(url)
        .then(response => {
            if (!response.ok) throw new Error('Network error');
            return response.json();
        })
        .then(data => {
            document.getElementById('complaint-stats-total').textContent = data.totalAllComplaints || 0;
            document.getElementById('complaint-stats-open').textContent = data.totalOpen || 0;
            document.getElementById('complaint-stats-closed').textContent = data.totalClosed || 0;
            document.getElementById('complaint-stats-pending').textContent = data.totalPending || 0;
        })
        .catch(err => {
            console.error('Fetch error:', err);
        });
}

document.getElementById('complaint-stats-form').addEventListener('submit', function (e) {
    e.preventDefault();
    const from = document.getElementById('complaint-stats-from').value;
    const to = document.getElementById('complaint-stats-to').value;
    fetchComplaintStats(from, to);
});

document.getElementById('complaint-stats-reset').addEventListener('click', function () {
    document.getElementById('complaint-stats-from').value = '';
    document.getElementById('complaint-stats-to').value = '';
    fetchComplaintStats('', ''); // triggers fetch with no params → all data
});

document.querySelectorAll('.card-stats').forEach(card => {
    const link = card.closest('a');
    card.addEventListener('click', function () {
        const status = link.getAttribute('data-status');
        const fromInput = document.getElementById('complaint-stats-from').value;
        const toInput = document.getElementById('complaint-stats-to').value;

        const params = new URLSearchParams();

        if (fromInput) params.append('reportDateFrom', fromInput.split('T')[0]);
        if (toInput) params.append('reportDateTo', toInput.split('T')[0]);

        if (status) params.append('status', status);

        params.append('sortBy', 'reportDate');
        params.append('asc', 'false');
        params.append('size', '10');

        link.href = '/complaints?' + params.toString();
    });
});

function initComplaintStatsForm() {
    setComplaintStatsDefaults(); // now does nothing → defaults to ALL data

    const from = document.getElementById('complaint-stats-from').value;
    const to = document.getElementById('complaint-stats-to').value;

    fetchComplaintStats(from, to); // sends no params → server returns everything
}

// Complaint Chart
let complaintChart = null;

function updateComplaintChart(mode, from = null, to = null, year = null, month = null) {
    if (mode === 'yearly') {
        let url = `/api/dashboards/monthly-complaint`;
        const params = [];

        if (year) params.push(`year=${year}`);

        if (params.length > 0) url += '?' + params.join('&');

        fetch(url)
            .then(r => r.json())
            .then(data => {
                if (!Array.isArray(data) || data.length === 0) return;

                const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
                const labels = Array(12).fill('');
                const openData = Array(12).fill(0);
                const closedData = Array(12).fill(0);
                const pendingData = Array(12).fill(0);

                data.forEach(d => {
                    const monthIndex = parseInt(d.date.split('-')[1]) - 1;
                    if (monthIndex >= 0 && monthIndex < 12) {
                        labels[monthIndex] = months[monthIndex];
                        openData[monthIndex] = d.open || 0;
                        closedData[monthIndex] = d.closed || 0;
                        pendingData[monthIndex] = d.pending || 0;
                    }
                });

                for (let i = 0; i < 12; i++) {
                    if (!labels[i]) labels[i] = months[i];
                }

                renderComplaintChart(labels, openData, closedData, pendingData, 'Monthly Ticket Summary');
            })
            .catch(err => console.error('Monthly fetch error:', err));

    } else if (mode === 'monthly') {
        if (!year) return;

        const monthNum = month || new Date().getMonth() + 1;
        const yearNum = year;

        const firstDay = new Date(Date.UTC(yearNum, monthNum - 1, 1));
        const lastDay = new Date(Date.UTC(yearNum, monthNum, 0));

        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const effectiveLastDay = new Date(Math.min(lastDay, today));

        const fromApi = toApiDateTime(formatDate(firstDay), false);
        const toApi = toApiDateTime(formatDate(effectiveLastDay), true);

        console.log("Monthly fetch:", fromApi, toApi);
        const url = `/api/dashboards/daily-complaint?from=${fromApi}&to=${toApi}`;

        fetch(url)
            .then(r => r.json())
            .then(data => {
                if (!Array.isArray(data)) return;

                const labels = data.map(d => d.date);
                const openData = data.map(d => d.open || 0);
                const closedData = data.map(d => d.closed || 0);
                const pendingData = data.map(d => d.pending || 0);

                renderComplaintChart(labels, openData, closedData, pendingData, `Daily Tickets for ${new Date(yearNum, monthNum - 1).toLocaleString('default', { month: 'long' })} ${yearNum}`);
            })
            .catch(err => console.error('Daily fetch error:', err));

    } else {
        const fromApi = toApiDateTime(from, false);
        const toApi = toApiDateTime(to, true);
        const url = `/api/dashboards/daily-complaint?from=${fromApi}&to=${toApi}`;

        fetch(url)
            .then(r => r.json())
            .then(data => {
                if (!Array.isArray(data)) return;

                const labels = data.map(d => d.date);
                const openData = data.map(d => d.open || 0);
                const closedData = data.map(d => d.closed || 0);
                const pendingData = data.map(d => d.pending || 0);

                renderComplaintChart(labels, openData, closedData, pendingData, 'Daily Ticket Summary');
            })
            .catch(err => console.error('Daily fetch error:', err));
    }
}

function renderComplaintChart(labels, open, closed, pending, title) {
    const ctx = document.getElementById('complaint-bar-chart').getContext('2d');

    if (complaintChart) {
        complaintChart.destroy();
    }

    const stackedValues = labels.map((_, i) => (open[i] || 0) + (closed[i] || 0) + (pending[i] || 0));
    const totalMax = Math.max(...stackedValues, 0);

    let stepSize;
    if (totalMax <= 20) stepSize = 5;
    else if (totalMax <= 50) stepSize = 10;
    else if (totalMax <= 100) stepSize = 20;
    else if (totalMax <= 200) stepSize = 25;
    else if (totalMax <= 500) stepSize = 50;
    else stepSize = Math.ceil(totalMax / 10 / 10) * 10;

    const yAxisMax = Math.ceil(totalMax / stepSize) * stepSize;

    // ✅ FIXED: Removed the extra '{' before data
    complaintChart = new Chart(ctx, {
        type: 'bar',
        data: {  // 👈 Was: "{  { ... }" — now correct: "data: { ... }"
            labels: labels,
            datasets: [
                { label: "Open", backgroundColor: '#fdaf4b', data: open },
                { label: "Closed", backgroundColor: '#59d05d', data: closed },
                { label: "Pending", backgroundColor: '#d9534f', data: pending }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                // title: {
                //     display: true,
                //     text: title,
                //     font: { size: 16 },
                //     padding: { top: 10, bottom: 20 }
                // },
                tooltip: {
                    enabled: true,
                    mode: 'index',
                    intersect: true,
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    borderColor: '#ccc',
                    borderWidth: 1,
                    cornerRadius: 6,
                    padding: 10,
                    displayColors: true,
                    callbacks: {
                        title: (tooltipItems) => {
                            return tooltipItems[0].label;
                        },
                        label: (tooltipItem) => {
                            const datasetLabel = tooltipItem.dataset.label;
                            const value = tooltipItem.raw;
                            return `${datasetLabel}: ${value}`;
                        },
                        afterLabel: () => ''
                    },
                    animation: {
                        duration: 0
                    }
                }
            },
            scales: {
                x: {
                    stacked: true,
                    ticks: { autoSkip: true }
                },
                y: {
                    stacked: true,
                    beginAtZero: true,
                    max: yAxisMax,
                    ticks: {
                        stepSize: stepSize,
                        callback: function (value) {
                            return Number.isInteger(value) ? value : '';
                        }
                    }
                }
            },
            onClick: (event, elements) => {
                if (elements.length === 0) return;

                const element = elements[0];
                const dataIndex = element.index;
                const datasetIndex = element.datasetIndex;

                const statusMap = ["OPEN", "CLOSED", "PENDING"];
                const status = statusMap[datasetIndex];

                const label = labels[dataIndex];

                let from, to;

                if (label.length === 3) {
                    const year = document.getElementById('complaint-year-select')?.value || new Date().getFullYear();
                    const monthMap = {
                        "Jan": 0, "Feb": 1, "Mar": 2, "Apr": 3, "May": 4, "Jun": 5,
                        "Jul": 6, "Aug": 7, "Sep": 8, "Oct": 9, "Nov": 10, "Dec": 11
                    };
                    const monthIndex = monthMap[label];
                    if (monthIndex !== undefined) {
                        const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();
                        from = `${year}-${String(monthIndex + 1).padStart(2, '0')}-01`;
                        to = `${year}-${String(monthIndex + 1).padStart(2, '0')}-${daysInMonth}`;
                    } else {
                        from = to = label;
                    }
                } else {
                    from = to = label;
                }

                const url = new URL('/complaints', window.location.origin);
                url.searchParams.set('reportDateFrom', encodeURIComponent(from));
                url.searchParams.set('reportDateTo', encodeURIComponent(to));
                url.searchParams.set('status', encodeURIComponent(status));
                url.searchParams.set('sortBy', 'reportDate');
                url.searchParams.set('asc', 'false');
                url.searchParams.set('size', '10');

                window.location.href = url.toString();
            }
        }
    });
}

// 👇 NEW UTILITY: Format date as YYYY-MM-DD
function formatDate(date) {
    return date.toISOString().split('T')[0];
}

// 👇 NEW UTILITY: Update week nav label and button states
function updateWeekNav(fromDate, toDate) {
    const label = document.getElementById('weekRangeLabel');
    const prevBtn = document.getElementById('prevWeekBtn');
    const nextBtn = document.getElementById('nextWeekBtn');

    // Parse dates
    const start = new Date(fromDate);
    const end = new Date(toDate);

    // Format label: "Week of Jan 1–7, 2024"
    const startMonth = start.toLocaleString('default', { month: 'short' });
    const endMonth = end.toLocaleString('default', { month: 'short' });
    const startDay = start.getDate();
    const endDay = end.getDate();
    const year = start.getFullYear();

    // If different months, show both
    const monthPart = startMonth === endMonth ? startMonth : `${startMonth}–${endMonth}`;
    label.textContent = `Week of ${monthPart} ${startDay}–${endDay}, ${year}`;

    // Enable Prev if not at beginning (we assume data can go back indefinitely)
    prevBtn.disabled = false;

    // Disable Next if end date is today or in the future
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    nextBtn.disabled = end >= today;
}

function initComplaintChartForm() {
    const now = new Date();
    const from = new Date(now);
    from.setDate(now.getDate() - 6); // 7-day window: today - 6 days

    // Set default date inputs
    document.getElementById('complaint-from').value = formatDate(from);
    document.getElementById('complaint-to').value = formatDate(now);

    populateYearSelector('complaint-year-select', now.getFullYear());

    const currentMonth = String(now.getMonth() + 1).padStart(2, '0');
    document.getElementById('complaint-month-select').value = currentMonth;

    // Hide selectors initially
    document.getElementById('complaint-year-selector').style.display = 'none';
    document.getElementById('complaint-month-selector').style.display = 'none';

    // Show weekly nav — now it's the default view
    document.getElementById('weekly-nav').style.display = 'flex';

    // Initialize label with real current week
    updateWeekNav(formatDate(from), formatDate(now));

    // Initial chart load — this is now the DEFAULT VIEW
    updateComplaintChart('daily', formatDate(from), formatDate(now));

    document.querySelector('.dropdown-menu').addEventListener('click', function (e) {
        if (e.target.closest('select, input, .btn, .input-group')) {
            e.stopPropagation();
        }
    });

    // Only keep monthly/yearly buttons
    document.querySelectorAll('.dropdown-menu .btn[data-range]').forEach(btn => {
        btn.addEventListener('click', () => {
            const range = btn.getAttribute('data-range');
            const year = document.getElementById('complaint-year-select').value;

            document.getElementById('complaint-year-selector').style.display = 'none';
            document.getElementById('complaint-month-selector').style.display = 'none';

            if (range === 'monthly') {
                document.getElementById('complaint-month-selector').style.display = 'block';
                const month = document.getElementById('complaint-month-select').value;
                updateComplaintChart('monthly', null, null, year, month);
            } else if (range === 'yearly') {
                document.getElementById('complaint-year-selector').style.display = 'block';
                updateComplaintChart('yearly', null, null, year);
            }
        });
    });

    document.getElementById('complaint-month-select').addEventListener('change', () => {
        const year = document.getElementById('complaint-year-select').value;
        const month = document.getElementById('complaint-month-select').value;
        updateComplaintChart('monthly', null, null, year, month);
    });

    document.getElementById('complaint-year-select').addEventListener('change', () => {
        const year = document.getElementById('complaint-year-select').value;
        const currentMode = document.querySelector('.btn.active[data-range]')?.getAttribute('data-range') ||
            document.querySelector('.btn[data-range]:nth-child(2)').getAttribute('data-range');

        if (currentMode === 'monthly') {
            const month = document.getElementById('complaint-month-select').value;
            updateComplaintChart('monthly', null, null, year, month);
        } else if (currentMode === 'yearly') {
            updateComplaintChart('yearly', null, null, year);
        }
    });

    document.getElementById('apply-complaint-filters').addEventListener('click', () => {
        const from = document.getElementById('complaint-from').value;
        const to = document.getElementById('complaint-to').value;
        const year = document.getElementById('complaint-year-select').value;
        const month = document.getElementById('complaint-month-select').value;

        if (document.getElementById('complaint-year-selector').style.display === 'block') {
            updateComplaintChart('yearly', null, null, year);
        } else if (document.getElementById('complaint-month-selector').style.display === 'block') {
            updateComplaintChart('monthly', null, null, year, month);
        } else {
            updateComplaintChart('daily', from, to);
        }
    });

    // 👇 Prev/Next Week Navigation — WORKS ON DAILY MODE (which is now default)
    document.getElementById('prevWeekBtn').addEventListener('click', () => {
        const from = new Date(document.getElementById('complaint-from').value);
        const to = new Date(document.getElementById('complaint-to').value);

        from.setDate(from.getDate() - 7);
        to.setDate(to.getDate() - 7);

        document.getElementById('complaint-from').value = formatDate(from);
        document.getElementById('complaint-to').value = formatDate(to);

        updateWeekNav(formatDate(from), formatDate(to));
        updateComplaintChart('daily', formatDate(from), formatDate(to));
    });

    document.getElementById('nextWeekBtn').addEventListener('click', () => {
        const from = new Date(document.getElementById('complaint-from').value);
        const to = new Date(document.getElementById('complaint-to').value);

        from.setDate(from.getDate() + 7);
        to.setDate(to.getDate() + 7);

        document.getElementById('complaint-from').value = formatDate(from);
        document.getElementById('complaint-to').value = formatDate(to);

        updateWeekNav(formatDate(from), formatDate(to));
        updateComplaintChart('daily', formatDate(from), formatDate(to));
    });
}


// let complaintChart = null;

// function updateComplaintChart(mode, from = null, to = null, year = null, month = null) {
//     if (mode === 'yearly') {
//         // Yearly: Show 12 months summary
//         let url = `/api/dashboards/monthly-complaint`;
//         const params = [];

//         if (year) params.push(`year=${year}`);

//         if (params.length > 0) url += '?' + params.join('&');

//         fetch(url)
//             .then(r => r.json())
//             .then(data => {
//                 if (!Array.isArray(data) || data.length === 0) return;

//                 const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
//                     "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
//                 const labels = Array(12).fill('');
//                 const openData = Array(12).fill(0);
//                 const closedData = Array(12).fill(0);
//                 const pendingData = Array(12).fill(0);

//                 data.forEach(d => {
//                     const monthIndex = parseInt(d.date.split('-')[1]) - 1;
//                     if (monthIndex >= 0 && monthIndex < 12) {
//                         labels[monthIndex] = months[monthIndex];
//                         openData[monthIndex] = d.open || 0;
//                         closedData[monthIndex] = d.closed || 0;
//                         pendingData[monthIndex] = d.pending || 0;
//                     }
//                 });

//                 for (let i = 0; i < 12; i++) {
//                     if (!labels[i]) labels[i] = months[i];
//                 }

//                 renderComplaintChart(labels, openData, closedData, pendingData, 'Monthly Ticket Summary');
//             })
//             .catch(err => console.error('Monthly fetch error:', err));

//     } else if (mode === 'monthly') {
//         // Monthly: Show DAILY data for the entire selected month (up to today if future)
//         if (!year) return; // Safety

//         const monthNum = month || new Date().getMonth() + 1; // default to current month
//         const yearNum = year;

//         const firstDay = new Date(Date.UTC(yearNum, monthNum - 1, 1));
//         const lastDay = new Date(Date.UTC(yearNum, monthNum, 0)); // Last day of month

//         // Cap to today if future month
//         const today = new Date();
//         today.setHours(0, 0, 0, 0);
//         const effectiveLastDay = new Date(Math.min(lastDay, today));

//         const fromApi = toApiDateTime(formatDate(firstDay), false);   // T00:00
//         const toApi = toApiDateTime(formatDate(effectiveLastDay), true); // T23:59

//         console.log("Monthly fetch:", fromApi, toApi);
//         const url = `/api/dashboards/daily-complaint?from=${fromApi}&to=${toApi}`;

//         fetch(url)
//             .then(r => r.json())
//             .then(data => {
//                 if (!Array.isArray(data)) return;

//                 const labels = data.map(d => d.date);
//                 const openData = data.map(d => d.open || 0);
//                 const closedData = data.map(d => d.closed || 0);
//                 const pendingData = data.map(d => d.pending || 0);

//                 renderComplaintChart(labels, openData, closedData, pendingData, `Daily Tickets for ${new Date(yearNum, monthNum - 1).toLocaleString('default', { month: 'long' })} ${yearNum}`);
//             })
//             .catch(err => console.error('Daily fetch error:', err));

//     } else {
//         // Weekly or custom date range
//         const fromApi = toApiDateTime(from, false);
//         const toApi = toApiDateTime(to, true);
//         const url = `/api/dashboards/daily-complaint?from=${fromApi}&to=${toApi}`;

//         fetch(url)
//             .then(r => r.json())
//             .then(data => {
//                 if (!Array.isArray(data)) return;

//                 const labels = data.map(d => d.date);
//                 const openData = data.map(d => d.open || 0);
//                 const closedData = data.map(d => d.closed || 0);
//                 const pendingData = data.map(d => d.pending || 0);

//                 renderComplaintChart(labels, openData, closedData, pendingData, 'Daily Ticket Summary');
//             })
//             .catch(err => console.error('Daily fetch error:', err));
//     }
// }

// function renderComplaintChart(labels, open, closed, pending, title) {
//     const ctx = document.getElementById('complaint-bar-chart').getContext('2d');

//     // Destroy previous chart if exists
//     if (complaintChart) {
//         complaintChart.destroy();
//     }

//     // Calculate max for Y-axis scaling
//     const stackedValues = labels.map((_, i) => (open[i] || 0) + (closed[i] || 0) + (pending[i] || 0));
//     const totalMax = Math.max(...stackedValues, 0);

//     let stepSize;
//     if (totalMax <= 20) stepSize = 5;
//     else if (totalMax <= 50) stepSize = 10;
//     else if (totalMax <= 100) stepSize = 20;
//     else if (totalMax <= 200) stepSize = 25;
//     else if (totalMax <= 500) stepSize = 50;
//     else stepSize = Math.ceil(totalMax / 10 / 10) * 10;

//     const yAxisMax = Math.ceil(totalMax / stepSize) * stepSize;

//     // Create the chart — CHART.JS V3 ONLY
//     complaintChart = new Chart(ctx, {
//         type: 'bar',
//         data: {
//             labels: labels,
//             datasets: [
//                 { label: "Open", backgroundColor: '#fdaf4b', data: open },
//                 { label: "Closed", backgroundColor: '#59d05d', data: closed },
//                 { label: "Pending", backgroundColor: '#d9534f', data: pending }
//             ]
//         },
//         options: {
//             responsive: true,
//             maintainAspectRatio: false,
//             plugins: {
//                 title: {
//                     display: true,
//                     text: title,
//                     font: { size: 16 },
//                     padding: { top: 10, bottom: 20 }
//                 },
//                 tooltip: {
//                     enabled: true,
//                     mode: 'index',           // Show all datasets at same X
//                     intersect: true,         // ✅ CRITICAL FIX: Only show if hovering OVER a segment
//                     backgroundColor: 'rgba(0, 0, 0, 0.8)',
//                     titleColor: '#fff',
//                     bodyColor: '#fff',
//                     borderColor: '#ccc',
//                     borderWidth: 1,
//                     cornerRadius: 6,
//                     padding: 10,
//                     displayColors: true,
//                     callbacks: {
//                         title: (tooltipItems) => {
//                             return tooltipItems[0].label; // e.g., "Jan" or "2024-03-15"
//                         },
//                         label: (tooltipItem) => {
//                             const datasetLabel = tooltipItem.dataset.label;
//                             const value = tooltipItem.raw;
//                             return `${datasetLabel}: ${value}`;
//                         },
//                         afterLabel: () => ''
//                     },
//                     // Optional: Add slight delay to avoid flicker on edge movements
//                     animation: {
//                         duration: 0 // Disable tooltip animations (prevents jump/flicker)
//                     }
//                 }
//             },
//             scales: {
//                 x: {
//                     stacked: true,
//                     ticks: { autoSkip: true }
//                 },
//                 y: {
//                     stacked: true,
//                     beginAtZero: true,
//                     max: yAxisMax,
//                     ticks: {
//                         stepSize: stepSize,
//                         callback: function(value) {
//                             return Number.isInteger(value) ? value : '';
//                         }
//                     }
//                 }
//             },
//             onClick: (event, elements) => {
//                 if (elements.length === 0) return;

//                 const element = elements[0];
//                 const dataIndex = element.index;
//                 const datasetIndex = element.datasetIndex;

//                 const statusMap = ["OPEN", "CLOSED", "PENDING"];
//                 const status = statusMap[datasetIndex];

//                 const label = labels[dataIndex];

//                 let from, to;

//                 if (label.length === 3) { // e.g., "Jan", "Feb"
//                     const year = document.getElementById('complaint-year-select')?.value || new Date().getFullYear();
//                     const monthMap = {
//                         "Jan": 0, "Feb": 1, "Mar": 2, "Apr": 3, "May": 4, "Jun": 5,
//                         "Jul": 6, "Aug": 7, "Sep": 8, "Oct": 9, "Nov": 10, "Dec": 11
//                     };
//                     const monthIndex = monthMap[label];
//                     if (monthIndex !== undefined) {
//                         const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();
//                         from = `${year}-${String(monthIndex + 1).padStart(2, '0')}-01`;
//                         to = `${year}-${String(monthIndex + 1).padStart(2, '0')}-${daysInMonth}`;
//                     } else {
//                         from = to = label;
//                     }
//                 } else {
//                     // Daily format: "YYYY-MM-DD"
//                     from = to = label;
//                 }

//                 const url = new URL('/complaints', window.location.origin);
//                 url.searchParams.set('reportDateFrom', encodeURIComponent(from));
//                 url.searchParams.set('reportDateTo', encodeURIComponent(to));
//                 url.searchParams.set('status', encodeURIComponent(status));
//                 url.searchParams.set('sortBy', 'reportDate');
//                 url.searchParams.set('asc', 'false');
//                 url.searchParams.set('size', '10');

//                 window.location.href = url.toString();
//             }
//         }
//     });
// }

// function initComplaintChartForm() {
//     const now = new Date();
//     const from = new Date(now);
//     from.setDate(now.getDate() - 6);

//     document.getElementById('complaint-from').value = formatDate(from);
//     document.getElementById('complaint-to').value = formatDate(now);

//     populateYearSelector('complaint-year-select', now.getFullYear());

//     // Initialize month selector to current month
//     const currentMonth = String(now.getMonth() + 1).padStart(2, '0');
//     document.getElementById('complaint-month-select').value = currentMonth;

//     // Hide both selectors initially
//     document.getElementById('complaint-year-selector').style.display = 'none';
//     document.getElementById('complaint-month-selector').style.display = 'none';

//     document.querySelector('.dropdown-menu').addEventListener('click', function (e) {
//         if (e.target.closest('select, input, .btn, .input-group')) {
//             e.stopPropagation(); // Keep dropdown open
//         }
//     });

//     document.querySelectorAll('.dropdown-menu .btn[data-range]').forEach(btn => {
//         btn.addEventListener('click', () => {
//             const range = btn.getAttribute('data-range');
//             const now = new Date();
//             const year = document.getElementById('complaint-year-select').value;

//             // Reset visibility
//             document.getElementById('complaint-year-selector').style.display = 'none';
//             document.getElementById('complaint-month-selector').style.display = 'none';

//             if (range === 'weekly') {
//                 const from = new Date(now);
//                 console.log('Weekly range selected', from, now);
//                 from.setDate(now.getDate() - 6);
//                 document.getElementById('complaint-from').value = formatDate(from);
//                 document.getElementById('complaint-to').value = formatDate(now);
//                 updateComplaintChart('daily', formatDate(from), formatDate(now));
//             } else if (range === 'monthly') {
//                 document.getElementById('complaint-month-selector').style.display = 'block';
//                 const month = document.getElementById('complaint-month-select').value;
//                 updateComplaintChart('monthly', null, null, year, month);
//             } else if (range === 'yearly') {
//                 document.getElementById('complaint-year-selector').style.display = 'block';
//                 updateComplaintChart('yearly', null, null, year);
//             }
//         });
//     });

//     // 👇 Handle month selection — triggers update based on current year
//     document.getElementById('complaint-month-select').addEventListener('change', () => {
//         const year = document.getElementById('complaint-year-select').value;
//         const month = document.getElementById('complaint-month-select').value;
//         updateComplaintChart('monthly', null, null, year, month);
//     });

//     // 👇 Handle year selection — update both yearly and monthly views
//     document.getElementById('complaint-year-select').addEventListener('change', () => {
//         const year = document.getElementById('complaint-year-select').value;
//         const currentMode = document.querySelector('.btn.active[data-range]')?.getAttribute('data-range') ||
//             document.querySelector('.btn[data-range]:nth-child(2)').getAttribute('data-range'); // default to monthly

//         if (currentMode === 'monthly') {
//             const month = document.getElementById('complaint-month-select').value;
//             updateComplaintChart('monthly', null, null, year, month);
//         } else if (currentMode === 'yearly') {
//             updateComplaintChart('yearly', null, null, year);
//         }
//     });

//     document.getElementById('apply-complaint-filters').addEventListener('click', () => {
//         const from = document.getElementById('complaint-from').value;
//         const to = document.getElementById('complaint-to').value;
//         const year = document.getElementById('complaint-year-select').value;
//         const month = document.getElementById('complaint-month-select').value;

//         if (document.getElementById('complaint-year-selector').style.display === 'block') {
//             updateComplaintChart('yearly', null, null, year);
//         } else if (document.getElementById('complaint-month-selector').style.display === 'block') {
//             updateComplaintChart('monthly', null, null, year, month);
//         } else {
//             updateComplaintChart('daily', from, to);
//         }
//     });

//     // Initial load
//     updateComplaintChart('daily', formatDate(from), formatDate(now));
// }

// Engineers Responsibility
let currentFrom = null;
let currentTo = null;

function formatShort(dateStr) {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

function fetchEngineerData(from = null, to = null) {
    let url = `${baseUrl}/api/dashboards/assignee-daily-status`;
    if (from && to) {
        url += `?from=${from}&to=${to}`;
    }

    fetch(url)
        .then(res => res.json())
        .then(data => {
            currentFrom = data.dates[0];
            currentTo = data.dates[data.dates.length - 1];

            const dayHeaderRow = document.getElementById('dayHeaders');
            const thead = dayHeaderRow.parentNode;

            // 👇 STEP 1: Clear existing dynamic headers (date row + subheader row)
            dayHeaderRow.innerHTML = ''; // Clear date headers

            // Remove existing subheader row if exists (to prevent stacking)
            const existingSubHeader = thead.querySelector('tr[data-subheader="true"]');
            if (existingSubHeader) {
                existingSubHeader.remove();
            }

            // Hide static status headers (Open, Pending, Closed)
            document.getElementById('openHeader').colSpan = 0;
            document.getElementById('pendingHeader').style.display = 'none';
            document.getElementById('closedHeader').colSpan = 0;

            const numDays = data.dates.length;

            // 👇 STEP 2: Render new date headers (grouped by date)
            for (let i = 0; i < numDays; i++) {
                const dateShort = formatShort(data.dates[i]);
                const thDate = document.createElement('th');

                thDate.colSpan = 2;
                thDate.classList.add('bg-primary');
                thDate.style.verticalAlign = 'middle';
                dayHeaderRow.appendChild(thDate);

                const a = document.createElement('a');
                a.href = `/complaints?reportDateFrom=${data.dates[i]}&reportDateTo=${data.dates[i]}`;
                a.className = 'font-weight-bold text-white';
                a.textContent = dateShort;
                thDate.appendChild(a);
            }

            // 👇 STEP 3: Create and insert subheader row (Open / Closed)
            const subHeaderRow = document.createElement('tr');
            subHeaderRow.setAttribute('data-subheader', 'true'); // marker for cleanup
            subHeaderRow.classList.add('bg-primary', 'text-white'); // 🔵 Primary background

            for (let i = 0; i < numDays; i++) {
                const thOpen = document.createElement('th');
                thOpen.style.fontSize = '0.8em';
                subHeaderRow.appendChild(thOpen);

                const aOpen = document.createElement('a');
                aOpen.href = `/complaints?reportDateFrom=${data.dates[i]}&reportDateTo=${data.dates[i]}&status=OPEN`;
                aOpen.className = 'font-weight-bold text-white';
                aOpen.textContent = 'Open';
                thOpen.appendChild(aOpen);

                const thClosed = document.createElement('th');
                thClosed.style.fontSize = '0.8em';
                subHeaderRow.appendChild(thClosed);

                const aClosed = document.createElement('a');
                aClosed.href = `/complaints?reportDateFrom=${data.dates[i]}&reportDateTo=${data.dates[i]}&status=CLOSED`;
                aClosed.className = 'font-weight-bold text-white';
                aClosed.textContent = 'Closed';
                thClosed.appendChild(aClosed);
            }

            // Insert subheader row right after dayHeaders
            thead.insertBefore(subHeaderRow, dayHeaderRow.nextSibling);

            // 👇 STEP 4: Render table body
            const tbody = document.getElementById('engineerTableBody');
            tbody.innerHTML = '';

            data.data.forEach(row => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                <td class="text-left fw-bold"><a class="font-weight-bold text-dark" href="/complaints?assigneeEmpId=${row.assigneeEmpId}">${row.assigneeName}</a></td>
                ${data.dates.map((date, i) => `
                    <td><a class="text-dark" href="/complaints?assigneeEmpId=${row.assigneeEmpId}&reportDateFrom=${data.dates[i]}&reportDateTo=${data.dates[i]}&status=OPEN">${row.open[i] || 0}</a></td>
                    <td><a class="text-dark" href="/complaints?assigneeEmpId=${row.assigneeEmpId}&reportDateFrom=${data.dates[i]}&reportDateTo=${data.dates[i]}&status=CLOSED">${row.closed[i] || 0}</a></td>
                `).join('')}`;
                tbody.appendChild(tr);
            });
        })
        .catch(err => {
            console.error('Failed to load engineer data:', err);
        });
}

function shiftRange(offsetDays) {
    if (!currentFrom || !currentTo) return;

    const from = new Date(currentFrom);
    const to = new Date(currentTo);

    const daysDiff = Math.ceil((to - from) / (1000 * 60 * 60 * 24)); // number of days

    const newFrom = new Date(from);
    newFrom.setDate(newFrom.getDate() + offsetDays);
    const newTo = new Date(to);
    newTo.setDate(newTo.getDate() + offsetDays);

    const fromStr = toDateTimeLocal(newFrom).slice(0, 16); // '2025-08-10T00:00'
    const toStr = toDateTimeLocal(newTo).slice(0, 16);

    fetchEngineerData(fromStr, toStr);
}

document.getElementById('prevEngineerBtn').addEventListener('click', () => {
    shiftRange(-1); // shift backward by 1 day window size
});

document.getElementById('nextEngineerBtn').addEventListener('click', () => {
    shiftRange(1);
});

document.getElementById('refreshEngineerBtn').addEventListener('click', () => {
    currentFrom = null;
    currentTo = null;
    fetchEngineerData();
});

// Work Report Chart
let wrChart = null;

function renderWrChart(labels, corrective, preventive, breakdown, other, title) {
    const ctx = document.getElementById('wr-equipment-line-chart').getContext('2d');
    if (wrChart) wrChart.destroy();

    // Calculate stacked totals for dynamic y-axis
    const stackedValues = labels.map((_, i) =>
        (corrective[i] || 0) +
        (preventive[i] || 0) +
        (breakdown[i] || 0) +
        (other[i] || 0)
    );
    const totalMax = Math.max(...stackedValues, 0);

    let stepSize;
    if (totalMax <= 20) {
        stepSize = 5;
    } else if (totalMax <= 50) {
        stepSize = 10;
    } else if (totalMax <= 100) {
        stepSize = 20;
    } else if (totalMax <= 200) {
        stepSize = 25;
    } else if (totalMax <= 500) {
        stepSize = 50;
    } else {
        stepSize = Math.ceil(totalMax / 10 / 10) * 10;
    }

    const yAxisMax = Math.ceil(totalMax / stepSize) * stepSize;

    wrChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [
                {
                    label: "Corrective Maintenance",
                    backgroundColor: '#d9534f',
                    data: corrective
                },
                {
                    label: "Preventive Maintenance",
                    backgroundColor: '#59d05d',
                    data: preventive
                },
                {
                    label: "Breakdown",
                    backgroundColor: '#fdaf4b',
                    data: breakdown
                },
                {
                    label: "Other",
                    backgroundColor: '#95a5a6',
                    data: other
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: title,
                    font: {
                        size: 16
                    }
                },
                tooltip: {
                    mode: 'index',
                    intersect: false,
                    callbacks: {
                        label: function (context) {
                            return `${context.dataset.label}: ${context.parsed.y}`;
                        }
                    }
                },
                legend: {
                    position: 'bottom',
                    labels: {
                        color: '#333', // Chart.js v3 uses 'color'
                        padding: 15
                    }
                }
            },
            scales: {
                x: {
                    stacked: true
                },
                y: {
                    stacked: true,
                    beginAtZero: true,
                    max: yAxisMax,
                    ticks: {
                        stepSize: stepSize,
                        callback: function (value) {
                            return Number.isInteger(value) ? value : null;
                        }
                    },
                    title: {
                        display: true,
                        text: 'Number of Reports'
                    }
                }
            }
        }
    });
}

function updateWrChart(mode, from = null, to = null, year = null, equipmentCode = null) {
    let url = '';
    if (mode === 'yearly') {
        url = `/api/dashboards/monthly-work-report-equipment?year=${year}`;
        if (equipmentCode) url += `&equipmentCode=${encodeURIComponent(equipmentCode)}`;
    } else {
        const fromApi = toApiDateTime(from, false);
        const toApi = toApiDateTime(to, true);
        url = `/api/dashboards/daily-work-report-equipment?from=${from}&to=${to}`;
        if (equipmentCode) url += `&equipmentCode=${encodeURIComponent(equipmentCode)}`;
    }

    console.log("Fetching:", url);

    fetch(url)
        .then(r => {
            if (!r.ok) throw new Error(`HTTP error! status: ${r.status}`);
            return r.json();
        })
        .then(data => {
            if (!Array.isArray(data)) {
                console.warn("Expected array, got:", data);
                return;
            }

            let labels, correctiveData, preventiveData, breakdownData, otherData;

            if (mode === 'yearly') {
                const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
                labels = Array(12).fill('');
                correctiveData = Array(12).fill(0);
                preventiveData = Array(12).fill(0);
                breakdownData = Array(12).fill(0);
                otherData = Array(12).fill(0);

                data.forEach(d => {
                    const i = d.month - 1;
                    if (i >= 0 && i < 12) {
                        labels[i] = months[i];
                        correctiveData[i] = d.correctiveMaintenanceCount || 0;
                        preventiveData[i] = d.preventiveMaintenanceCount || 0;
                        breakdownData[i] = d.breakdownCount || 0;
                        otherData[i] = d.otherCount || 0;
                    }
                });

                // Fill in any missing months
                for (let i = 0; i < 12; i++) {
                    if (!labels[i]) labels[i] = months[i];
                }

                renderWrChart(labels, correctiveData, preventiveData, breakdownData, otherData, 'Monthly Work Report');
            } else {
                labels = data.map(d => d.date);
                correctiveData = data.map(d => d.correctiveMaintenanceCount || 0);
                preventiveData = data.map(d => d.preventiveMaintenanceCount || 0);
                breakdownData = data.map(d => d.breakdownCount || 0);
                otherData = data.map(d => d.otherCount || 0);

                renderWrChart(labels, correctiveData, preventiveData, breakdownData, otherData, 'Daily Work Report');
            }
        })
        .catch(err => {
            console.error("Error loading chart data:", err);
            const ctx = document.getElementById('wr-equipment-line-chart').getContext('2d');
            ctx.save();
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.font = "16px Arial";
            ctx.fillText("Failed to load data", ctx.canvas.width / 2, ctx.canvas.height / 2);
            ctx.restore();
        });
}

function initWrChartForm() {
    const now = new Date();
    const from = new Date(now);
    from.setDate(now.getDate() - 6);

    document.getElementById('wr-from').value = formatDate(from);
    document.getElementById('wr-to').value = formatDate(now);

    populateYearSelector('wr-year-select', now.getFullYear());

    fetch('/api/dashboards/equipment-complaint-count')
        .then(r => r.json())
        .then(equipmentList => {
            const select = document.getElementById('wr-equipment');
            equipmentList
                .sort((a, b) => a.equipmentName.localeCompare(b.equipmentName))
                .forEach(item => {
                    const opt = new Option(item.equipmentName, item.equipmentCode);
                    select.appendChild(opt);
                });

            const initialEquipment = document.getElementById('wr-equipment').value;
            updateWrChart('daily', formatDate(from), formatDate(now), null, initialEquipment);
        })
        .catch(err => {
            console.warn("Failed to load equipment list:", err);
            const initialEquipment = document.getElementById('wr-equipment').value;
            updateWrChart('daily', formatDate(from), formatDate(now), null, initialEquipment);
        });

    document.querySelectorAll('.dropdown-menu .btn[data-range]').forEach(btn => {
        btn.addEventListener('click', () => {
            const range = btn.getAttribute('data-range');
            const now = new Date();
            const equipmentCode = document.getElementById('wr-equipment').value;

            if (range === 'weekly') {
                document.getElementById('wr-year-selector').style.display = 'none';
                const from = new Date(now);
                from.setDate(now.getDate() - 6);
                document.getElementById('wr-from').value = formatDate(from);
                document.getElementById('wr-to').value = formatDate(now);
                updateWrChart('daily', formatDate(from), formatDate(now), null, equipmentCode);
            } else if (range === 'monthly') {
                document.getElementById('wr-year-selector').style.display = 'none';
                const from = new Date(now.getFullYear(), now.getMonth(), 1);
                document.getElementById('wr-from').value = formatDate(from);
                document.getElementById('wr-to').value = formatDate(now);
                updateWrChart('daily', formatDate(from), formatDate(now), null, equipmentCode);
            } else if (range === 'yearly') {
                document.getElementById('wr-year-selector').style.display = 'block';
                const year = document.getElementById('wr-year-select').value;
                updateWrChart('yearly', null, null, year, equipmentCode);
            }
        });
    });

    document.getElementById('wr-year-select').addEventListener('change', () => {
        const year = document.getElementById('wr-year-select').value;
        const equipmentCode = document.getElementById('wr-equipment').value;
        updateWrChart('yearly', null, null, year, equipmentCode);
    });

    document.getElementById('apply-filters-btn').addEventListener('click', () => {
        const from = document.getElementById('wr-from').value;
        const to = document.getElementById('wr-to').value;
        const equipmentCode = document.getElementById('wr-equipment').value;
        const year = document.getElementById('wr-year-select').value;

        if (document.getElementById('wr-year-selector').style.display === 'block') {
            updateWrChart('yearly', null, null, year, equipmentCode);
        } else {
            document.getElementById('wr-year-selector').style.display = 'none';
            updateWrChart('daily', from, to, null, equipmentCode);
        }
    });
}

document.addEventListener('DOMContentLoaded', () => {
    const equipmentSelect = document.getElementById('wr-equipment');

    if (!equipmentSelect) {
        console.error("Element #wr-equipment not found");
        return;
    }

    ['mousedown', 'click', 'focusin'].forEach(eventType => {
        equipmentSelect.addEventListener(eventType, (e) => {
            e.stopPropagation();
        });
    });
    initWrChartForm();
});

// Breakdown Chart
let breakdownChart = null;

function updateBreakdownChart(mode, from = null, to = null, year = null) {
    const ctx = document.getElementById('breakdown-line-chart').getContext('2d');
    if (breakdownChart) breakdownChart.destroy();

    if (mode === 'yearly') {
        fetch(`/api/dashboards/monthly-breakdown?year=${year}`)
            .then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            })
            .then(data => {
                if (!Array.isArray(data)) return;

                const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
                const values = Array(12).fill(0);
                const counts = Array(12).fill(0);

                data.forEach(d => {
                    const i = d.month - 1;
                    if (i >= 0 && i < 12) {
                        values[i] = d.totalTimeMinutes || 0;
                        counts[i] = d.breakdownCount || 0;
                    }
                });

                breakdownChart = new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: months,
                        datasets: [{
                            label: "Total Time (min)",
                            borderColor: "#1d7af3",
                            pointBorderColor: "#FFF",
                            pointBackgroundColor: "#1d7af3",
                            pointBorderWidth: 2,
                            pointHoverRadius: 4,
                            pointRadius: 4,
                            backgroundColor: 'transparent',
                            fill: true,
                            borderWidth: 2,
                            data: values,
                            breakdownCounts: counts // Attach count for tooltip
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        legend: {
                            position: 'bottom',
                            labels: { fontColor: '#1d7af3', padding: 15 }
                        },
                        tooltips: {
                            mode: "nearest",
                            intersect: false,
                            callbacks: {
                                label: function (tooltipItem, data) {
                                    const dataset = data.datasets[0];
                                    const value = dataset.data[tooltipItem.index];
                                    const count = dataset.breakdownCounts[tooltipItem.index];
                                    return [
                                        `Total Time: ${value} min`,
                                        `Breakdown Count: ${count}`
                                    ];
                                }
                            }
                        },
                        scales: {
                            x: { stacked: false },
                            y: {
                                beginAtZero: true,
                                title: {
                                    display: true,
                                    text: 'Total Time (minutes)'
                                },
                                ticks: {
                                    stepSize: Math.max(1, Math.round(Math.max(...values) / 10) || 1)
                                }
                            }
                        }
                    }
                });
            })
            .catch(err => console.error('Monthly breakdown fetch error:', err));

    } else {
        fetch(`/api/dashboards/daily-breakdown?from=${from}&to=${to}`)
            .then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            })
            .then(data => {
                if (!Array.isArray(data)) return;

                const labels = data.map(d => d.date);
                const values = data.map(d => d.totalTimeMinutes || 0);
                const counts = data.map(d => d.breakdownCount || 0);

                breakdownChart = new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: labels,
                        datasets: [{
                            label: "Total Time (min)",
                            borderColor: "#1d7af3",
                            pointBorderColor: "#FFF",
                            pointBackgroundColor: "#1d7af3",
                            pointBorderWidth: 2,
                            pointHoverRadius: 4,
                            pointRadius: 4,
                            backgroundColor: 'transparent',
                            fill: true,
                            borderWidth: 2,
                            data: values,
                            breakdownCounts: counts
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        legend: {
                            position: 'bottom',
                            labels: { fontColor: '#1d7af3', padding: 15 }
                        },
                        tooltips: {
                            mode: "nearest",
                            intersect: false,
                            callbacks: {
                                label: function (tooltipItem, data) {
                                    const dataset = data.datasets[0];
                                    const value = dataset.data[tooltipItem.index];
                                    const count = dataset.breakdownCounts[tooltipItem.index];
                                    return [
                                        `Total Time: ${value} min`,
                                        `Breakdown Count: ${count}`
                                    ];
                                }
                            }
                        },
                        scales: {
                            x: { stacked: false },
                            y: {
                                beginAtZero: true,
                                title: {
                                    display: true,
                                    text: 'Total Time (minutes)'
                                },
                                ticks: {
                                    stepSize: Math.max(1, Math.round(Math.max(...values) / 10) || 1)
                                }
                            }
                        }
                    }
                });
            })
            .catch(err => console.error('Daily breakdown fetch error:', err));
    }
}

function initBreakdownChartForm() {
    const now = new Date();
    const from = new Date(now);
    from.setDate(now.getDate() - 6);

    document.getElementById('breakdown-from').value = formatDate(from);
    document.getElementById('breakdown-to').value = formatDate(now);

    populateYearSelector('breakdown-year-select', now.getFullYear());

    document.querySelector('.dropdown-menu').addEventListener('click', function (e) {
        if (e.target.closest('select, input, .btn, .input-group')) {
            e.stopPropagation(); // 🔑 Keep dropdown open
        }
    });

    document.querySelectorAll('.dropdown-menu .btn[data-range]').forEach(btn => {
        btn.addEventListener('click', () => {
            const range = btn.getAttribute('data-range');
            const now = new Date();

            if (range === 'weekly') {
                document.getElementById('breakdown-year-selector').style.display = 'none';
                const from = new Date(now);
                from.setDate(now.getDate() - 6);
                document.getElementById('breakdown-from').value = formatDate(from);
                document.getElementById('breakdown-to').value = formatDate(now);
                updateBreakdownChart('daily', formatDate(from), formatDate(now));
            } else if (range === 'monthly') {
                document.getElementById('breakdown-year-selector').style.display = 'none';
                const from = new Date(now.getFullYear(), now.getMonth(), 1);
                document.getElementById('breakdown-from').value = formatDate(from);
                document.getElementById('breakdown-to').value = formatDate(now);
                updateBreakdownChart('daily', formatDate(from), formatDate(now));
            } else if (range === 'yearly') {
                document.getElementById('breakdown-year-selector').style.display = 'block';
                const year = document.getElementById('breakdown-year-select').value;
                updateBreakdownChart('yearly', null, null, year);
            }
        });
    });

    document.getElementById('breakdown-year-select').addEventListener('change', () => {
        const year = document.getElementById('breakdown-year-select').value;
        updateBreakdownChart('yearly', null, null, year);
    });

    document.getElementById('apply-breakdown-filters').addEventListener('click', () => {
        const from = document.getElementById('breakdown-from').value;
        const to = document.getElementById('breakdown-to').value;
        const year = document.getElementById('breakdown-year-select').value;

        if (document.getElementById('breakdown-year-selector').style.display === 'block') {
            updateBreakdownChart('yearly', null, null, year);
        } else {
            document.getElementById('breakdown-year-selector').style.display = 'none';
            updateBreakdownChart('daily', from, to);
        }
    });

    updateBreakdownChart('daily', formatDate(from), formatDate(now));
}

// Equipment Repaired
function formatNumber(num) {
    return num.toLocaleString();
}

function formatMinutes(minutes) {
    return `${formatNumber(minutes)} min`;
}

async function initEquipmentWorkList() {
    const container = document.getElementById('equipment-work-list-container');
    if (!container) {
        console.error('❌ Container not found');
        return;
    }

    container.innerHTML = '<div class="text-center py-3">Loading...</div>';

    try {
        const response = await fetch('/api/dashboards/equipment-count');
        if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`);

        const allData = await response.json();
        allData.sort((a, b) => (b.totalTime || 0) - (a.totalTime || 0));

        let currentPage = 1;

        // Render initial page
        renderPage(allData, currentPage);

        // Setup buttons
        const prevBtn = document.getElementById('equipment-work-prev-btn');
        const nextBtn = document.getElementById('equipment-work-next-btn');
        const refreshBtn = document.getElementById('equipment-work-refresh-btn');

        if (prevBtn) {
            prevBtn.addEventListener('click', () => {
                if (currentPage > 1) {
                    currentPage--;
                    renderPage(allData, currentPage);
                }
            });
        }

        if (nextBtn) {
            nextBtn.addEventListener('click', () => {
                if (allData.length > currentPage * 5) {
                    currentPage++;
                    renderPage(allData, currentPage);
                }
            });
        }

        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => {
                initEquipmentWorkList(); // Reload everything
            });
        }

        // Scroll navigation
        container.addEventListener('wheel', (e) => {
            if (e.deltaY === 0) return;
            e.preventDefault();

            if (e.deltaY > 0 && currentPage * 5 < allData.length) {
                currentPage++;
                renderPage(allData, currentPage);
            } else if (e.deltaY < 0 && currentPage > 1) {
                currentPage--;
                renderPage(allData, currentPage);
            }
        });

        container.style.cursor = 'grab';
        container.setAttribute('title', 'Scroll to navigate pages');

    } catch (err) {
        console.error('🚨 Error loading equipment work data:', err);
        container.innerHTML = `
            <div class="text-center text-muted py-3">
                Failed to load data: ${err.message}
            </div>
        `;
    }
}

function renderPage(allData, page) {
    const container = document.getElementById('equipment-work-list-container');
    if (!container) return;

    const start = (page - 1) * 5;
    const end = start + 5;
    const pageData = allData.slice(start, end);

    container.innerHTML = '';

    if (pageData.length === 0) {
        container.innerHTML = '<div class="text-center text-muted py-3">No data</div>';
        return;
    }

    pageData.forEach((item, index) => {
        const rank = start + index + 1;
        const avatarBg = 'bg-primary';
        const timeColor = rank === 1 ? 'text-danger' : rank === 2 ? 'text-warning' : rank === 3 ? 'text-success' : 'text-dark';

        const row = document.createElement('div');
        row.className = `d-flex align-items-center ${index < pageData.length - 1 ? 'mb-3' : ''}`;
        row.innerHTML = `
            <div class="avatar ${avatarBg} text-white rounded-circle d-flex align-items-center justify-content-center"
                 style="width: 40px; height: 40px; font-weight: bold;">
                ${rank}
            </div>
            <div class="flex-1 pt-1 ml-3">
                <h6 class="fw-bold mb-0">${item.equipmentName}</h6>
                <small class="text-muted">Code: ${item.equipmentCode}</small>
                <div class="mt-1">
                    <span class="badge bg-info ms-1">
                        <a href="/work-reports?equipmentCode=${item.equipmentCode}" class="text-dark text-decoration-none">
                            Work Reports: ${item.totalWorkReports}
                        </a>
                    </span>
                    <span class="badge bg-warning ms-1">
                        <a href="/complaints?equipmentCode=${item.equipmentCode}" class="text-dark text-decoration-none">
                            Complaints: ${item.totalComplaints}
                        </a>
                    </span>
                </div>
            </div>
            <div class="text-end ml-2">
                <h5 class="fw-bold ${timeColor}">${formatMinutes(item.totalTime)}</h5>
                <small class="text-muted d-block">Occurrences: <strong>${formatNumber(item.totalOccurrences)}</strong></small>
            </div>
        `;
        container.appendChild(row);

        const sep = document.createElement('div');
        sep.className = 'separator-dashed';
        container.appendChild(sep);
    });
}

window.addEventListener('DOMContentLoaded', () => {
    initComplaintStatsForm();
    initComplaintChartForm();
    fetchEngineerData();
    initEquipmentWorkList();
    initBreakdownChartForm();
});