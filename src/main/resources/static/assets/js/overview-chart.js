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

        if (status) params.append('state', status);

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

        console.log("Daily fetch:", url);
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
                url.searchParams.set('state', encodeURIComponent(status));
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
    document.querySelectorAll('.complaint-filter').forEach(btn => {
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

// Engineers Responsibility
let currentFrom = null;
let currentTo = null;
let totalData = [];

function formatShort(dateStr) {
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

function fetchTotalStatus() {
    return fetch(`${baseUrl}/api/dashboards/assignee-total-status`)
        .then(res => res.json())
        .then(data => {
            totalData = data;
            return data;
        })
        .catch(err => {
            console.error('Failed to load total status:', err);
            totalData = [];
            return [];
        });
}

// Helper: Create status cell with conditional highlighting
function createStatusCell(value, status, url) {
    const cell = document.createElement('td');
    const link = document.createElement('a');
    link.className = 'text-dark d-block';
    link.href = url;
    link.textContent = value;

    // Add background color for non-zero values
    if (value > 0) {
        if (status === 'OPEN') {
            cell.classList.add('bg-warning', 'text-dark'); // Yellow
        } else if (status === 'PENDING') {
            cell.classList.add('bg-danger', 'text-white'); // Light red (Bootstrap danger)
        } else if (status === 'CLOSED') {
            cell.classList.add('bg-success', 'text-white'); // Light green (Bootstrap success)
        }
    }

    cell.appendChild(link);
    return cell;
}

function fetchEngineerData(from = null, to = null) {
    fetchTotalStatus().then(() => {
        let url = `${baseUrl}/api/dashboards/assignee-daily-status`;
        if (from && to) url += `?from=${from}&to=${to}`;

        fetch(url)
            .then(res => res.json())
            .then(data => {
                currentFrom = data.dates[0];
                currentTo = data.dates[data.dates.length - 1];

                const thead = document.querySelector('#engineerTableBody').closest('table').querySelector('thead');
                const mainHeaderRow = thead.rows[0];
                const subHeaderRow = thead.rows[1];

                // Clear dynamic date headers (keep "Engineer" and "Total")
                while (mainHeaderRow.cells.length > 2) mainHeaderRow.deleteCell(-1);
                while (subHeaderRow.cells.length > 3) subHeaderRow.deleteCell(-1);

                // Add date headers
                data.dates.forEach(date => {
                    const dateShort = formatShort(date);

                    const thDate = document.createElement('th');
                    thDate.colSpan = 3;
                    thDate.classList.add('bg-primary', 'align-middle');
                    const aDate = document.createElement('a');
                    aDate.href = `/complaints?reportDateFrom=${date}&reportDateTo=${date}`;
                    aDate.className = 'font-weight-bold text-white';
                    aDate.textContent = dateShort;
                    thDate.appendChild(aDate);
                    mainHeaderRow.appendChild(thDate);

                    // Sub-headers: O / P / C (compact)
                    ['OPEN', 'PENDING', 'CLOSED'].forEach(status => {
                        const th = document.createElement('th');
                        const a = document.createElement('a');
                        a.href = `/complaints?reportDateFrom=${date}&reportDateTo=${date}&state=${status}`;
                        a.className = 'font-weight-bold text-white';
                        a.textContent = status.charAt(0);
                        th.appendChild(a);
                        subHeaderRow.appendChild(th);
                    });
                });

                // Render body
                const tbody = document.getElementById('engineerTableBody');
                tbody.innerHTML = '';

                const allAssignees = new Map();
                totalData.forEach(total => {
                    allAssignees.set(total.assigneeEmpId, {
                        name: total.assigneeName,
                        empId: total.assigneeEmpId,
                        totalOpen: total.totalOpen,
                        totalPending: total.totalPending,
                        totalClosed: total.totalClosed,
                        dailyData: null
                    });
                });

                data.data.forEach(daily => {
                    if (allAssignees.has(daily.assigneeEmpId)) {
                        allAssignees.get(daily.assigneeEmpId).dailyData = daily;
                    }
                });

                allAssignees.forEach(assignee => {
                    const tr = document.createElement('tr');

                    // Engineer name
                    const nameCell = document.createElement('td');
                    nameCell.className = 'text-left fw-bold';
                    const nameLink = document.createElement('a');
                    nameLink.className = 'font-weight-bold text-dark';
                    nameLink.href = `/complaints?assigneeEmpId=${assignee.empId}`;
                    nameLink.textContent = assignee.name;
                    nameCell.appendChild(nameLink);
                    tr.appendChild(nameCell);

                    // Total columns (O/P/C)
                    const totalCells = [
                        { status: 'OPEN', value: assignee.totalOpen },
                        { status: 'PENDING', value: assignee.totalPending },
                        { status: 'CLOSED', value: assignee.totalClosed }
                    ].map(item => {
                        const url = `/complaints?assigneeEmpId=${assignee.empId}&state=${item.status}`;
                        return createStatusCell(item.value, item.status, url);
                    });
                    tr.append(...totalCells);

                    // Daily columns (O/P/C for each date)
                    if (assignee.dailyData) {
                        data.dates.forEach((_, i) => {
                            // Open
                            const openUrl = `/complaints?assigneeEmpId=${assignee.empId}&reportDateFrom=${data.dates[i]}&reportDateTo=${data.dates[i]}`;
                            tr.appendChild(createStatusCell(assignee.dailyData.open[i] || 0, 'OPEN', openUrl));

                            // Pending
                            const pendingUrl = `/complaints?assigneeEmpId=${assignee.empId}&reportDateFrom=${data.dates[i]}&reportDateTo=${data.dates[i]}&state=PENDING`;
                            tr.appendChild(createStatusCell(assignee.dailyData.pending[i] || 0, 'PENDING', pendingUrl));

                            // Closed
                            const closedUrl = `/complaints?assigneeEmpId=${assignee.empId}&closeDate=${data.dates[i]}&state=CLOSED`;
                            tr.appendChild(createStatusCell(assignee.dailyData.closed[i] || 0, 'CLOSED', closedUrl));
                        });
                    } else {
                        // No daily data → show zeros (no highlighting)
                        data.dates.forEach(() => {
                            tr.innerHTML += '<td>0</td><td>0</td><td>0</td>';
                        });
                    }

                    tbody.appendChild(tr);
                });
            })
            .catch(err => console.error('Failed to load engineer data:', err));
    });
}

document.getElementById('prevEngineerBtn')?.addEventListener('click', () => shiftRange(-7));
document.getElementById('nextEngineerBtn')?.addEventListener('click', () => shiftRange(7));
document.getElementById('refreshEngineerBtn')?.addEventListener('click', () => {
    currentFrom = null; currentTo = null; fetchEngineerData();
});

function shiftRange(offsetDays) {
    if (!currentFrom || !currentTo) return;
    const from = new Date(currentFrom);
    const to = new Date(currentTo);
    from.setDate(from.getDate() + offsetDays);
    to.setDate(to.getDate() + offsetDays);
    const fromStr = from.toISOString().slice(0, 16);
    const toStr = to.toISOString().slice(0, 16);
    fetchEngineerData(fromStr, toStr);
}

fetchEngineerData();

// Work Report Chart
let wrChart = null;

function updateWrChart(mode, from = null, to = null, year = null, month = null) {
    if (mode === 'yearly') {
        const url = `/api/dashboards/monthly-work-report?year=${year}`;
        fetch(url)
            .then(r => r.json())
            .then(data => {
                if (!Array.isArray(data)) return;

                const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
                const labels = Array(12).fill('');
                const correctiveData = Array(12).fill(0);
                const preventiveData = Array(12).fill(0);
                const breakdownData = Array(12).fill(0);
                const otherData = Array(12).fill(0);

                data.forEach(d => {
                    const i = d.month - 1;
                    if (i >= 0 && i < 12) {
                        labels[i] = months[i];
                        correctiveData[i] = d.correctiveMaintenance || 0;
                        preventiveData[i] = d.preventiveMaintenance || 0;
                        breakdownData[i] = d.breakdown || 0;
                        otherData[i] = d.other || 0;
                    }
                });

                for (let i = 0; i < 12; i++) {
                    if (!labels[i]) labels[i] = months[i];
                }

                renderWrChart(labels, correctiveData, preventiveData, breakdownData, otherData, 'Monthly Work Report');
            })
            .catch(err => console.error('Work Report yearly fetch error:', err));

    } else if (mode === 'monthly') {
        if (!year) return;

        const monthNum = month || new Date().getMonth() + 1;
        const yearNum = year;

        const firstDay = new Date(Date.UTC(yearNum, monthNum - 1, 1));
        const lastDay = new Date(Date.UTC(yearNum, monthNum, 0));
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const effectiveLastDay = new Date(Math.min(lastDay, today));

        const fromStr = formatDate(firstDay);
        const toStr = formatDate(effectiveLastDay);
        const url = `/api/dashboards/daily-work-report?from=${fromStr}&to=${toStr}`;

        fetch(url)
            .then(r => r.json())
            .then(data => {
                if (!Array.isArray(data)) return;

                const labels = data.map(d => d.date);
                const correctiveData = data.map(d => d.correctiveMaintenance || 0);
                const preventiveData = data.map(d => d.preventiveMaintenance || 0);
                const breakdownData = data.map(d => d.breakdown || 0);
                const otherData = data.map(d => d.other || 0);

                renderWrChart(labels, correctiveData, preventiveData, breakdownData, otherData,
                    `Daily Work Reports for ${new Date(yearNum, monthNum - 1).toLocaleString('default', { month: 'long' })} ${yearNum}`);
            })
            .catch(err => console.error('Work Report monthly fetch error:', err));

    } else {
        const url = `/api/dashboards/daily-work-report?from=${from}&to=${to}`;
        fetch(url)
            .then(r => r.json())
            .then(data => {
                if (!Array.isArray(data)) return;

                const labels = data.map(d => d.date);
                const correctiveData = data.map(d => d.correctiveMaintenance || 0);
                const preventiveData = data.map(d => d.preventiveMaintenance || 0);
                const breakdownData = data.map(d => d.breakdown || 0);
                const otherData = data.map(d => d.other || 0);

                renderWrChart(labels, correctiveData, preventiveData, breakdownData, otherData, 'Daily Work Report');
            })
            .catch(err => console.error('Work Report daily fetch error:', err));
    }
}

function renderWrChart(labels, corrective, preventive, breakdown, other, title) {
    const ctx = document.getElementById('wr-equipment-line-chart').getContext('2d');
    if (wrChart) wrChart.destroy();

    const stackedValues = labels.map((_, i) =>
        (corrective[i] || 0) + (preventive[i] || 0) + (breakdown[i] || 0) + (other[i] || 0)
    );
    const totalMax = Math.max(...stackedValues, 0);

    let stepSize;
    if (totalMax <= 20) stepSize = 5;
    else if (totalMax <= 50) stepSize = 10;
    else if (totalMax <= 100) stepSize = 20;
    else if (totalMax <= 200) stepSize = 25;
    else if (totalMax <= 500) stepSize = 50;
    else stepSize = Math.ceil(totalMax / 10 / 10) * 10;

    const yAxisMax = Math.ceil(totalMax / stepSize) * stepSize;

    wrChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [
                { label: "Corrective Maintenance", backgroundColor: '#fdaf4b', data: corrective },
                { label: "Preventive Maintenance", backgroundColor: '#59d05d', data: preventive },
                { label: "Breakdown", backgroundColor: '#d9534f', data: breakdown },
                { label: "Other", backgroundColor: '#95a5a6', data: other }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
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
                        title: (tooltipItems) => tooltipItems[0].label,
                        label: (tooltipItem) => {
                            const datasetLabel = tooltipItem.dataset.label;
                            const value = tooltipItem.raw;
                            return `${datasetLabel}: ${value}`;
                        }
                    }
                }
            },
            scales: {
                x: { stacked: true, ticks: { autoSkip: true } },
                y: {
                    stacked: true,
                    beginAtZero: true,
                    max: yAxisMax,
                    ticks: {
                        stepSize: stepSize,
                        callback: v => Number.isInteger(v) ? v : ''
                    }
                }
            },
            onClick: (event, elements) => {
                if (elements.length === 0) return;

                const element = elements[0];
                const dataIndex = element.index;
                const datasetIndex = element.datasetIndex;

                const groupMap = ["CORRECTIVE_MAINTENANCE", "PREVENTIVE_MAINTENANCE", "BREAKDOWN", "OTHER"];
                const group = groupMap[datasetIndex];
                const label = labels[dataIndex];

                let from, to;
                if (label.length === 3) {
                    const year = document.getElementById('wr-year-select')?.value || new Date().getFullYear();
                    const monthMap = { "Jan": 0, "Feb": 1, "Mar": 2, "Apr": 3, "May": 4, "Jun": 5, "Jul": 6, "Aug": 7, "Sep": 8, "Oct": 9, "Nov": 10, "Dec": 11 };
                    const monthIndex = monthMap[label];
                    if (monthIndex !== undefined) {
                        const days = new Date(year, monthIndex + 1, 0).getDate();
                        from = `${year}-${String(monthIndex + 1).padStart(2, '0')}-01`;
                        to = `${year}-${String(monthIndex + 1).padStart(2, '0')}-${days}`;
                    } else {
                        from = to = label;
                    }
                } else {
                    from = to = label;
                }

                const url = new URL('/work-reports', window.location.origin);
                url.searchParams.set('reportDateFrom', from);
                url.searchParams.set('reportDateTo', to);
                url.searchParams.set('group', group);
                url.searchParams.set('sortBy', 'reportDate');
                url.searchParams.set('asc', 'true');
                url.searchParams.set('page', '1');
                window.location.href = url.toString();
            }
        }
    });
}

