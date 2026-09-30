<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Batch Detail - MedRoute AI</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="<c:url value='/assets/css/main.css'/>" rel="stylesheet">
</head>
<body>
    <div class="container py-4">
        <a href="<c:url value='/inventory/list'/>" class="btn btn-outline-secondary mb-3">&larr; Back to List</a>
        
        <div class="card shadow-sm mb-4">
            <div class="card-header bg-white d-flex justify-content-between align-items-center">
                <h4 class="mb-0"><c:out value="${item.medicineName}"/> (Batch: <c:out value="${item.batchNumber}"/>)</h4>
                <a href="<c:url value='/inventory/adjust/${item.id}'/>" class="btn btn-warning btn-sm">Adjust Stock</a>
            </div>
            <div class="card-body row">
                <div class="col-md-3 mb-2"><strong>Total Quantity:</strong> <c:out value="${item.quantity}"/></div>
                <div class="col-md-3 mb-2"><strong>Reserved:</strong> <c:out value="${item.reservedQuantity}"/></div>
                <div class="col-md-3 mb-2"><strong>Available:</strong> <c:out value="${item.quantity - item.reservedQuantity}"/></div>
                <div class="col-md-3 mb-2"><strong>Expiry Date:</strong> <c:out value="${item.expiryDate}"/></div>
                <div class="col-md-3 mb-2"><strong>Min / Target:</strong> <c:out value="${item.minQuantity}"/> / <c:out value="${item.targetQuantity}"/></div>
                <div class="col-md-3 mb-2"><strong>Supplier:</strong> <c:out value="${item.supplierName}"/></div>
            </div>
        </div>

        <ul class="nav nav-tabs mb-3" id="detailTabs">
            <li class="nav-item"><a class="nav-link active" data-bs-toggle="tab" href="#tx">Transaction History</a></li>
            <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#consumption">Consumption</a></li>
        </ul>

        <div class="tab-content">
            <div class="tab-pane fade show active" id="tx">
                <div class="card shadow-sm">
                    <div class="card-body p-0">
                        <table class="table table-hover mb-0">
                            <thead class="table-light">
                                <tr><th>Date</th><th>Type</th><th>Quantity</th><th>User</th><th>Reason</th></tr>
                            </thead>
                            <tbody>
                                <c:forEach items="${transactions}" var="tx">
                                    <tr>
                                        <td><c:out value="${tx.date}"/></td>
                                        <td><c:out value="${tx.type}"/></td>
                                        <td><c:out value="${tx.quantity}"/></td>
                                        <td><c:out value="${tx.userName}"/></td>
                                        <td><c:out value="${tx.reason}"/></td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty transactions}"><tr><td colspan="5" class="text-center text-muted">No transactions.</td></tr></c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
            <div class="tab-pane fade" id="consumption">
                <div class="card shadow-sm">
                    <div class="card-body p-0">
                        <table class="table table-hover mb-0">
                            <thead class="table-light">
                                <tr><th>Date</th><th>Consumed By</th><th>Quantity</th><th>Notes</th></tr>
                            </thead>
                            <tbody>
                                <c:forEach items="${consumptions}" var="cons">
                                    <tr>
                                        <td><c:out value="${cons.date}"/></td>
                                        <td><c:out value="${cons.department}"/></td>
                                        <td><c:out value="${cons.quantity}"/></td>
                                        <td><c:out value="${cons.notes}"/></td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty consumptions}"><tr><td colspan="4" class="text-center text-muted">No consumption records.</td></tr></c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
