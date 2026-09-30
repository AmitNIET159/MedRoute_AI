<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>403 Access Denied - MedRoute AI</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/main.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
</head>
<body>
    <div class="error-layout">
        <div class="error-card">
            <div class="error-code">403</div>
            <h1 class="error-title">Access Denied</h1>
            <p class="error-desc">You don't have permission to access this page.</p>
            
            <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-primary d-inline-block px-4 py-2">Go to Dashboard</a>
            
            <div class="mt-4">
                <a href="${pageContext.request.contextPath}/contact" class="text-blue-500 text-sm text-decoration-none">Contact Administrator</a>
            </div>
            
            <div class="mt-5 text-gray-500 text-sm font-medium">
                MedRoute AI
            </div>
        </div>
    </div>
</body>
</html>