function initWrChartForm() {
    const now = new Date();
    const from = new Date(now);
    from.setDate(now.getDate() - 6);

    document.getElementById('wr-from').value = formatDate(from);
    document.getElementById('wr-to').value = formatDate(now);

    populateYearSelector('wr-year-select', now.getFullYear());
    document.getElementById('wr-month-select').value = String(now.getMonth() + 1).padStart(2, '0');

    document.getElementById('wr-year-selector').style.display = 'none';
    document.getElementById('wr-month-selector').style.display = 'none';
    document.getElementById('wr-weekly-nav').style.display = 'flex';

    updateWrWeekNav(formatDate(from), formatDate(now));
    updateWrChart('daily', formatDate(from), formatDate(now));

    // Keep dropdown open
    document.querySelector('#wrFilterDropdown + .dropdown-menu').addEventListener('click', e => {
        if (e.target.closest('select, input, .btn, .input-group')) e.stopPropagation();
    });

    // Filter buttons
    document.querySelectorAll('.wr-filter').forEach(btn => {
        btn.addEventListener('click', () => {
            const range = btn.getAttribute('data-range');
            const year = document.getElementById('wr-year-select').value;

            document.getElementById('wr-year-selector').style.display = 'none';
            document.getElementById('wr-month-selector').style.display = 'none';

            if (range === 'monthly') {
                document.getElementById('wr-month-selector').style.display = 'block';
                const month = document.getElementById('wr-month-select').value;
                updateWrChart('monthly', null, null, year, month);
            } else if (range === 'yearly') {
                document.getElementById('wr-year-selector').style.display = 'block';
                updateWrChart('yearly', null, null, year);
            }
        });
    });

    document.getElementById('wr-month-select').addEventListener('change', () => {
        const year = document.getElementById('wr-year-select').value;
        const month = document.getElementById('wr-month-select').value;
        updateWrChart('monthly', null, null, year, month);
    });

    document.getElementById('wr-year-select').addEventListener('change', () => {
        const year = document.getElementById('wr-year-select').value;
        const currentMode = document.querySelector('.wr-filter[data-range="monthly"]')?.classList.contains('active') ? 'monthly' : 'yearly';
        if (currentMode === 'monthly') {
            const month = document.getElementById('wr-month-select').value;
            updateWrChart('monthly', null, null, year, month);
        } else {
            updateWrChart('yearly', null, null, year);
        }
    });

    document.getElementById('apply-wr-filters').addEventListener('click', () => {
        const from = document.getElementById('wr-from').value;
        const to = document.getElementById('wr-to').value;
        const year = document.getElementById('wr-year-select').value;
        const month = document.getElementById('wr-month-select').value;

        if (document.getElementById('wr-year-selector').style.display === 'block') {
            updateWrChart('yearly', null, null, year);
        } else if (document.getElementById('wr-month-selector').style.display === 'block') {
            updateWrChart('monthly', null, null, year, month);
        } else {
            updateWrChart('daily', from, to);
        }
    });

    // Weekly nav
    document.getElementById('wr-prevWeekBtn').addEventListener('click', () => {
        const from = new Date(document.getElementById('wr-from').value);
        const to = new Date(document.getElementById('wr-to').value);
        from.setDate(from.getDate() - 7);
        to.setDate(to.getDate() - 7);
        const f = formatDate(from), t = formatDate(to);
        document.getElementById('wr-from').value = f;
        document.getElementById('wr-to').value = t;
        updateWrWeekNav(f, t);
        updateWrChart('daily', f, t);
    });

    document.getElementById('wr-nextWeekBtn').addEventListener('click', () => {
        const from = new Date(document.getElementById('wr-from').value);
        const to = new Date(document.getElementById('wr-to').value);
        from.setDate(from.getDate() + 7);
        to.setDate(to.getDate() + 7);
        const f = formatDate(from), t = formatDate(to);
        document.getElementById('wr-from').value = f;
        document.getElementById('wr-to').value = t;
        updateWrWeekNav(f, t);
        updateWrChart('daily', f, t);
    });
}

