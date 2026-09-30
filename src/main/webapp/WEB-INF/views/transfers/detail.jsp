<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<div class="page-header">
    <div class="page-header-content">
        <div class="mb-2">
            <a href="${pageContext.request.contextPath}/transfers" class="btn btn-sm btn-secondary">
                <i class="bi bi-arrow-left me-1"></i> Back to Transfers
            </a>
        </div>
        <h1 class="page-title">
            <i class="bi bi-arrow-left-right text-primary me-2"></i>Transfer #${transfer.id}
        </h1>
        <p class="page-description">
            <c:choose>
                <c:when test="${not empty transfer.fromFacilityName}"><c:out value="${transfer.fromFacilityName}"/></c:when>
                <c:otherwise>Facility #<c:out value="${transfer.fromFacilityId}"/></c:otherwise>
            </c:choose>
            &rarr; 
            <c:choose>
                <c:when test="${not empty transfer.toFacilityName}"><c:out value="${transfer.toFacilityName}"/></c:when>
                <c:otherwise>Facility #<c:out value="${transfer.toFacilityId}"/></c:otherwise>
            </c:choose>
            &bull; Created: ${transfer.createdAt != null ? transfer.createdAt.toLocalDate() : '-'}
        </p>
    </div>
    <div class="page-header-actions">
        <span class="badge badge-status-${transfer.status.toString().toLowerCase().replace('_', '-')} fs-6">
            <i class="bi bi-record-fill me-1" style="font-size: 9px;"></i>${transfer.status}
        </span>
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

<!-- Visual Transfer Lifecycle Stepper -->
<div class="lifecycle-stepper">
    <c:choose>
        <c:when test="${transfer.status == 'REJECTED' || transfer.status == 'CANCELLED' || transfer.status == 'EXPIRED'}">
            <div class="lifecycle-step completed">
                <div class="lifecycle-step-dot"><i class="bi bi-check"></i></div>
                <div class="lifecycle-step-label">Requested</div>
            </div>
            <div class="lifecycle-line completed"></div>
            <div class="lifecycle-step terminal-failed">
                <div class="lifecycle-step-dot"><i class="bi bi-x-lg"></i></div>
                <div class="lifecycle-step-label">${transfer.status}</div>
            </div>
        </c:when>
        <c:otherwise>
            <%-- Step 1: Requested --%>
            <div class="lifecycle-step ${transfer.status != 'REQUESTED' ? 'completed' : 'active'}">
                <div class="lifecycle-step-dot">
                    <c:choose>
                        <c:when test="${transfer.status != 'REQUESTED'}"><i class="bi bi-check"></i></c:when>
                        <c:otherwise>1</c:otherwise>
                    </c:choose>
                </div>
                <div class="lifecycle-step-label">Requested</div>
            </div>
            <div class="lifecycle-line ${transfer.status != 'REQUESTED' ? 'completed' : ''}"></div>

            <%-- Step 2: Accepted --%>
            <div class="lifecycle-step ${(transfer.status == 'SCHEDULED' || transfer.status == 'IN_TRANSIT' || transfer.status == 'RECEIVED' || transfer.status == 'COMPLETED') ? 'completed' : (transfer.status == 'ACCEPTED' ? 'active' : '')}">
                <div class="lifecycle-step-dot">
                    <c:choose>
                        <c:when test="${transfer.status == 'SCHEDULED' || transfer.status == 'IN_TRANSIT' || transfer.status == 'RECEIVED' || transfer.status == 'COMPLETED'}"><i class="bi bi-check"></i></c:when>
                        <c:otherwise>2</c:otherwise>
                    </c:choose>
                </div>
                <div class="lifecycle-step-label">Accepted</div>
            </div>
            <div class="lifecycle-line ${(transfer.status == 'SCHEDULED' || transfer.status == 'IN_TRANSIT' || transfer.status == 'RECEIVED' || transfer.status == 'COMPLETED') ? 'completed' : ''}"></div>

            <%-- Step 3: Scheduled --%>
            <div class="lifecycle-step ${(transfer.status == 'IN_TRANSIT' || transfer.status == 'RECEIVED' || transfer.status == 'COMPLETED') ? 'completed' : (transfer.status == 'SCHEDULED' ? 'active' : '')}">
                <div class="lifecycle-step-dot">
                    <c:choose>
                        <c:when test="${transfer.status == 'IN_TRANSIT' || transfer.status == 'RECEIVED' || transfer.status == 'COMPLETED'}"><i class="bi bi-check"></i></c:when>
                        <c:otherwise>3</c:otherwise>
                    </c:choose>
                </div>
                <div class="lifecycle-step-label">Scheduled</div>
            </div>
            <div class="lifecycle-line ${(transfer.status == 'IN_TRANSIT' || transfer.status == 'RECEIVED' || transfer.status == 'COMPLETED') ? 'completed' : ''}"></div>

            <%-- Step 4: In Transit --%>
            <div class="lifecycle-step ${(transfer.status == 'RECEIVED' || transfer.status == 'COMPLETED') ? 'completed' : (transfer.status == 'IN_TRANSIT' ? 'active' : '')}">
                <div class="lifecycle-step-dot">
                    <c:choose>
                        <c:when test="${transfer.status == 'RECEIVED' || transfer.status == 'COMPLETED'}"><i class="bi bi-check"></i></c:when>
                        <c:otherwise><i class="bi bi-truck"></i></c:otherwise>
                    </c:choose>
                </div>
                <div class="lifecycle-step-label">In Transit</div>
            </div>
            <div class="lifecycle-line ${transfer.status == 'COMPLETED' ? 'completed' : ''}"></div>

            <%-- Step 5: Completed --%>
            <div class="lifecycle-step ${transfer.status == 'COMPLETED' ? 'completed' : ''}">
                <div class="lifecycle-step-dot">
                    <c:choose>
                        <c:when test="${transfer.status == 'COMPLETED'}"><i class="bi bi-check-all"></i></c:when>
                        <c:otherwise>5</c:otherwise>
                    </c:choose>
                </div>
                <div class="lifecycle-step-label">Delivered</div>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<!-- Main Details Grid -->
