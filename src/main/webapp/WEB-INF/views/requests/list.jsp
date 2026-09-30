<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<div class="page-header">
    <div class="page-header-content">
        <h1 class="page-title"><i class="bi bi-send-exclamation text-primary me-2"></i>Emergency Requests</h1>
        <p class="page-description">Critical medicine shortages and emergency supply chain dispatch requests.</p>
    </div>
    <div class="page-header-actions">
        <a href="${pageContext.request.contextPath}/requests/new" class="btn btn-sm btn-primary">
            <i class="bi bi-plus-lg me-1"></i> New Emergency Request
        </a>
    </div>
</div>

<%-- Flash Messages --%>
<c:if test="${not empty success}">
    <div class="alert alert-success alert-dismissible" role="alert" style="margin-bottom: var(--space-4);">
        <i class="alert-icon bi bi-check-circle-fill"></i>
        <div class="alert-content"><c:out value="${success}"/></div>
        <button class="alert-close" aria-label="Close" onclick="this.parentElement.remove();">&times;</button>
    </div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger alert-dismissible" role="alert" style="margin-bottom: var(--space-4);">
        <i class="alert-icon bi bi-exclamation-triangle-fill"></i>
        <div class="alert-content"><c:out value="${error}"/></div>
        <button class="alert-close" aria-label="Close" onclick="this.parentElement.remove();">&times;</button>
    </div>
</c:if>

<%-- Command Center Filter Bar --%>
<div class="command-filter-bar">
    <form action="${pageContext.request.contextPath}/requests" method="GET" class="d-flex align-items-center flex-wrap" style="gap: var(--space-3); width: 100%;">
        <div class="d-flex align-items-center" style="gap: var(--space-2);">
            <label class="form-label mb-0 text-muted small" style="white-space:nowrap;">Status Filter:</label>
            <select class="form-select form-select-sm" name="status" onchange="this.form.submit()" style="min-width: 160px;">
                <option value="">All Statuses</option>
                <option value="OPEN" ${statusFilter == 'OPEN' ? 'selected' : ''}>Open</option>
                <option value="PARTIALLY_FULFILLED" ${statusFilter == 'PARTIALLY_FULFILLED' ? 'selected' : ''}>Partially Fulfilled</option>
                <option value="FULFILLED" ${statusFilter == 'FULFILLED' ? 'selected' : ''}>Fulfilled</option>
                <option value="CANCELLED" ${statusFilter == 'CANCELLED' ? 'selected' : ''}>Cancelled</option>
                <option value="EXPIRED" ${statusFilter == 'EXPIRED' ? 'selected' : ''}>Expired</option>
            </select>
        </div>
        <div class="ms-auto text-muted small">
            <c:if test="${not empty statusFilter}">
                <a href="${pageContext.request.contextPath}/requests" class="btn btn-sm btn-secondary">
                    <i class="bi bi-x-circle me-1"></i> Clear Filter
                </a>
            </c:if>
        </div>
    </form>
</div>