// Utility
function updateWrWeekNav(fromDate, toDate) {
    const label = document.getElementById('wr-weekRangeLabel');
    const start = new Date(fromDate), end = new Date(toDate);
    const startMonth = start.toLocaleString('default', { month: 'short' });
    const endMonth = end.toLocaleString('default', { month: 'short' });
    const monthPart = startMonth === endMonth ? startMonth : `${startMonth}–${endMonth}`;
    label.textContent = `Week of ${monthPart} ${start.getDate()}–${end.getDate()}, ${start.getFullYear()}`;
    document.getElementById('wr-prevWeekBtn').disabled = false;
    const today = new Date(); today.setHours(0, 0, 0, 0);
    document.getElementById('wr-nextWeekBtn').disabled = end >= today;
}

let breakdownChart = null;

function updateBreakdownChart(mode, from = null, to = null, year = null, month = null) {
    if (mode === 'yearly') {
        const url = `/api/dashboards/monthly-breakdown?year=${year}`;
        fetch(url)
            .then(r => r.json())
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

                renderBreakdownChart(months, values, counts, 'Monthly Breakdown Summary');
            })
            .catch(err => console.error('Breakdown yearly error:', err));
        console.log("url", url);

    } else if (mode === 'monthly') {
        if (!year) return;

        const monthNum = month || new Date().getMonth() + 1;
        const yearNum = year;

        const firstDay = new Date(Date.UTC(yearNum, monthNum - 1, 1));
        const lastDay = new Date(Date.UTC(yearNum, monthNum, 0));
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const effectiveLastDay = new Date(Math.min(lastDay, today));

        const fromStr = formatDate(firstDay);
        const toStr = formatDate(effectiveLastDay);
        const url = `/api/dashboards/daily-breakdown?from=${fromStr}&to=${toStr}`;

        fetch(url)
            .then(r => r.json())
            .then(data => {
                if (!Array.isArray(data)) return;

                const labels = data.map(d => d.date);
                const values = data.map(d => d.totalTimeMinutes || 0);
                const counts = data.map(d => d.breakdownCount || 0);

                renderBreakdownChart(labels, values, counts,
                    `Daily Breakdown for ${new Date(yearNum, monthNum - 1).toLocaleString('default', { month: 'long' })} ${yearNum}`);
            })
            .catch(err => console.error('Breakdown monthly error:', err));

    } else {
        const url = `/api/dashboards/daily-breakdown?from=${from}&to=${to}`;
        fetch(url)
            .then(r => r.json())
            .then(data => {
                if (!Array.isArray(data)) return;

                const labels = data.map(d => d.date);
                const values = data.map(d => d.totalTimeMinutes || 0);
                const counts = data.map(d => d.breakdownCount || 0);

                renderBreakdownChart(labels, values, counts, 'Daily Breakdown');
            })
            .catch(err => console.error('Breakdown daily error:', err));
    }
}