<div class="row" style="display: flex; gap: var(--space-6); margin-bottom: var(--space-6); flex-wrap: wrap;">
    <!-- Logistics Parameters Card -->
    <div class="card" style="flex: 1; min-width: 320px;">
        <div class="card-header">
            <span class="card-header-title"><i class="bi bi-info-circle text-primary me-2"></i>Logistics Parameters</span>
        </div>
        <div class="card-body">
            <table class="data-table">
                <tbody>
                    <tr>
                        <td class="text-muted" style="width: 45%;">Source (Donor):</td>
                        <td class="fw-bold text-slate-100">${transfer.fromFacilityName}</td>
                    </tr>
                    <tr>
                        <td class="text-muted">Destination (Requester):</td>
                        <td class="fw-bold text-cyan-300">${transfer.toFacilityName}</td>
                    </tr>
                    <tr>
                        <td class="text-muted">Medicine:</td>
                        <td class="fw-bold text-slate-100">${transfer.medicineName}</td>
                    </tr>
                    <tr>
                        <td class="text-muted">Quantity:</td>
                        <td><span class="badge badge-role-hospital font-monospace">${transfer.requestedQuantity} units</span></td>
                    </tr>
                    <c:if test="${not empty transfer.matchScore}">
                        <tr>
                            <td class="text-muted">Match Score:</td>
                            <td>
                                <span class="text-success fw-bold font-monospace">
                                    <fmt:formatNumber value="${transfer.matchScore * 100}" maxFractionDigits="0"/>%
                                </span>
                            </td>
                        </tr>
                    </c:if>
                    <c:if test="${not empty transfer.distanceKm}">
                        <tr>
                            <td class="text-muted">Route Distance:</td>
                            <td class="font-monospace text-slate-200">
                                <fmt:formatNumber value="${transfer.distanceKm}" maxFractionDigits="1"/> km
                            </td>
                        </tr>
                    </c:if>
                    <tr>
                        <td class="text-muted">Initiated By:</td>
                        <td>${transfer.requestedByName != null ? transfer.requestedByName : 'System User'}</td>
                    </tr>
                    <tr>
                        <td class="text-muted">Approved By:</td>
                        <td>${transfer.approvedByName != null ? transfer.approvedByName : 'Pending Authorization'}</td>
                    </tr>
                    <c:if test="${not empty transfer.notes}">
                        <tr>
                            <td class="text-muted">Operational Notes:</td>
                            <td class="text-slate-300 font-monospace small">${transfer.notes}</td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Reserved Batches / Manifest Card -->
    <div class="card" style="flex: 1; min-width: 320px;">
        <div class="card-header">
            <span class="card-header-title"><i class="bi bi-box-seam text-primary me-2"></i>Reserved Inventory Batches</span>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${empty transfer.items}">
                    <div class="text-center p-4 text-muted">
                        <i class="bi bi-inbox fs-2 mb-2 d-block text-slate-500"></i>
                        <p class="small mb-0">No physical batches reserved for this transfer yet.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Batch Code</th>
                                <th>Medicine</th>
                                <th>Reserved Qty</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach items="${transfer.items}" var="item">
                                <tr>
                                    <td class="font-monospace text-cyan-400">${item.batchNumber != null ? item.batchNumber : 'AUTO-ASSIGN'}</td>
                                    <td>${transfer.medicineName}</td>
                                    <td><span class="badge badge-secondary font-monospace">${item.quantity} units</span></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<!-- Operational Actions Toolbar -->
