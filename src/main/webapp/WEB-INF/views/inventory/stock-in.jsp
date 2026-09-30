<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Stock In - MedRoute AI</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="<c:url value='/assets/css/main.css'/>" rel="stylesheet">
</head>
<body>
    <div class="container py-5 max-w-md" style="max-width: 600px;">
        <div class="card shadow-sm">
            <div class="card-header bg-white py-3">
                <h4 class="mb-0 text-primary">Stock In</h4>
            </div>
            <div class="card-body">
                <form action="<c:url value='/inventory/stock-in'/>" method="POST">
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
                    
                    <div class="mb-3">
                        <label class="form-label">Medicine</label>
                        <select name="medicineId" class="form-select" required>
                            <option value="">Select Medicine...</option>
                            <c:forEach items="${medicines}" var="med">
                                <option value="<c:out value='${med.id}'/>"><c:out value='${med.name}'/></option>
                            </c:forEach>
                        </select>
                    </div>

                    <div class="mb-3">
                        <label class="form-label">Batch Number</label>
                        <input type="text" name="batchNumber" class="form-control" required>
                    </div>

                    <div class="mb-3">
                        <label class="form-label">Quantity</label>
                        <input type="number" name="quantity" class="form-control" min="1" required>
                    </div>

                    <div class="mb-3">
                        <label class="form-label">Received Date</label>
                        <input type="date" name="receivedDate" class="form-control" required>
                    </div>

                    <div class="mb-4">
                        <label class="form-label">Expiry Date</label>
                        <input type="date" name="expiryDate" class="form-control" required>
                    </div>

                    <div class="d-flex justify-content-end gap-2">
                        <a href="<c:url value='/inventory/list'/>" class="btn btn-outline-secondary">Cancel</a>
                        <button type="submit" class="btn btn-primary">Save Stock</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</body>
</html>
