<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<div class="page-header">
    <div class="page-header-content">
        <h1 class="page-title"><i class="bi bi-arrow-left-right me-2 text-primary"></i>Stock Transfers</h1>
        <p class="page-description">Real-time inter-facility logistics, dispatch tracking, and stock movement.</p>
    </div>
    <div class="page-header-actions">
        <a href="${pageContext.request.contextPath}/requests" class="btn btn-sm btn-primary">
            <i class="bi bi-plus-circle"></i> Create from Request
        </a>
    </div>
</div>

<!-- Alert Messages -->
<c:if test="${not empty successMessage}">
    <div class="alert alert-success alert-dismissible" role="alert" style="margin-bottom: var(--space-4);">
        <i class="alert-icon bi bi-check-circle-fill"></i>
        <div class="alert-content"><c:out value="${successMessage}"/></div>
        <button class="alert-close" aria-label="Close" onclick="this.parentElement.remove();">&times;</button>
    </div>
</c:if>
<c:if test="${not empty errorMessage}">
    <div class="alert alert-danger alert-dismissible" role="alert" style="margin-bottom: var(--space-4);">
        <i class="alert-icon bi bi-exclamation-triangle-fill"></i>
        <div class="alert-content"><c:out value="${errorMessage}"/></div>
        <button class="alert-close" aria-label="Close" onclick="this.parentElement.remove();">&times;</button>
    </div>
</c:if>

<!-- Filters and Tab Navigation -->
<div class="command-filter-bar">
    <div class="btn-group me-auto">
        <a class="btn btn-sm ${currentTab == 'incoming' ? 'btn-primary' : 'btn-secondary'}" 
           href="${pageContext.request.contextPath}/transfers?tab=incoming">
           <i class="bi bi-box-arrow-in-down-right me-1"></i> Incoming Transfers
        </a>
        <a class="btn btn-sm ${currentTab == 'outgoing' ? 'btn-primary' : 'btn-secondary'}" 
           href="${pageContext.request.contextPath}/transfers?tab=outgoing">
           <i class="bi bi-box-arrow-up-right me-1"></i> Outgoing Transfers
        </a>
        <c:if test="${(not empty sessionScope.userRole ? sessionScope.userRole : sessionScope.ROLE) == 'ADMIN'}">
            <a class="btn btn-sm ${currentTab == 'all' ? 'btn-primary' : 'btn-secondary'}" 
               href="${pageContext.request.contextPath}/transfers?tab=all">
               <i class="bi bi-globe me-1"></i> All Network Transfers
            </a>
        </c:if>
    </div>

    <form action="${pageContext.request.contextPath}/transfers" method="get" class="d-flex align-items-center" style="gap: var(--space-2);">
        <input type="hidden" name="tab" value="${currentTab}" />
        <label class="form-label mb-0 text-muted small" style="white-space:nowrap;">Status:</label>
        <select name="status" class="form-select form-select-sm" onchange="this.form.submit()" style="min-width: 140px;">
            <option value="">All Statuses</option>
            <c:forEach items="${statuses}" var="st">
                <option value="${st}" ${currentStatus == st ? 'selected' : ''}>${st}</option>
            </c:forEach>
        </select>
    </form>
</div>