function renderBreakdownChart(labels, values, counts, title) {
    const ctx = document.getElementById('breakdown-line-chart').getContext('2d');
    if (breakdownChart) breakdownChart.destroy();

    const maxVal = Math.max(...values, 0);
    let stepSize = maxVal <= 20 ? 5 : maxVal <= 50 ? 10 : maxVal <= 100 ? 20 : maxVal <= 200 ? 25 : maxVal <= 500 ? 50 : Math.ceil(maxVal / 10 / 10) * 10;
    const yAxisMax = Math.ceil(maxVal / stepSize) * stepSize;

    breakdownChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: "Total Time (min)",
                borderColor: "#1d7af3",
                pointBackgroundColor: "#1d7af3",
                pointRadius: 4,
                borderWidth: 2,
                fill: false,
                data: values,
                breakdownCounts: counts
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                tooltip: {
                    mode: 'index',
                    intersect: false,
                    callbacks: {
                        label: ctx => [
                            `Total Time: ${ctx.parsed.y} min`,
                            `Breakdown Count: ${ctx.dataset.breakdownCounts[ctx.dataIndex]}`
                        ]
                    }
                }
            },
            scales: {
                x: { grid: { display: false } },
                y: {
                    beginAtZero: true,
                    title: { display: true, text: 'Total Time (minutes)' },
                    ticks: { stepSize: stepSize }
                }
            },
            onClick: (event, elements) => {
                if (elements.length === 0) return;

                const label = labels[elements[0].index];
                let from, to;
                if (label.length === 3) {
                    const year = document.getElementById('breakdown-year-select')?.value || new Date().getFullYear();
                    const monthMap = { "Jan": 0, "Feb": 1, "Mar": 2, "Apr": 3, "May": 4, "Jun": 5, "Jul": 6, "Aug": 7, "Sep": 8, "Oct": 9, "Nov": 10, "Dec": 11 };
                    const i = monthMap[label];
                    if (i !== undefined) {
                        const days = new Date(year, i + 1, 0).getDate();
                        from = `${year}-${String(i + 1).padStart(2, '0')}-01`;
                        to = `${year}-${String(i + 1).padStart(2, '0')}-${days}`;
                    } else {
                        from = to = label;
                    }
                } else {
                    from = to = label;
                }

                const url = new URL('/work-reports', window.location.origin);
                url.searchParams.set('reportDateFrom', from);
                url.searchParams.set('reportDateTo', to);
                url.searchParams.set('group', 'BREAKDOWN');
                url.searchParams.set('sortBy', 'reportDate');
                url.searchParams.set('asc', 'true');
                url.searchParams.set('page', '1');
                window.location.href = url.toString();
            }
        }
    });
}

