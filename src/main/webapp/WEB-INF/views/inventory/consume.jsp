<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Consume Stock - MedRoute AI</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="<c:url value='/assets/css/main.css'/>" rel="stylesheet">
</head>
<body>
    <div class="container py-5 max-w-md" style="max-width: 600px;">
        <div class="card shadow-sm">
            <div class="card-header bg-white py-3">
                <h4 class="mb-0 text-primary">Consume Stock</h4>
            </div>
            <div class="card-body">
                <c:if test="${not empty errorMsg}">
                    <div class="alert alert-danger"><c:out value="${errorMsg}"/></div>
                </c:if>
                <form action="<c:url value='/inventory/consume'/>" method="POST" id="consumeForm">
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
                    
                    <div class="mb-3">
                        <label class="form-label">Select Item (Batch)</label>
                        <select name="inventoryId" id="inventoryId" class="form-select" required onchange="updateAvailable()">
                            <option value="" data-available="0">Select batch...</option>
                            <c:forEach items="${inventoryList}" var="item">
                                <option value="<c:out value='${item.id}'/>" data-available="<c:out value='${item.quantity - item.reservedQuantity}'/>">
                                    <c:out value='${item.medicineName} - ${item.batchNumber}'/>
                                </option>
                            </c:forEach>
                        </select>
                        <div class="form-text">Available Stock: <span id="availableStockText">0</span></div>
                    </div>

                    <div class="mb-3">
                        <label class="form-label">Quantity to Consume</label>
                        <input type="number" name="quantity" id="consumeQuantity" class="form-control" min="1" required>
                    </div>

                    <div class="mb-4">
                        <label class="form-label">Date</label>
                        <input type="date" name="date" class="form-control" required>
                    </div>

                    <div class="d-flex justify-content-end gap-2">
                        <a href="<c:url value='/inventory/list'/>" class="btn btn-outline-secondary">Cancel</a>
                        <button type="submit" class="btn btn-primary" id="submitBtn">Record Consumption</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <script>
        function updateAvailable() {
            const select = document.getElementById('inventoryId');
            const selectedOption = select.options[select.selectedIndex];
            const available = parseInt(selectedOption.getAttribute('data-available') || '0', 10);
            document.getElementById('availableStockText').innerText = available;
            
            const qtyInput = document.getElementById('consumeQuantity');
            qtyInput.max = available;
        }

        document.getElementById('consumeForm').addEventListener('submit', function(e) {
            const select = document.getElementById('inventoryId');
            const selectedOption = select.options[select.selectedIndex];
            const available = parseInt(selectedOption.getAttribute('data-available') || '0', 10);
            const qty = parseInt(document.getElementById('consumeQuantity').value || '0', 10);

            if (qty > available) {
                e.preventDefault();
                alert('Cannot consume more than available stock.');
            }
        });
    </script>
</body>
</html>
