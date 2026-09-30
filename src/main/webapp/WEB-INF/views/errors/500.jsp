<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>500 Server Error - MedRoute AI</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/main.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
</head>
<body>
    <div class="error-layout">
        <div class="error-card">
            <div class="error-code">500</div>
            <h1 class="error-title">Something Went Wrong</h1>
            <p class="error-desc">We're experiencing a temporary issue. Please try again later.</p>
            
            <a href="${pageContext.request.contextPath}/" class="btn btn-primary d-inline-block px-4 py-2">Go Home</a>
            
            <div class="mt-5 text-gray-500 text-sm font-medium">
                MedRoute AI
            </div>
        </div>
    </div>
</body>
</html>
