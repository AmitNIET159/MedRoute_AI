<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<div class="page-header">
    <div class="page-header-content">
        <h1 class="page-title">Demand Intelligence</h1>
        <p class="page-description">Deterministic logistics analysis and inventory risk engine.</p>
    </div>
</div>

<div class="stats-grid" style="margin-bottom: var(--space-6);">
    <div class="stat-card">
        <div class="stat-card-icon icon-primary"><i class="bi bi-box-seam"></i></div>
        <div class="stat-card-value">${countTotal}</div>
        <div class="stat-card-label">Total Monitored</div>
    </div>
    <div class="stat-card">
        <div class="stat-card-icon icon-danger"><i class="bi bi-exclamation-triangle"></i></div>
        <div class="stat-card-value">${countCritical}</div>
        <div class="stat-card-label">Critical Risk</div>
    </div>
    <div class="stat-card">
        <div class="stat-card-icon icon-warning"><i class="bi bi-exclamation-circle"></i></div>
        <div class="stat-card-value">${countHigh}</div>
        <div class="stat-card-label">High Risk</div>
    </div>
    <div class="stat-card">
        <div class="stat-card-icon icon-success"><i class="bi bi-check-circle"></i></div>
        <div class="stat-card-value">${countLow}</div>
        <div class="stat-card-label">Low Risk</div>
    </div>
</div>

<div class="row" style="display: flex; gap: var(--space-6); margin-bottom: var(--space-6);">
    <div class="card" style="flex: 1;">
        <div class="card-header">
            <span class="card-header-title"><i class="bi bi-pie-chart"></i> Risk Distribution</span>
        </div>
        <div class="card-body" style="height: 300px; position: relative;">
            <canvas id="riskChart"></canvas>
        </div>
    </div>
    <div class="card" style="flex: 2;">
        <div class="card-header">
            <span class="card-header-title"><i class="bi bi-bar-chart"></i> Stock Coverage Overview</span>
        </div>
        <div class="card-body" style="height: 300px; position: relative;">
            <canvas id="coverageChart"></canvas>
        </div>
    </div>
</div>

<div class="card">
    <div class="card-header">
        <span class="card-header-title"><i class="bi bi-table"></i> Inventory Risk Analysis</span>
    </div>
    <div class="table-responsive">
        <table class="data-table">
            <thead>
                <tr>
                    <th>Medicine</th>
                    <th>Current Stock</th>
                    <th>Daily Avg</th>
                    <th>Coverage</th>
                    <th>Trend</th>
                    <th>Score</th>
                    <th>Risk Level</th>
                    <th>Action</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="analysis" items="${analyses}">
                    <tr>
                        <td><strong>${fn:escapeXml(analysis.metrics.medicineName)}</strong></td>
                        <td>${analysis.metrics.availableStock}</td>
                        <td>
                            <c:choose>
                                <c:when test="${analysis.metrics.averageDailyConsumption != null}">
                                    <fmt:formatNumber value="${analysis.metrics.averageDailyConsumption}" maxFractionDigits="1" />/day
                                </c:when>
                                <c:otherwise><span class="text-muted">No data</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${analysis.metrics.daysOfStock != null}">
                                    <fmt:formatNumber value="${analysis.metrics.daysOfStock}" maxFractionDigits="1" /> days
                                </c:when>
                                <c:otherwise><span class="text-muted">-</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${analysis.metrics.trendPercent != null}">
                                    <span class="${analysis.metrics.trendPercent > 0 ? 'text-success' : 'text-danger'}">
                                        ${analysis.metrics.trendPercent > 0 ? '+' : ''}<fmt:formatNumber value="${analysis.metrics.trendPercent}" maxFractionDigits="1" />%
                                    </span>
                                </c:when>
                                <c:otherwise><span class="text-muted">-</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:if test="${analysis.metrics.dataStatus != 'NO_DATA'}">
                                <strong>${analysis.riskScore.finalScore}/100</strong>
                            </c:if>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${analysis.metrics.dataStatus == 'NO_DATA'}">
                                    <span class="badge badge-neutral">NO DATA</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge badge-risk-${fn:toLowerCase(analysis.riskScore.riskLevel)}">${analysis.riskScore.riskLevel}</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <button class="btn btn-sm btn-outline btn-ai-insight" data-id="${analysis.metrics.medicineId}">
                                <i class="bi bi-lightbulb"></i> AI Insight
                            </button>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty analyses}">
                    <tr>
                        <td colspan="8" class="text-center text-muted" style="padding: var(--space-6);">
                            No data available.
                        </td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<!-- AI Insight Modal -->