function initBreakdownChartForm() {
    const now = new Date();
    const from = new Date(now);
    from.setDate(now.getDate() - 6);

    document.getElementById('breakdown-from').value = formatDate(from);
    document.getElementById('breakdown-to').value = formatDate(now);

    populateYearSelector('breakdown-year-select', now.getFullYear());
    document.getElementById('breakdown-month-select').value = String(now.getMonth() + 1).padStart(2, '0');

    document.getElementById('breakdown-year-selector').style.display = 'none';
    document.getElementById('breakdown-month-selector').style.display = 'none';
    document.getElementById('breakdown-weekly-nav').style.display = 'flex';

    updateBreakdownWeekNav(formatDate(from), formatDate(now));
    updateBreakdownChart('daily', formatDate(from), formatDate(now));

    // Keep dropdown open
    document.querySelector('#breakdownFilterDropdown + .dropdown-menu').addEventListener('click', e => {
        if (e.target.closest('select, input, .btn, .input-group')) e.stopPropagation();
    });

    // Filter buttons
    document.querySelectorAll('.breakdown-filter').forEach(btn => {
        btn.addEventListener('click', () => {
            const range = btn.getAttribute('data-range');
            const year = document.getElementById('breakdown-year-select').value;

            document.getElementById('breakdown-year-selector').style.display = 'none';
            document.getElementById('breakdown-month-selector').style.display = 'none';

            if (range === 'monthly') {
                document.getElementById('breakdown-month-selector').style.display = 'block';
                const month = document.getElementById('breakdown-month-select').value;
                updateBreakdownChart('monthly', null, null, year, month);
            } else if (range === 'yearly') {
                document.getElementById('breakdown-year-selector').style.display = 'block';
                updateBreakdownChart('yearly', null, null, year);
            }
        });
    });

    document.getElementById('breakdown-month-select').addEventListener('change', () => {
        const year = document.getElementById('breakdown-year-select').value;
        const month = document.getElementById('breakdown-month-select').value;
        updateBreakdownChart('monthly', null, null, year, month);
    });

    document.getElementById('breakdown-year-select').addEventListener('change', () => {
        const year = document.getElementById('breakdown-year-select').value;
        const currentMode = document.querySelector('.breakdown-filter[data-range="monthly"]')?.classList.contains('active') ? 'monthly' : 'yearly';
        if (currentMode === 'monthly') {
            const month = document.getElementById('breakdown-month-select').value;
            updateBreakdownChart('monthly', null, null, year, month);
        } else {
            updateBreakdownChart('yearly', null, null, year);
        }
    });

    document.getElementById('apply-breakdown-filters').addEventListener('click', () => {
        const from = document.getElementById('breakdown-from').value;
        const to = document.getElementById('breakdown-to').value;
        const year = document.getElementById('breakdown-year-select').value;
        const month = document.getElementById('breakdown-month-select').value;

        if (document.getElementById('breakdown-year-selector').style.display === 'block') {
            updateBreakdownChart('yearly', null, null, year);
        } else if (document.getElementById('breakdown-month-selector').style.display === 'block') {
            updateBreakdownChart('monthly', null, null, year, month);
        } else {
            updateBreakdownChart('daily', from, to);
        }
    });

    // Weekly nav
    document.getElementById('breakdown-prevWeekBtn').addEventListener('click', () => {
        const from = new Date(document.getElementById('breakdown-from').value);
        const to = new Date(document.getElementById('breakdown-to').value);
        from.setDate(from.getDate() - 7);
        to.setDate(to.getDate() - 7);
        const f = formatDate(from), t = formatDate(to);
        document.getElementById('breakdown-from').value = f;
        document.getElementById('breakdown-to').value = t;
        updateBreakdownWeekNav(f, t);
        updateBreakdownChart('daily', f, t);
    });

    document.getElementById('breakdown-nextWeekBtn').addEventListener('click', () => {
        const from = new Date(document.getElementById('breakdown-from').value);
        const to = new Date(document.getElementById('breakdown-to').value);
        from.setDate(from.getDate() + 7);
        to.setDate(to.getDate() + 7);
        const f = formatDate(from), t = formatDate(to);
        document.getElementById('breakdown-from').value = f;
        document.getElementById('breakdown-to').value = t;
        updateBreakdownWeekNav(f, t);
        updateBreakdownChart('daily', f, t);
    });
}

