<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<div class="container-fluid p-0">

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
                <i class="bi bi-search text-primary me-2"></i>Deterministic Matching Engine
            </h2>
            <p class="text-secondary small mb-0">
                Request #<c:out value="${request.id}"/> &mdash;
                <span class="text-light fw-semibold"><c:out value="${request.medicineName}"/></span> &mdash;
                <strong class="text-info"><c:out value="${request.remainingQuantity}"/></strong> units required
            </p>
        </div>
        <a href="<c:url value='/requests/${request.id}'/>" class="btn btn-outline-secondary">
            <i class="bi bi-arrow-left me-1"></i>Back to Request
        </a>
    </div>

    <%-- Request Summary Card --%>
    <div class="card bg-dark border-secondary shadow-lg mb-4">
        <div class="card-body py-3">
            <div class="row text-center g-3 align-items-center">
                <div class="col-md-2 col-6">
                    <div class="text-secondary small text-uppercase fw-semibold">Medicine</div>
                    <div class="text-light fw-bold"><c:out value="${request.medicineName}"/></div>
                </div>
                <div class="col-md-2 col-6">
                    <div class="text-secondary small text-uppercase fw-semibold">Facility</div>
                    <div class="text-light fw-bold"><c:out value="${request.facilityName}"/></div>
                </div>
                <div class="col-md-2 col-6">
                    <div class="text-secondary small text-uppercase fw-semibold">Urgency</div>
                    <div>
                        <c:choose>
                            <c:when test="${request.urgency == 'CRITICAL'}"><span class="badge bg-danger">CRITICAL</span></c:when>
                            <c:when test="${request.urgency == 'HIGH'}"><span class="badge bg-warning text-dark">HIGH</span></c:when>
                            <c:when test="${request.urgency == 'MEDIUM'}"><span class="badge bg-info text-dark">MEDIUM</span></c:when>
                            <c:otherwise><span class="badge bg-secondary">LOW</span></c:otherwise>
                        </c:choose>
                    </div>
                </div>
                <div class="col-md-2 col-6">
                    <div class="text-secondary small text-uppercase fw-semibold">Deficit</div>
                    <div class="text-danger fw-bold fs-5"><c:out value="${request.remainingQuantity}"/></div>
                </div>
                <div class="col-md-2 col-6">
                    <div class="text-secondary small text-uppercase fw-semibold">Required By</div>
                    <div class="text-light"><c:out value="${request.requiredByDate}"/></div>
                </div>
                <div class="col-md-2 col-6">
                    <div class="text-secondary small text-uppercase fw-semibold">Search Radius</div>
                    <div class="text-primary fw-bold"><fmt:formatNumber value="${searchRadius}" maxFractionDigits="0"/> km</div>
                </div>
            </div>
        </div>
    </div>

    <c:choose>
        <c:when test="${not canMatch}">
            <div class="alert alert-warning border-0 shadow">
                <i class="bi bi-info-circle me-2"></i>
                This request is in <strong><c:out value="${request.status}"/></strong> status and cannot be matched.
            </div>
        </c:when>
        <c:when test="${empty candidates}">
            <div class="card bg-dark border-secondary shadow-lg">
                <div class="card-body text-center py-5">
                    <i class="bi bi-geo-alt fs-1 text-secondary d-block mb-3"></i>
                    <h5 class="text-light">No Matching Donor Facilities Found</h5>
                    <p class="text-secondary">
                        No facilities within <fmt:formatNumber value="${searchRadius}" maxFractionDigits="0"/> km
                        have sufficient transferable stock for this medicine.
                    </p>
                    <c:if test="${searchRadius < 100}">
                        <a href="<c:url value='/matching/request/${request.id}'/>?radius=100"
                           class="btn btn-outline-primary mt-2">
                            <i class="bi bi-arrows-angle-expand me-1"></i>Expand Search Radius to 100 km
                        </a>
                    </c:if>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <%-- Results Summary --%>
            <div class="d-flex justify-content-between align-items-center mb-3">
                <div>
                    <span class="badge bg-primary me-2">${fn:length(candidates)} Candidate(s) Found</span>
                    <span class="text-secondary small">Ranked deterministically across distance, surplus, donor risk, expiry, and reliability.</span>
                </div>
            </div>

            <%-- Candidate Cards --%>
            <c:forEach var="cand" items="${candidates}" varStatus="idx">
                <div class="card bg-dark border-secondary shadow-lg mb-3 ${cand.matchStatus == 'RECOMMENDED' ? 'border-primary' : ''}">
                    <div class="card-body p-4">
                        <div class="row align-items-center g-3">
                            <%-- Rank & Score --%>
                            <div class="col-md-2 text-center border-end border-secondary">
                                <div class="fs-6 fw-semibold text-secondary">RANK #${idx.index + 1}</div>
                                <div class="fs-1 fw-bold
                                    ${cand.matchScore >= 70 ? 'text-success' :
                                      cand.matchScore >= 50 ? 'text-info' :
                                      cand.matchScore >= 30 ? 'text-warning' : 'text-danger'}">
                                    ${cand.matchScore}
                                </div>
                                <c:choose>
                                    <c:when test="${cand.matchStatus == 'RECOMMENDED'}">
                                        <span class="badge bg-success">RECOMMENDED</span>
                                    </c:when>
                                    <c:when test="${cand.matchStatus == 'SUITABLE'}">
                                        <span class="badge bg-primary">SUITABLE</span>
                                    </c:when>
                                    <c:when test="${cand.matchStatus == 'LOW_CONFIDENCE'}">
                                        <span class="badge bg-warning text-dark">LOW CONF.</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge bg-danger">NOT RECOM.</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>

                            <%-- Facility Info --%>
                            <div class="col-md-3">
                                <h6 class="mb-1 text-light fw-bold"><c:out value="${cand.facilityName}"/></h6>
                                <div class="text-secondary small mb-2">
                                    <c:out value="${cand.facilityType}"/> &bull; <c:out value="${cand.city}"/>
                                </div>
                                <div class="text-info small">
                                    <i class="bi bi-geo-alt me-1"></i>
                                    <strong><fmt:formatNumber value="${cand.distanceKm}" maxFractionDigits="1"/> km</strong> away
                                </div>
                            </div>

                            <%-- Stock Info --%>
                            <div class="col-md-3">
                                <div class="small text-secondary text-uppercase fw-semibold">Available / Transferable</div>
                                <div class="text-light mt-1">
                                    <strong><c:out value="${cand.availableQuantity}"/></strong> on hand
                                    &rarr; <strong class="text-success"><c:out value="${cand.transferableQuantity}"/></strong> transferable
                                </div>
                                <div class="small text-secondary mt-1">
                                    Nearest Expiry: <span class="text-light"><c:out value="${cand.nearestExpiryDays}"/> days</span>
                                </div>
                                <div class="small text-secondary mt-1">
                                    Reliability Score:
                                    <span class="text-light">
                                        <c:choose>
                                            <c:when test="${cand.facilityReliabilityScore != null}">
                                                <fmt:formatNumber value="${cand.facilityReliabilityScore}" maxFractionDigits="2"/>
                                            </c:when>
                                            <c:otherwise>N/A</c:otherwise>
                                        </c:choose>
                                    </span>
                                </div>
                            </div>

                            <%-- Score Breakdown --%>
                            <div class="col-md-4">
                                <div class="small text-secondary text-uppercase fw-semibold mb-2">Deterministic Scoring Weights</div>
                                <div class="small bg-black bg-opacity-25 p-2 rounded border border-secondary">
                                    <div class="d-flex justify-content-between text-secondary mb-1">
                                        <span>Distance (25%)</span><span class="text-light">${cand.distanceScore}</span>
                                    </div>
                                    <div class="d-flex justify-content-between text-secondary mb-1">
                                        <span>Surplus (30%)</span><span class="text-light">${cand.surplusScore}</span>
                                    </div>
                                    <div class="d-flex justify-content-between text-secondary mb-1">
                                        <span>Donor Risk (20%)</span><span class="text-light">${cand.donorRiskScoreValue}</span>
                                    </div>
                                    <div class="d-flex justify-content-between text-secondary mb-1">
                                        <span>Expiry (15%)</span><span class="text-light">${cand.expiryScore}</span>
                                    </div>
                                    <div class="d-flex justify-content-between text-secondary">
                                        <span>Reliability (10%)</span><span class="text-light">${cand.reliabilityScoreValue}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <%-- Request Transfer Action --%>
                    <div class="card-footer bg-dark border-top border-secondary d-flex justify-content-between align-items-center flex-wrap gap-2">
                        <div class="text-secondary small">
                            Max transferable surplus: <strong class="text-success"><c:out value="${cand.transferableQuantity}"/> units</strong>
                        </div>
                        <form action="${pageContext.request.contextPath}/transfers/create" method="POST" class="d-inline">
                            <c:if test="${not empty _csrf}">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                            </c:if>
                            <input type="hidden" name="emergencyRequestId" value="${request.id}"/>
                            <input type="hidden" name="fromFacilityId" value="${cand.facilityId}"/>
                            <input type="hidden" name="matchScore" value="${cand.matchScore}"/>
                            <input type="hidden" name="distanceKm" value="${cand.distanceKm}"/>
                            <div class="input-group input-group-sm" style="max-width: 280px;">
                                <input type="number" name="quantity" class="form-control bg-dark text-light border-secondary"
                                       value="${cand.transferableQuantity < request.remainingQuantity ? cand.transferableQuantity : request.remainingQuantity}"
                                       min="1"
                                       max="${cand.transferableQuantity < request.remainingQuantity ? cand.transferableQuantity : request.remainingQuantity}"
                                       required/>
                                <button type="submit" class="btn btn-primary btn-sm px-3">
                                    <i class="bi bi-arrow-left-right me-1"></i>Request Transfer
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </c:forEach>

            <div class="alert alert-dark border-secondary mt-3 text-secondary" role="alert">
                <i class="bi bi-info-circle text-primary me-2"></i>
                <strong class="text-light">Deterministic Algorithm Notice:</strong> Match scores are calculated using algorithmic weights (distance, surplus, donor risk, expiry, reliability). FIFO batch reservation is atomic and enforced during transfer creation.
            </div>
        </c:otherwise>
    </c:choose>

</div>
