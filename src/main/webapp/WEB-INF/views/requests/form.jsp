<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<div class="container-fluid p-0">
    <c:if test="${not empty error}">
        <div class="alert alert-danger alert-dismissible fade show mb-4" role="alert">
            <i class="bi bi-exclamation-triangle me-2"></i><c:out value="${error}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
            <h2 class="h3 fw-bold text-light mb-1"><i class="bi bi-send text-primary me-2"></i>New Emergency Request</h2>
            <p class="text-secondary small mb-0">Broadcast an urgent medicine allocation request to regional network facilities.</p>
        </div>
        <a href="<c:url value='/requests'/>" class="btn btn-outline-secondary">
            <i class="bi bi-arrow-left me-1"></i>Back to Requests
        </a>
    </div>

    <div class="row justify-content-center">
        <div class="col-lg-8">
            <div class="card bg-dark border-secondary shadow-lg">
                <div class="card-header bg-dark border-secondary py-3">
                    <h5 class="mb-0 text-light"><i class="bi bi-clipboard-plus text-primary me-2"></i>Emergency Request Parameters</h5>
                </div>
                <div class="card-body p-4">
                    <form action="<c:url value='/requests'/>" method="POST" id="requestForm">
                        <c:if test="${not empty _csrf}">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        </c:if>

                        <%-- Medicine Selection --%>
                        <div class="mb-3">
                            <label for="medicineId" class="form-label text-light fw-semibold">Medicine <span class="text-danger">*</span></label>
                            <select class="form-select bg-dark text-light border-secondary" id="medicineId" name="medicineId" required>
                                <option value="">-- Select Required Medicine --</option>
                                <c:forEach var="med" items="${medicines}">
                                    <option value="${med.id}">
                                        <c:out value="${med.name}"/>
                                        <c:if test="${not empty med.genericName}"> (<c:out value="${med.genericName}"/>)</c:if>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>

                        <%-- Quantity --%>
                        <div class="mb-3">
                            <label for="quantityNeeded" class="form-label text-light fw-semibold">Quantity Needed <span class="text-danger">*</span></label>
                            <input type="number" class="form-control bg-dark text-light border-secondary" id="quantityNeeded" name="quantityNeeded"
                                   min="1" max="100000" required placeholder="Enter required dosage units">
                        </div>

                        <%-- Urgency --%>
                        <div class="mb-3">
                            <label for="urgency" class="form-label text-light fw-semibold">Urgency Level <span class="text-danger">*</span></label>
                            <select class="form-select bg-dark text-light border-secondary" id="urgency" name="urgency" required>
                                <option value="">-- Select Urgency Level --</option>
                                <c:forEach var="u" items="${urgencyValues}">
                                    <option value="${u}">${u}</option>
                                </c:forEach>
                            </select>
                            <div class="form-text text-secondary mt-1">
                                <span class="text-info fw-semibold">LOW</span>: Buffer replenishment &nbsp;|&nbsp;
                                <span class="text-primary fw-semibold">MEDIUM</span>: Expected shortage &nbsp;|&nbsp;
                                <span class="text-warning fw-semibold">HIGH</span>: Imminent stockout &nbsp;|&nbsp;
                                <span class="text-danger fw-semibold">CRITICAL</span>: Active critical stockout
                            </div>
                        </div>

                        <%-- Required By Date --%>
                        <div class="mb-3">
                            <label for="requiredByDate" class="form-label text-light fw-semibold">Required By Date <span class="text-danger">*</span></label>
                            <input type="date" class="form-control bg-dark text-light border-secondary" id="requiredByDate" name="requiredByDate" required>
                        </div>

                        <%-- Reason --%>
                        <div class="mb-4">
                            <label for="reason" class="form-label text-light fw-semibold">Reason / Clinical Justification</label>
                            <textarea class="form-control bg-dark text-light border-secondary" id="reason" name="reason" rows="3"
                                      placeholder="Describe why this medicine is needed urgently..." maxlength="1000"></textarea>
                        </div>

                        <hr class="border-secondary my-4">
                        <div class="d-flex justify-content-end gap-2">
                            <a href="<c:url value='/requests'/>" class="btn btn-outline-secondary">Cancel</a>
                            <button type="submit" class="btn btn-primary px-4">
                                <i class="bi bi-send me-1"></i>Submit Request
                            </button>
                        </div>
                    </form>
                </div>
            </div>

            <div class="alert alert-dark border-secondary mt-3 text-secondary" role="alert">
                <i class="bi bi-info-circle text-primary me-2"></i>
                <strong class="text-light">Logistics Notice:</strong> Urgency levels establish supply chain routing priority across the network. Requesting facility is automatically bound to your active session.
            </div>
        </div>
    </div>
</div>

<script>
    (function() {
        var dateInput = document.getElementById('requiredByDate');
        if (dateInput) {
            var today = new Date().toISOString().split('T')[0];
            dateInput.setAttribute('min', today);
        }
    })();
</script>