function updateBreakdownWeekNav(fromDate, toDate) {
    const label = document.getElementById('breakdown-weekRangeLabel');
    const start = new Date(fromDate), end = new Date(toDate);
    const startMonth = start.toLocaleString('default', { month: 'short' });
    const endMonth = end.toLocaleString('default', { month: 'short' });
    const monthPart = startMonth === endMonth ? startMonth : `${startMonth}–${endMonth}`;
    label.textContent = `Week of ${monthPart} ${start.getDate()}–${end.getDate()}, ${start.getFullYear()}`;
    document.getElementById('breakdown-prevWeekBtn').disabled = false;
    const today = new Date(); today.setHours(0, 0, 0, 0);
    document.getElementById('breakdown-nextWeekBtn').disabled = end >= today;
}

// Equipment Repaired
function formatNumber(num) {
    return num.toLocaleString();
}

function formatMinutesToDH(minutes) {
    if (!minutes || minutes <= 0) return '0 hr';

    // Round minutes to nearest hour (30+ minutes rounds up)
    const totalHours = Math.round(minutes / 60);

    const days = Math.floor(totalHours / 24);
    const hours = totalHours % 24;

    const parts = [];
    if (days > 0) parts.push(`${days} Day${days > 1 ? 's' : ''}`);
    if (hours > 0) parts.push(`${hours} Hour${hours > 1 ? 's' : ''}`);

    return parts.length > 0 ? parts.join(' ') : '0 hr';
}

