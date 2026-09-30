<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div class="page-header">
    <div class="page-header-content">
        <h1 class="page-title">Analytics Dashboard</h1>
        <p class="page-description">Comprehensive analytics for inventory, demand, and logistics performance.</p>
    </div>
    <div class="page-header-actions">
        <form id="dateFilterForm" class="d-flex" style="gap: var(--space-3);">
            <input type="date" id="startDate" name="startDate" class="form-control form-control-sm" title="Start Date">
            <input type="date" id="endDate" name="endDate" class="form-control form-control-sm" title="End Date">
            <button type="submit" class="btn btn-sm btn-primary">Apply</button>
        </form>
    </div>
</div>

<div id="analyticsLoading" class="text-center my-5" style="display: none;">
    <div class="spinner"></div>
    <p class="mt-3 text-muted">Loading analytics data...</p>
</div>

<div id="analyticsContent">
    <div class="row" style="display: flex; gap: var(--space-6); margin-bottom: var(--space-6);">
        <div class="card" style="flex: 2;">
            <div class="card-header">
                <span class="card-header-title"><i class="bi bi-graph-up"></i> Inventory Trends</span>
            </div>
            <div class="card-body" style="height: 300px; position: relative;">
                <canvas id="inventoryTrendsChart"></canvas>
                <div id="inventoryTrendsEmpty" class="text-center text-muted" style="display:none; padding-top: 130px;">No data available</div>
            </div>
        </div>
        <div class="card" style="flex: 1;">
            <div class="card-header">
                <span class="card-header-title"><i class="bi bi-pie-chart"></i> Risk Distribution</span>
            </div>
            <div class="card-body" style="height: 300px; position: relative;">
                <canvas id="riskDistributionChart"></canvas>
                <div id="riskDistributionEmpty" class="text-center text-muted" style="display:none; padding-top: 130px;">No data available</div>
            </div>
        </div>
    </div>

    <div class="row" style="display: flex; gap: var(--space-6); margin-bottom: var(--space-6);">
        <div class="card" style="flex: 1;">
            <div class="card-header">
                <span class="card-header-title"><i class="bi bi-bar-chart"></i> Transfer Volume</span>
            </div>
            <div class="card-body" style="height: 300px; position: relative;">
                <canvas id="transferVolumeChart"></canvas>
                <div id="transferVolumeEmpty" class="text-center text-muted" style="display:none; padding-top: 130px;">No data available</div>
            </div>
        </div>
        <div class="card" style="flex: 1;">
            <div class="card-header">
                <span class="card-header-title"><i class="bi bi-bar-chart-steps"></i> Demand Patterns</span>
            </div>
            <div class="card-body" style="height: 300px; position: relative;">
                <canvas id="demandPatternsChart"></canvas>
                <div id="demandPatternsEmpty" class="text-center text-muted" style="display:none; padding-top: 130px;">No data available</div>
            </div>
        </div>
    </div>

    <c:if test="${sessionScope.ROLE == 'ADMIN'}">
        <div class="row" style="display: flex; gap: var(--space-6); margin-bottom: var(--space-6);">
            <div class="card" style="flex: 1;">
                <div class="card-header">
                    <span class="card-header-title"><i class="bi bi-radar"></i> Facility Performance</span>
                </div>
                <div class="card-body" style="height: 400px; position: relative;">
                    <canvas id="facilityPerformanceChart"></canvas>
                    <div id="facilityPerformanceEmpty" class="text-center text-muted" style="display:none; padding-top: 180px;">No data available</div>
                </div>
            </div>
        </div>
    </c:if>
</div>