<div class="card">
    <div class="card-header">
        <span class="card-header-title"><i class="bi bi-sliders text-primary me-2"></i>Dispatch Controls</span>
    </div>
    <div class="card-body d-flex gap-2 flex-wrap align-items-center">
        <c:set var="curFacilityId" value="${not empty sessionScope.facilityId ? sessionScope.facilityId : sessionScope.FACILITY_ID}"/>
        <c:set var="curRole" value="${not empty sessionScope.userRole ? sessionScope.userRole : sessionScope.ROLE}"/>

        <c:if test="${transfer.status == 'REQUESTED'}">
            <c:if test="${curFacilityId == transfer.fromFacilityId || curRole == 'ADMIN'}">
                <form action="${pageContext.request.contextPath}/transfers/${transfer.id}/accept" method="post" class="d-inline">
                    <button type="submit" class="btn btn-sm btn-success">
                        <i class="bi bi-check-lg me-1"></i> Accept Transfer
                    </button>
                </form>
                <form action="${pageContext.request.contextPath}/transfers/${transfer.id}/reject" method="post" class="d-inline">
                    <button type="submit" class="btn btn-sm btn-danger">
                        <i class="bi bi-x-lg me-1"></i> Reject
                    </button>
                </form>
            </c:if>
            <c:if test="${curFacilityId == transfer.toFacilityId || curRole == 'ADMIN'}">
                <form action="${pageContext.request.contextPath}/transfers/${transfer.id}/cancel" method="post" class="d-inline">
                    <button type="submit" class="btn btn-sm btn-secondary">
                        <i class="bi bi-slash-circle me-1"></i> Cancel Request
                    </button>
                </form>
            </c:if>
        </c:if>

        <c:if test="${transfer.status == 'ACCEPTED'}">
            <c:if test="${curFacilityId == transfer.fromFacilityId || curRole == 'ADMIN'}">
                <button type="button" class="btn btn-sm btn-outline" onclick="document.getElementById('scheduleModal').style.display='flex'">
                    <i class="bi bi-calendar-event me-1"></i> Schedule
                </button>
                <form action="${pageContext.request.contextPath}/transfers/${transfer.id}/dispatch" method="post" class="d-inline">
                    <button type="submit" class="btn btn-sm btn-primary">
                        <i class="bi bi-truck me-1"></i> Dispatch Manifest
                    </button>
                </form>
                <form action="${pageContext.request.contextPath}/transfers/${transfer.id}/cancel" method="post" class="d-inline">
                    <button type="submit" class="btn btn-sm btn-secondary">
                        <i class="bi bi-slash-circle me-1"></i> Cancel
                    </button>
                </form>
            </c:if>
        </c:if>

        <c:if test="${transfer.status == 'SCHEDULED'}">
            <c:if test="${curFacilityId == transfer.fromFacilityId || curRole == 'ADMIN'}">
                <form action="${pageContext.request.contextPath}/transfers/${transfer.id}/dispatch" method="post" class="d-inline">
                    <button type="submit" class="btn btn-sm btn-primary">
                        <i class="bi bi-truck me-1"></i> Dispatch Now
                    </button>
                </form>
                <form action="${pageContext.request.contextPath}/transfers/${transfer.id}/cancel" method="post" class="d-inline">
                    <button type="submit" class="btn btn-sm btn-secondary">
                        <i class="bi bi-slash-circle me-1"></i> Cancel
                    </button>
                </form>
            </c:if>
        </c:if>

        <c:if test="${transfer.status == 'IN_TRANSIT'}">
            <c:if test="${curFacilityId == transfer.toFacilityId || curRole == 'ADMIN'}">
                <form action="${pageContext.request.contextPath}/transfers/${transfer.id}/receive" method="post" class="d-inline">
                    <button type="submit" class="btn btn-sm btn-success">
                        <i class="bi bi-box-seam me-1"></i> Acknowledge Receipt
                    </button>
                </form>
            </c:if>
        </c:if>
        
        <c:if test="${transfer.status == 'RECEIVED' || transfer.status == 'COMPLETED' || transfer.status == 'REJECTED' || transfer.status == 'CANCELLED' || transfer.status == 'EXPIRED'}">
            <span class="text-muted small"><i class="bi bi-lock me-1"></i> Terminal lifecycle state. No further actions permitted.</span>
        </c:if>
    </div>
</div>

<!-- Schedule Modal Dialog -->
<div id="scheduleModal" class="modal-overlay" style="display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.75); z-index: var(--z-modal); align-items: center; justify-content: center;">
    <div class="card" style="width: 100%; max-width: 480px; margin: 1rem;">
        <div class="card-header">
            <span class="card-header-title"><i class="bi bi-calendar-event me-2 text-primary"></i>Schedule Transfer</span>
            <button class="btn btn-sm btn-icon" onclick="document.getElementById('scheduleModal').style.display='none'">&times;</button>
        </div>
        <form action="${pageContext.request.contextPath}/transfers/${transfer.id}/schedule" method="post">
            <div class="card-body">
                <div class="mb-3">
                    <label for="scheduleNotes" class="form-label">Dispatch Notes / Transport Reference</label>
                    <textarea class="form-control" id="scheduleNotes" name="notes" rows="3" required placeholder="e.g. Courier vehicle RJ14-AB-1234, ETA 2 hours"></textarea>
                </div>
            </div>
            <div class="card-footer d-flex justify-content-end" style="gap: var(--space-2);">
                <button type="button" class="btn btn-sm btn-secondary" onclick="document.getElementById('scheduleModal').style.display='none'">Dismiss</button>
                <button type="submit" class="btn btn-sm btn-primary">Save & Schedule</button>
            </div>
        </form>
    </div>
</div>