async function initEquipmentWorkList() {
    const container = document.getElementById('equipment-count-container');
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
    const container = document.getElementById('equipment-count-container');
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
                <small class="text-muted">${item.equipmentCode}</small>
                <div class="mt-1">
                    <span class="badge bg-info ms-1">
                        <a href="/work-reports?equipmentCode=${item.equipmentCode}" class="text-white">
                            Work Reports: ${item.totalWorkReports}
                        </a>
                    </span>
                    <span class="badge bg-warning ms-1">
                        <a href="/complaints?equipmentCode=${item.equipmentCode}" class="text-white">
                            Complaints: ${item.totalComplaints}
                        </a>
                    </span>
                </div>
            </div>
            <div class="text-end ml-2">
                <h5 class="fw-bold ${timeColor}">${formatMinutesToDH(item.totalTime)}</h5>
                <small class="text-muted d-block">Occurrences: <strong>${formatNumber(item.totalOccurrences)}</strong></small>
            </div>
        `;
        container.appendChild(row);

        const sep = document.createElement('div');
        sep.className = 'separator-dashed';
        container.appendChild(sep);
    });
}

function formatNumber(num) {
    return num.toLocaleString();
}

async function initEquipmentStatusList() {
    const container = document.getElementById('equipment-status-container');
    if (!container) {
        console.error('❌ Equipment status container not found');
        return;
    }

    container.innerHTML = '<div class="text-center py-3">Loading...</div>';

    try {
        const response = await fetch('/api/dashboards/equipment-status');
        if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`);

        const allData = await response.json();

        // Compute total active issues and sort by it (desc)
        allData.forEach(item => {
            item.totalActive = (item.openWr || 0) + (item.pendingWr || 0) +
                (item.openCp || 0) + (item.pendingCp || 0);
        });
        allData.sort((a, b) => b.totalActive - a.totalActive);

        let currentPage = 1;

        // Render initial page
        renderStatusPage(allData, currentPage);

        // Setup buttons
        const prevBtn = document.getElementById('equipment-status-prev-btn');
        const nextBtn = document.getElementById('equipment-status-next-btn');
        const refreshBtn = document.getElementById('equipment-status-refresh-btn');

        if (prevBtn) {
            prevBtn.addEventListener('click', () => {
                if (currentPage > 1) {
                    currentPage--;
                    renderStatusPage(allData, currentPage);
                }
            });
        }

        if (nextBtn) {
            nextBtn.addEventListener('click', () => {
                if (allData.length > currentPage * 5) {
                    currentPage++;
                    renderStatusPage(allData, currentPage);
                }
            });
        }

        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => {
                initEquipmentStatusList(); // Reload everything
            });
        }

        // Scroll navigation
        container.addEventListener('wheel', (e) => {
            if (e.deltaY === 0) return;
            e.preventDefault();

            if (e.deltaY > 0 && currentPage * 5 < allData.length) {
                currentPage++;
                renderStatusPage(allData, currentPage);
            } else if (e.deltaY < 0 && currentPage > 1) {
                currentPage--;
                renderStatusPage(allData, currentPage);
            }
        });

        container.style.cursor = 'grab';
        container.setAttribute('title', 'Scroll to navigate pages');

    } catch (err) {
        console.error('🚨 Error loading equipment status data:', err);
        container.innerHTML = `
            <div class="text-center text-muted py-3">
                Failed to load data: ${err.message}
            </div>
        `;
    }
}

