<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Expiry Tracking - MedRoute AI</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="<c:url value='/assets/css/main.css'/>" rel="stylesheet">
</head>
<body>
    <div class="container-fluid py-4">
        <h2 class="text-primary mb-4">Expiry Tracking</h2>

        <div class="card shadow-sm mb-4">
            <div class="card-body bg-light">
                <form action="<c:url value='/inventory/expiry'/>" method="GET" class="row g-3">
                    <div class="col-md-4">
                        <select class="form-select" name="filter">
                            <option value="ALL" ${param.filter == 'ALL' ? 'selected' : ''}>All Expiring/Expired</option>
                            <option value="EXPIRING_SOON" ${param.filter == 'EXPIRING_SOON' ? 'selected' : ''}>Expiring Soon (30-90 days)</option>
                            <option value="CRITICAL" ${param.filter == 'CRITICAL' ? 'selected' : ''}>Critical (<30 days)</option>
                            <option value="EXPIRED" ${param.filter == 'EXPIRED' ? 'selected' : ''}>Expired</option>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <button type="submit" class="btn btn-secondary w-100">Filter</button>
                    </div>
                </form>
            </div>
        </div>

        <div class="card shadow-sm">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead class="table-light">
                            <tr>
                                <th>Medicine</th>
                                <th>Batch No.</th>
                                <th>Expiry Date</th>
                                <th>Days Left</th>
                                <th>Available Qty</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach items="${expiryList}" var="item">
                                <tr>
                                    <td><c:out value="${item.medicineName}"/></td>
                                    <td><c:out value="${item.batchNumber}"/></td>
                                    <td><c:out value="${item.expiryDate}"/></td>
                                    <td><c:out value="${item.daysToExpiry}"/></td>
                                    <td><c:out value="${item.quantity - item.reservedQuantity}"/></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${item.daysToExpiry < 0}">
                                                <span class="badge bg-dark">Expired</span>
                                            </c:when>
                                            <c:when test="${item.daysToExpiry <= 30}">
                                                <span class="badge bg-danger">Critical</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge bg-warning text-dark">Expiring Soon</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty expiryList}">
                                <tr><td colspan="6" class="text-center py-4 text-muted">No expiring items found.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
