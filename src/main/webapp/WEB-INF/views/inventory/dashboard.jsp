<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Inventory Dashboard - MedRoute AI</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="<c:url value='/assets/css/main.css'/>" rel="stylesheet">
    <link href="<c:url value='/assets/css/components.css'/>" rel="stylesheet">
    <style>
        .kpi-card { border-left: 4px solid #0d6efd; background-color: #f8f9fa; }
        .kpi-low { border-left-color: #ffc107; }
        .kpi-critical { border-left-color: #dc3545; }
        .kpi-excess { border-left-color: #198754; }
    </style>
</head>
<body>
    <div class="container-fluid py-4">
        <h2 class="mb-4 text-primary">Inventory Dashboard</h2>
        
        <div class="row g-3 mb-4">
            <div class="col-md-2">
                <div class="card kpi-card shadow-sm">
                    <div class="card-body text-center">
                        <h6 class="text-muted">Total Items</h6>
                        <h4><c:out value="${kpis.totalItems}" default="0"/></h4>
                    </div>
                </div>
            </div>
            <div class="col-md-2">
                <div class="card kpi-card kpi-low shadow-sm">
                    <div class="card-body text-center">
                        <h6 class="text-muted">Low Stock</h6>
                        <h4><c:out value="${kpis.lowStock}" default="0"/></h4>
                    </div>
                </div>
            </div>
            <div class="col-md-2">
                <div class="card kpi-card kpi-critical shadow-sm">
                    <div class="card-body text-center">
                        <h6 class="text-muted">Critical</h6>
                        <h4><c:out value="${kpis.critical}" default="0"/></h4>
                    </div>
                </div>
            </div>
            <div class="col-md-2">
                <div class="card kpi-card kpi-excess shadow-sm">
                    <div class="card-body text-center">
                        <h6 class="text-muted">Excess</h6>
                        <h4><c:out value="${kpis.excess}" default="0"/></h4>
                    </div>
                </div>
            </div>
            <div class="col-md-2">
                <div class="card kpi-card kpi-low shadow-sm">
                    <div class="card-body text-center">
                        <h6 class="text-muted">Expiring Soon</h6>
                        <h4><c:out value="${kpis.expiring}" default="0"/></h4>
                    </div>
                </div>
            </div>
            <div class="col-md-2">
                <div class="card kpi-card kpi-critical shadow-sm">
                    <div class="card-body text-center">
                        <h6 class="text-muted">Expired</h6>
                        <h4><c:out value="${kpis.expired}" default="0"/></h4>
                    </div>
                </div>
            </div>
        </div>

        <div class="row g-4">
            <div class="col-md-4">
                <div class="card shadow-sm h-100">
                    <div class="card-header bg-white">
                        <h5 class="mb-0">Risk Distribution</h5>
                    </div>
                    <div class="card-body">
                        <canvas id="riskChart"></canvas>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card shadow-sm h-100">
                    <div class="card-header bg-white">
                        <h5 class="mb-0">Expiry Overview</h5>
                    </div>
                    <div class="card-body">
                        <canvas id="expiryChart"></canvas>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card shadow-sm h-100">
                    <div class="card-header bg-white">
                        <h5 class="mb-0">Consumption Trend</h5>
                    </div>
                    <div class="card-body">
                        <canvas id="trendChart"></canvas>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script>
        document.addEventListener("DOMContentLoaded", function() {
            const riskData = JSON.parse('<c:out value="${fn:escapeXml(riskDataJson)}" default="[0,0,0,0]"/>');
            new Chart(document.getElementById('riskChart'), {
                type: 'doughnut',
                data: {
                    labels: ['Normal', 'Low', 'Critical', 'Excess'],
                    datasets: [{
                        data: riskData,
                        backgroundColor: ['#0d6efd', '#ffc107', '#dc3545', '#198754']
                    }]
                }
            });

            const expiryData = JSON.parse('<c:out value="${fn:escapeXml(expiryDataJson)}" default="[0,0,0]"/>');
            new Chart(document.getElementById('expiryChart'), {
                type: 'pie',
                data: {
                    labels: ['Safe', 'Expiring Soon', 'Expired'],
                    datasets: [{
                        data: expiryData,
                        backgroundColor: ['#198754', '#ffc107', '#dc3545']
                    }]
                }
            });

            const trendData = JSON.parse('<c:out value="${fn:escapeXml(trendDataJson)}" default="[]"/>');
            const trendLabels = JSON.parse('<c:out value="${fn:escapeXml(trendLabelsJson)}" default="[]"/>');
            new Chart(document.getElementById('trendChart'), {
                type: 'line',
                data: {
                    labels: trendLabels,
                    datasets: [{
                        label: 'Consumption',
                        data: trendData,
                        borderColor: '#0d6efd',
                        tension: 0.1
                    }]
                }
            });
        });
    </script>
</body>
</html>
