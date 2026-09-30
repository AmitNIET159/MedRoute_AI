<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<div class="page-header">
    <div class="page-header-content">
        <h1 class="page-title">AI Logistics Insights</h1>
        <p class="page-description">Review recent AI-generated logistics insights and deterministic fallbacks.</p>
    </div>
</div>

<div class="card">
    <div class="card-header">
        <span class="card-header-title"><i class="bi bi-lightbulb"></i> Recent Insights</span>
    </div>
    <div class="table-responsive">
        <table class="data-table">
            <thead>
                <tr>
                    <th>Date</th>
                    <th>Type</th>
                    <th>Title</th>
                    <th>Severity</th>
                    <th>Risk Score</th>
                    <th>Action</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="insight" items="${insights}">
                    <tr>
                        <td>
                            <c:if test="${insight.createdAt != null}">
                                <script>document.write(MedRoute.util.formatDate('${insight.createdAt}'));</script>
                            </c:if>
                        </td>
                        <td>
                            <span class="badge badge-neutral">${insight.insightType}</span>
                        </td>
                        <td><strong>${fn:escapeXml(insight.title)}</strong></td>
                        <td>
                            <c:choose>
                                <c:when test="${insight.severity == 'CRITICAL'}">
                                    <span class="badge badge-risk-critical">CRITICAL</span>
                                </c:when>
                                <c:when test="${insight.severity == 'WARNING'}">
                                    <span class="badge badge-risk-moderate">WARNING</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge badge-risk-low">INFO</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:if test="${insight.riskScore != null}">
                                ${insight.riskScore}/100
                            </c:if>
                        </td>
                        <td>
                            <button class="btn btn-sm btn-outline btn-view-insight" data-content="${fn:escapeXml(insight.content)}" data-title="${fn:escapeXml(insight.title)}">
                                View
                            </button>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty insights}">
                    <tr>
                        <td colspan="6" class="text-center text-muted" style="padding: var(--space-6);">
                            No insights available yet. Run demand analysis to generate insights.
                        </td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<!-- Modal for Viewing Insight -->
<div class="modal-backdrop" id="viewModalBackdrop"></div>
<div class="modal" id="viewModal">
    <div class="modal-header">
        <h5 class="modal-title" id="viewModalTitle">Insight Details</h5>
        <button type="button" class="btn-close" onclick="closeViewModal()"></button>
    </div>
    <div class="modal-body">
        <div id="viewModalContent" style="white-space: pre-wrap; line-height: 1.6;"></div>
    </div>
    <div class="modal-footer">
        <button type="button" class="btn btn-secondary" onclick="closeViewModal()">Close</button>
    </div>
</div>

<script>
    document.addEventListener('DOMContentLoaded', function() {
        document.querySelectorAll('.btn-view-insight').forEach(btn => {
            btn.addEventListener('click', function() {
                const title = this.getAttribute('data-title');
                const content = this.getAttribute('data-content');
                document.getElementById('viewModalTitle').textContent = title;
                document.getElementById('viewModalContent').textContent = content;
                
                document.getElementById('viewModalBackdrop').classList.add('active');
                document.getElementById('viewModal').classList.add('active');
            });
        });
    });

    function closeViewModal() {
        document.getElementById('viewModalBackdrop').classList.remove('active');
        document.getElementById('viewModal').classList.remove('active');
    }
</script>