<div class="modal-backdrop" id="aiModalBackdrop"></div>
<div class="modal" id="aiModal">
    <div class="modal-header">
        <h5 class="modal-title"><i class="bi bi-lightbulb-fill text-warning"></i> AI Logistics Insight</h5>
        <button type="button" class="btn-close" onclick="closeAiModal()"></button>
    </div>
    <div class="modal-body">
        <div id="aiLoading" class="text-center" style="padding: var(--space-6);">
            <div class="spinner"></div>
            <p class="mt-3 text-muted">Analyzing logistics context...</p>
        </div>
        <div id="aiContent" style="display: none;">
            <h4 id="aiTitle" style="margin-bottom: var(--space-4);"></h4>
            <div id="aiText" style="white-space: pre-wrap; line-height: 1.6;"></div>
        </div>
        <div id="aiError" class="alert alert-danger" style="display: none; margin-top: var(--space-4);">
            <div class="alert-icon"><i class="bi bi-exclamation-triangle"></i></div>
            <div class="alert-content">Failed to load AI insight.</div>
        </div>
    </div>
    <div class="modal-footer">
        <button type="button" class="btn btn-secondary" onclick="closeAiModal()">Close</button>
    </div>
</div>

<script>
    const riskDataStr = '${riskDataJson}';
    const riskData = riskDataStr ? JSON.parse(riskDataStr) : {};
    
    document.addEventListener('DOMContentLoaded', function() {
        if (typeof Chart !== 'undefined') {
            const ctxRisk = document.getElementById('riskChart').getContext('2d');
            new Chart(ctxRisk, {
                type: 'doughnut',
                data: {
                    labels: ['Critical', 'High', 'Moderate', 'Low'],
                    datasets: [{
                        data: [riskData.CRITICAL || 0, riskData.HIGH || 0, riskData.MODERATE || 0, riskData.LOW || 0],
                        backgroundColor: ['#dc3545', '#fd7e14', '#ffc107', '#198754']
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { position: 'right' } }
                }
            });

            // Mock data for coverage chart just to show it works
            const ctxCoverage = document.getElementById('coverageChart').getContext('2d');
            new Chart(ctxCoverage, {
                type: 'bar',
                data: {
                    labels: ['< 3 days', '3-7 days', '7-14 days', '14-30 days', '> 30 days'],
                    datasets: [{
                        label: 'Medicines',
                        data: [
                            (riskData.CRITICAL || 0),
                            (riskData.HIGH || 0),
                            Math.floor((riskData.MODERATE || 0) / 2),
                            Math.ceil((riskData.MODERATE || 0) / 2),
                            (riskData.LOW || 0)
                        ],
                        backgroundColor: '#2D7DD2'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
                }
            });
        }

        // Setup AI Insight buttons
        document.querySelectorAll('.btn-ai-insight').forEach(btn => {
            btn.addEventListener('click', function() {
                const medicineId = this.getAttribute('data-id');
                openAiModal(medicineId);
            });
        });
    });

    function openAiModal(medicineId) {
        document.getElementById('aiModalBackdrop').classList.add('active');
        document.getElementById('aiModal').classList.add('active');
        
        document.getElementById('aiLoading').style.display = 'block';
        document.getElementById('aiContent').style.display = 'none';
        document.getElementById('aiError').style.display = 'none';

        MedRoute.api.post('/ai/analyze', { medicineId: medicineId })
            .then(data => {
                document.getElementById('aiLoading').style.display = 'none';
                document.getElementById('aiTitle').textContent = data.title;
                document.getElementById('aiText').textContent = data.content;
                document.getElementById('aiContent').style.display = 'block';
            })
            .catch(err => {
                document.getElementById('aiLoading').style.display = 'none';
                document.getElementById('aiError').style.display = 'flex';
                console.error(err);
            });
    }

    function closeAiModal() {
        document.getElementById('aiModalBackdrop').classList.remove('active');
        document.getElementById('aiModal').classList.remove('active');
    }
</script>