<script>
    let charts = {};

    document.addEventListener('DOMContentLoaded', function() {
        // Set default dates (last 30 days)
        const today = new Date();
        const thirtyDaysAgo = new Date(today);
        thirtyDaysAgo.setDate(today.getDate() - 30);
        
        document.getElementById('startDate').value = thirtyDaysAgo.toISOString().split('T')[0];
        document.getElementById('endDate').value = today.toISOString().split('T')[0];

        loadAnalytics();

        document.getElementById('dateFilterForm').addEventListener('submit', function(e) {
            e.preventDefault();
            loadAnalytics();
        });
    });

    function loadAnalytics() {
        const startDate = document.getElementById('startDate').value;
        const endDate = document.getElementById('endDate').value;
        const queryParams = '?startDate=' + startDate + '&endDate=' + endDate;
        const basePath = '${pageContext.request.contextPath}';

        document.getElementById('analyticsLoading').style.display = 'block';
        document.getElementById('analyticsContent').style.opacity = '0.5';

        let loaded = 0;
        const total = '${sessionScope.ROLE}' === 'ADMIN' ? 5 : 4;
        function done() {
            loaded++;
            if (loaded >= total) {
                document.getElementById('analyticsLoading').style.display = 'none';
                document.getElementById('analyticsContent').style.opacity = '1';
            }
        }

        fetch(basePath + '/api/analytics/inventory-trends' + queryParams)
            .then(res => res.json()).then(data => { renderInventoryTrends(data); done(); })
            .catch(e => { console.error('inventory-trends error:', e); done(); });

        fetch(basePath + '/api/analytics/transfer-metrics' + queryParams)
            .then(res => res.json()).then(data => { renderTransferMetrics(data); done(); })
            .catch(e => { console.error('transfer-metrics error:', e); done(); });

        fetch(basePath + '/api/analytics/demand-patterns' + queryParams)
            .then(res => res.json()).then(data => { renderDemandPatterns(data); done(); })
            .catch(e => { console.error('demand-patterns error:', e); done(); });

        fetch(basePath + '/api/analytics/risk-heatmap')
            .then(res => res.json()).then(data => { renderRiskDistribution(data); done(); })
            .catch(e => { console.error('risk-heatmap error:', e); done(); });

        if ('${sessionScope.ROLE}' === 'ADMIN') {
            fetch(basePath + '/api/analytics/facility-performance' + queryParams)
                .then(res => res.json()).then(data => { renderFacilityPerformance(data); done(); })
                .catch(e => { console.error('facility-performance error:', e); done(); });
        }
    }

    function safeRender(chartId, config, isEmpty) {
        const canvas = document.getElementById(chartId);
        const emptyId = chartId.replace('Chart', '') + 'Empty';
        const emptyDiv = document.getElementById(emptyId);
        
        if (isEmpty) {
            canvas.style.display = 'none';
            if (emptyDiv) emptyDiv.style.display = 'block';
            if (charts[chartId]) {
                charts[chartId].destroy();
                charts[chartId] = null;
            }
        } else {
            canvas.style.display = 'block';
            if (emptyDiv) emptyDiv.style.display = 'none';
            if (charts[chartId]) {
                charts[chartId].destroy();
            }
            const ctx2 = canvas.getContext('2d');
            charts[chartId] = new Chart(ctx2, config);
        }
    }

    function renderInventoryTrends(data) {
        const isEmpty = !data.labels || data.labels.length === 0;
        
        const config = {
            type: 'line',
            data: {
                labels: data.labels,
                datasets: [
                    { label: 'Received', data: data.datasets ? data.datasets.RECEIVED : [], borderColor: '#198754', tension: 0.1 },
                    { label: 'Dispensed', data: data.datasets ? data.datasets.DISPENSED : [], borderColor: '#dc3545', tension: 0.1 },
                    { label: 'Consumed', data: data.datasets ? data.datasets.CONSUMPTION : [], borderColor: '#ffc107', tension: 0.1 }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: { y: { beginAtZero: true } }
            }
        };
        safeRender('inventoryTrendsChart', config, isEmpty);
    }

    function renderTransferMetrics(data) {
        const isEmpty = !data.labels || data.labels.length === 0;

        const config = {
            type: 'bar',
            data: {
                labels: data.labels,
                datasets: [
                    { label: 'Requested', data: data.datasets ? data.datasets.REQUESTED : [], backgroundColor: '#0dcaf0' },
                    { label: 'Completed', data: data.datasets ? data.datasets.COMPLETED : [], backgroundColor: '#198754' },
                    { label: 'Cancelled', data: data.datasets ? data.datasets.CANCELLED : [], backgroundColor: '#dc3545' }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: { y: { beginAtZero: true, stacked: true }, x: { stacked: true } }
            }
        };
        safeRender('transferVolumeChart', config, isEmpty);
    }

    function renderDemandPatterns(data) {
        const isEmpty = !data.medicineLabels || data.medicineLabels.length === 0;

        const config = {
            type: 'bar',
            data: {
                labels: data.medicineLabels,
                datasets: [{
                    label: 'Quantity Needed',
                    data: data.medicineQuantities,
                    backgroundColor: '#6f42c1'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: { y: { beginAtZero: true } },
                plugins: { legend: { display: false } }
            }
        };
        safeRender('demandPatternsChart', config, isEmpty);
    }

    function renderRiskDistribution(data) {
        const total = (data.CRITICAL || 0) + (data.HIGH || 0) + (data.MODERATE || 0) + (data.LOW || 0);
        const isEmpty = total === 0;

        const config = {
            type: 'doughnut',
            data: {
                labels: ['Critical', 'High', 'Moderate', 'Low'],
                datasets: [{
                    data: [data.CRITICAL || 0, data.HIGH || 0, data.MODERATE || 0, data.LOW || 0],
                    backgroundColor: ['#dc3545', '#fd7e14', '#ffc107', '#198754']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'right' } }
            }
        };
        safeRender('riskDistributionChart', config, isEmpty);
    }

    function renderFacilityPerformance(data) {
        const isEmpty = !data.labels || data.labels.length === 0;

        const config = {
            type: 'radar',
            data: {
                labels: data.labels,
                datasets: [
                    {
                        label: 'Total Transfers',
                        data: data.totalTransfers,
                        backgroundColor: 'rgba(13, 202, 240, 0.2)',
                        borderColor: '#0dcaf0',
                        pointBackgroundColor: '#0dcaf0'
                    },
                    {
                        label: 'Completed Transfers',
                        data: data.completedTransfers,
                        backgroundColor: 'rgba(25, 135, 84, 0.2)',
                        borderColor: '#198754',
                        pointBackgroundColor: '#198754'
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false
            }
        };
        safeRender('facilityPerformanceChart', config, isEmpty);
    }
</script>