<!-- Transfers Table Card -->
<div class="card">
    <div class="card-header">
        <span class="card-header-title">
            <i class="bi bi-truck me-1 text-primary"></i> Active Transfer Manifests
        </span>
        <span class="badge badge-role-admin">
            Page ${currentPage} of ${totalPages}
        </span>
    </div>
    <div class="table-responsive">
        <c:choose>
            <c:when test="${empty transfers}">
                <div class="text-center p-5">
                    <div style="width: 54px; height: 54px; border-radius: 50%; background: rgba(56, 189, 248, 0.1); display: inline-flex; align-items: center; justify-content: center; margin-bottom: var(--space-3); color: var(--color-primary);">
                        <i class="bi bi-inbox fs-2"></i>
                    </div>
                    <h5 class="text-slate-200">No Transfers Found</h5>
                    <p class="text-muted small">No logistics transfers recorded in this category yet.</p>
                </div>
            </c:when>
            <c:otherwise>
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Transfer ID</th>
                            <th>Logistics Route (From &rarr; To)</th>
                            <th>Medicine</th>
                            <th>Quantity</th>
                            <th>Status</th>
                            <th>Match Score</th>
                            <th>Distance</th>
                            <th>Created</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${transfers}" var="transfer">
                            <tr>
                                <td>
                                    <a href="${pageContext.request.contextPath}/transfers/${transfer.id}" class="fw-bold" style="color: var(--cyan-400); text-decoration: none;">
                                        #${transfer.id}
                                    </a>
                                </td>
                                <td>
                                    <div class="d-flex align-items-center" style="gap: 6px;">
                                        <span class="badge badge-secondary" style="font-weight: 500;">
                                            <c:choose>
                                                <c:when test="${not empty transfer.fromFacilityName}"><c:out value="${transfer.fromFacilityName}"/></c:when>
                                                <c:otherwise>Facility #<c:out value="${transfer.fromFacilityId}"/></c:otherwise>
                                            </c:choose>
                                        </span>
                                        <i class="bi bi-arrow-right text-primary"></i>
                                        <span class="badge badge-secondary" style="font-weight: 600; color: var(--cyan-300);">
                                            <c:choose>
                                                <c:when test="${not empty transfer.toFacilityName}"><c:out value="${transfer.toFacilityName}"/></c:when>
                                                <c:otherwise>Facility #<c:out value="${transfer.toFacilityId}"/></c:otherwise>
                                            </c:choose>
                                        </span>
                                    </div>
                                </td>
                                <td>
                                    <span class="fw-medium text-slate-100">${transfer.medicineName}</span>
                                </td>
                                <td>
                                    <span class="badge badge-role-hospital font-monospace">${transfer.requestedQuantity} units</span>
                                </td>
                                <td>
                                    <span class="badge badge-status-${transfer.status.toString().toLowerCase().replace('_', '-')}">
                                        <i class="bi bi-record-fill me-1" style="font-size: 8px;"></i>${transfer.status}
                                    </span>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty transfer.matchScore}">
                                            <span class="text-success fw-bold font-monospace">
                                                <fmt:formatNumber value="${transfer.matchScore * 100}" maxFractionDigits="0"/>%
                                            </span>
                                        </c:when>
                                        <c:otherwise><span class="text-muted">-</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty transfer.distanceKm}">
                                            <span class="font-monospace text-slate-300">
                                                <fmt:formatNumber value="${transfer.distanceKm}" maxFractionDigits="1"/> km
                                            </span>
                                        </c:when>
                                        <c:otherwise><span class="text-muted">-</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <span class="text-muted small font-monospace">
                                        ${transfer.createdAt != null ? transfer.createdAt.toLocalDate() : '-'}
                                    </span>
                                </td>
                                <td>
                                    <a href="${pageContext.request.contextPath}/transfers/${transfer.id}" 
                                       class="btn btn-sm btn-outline" title="Inspect Transfer">
                                        <i class="bi bi-eye"></i> View
                                    </a>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
    <c:if test="${totalPages > 1}">
        <div class="card-footer d-flex justify-content-center">
            <div class="btn-group">
                <a class="btn btn-sm btn-secondary ${currentPage == 1 ? 'disabled' : ''}" 
                   href="${pageContext.request.contextPath}/transfers?tab=${currentTab}&status=${currentStatus}&page=${currentPage - 1}">Previous</a>
                <c:forEach begin="1" end="${totalPages}" var="p">
                    <a class="btn btn-sm ${currentPage == p ? 'btn-primary' : 'btn-secondary'}" 
                       href="${pageContext.request.contextPath}/transfers?tab=${currentTab}&status=${currentStatus}&page=${p}">${p}</a>
                </c:forEach>
                <a class="btn btn-sm btn-secondary ${currentPage == totalPages ? 'disabled' : ''}" 
                   href="${pageContext.request.contextPath}/transfers?tab=${currentTab}&status=${currentStatus}&page=${currentPage + 1}">Next</a>
            </div>
        </div>
    </c:if>
</div>