<%-- Requests Table Card --%>
<div class="card">
    <div class="card-header">
        <span class="card-header-title">
            <i class="bi bi-list-columns-reverse text-primary me-2"></i>Emergency Request Manifests
        </span>
        <span class="badge badge-role-admin">
            Page ${currentPage} of ${totalPages > 0 ? totalPages : 1}
        </span>
    </div>
    <div class="table-responsive">
        <table class="data-table">
            <thead>
                <tr>
                    <th>Request ID</th>
                    <c:if test="${isAdmin}"><th>Facility</th></c:if>
                    <th>Medicine</th>
                    <th>Qty Needed</th>
                    <th>Fulfillment</th>
                    <th>Urgency Level</th>
                    <th>Required By</th>
                    <th>Status</th>
                    <th>Date</th>
                    <th>Action</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${empty requests}">
                        <tr>
                            <td colspan="${isAdmin ? 10 : 9}" class="text-center py-5">
                                <div style="width: 50px; height: 50px; border-radius: 50%; background: rgba(56, 189, 248, 0.1); display: inline-flex; align-items: center; justify-content: center; margin-bottom: var(--space-3); color: var(--color-primary);">
                                    <i class="bi bi-inbox fs-3"></i>
                                </div>
                                <h5 class="text-slate-200">No Emergency Requests</h5>
                                <p class="text-muted small">No active emergency supply requests matching your criteria.</p>
                            </td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="req" items="${requests}">
                            <tr>
                                <td>
                                    <a href="${pageContext.request.contextPath}/requests/${req.id}" class="fw-bold" style="color: var(--cyan-400); text-decoration: none;">
                                        #<c:out value="${req.id}"/>
                                    </a>
                                </td>
                                <c:if test="${isAdmin}">
                                    <td>
                                        <span class="badge badge-secondary">
                                            <c:out value="${req.facilityName}" default="-"/>
                                        </span>
                                    </td>
                                </c:if>
                                <td>
                                    <span class="fw-medium text-slate-100"><c:out value="${req.medicineName}" default="-"/></span>
                                </td>
                                <td>
                                    <span class="font-monospace fw-bold text-slate-100"><c:out value="${req.quantityNeeded}"/></span>
                                </td>
                                <td>
                                    <div class="d-flex align-items-center" style="gap: 6px;">
                                        <span class="font-monospace text-slate-300"><c:out value="${req.quantityFulfilled}"/></span>
                                        <c:if test="${req.quantityNeeded > 0}">
                                            <span class="badge badge-secondary font-monospace" style="font-size: 10px;">
                                                <fmt:formatNumber value="${req.fulfillmentPercentage}" maxFractionDigits="0"/>%
                                            </span>
                                        </c:if>
                                    </div>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${req.urgency == 'CRITICAL'}">
                                            <span class="badge badge-risk-critical">
                                                <i class="bi bi-exclamation-octagon-fill me-1" style="font-size: 9px;"></i>CRITICAL
                                            </span>
                                        </c:when>
                                        <c:when test="${req.urgency == 'HIGH'}">
                                            <span class="badge badge-risk-high">
                                                <i class="bi bi-exclamation-triangle-fill me-1" style="font-size: 9px;"></i>HIGH
                                            </span>
                                        </c:when>
                                        <c:when test="${req.urgency == 'MEDIUM'}">
                                            <span class="badge badge-risk-moderate">MEDIUM</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-risk-low">LOW</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <span class="font-monospace text-slate-300"><c:out value="${req.requiredByDate}"/></span>
                                </td>
                                <td>
                                    <span class="badge badge-status-${req.status.toString().toLowerCase().replace('_', '-')}">
                                        <i class="bi bi-record-fill me-1" style="font-size: 8px;"></i>${req.status}
                                    </span>
                                </td>
                                <td>
                                    <span class="text-muted small font-monospace">
                                        ${req.createdAt != null ? req.createdAt.toLocalDate() : '-'}
                                    </span>
                                </td>
                                <td>
                                    <a href="${pageContext.request.contextPath}/requests/${req.id}" class="btn btn-sm btn-outline">
                                        <i class="bi bi-eye"></i> View
                                    </a>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
    <c:if test="${totalPages > 1}">
        <div class="card-footer d-flex justify-content-center">
            <div class="btn-group">
                <a class="btn btn-sm btn-secondary ${currentPage <= 1 ? 'disabled' : ''}" 
                   href="${pageContext.request.contextPath}/requests?page=${currentPage - 1}&status=${statusFilter}">Previous</a>
                <c:forEach begin="1" end="${totalPages}" var="i">
                    <a class="btn btn-sm ${i == currentPage ? 'btn-primary' : 'btn-secondary'}" 
                       href="${pageContext.request.contextPath}/requests?page=${i}&status=${statusFilter}"><c:out value="${i}"/></a>
                </c:forEach>
                <a class="btn btn-sm btn-secondary ${currentPage >= totalPages ? 'disabled' : ''}" 
                   href="${pageContext.request.contextPath}/requests?page=${currentPage + 1}&status=${statusFilter}">Next</a>
            </div>
        </div>
    </c:if>
</div>
