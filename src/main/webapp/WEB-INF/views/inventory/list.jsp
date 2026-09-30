<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<div class="container-fluid p-0">
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
            <h2 class="h3 fw-bold text-light mb-1">
                <i class="bi bi-boxes text-primary me-2"></i>Stock & Inventory Telemetry
            </h2>
            <p class="text-secondary small mb-0">Live batch reserves, safety thresholds, and expiration timelines.</p>
        </div>
        <div class="d-flex gap-2">
            <span class="badge bg-primary fs-6 d-flex align-items-center">
                <i class="bi bi-shield-check me-1"></i>FIFO Enforced
            </span>
        </div>
    </div>

    <%-- Filter Bar --%>
    <div class="card bg-dark border-secondary shadow-lg mb-4">
        <div class="card-body p-3">
            <form action="<c:url value='/inventory'/>" method="GET" class="row g-2 align-items-center">
                <div class="col-md-4">
                    <div class="input-group input-group-sm">
                        <span class="input-group-text bg-dark text-secondary border-secondary"><i class="bi bi-search"></i></span>
                        <input type="text" class="form-control bg-dark text-light border-secondary" name="medicine"
                               placeholder="Search medicine name..." value="<c:out value='${param.medicine}'/>">
                    </div>
                </div>
                <div class="col-md-3">
                    <select class="form-select form-select-sm bg-dark text-light border-secondary" name="category">
                        <option value="">All Categories</option>
                        <c:forEach items="${categories}" var="cat">
                            <option value="<c:out value='${cat}'/>" ${param.category == cat ? 'selected' : ''}><c:out value='${cat}'/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-3">
                    <select class="form-select form-select-sm bg-dark text-light border-secondary" name="status">
                        <option value="">All Stock Statuses</option>
                        <option value="NORMAL" ${param.status == 'NORMAL' ? 'selected' : ''}>Normal Stock</option>
                        <option value="LOW" ${param.status == 'LOW' ? 'selected' : ''}>Low Stock Buffer</option>
                        <option value="CRITICAL" ${param.status == 'CRITICAL' ? 'selected' : ''}>Critical Stockout</option>
                    </select>
                </div>
                <div class="col-md-2">
                    <button type="submit" class="btn btn-outline-primary btn-sm w-100">
                        <i class="bi bi-funnel me-1"></i>Filter
                    </button>
                </div>
            </form>
        </div>
    </div>

    <%-- Inventory Table --%>
    <div class="card bg-dark border-secondary shadow-lg">
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-dark table-hover align-middle mb-0">
                    <thead class="table-dark text-uppercase small text-secondary border-secondary">
                        <tr>
                            <th class="ps-4">Medicine</th>
                            <th>Batch No.</th>
                            <th>Available Qty</th>
                            <th>Reserved Qty</th>
                            <th>Min / Target Buffer</th>
                            <th>Expiry Date</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${inventoryList}" var="item">
                            <tr>
                                <td class="ps-4">
                                    <span class="text-light fw-semibold"><c:out value="${item.medicineName}"/></span>
                                </td>
                                <td class="text-secondary font-monospace small"><c:out value="${item.batchNumber}"/></td>
                                <td>
                                    <span class="fs-6 fw-bold text-success">
                                        <c:out value="${item.quantity - item.reservedQuantity}"/>
                                    </span>
                                </td>
                                <td class="text-warning"><c:out value="${item.reservedQuantity}"/></td>
                                <td class="text-secondary small">
                                    <c:out value="${item.minimumStock}"/> / <c:out value="${item.targetStock}"/>
                                </td>
                                <td class="text-light small"><c:out value="${item.expiryDate}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${item.status.name() == 'CRITICAL'}"><span class="badge bg-danger">CRITICAL</span></c:when>
                                        <c:when test="${item.status.name() == 'LOW'}"><span class="badge bg-warning text-dark">LOW</span></c:when>
                                        <c:when test="${item.status.name() == 'EXPIRED'}"><span class="badge bg-dark border border-secondary">EXPIRED</span></c:when>
                                        <c:otherwise><span class="badge bg-success">NORMAL</span></c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty inventoryList}">
                            <tr>
                                <td colspan="7" class="text-center py-5 text-secondary">
                                    <i class="bi bi-box-seam fs-2 d-block mb-2 text-secondary"></i>
                                    No active inventory batches recorded for this facility.
                                </td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
    
    <c:if test="${totalPages > 1}">
        <nav class="mt-4">
            <ul class="pagination pagination-sm justify-content-center">
                <li class="page-item ${currentPage == 1 ? 'disabled' : ''}">
                    <a class="page-link bg-dark text-light border-secondary" href="?page=${currentPage - 1}">Previous</a>
                </li>
                <li class="page-item disabled">
                    <span class="page-link bg-dark text-secondary border-secondary">Page <c:out value="${currentPage}"/> of <c:out value="${totalPages}"/></span>
                </li>
                <li class="page-item ${currentPage == totalPages ? 'disabled' : ''}">
                    <a class="page-link bg-dark text-light border-secondary" href="?page=${currentPage + 1}">Next</a>
                </li>
            </ul>
        </nav>
    </c:if>
</div>