function renderStatusPage(allData, page) {
    const container = document.getElementById('equipment-status-container');
    if (!container) return;

    const start = (page - 1) * 7;
    const end = start + 7;
    const pageData = allData.slice(start, end);

    container.innerHTML = '';

    if (pageData.length === 0) {
        container.innerHTML = '<div class="text-center text-muted py-3">No equipment in trouble</div>';
        return;
    }

    pageData.forEach((item, index) => {
        const hasCp = (item.openCp || 0) > 0 || (item.pendingCp || 0) > 0;
        const hasWr = (item.openWr || 0) > 0 || (item.pendingWr || 0) > 0;

        let badgesHtml = '';

        if (!hasCp && !hasWr) {
            // Show "Good" badge if nothing is open or pending
            badgesHtml = `<span class="badge badge-success align-self-start px-2 py-1">Good</span>`;
        } else {
            const cpBadges = [];
            const wrBadges = [];

            // Only add CP label and counts if CP has data
            if (hasCp) {
                cpBadges.push(`<span class="badge badge-primary text-white font-weight-normal mr-2">
                    <a href="/complaints?equipmentCode=${item.code}" class="text-white">
                        Complaints
                    </a>
                </span>`);
                if ((item.openCp || 0) > 0) {
                    cpBadges.push(`<span class="badge badge-warning mr-2">
                        <a href="/complaints?equipmentCode=${item.code}&state=OPEN" class="text-white">
                            Open: ${formatNumber(item.openCp)}
                        </a>    
                    </span>`);
                }
                if ((item.pendingCp || 0) > 0) {
                    cpBadges.push(`<span class="badge badge-danger">
                        <a href="/complaints?equipmentCode=${item.code}&state=PENDING" class="text-white">
                            Pending: ${formatNumber(item.pendingCp)}
                        </a> 
                    </span>`);
                }
            }

            // Only add WR label and counts if WR has data
            if (hasWr) {
                wrBadges.push(`<span class="badge badge-secondary text-white font-weight-normal mr-2">
                    <a href="/work-reports?equipmentCode=${item.code}" class="text-white">
                        Work Reports
                    </a>
                </span>`);
                if ((item.openWr || 0) > 0) {
                    wrBadges.push(`<span class="badge badge-warning mr-2">
                        <a href="/work-reports?equipmentCode=${item.code}&state=OPEN" class="text-white">
                            Open: ${formatNumber(item.openWr)}
                        </a>
                    </span>`);
                }
                if ((item.pendingWr || 0) > 0) {
                    wrBadges.push(`<span class="badge badge-danger">
                        <a href="/work-reports?equipmentCode=${item.code}&state=PENDING" class="text-white">
                            Pending: ${formatNumber(item.pendingWr)}
                        </a>    
                    </span>`);
                }
            }

            // Combine CP and WR lines
            const lines = [];
            if (cpBadges.length) lines.push(`<span class="d-flex align-items-center mb-1">${cpBadges.join('')}</span>`);
            if (wrBadges.length) lines.push(`<span class="d-flex align-items-center">${wrBadges.join('')}</span>`);

            badgesHtml = `<div class="text-right">${lines.join('')}</div>`;
        }

        const row = document.createElement('div');
        row.className = 'd-flex justify-content-between align-items-start mb-3';
        row.innerHTML = `
        <div>
            <div class="font-weight-bold text-dark">${item.name}</div>
            <small class="text-muted">${item.code}</small>
        </div>
        ${badgesHtml}
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
    initWrChartForm();
    initBreakdownChartForm();
    initEquipmentWorkList();
    initEquipmentStatusList();
});