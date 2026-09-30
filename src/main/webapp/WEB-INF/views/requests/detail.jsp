<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<div class="container-fluid p-0">

    <%-- Flash Messages --%>
    <c:if test="${not empty success}">
        <div class="alert alert-success alert-dismissible fade show mb-4" role="alert">
            <i class="bi bi-check-circle me-2"></i><c:out value="${success}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-danger alert-dismissible fade show mb-4" role="alert">
            <i class="bi bi-exclamation-triangle me-2"></i><c:out value="${error}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <%-- Header --%>
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
            <h2 class="h3 fw-bold text-light mb-1">
                <i class="bi bi-send text-primary me-2"></i>Emergency Request #<c:out value="${request.id}"/>
            </h2>
            <p class="text-secondary small mb-0">Operational medicine requisition and candidate matching telemetry.</p>
        </div>
        <div class="d-flex gap-2">
            <a href="<c:url value='/requests'/>" class="btn btn-outline-secondary">
                <i class="bi bi-arrow-left me-1"></i>Back
            </a>
            <c:if test="${canMatch}">
                <a href="<c:url value='/matching/request/${request.id}'/>" class="btn btn-primary">
                    <i class="bi bi-search me-1"></i>Find Donors
                </a>
            </c:if>
        </div>
    </div>

    <div class="row g-4">
        <%-- Main Details --%>
        <div class="col-lg-8">
            <div class="card bg-dark border-secondary shadow-lg mb-4">
                <div class="card-header bg-dark border-secondary py-3 d-flex justify-content-between align-items-center">
                    <h5 class="mb-0 text-light"><i class="bi bi-info-circle text-primary me-2"></i>Request Parameters</h5>
                    <c:choose>
                        <c:when test="${request.status == 'OPEN'}">
                            <span class="badge bg-primary fs-6">OPEN</span>
                        </c:when>
                        <c:when test="${request.status == 'PARTIALLY_FULFILLED'}">
                            <span class="badge bg-info text-dark fs-6">PARTIALLY FULFILLED</span>
                        </c:when>
                        <c:when test="${request.status == 'FULFILLED'}">
                            <span class="badge bg-success fs-6">FULFILLED</span>
                        </c:when>
                        <c:when test="${request.status == 'CANCELLED'}">
                            <span class="badge bg-secondary fs-6">CANCELLED</span>
                        </c:when>
                        <c:when test="${request.status == 'EXPIRED'}">
                            <span class="badge bg-dark border border-secondary fs-6">EXPIRED</span>
                        </c:when>
                    </c:choose>
                </div>
                <div class="card-body p-4">
                    <div class="table-responsive">
                        <table class="table table-dark table-borderless align-middle mb-0">
                            <tbody>
                                <tr>
                                    <td class="text-secondary" style="width:180px;">Requesting Facility</td>
                                    <td class="text-light fw-bold"><c:out value="${request.facilityName}" default="-"/></td>
                                </tr>
                                <tr>
                                    <td class="text-secondary">Medicine</td>
                                    <td class="text-light fw-bold"><c:out value="${request.medicineName}" default="-"/></td>
                                </tr>
                                <tr>
                                    <td class="text-secondary">Quantity Needed</td>
                                    <td class="text-light fs-5"><c:out value="${request.quantityNeeded}"/> units</td>
                                </tr>
                                <tr>
                                    <td class="text-secondary">Quantity Fulfilled</td>
                                    <td class="text-info fw-bold">
                                        <c:out value="${request.quantityFulfilled}"/>
                                        <c:if test="${request.quantityNeeded > 0}">
                                            <span class="text-secondary small">
                                                (<fmt:formatNumber value="${request.fulfillmentPercentage}" maxFractionDigits="0"/>%)
                                            </span>
                                        </c:if>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="text-secondary">Remaining Deficit</td>
                                    <td>
                                        <span class="fs-5 fw-bold ${request.remainingQuantity > 0 ? 'text-danger' : 'text-success'}">
                                            <c:out value="${request.remainingQuantity}"/> units
                                        </span>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="text-secondary">Urgency Level</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${request.urgency == 'CRITICAL'}">
                                                <span class="badge bg-danger">CRITICAL</span>
                                            </c:when>
                                            <c:when test="${request.urgency == 'HIGH'}">
                                                <span class="badge bg-warning text-dark">HIGH</span>
                                            </c:when>
                                            <c:when test="${request.urgency == 'MEDIUM'}">
                                                <span class="badge bg-info text-dark">MEDIUM</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge bg-secondary">LOW</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="text-secondary">Required By Date</td>
                                    <td class="text-light">
                                        <c:out value="${request.requiredByDate}"/>
                                        <c:if test="${request.overdue}">
                                            <span class="badge bg-danger ms-2">OVERDUE</span>
                                        </c:if>
                                    </td>
                                </tr>
                                <c:if test="${not empty request.reason}">
                                    <tr>
                                        <td class="text-secondary">Justification</td>
                                        <td class="text-light fst-italic"><c:out value="${request.reason}"/></td>
                                    </tr>
                                </c:if>
                                <tr>
                                    <td class="text-secondary">Created By</td>
                                    <td class="text-secondary"><c:out value="${request.createdByName}" default="-"/></td>
                                </tr>
                                <tr>
                                    <td class="text-secondary">Created At</td>
                                    <td class="text-secondary small"><c:out value="${request.createdAt}"/></td>
                                </tr>
                                <tr>
                                    <td class="text-secondary">Last Updated</td>
                                    <td class="text-secondary small"><c:out value="${request.updatedAt}"/></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <%-- Sidebar Actions --%>
        <div class="col-lg-4">
            <%-- Actions Card --%>
            <div class="card bg-dark border-secondary shadow-lg mb-4">
                <div class="card-header bg-dark border-secondary py-3">
                    <h6 class="mb-0 text-light"><i class="bi bi-gear text-primary me-2"></i>Operational Actions</h6>
                </div>
                <div class="card-body p-3">
                    <c:if test="${canMatch}">
                        <a href="<c:url value='/matching/request/${request.id}'/>" class="btn btn-primary w-100 mb-3 py-2">
                            <i class="bi bi-search me-1"></i>Find Matching Donors
                        </a>
                    </c:if>

                    <c:if test="${canCancel}">
                        <form action="<c:url value='/requests/${request.id}/cancel'/>" method="POST"
                              onsubmit="return confirm('Are you sure you want to cancel this emergency request?');">
                            <c:if test="${not empty _csrf}">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                            </c:if>
                            <button type="submit" class="btn btn-outline-danger w-100 py-2">
                                <i class="bi bi-x-circle me-1"></i>Cancel Request
                            </button>
                        </form>
                    </c:if>

                    <c:if test="${not canMatch and not canCancel}">
                        <p class="text-secondary mb-0 small">No operational actions available for current status.</p>
                    </c:if>
                </div>
            </div>

            <%-- Fulfillment Progress --%>
            <div class="card bg-dark border-secondary shadow-lg">
                <div class="card-header bg-dark border-secondary py-3">
                    <h6 class="mb-0 text-light"><i class="bi bi-pie-chart text-primary me-2"></i>Fulfillment Progress</h6>
                </div>
                <div class="card-body p-4">
                    <div class="progress bg-secondary mb-2" style="height: 18px;">
                        <div class="progress-bar ${request.fulfillmentPercentage >= 100 ? 'bg-success' : 'bg-primary'}"
                             role="progressbar"
                             style="width: <fmt:formatNumber value="${request.fulfillmentPercentage}" maxFractionDigits="0"/>%">
                            <fmt:formatNumber value="${request.fulfillmentPercentage}" maxFractionDigits="0"/>%
                        </div>
                    </div>
                    <div class="d-flex justify-content-between text-secondary small mt-2">
                        <span><c:out value="${request.quantityFulfilled}"/> fulfilled</span>
                        <span><c:out value="${request.quantityNeeded}"/> requested</span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
