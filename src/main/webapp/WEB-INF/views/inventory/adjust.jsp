<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Adjust Inventory - MedRoute AI</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="<c:url value='/assets/css/main.css'/>" rel="stylesheet">
</head>
<body>
    <div class="container py-5" style="max-width: 500px;">
        <div class="card shadow-sm">
            <div class="card-header bg-white py-3">
                <h4 class="mb-0 text-primary">Adjust Stock</h4>
            </div>
            <div class="card-body">
                <div class="mb-4 bg-light p-3 rounded">
                    <strong>Item:</strong> <c:out value="${item.medicineName}"/><br>
                    <strong>Batch:</strong> <c:out value="${item.batchNumber}"/><br>
                    <strong>Current Quantity:</strong> <c:out value="${item.quantity}"/>
                </div>
                
                <form action="<c:url value='/inventory/adjust'/>" method="POST">
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
                    <input type="hidden" name="inventoryId" value="<c:out value='${item.id}'/>">
                    
                    <div class="mb-3">
                        <label class="form-label">Quantity Change (+/-)</label>
                        <input type="number" name="quantityChange" class="form-control" required placeholder="e.g. -5 or 10">
                    </div>

                    <div class="mb-4">
                        <label class="form-label">Reason</label>
                        <textarea name="reason" class="form-control" rows="3" required placeholder="Reason for adjustment"></textarea>
                    </div>

                    <div class="d-flex justify-content-end gap-2">
                        <a href="<c:url value='/inventory/detail/${item.id}'/>" class="btn btn-outline-secondary">Cancel</a>
                        <button type="submit" class="btn btn-warning">Apply Adjustment</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</body>
</html>
