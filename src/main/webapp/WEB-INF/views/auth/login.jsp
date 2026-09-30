<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - MedRoute AI</title>
    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
    <!-- Custom CSS -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/main.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
</head>
<body>
    <div class="auth-layout">
        <!-- Left Panel -->
        <div class="auth-panel-left">
            <a href="${pageContext.request.contextPath}/" class="auth-brand">
                <i class="bi bi-shield-plus text-blue-500"></i> MedRoute AI
            </a>
            
            <h1 class="auth-tagline">Intelligent healthcare logistics <span>for everyone</span>.</h1>
            
            <div class="stat-card stat-1">
                <div class="stat-value">23,450+</div>
                <div class="stat-label">Medicines Tracked</div>
            </div>
            
            <div class="stat-card stat-2">
                <div class="stat-value">180+</div>
                <div class="stat-label">Facilities Connected</div>
            </div>
            
            <div class="auth-illustration">
                <!-- Inline SVG Illustration -->
                <svg width="400" height="300" viewBox="0 0 400 300" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <!-- Route Lines -->
                    <path d="M 100 150 C 150 50, 250 50, 300 150" stroke="rgba(255,255,255,0.2)" stroke-width="4" stroke-dasharray="10 10"/>
                    <path d="M 100 150 C 150 250, 250 250, 300 150" stroke="rgba(255,255,255,0.2)" stroke-width="4" stroke-dasharray="10 10"/>
                    <path d="M 100 150 C 150 50, 250 50, 300 150" stroke="#0F7B6C" stroke-width="4" stroke-dasharray="10 10" style="animation: pulse-route 3s linear infinite;"/>
                    
                    <!-- Nodes -->
                    <!-- Hospital (Left) -->
                    <circle cx="100" cy="150" r="30" fill="var(--navy-800)" stroke="var(--blue-500)" stroke-width="4"/>
                    <path d="M 90 150 h 20 m -10 -10 v 20" stroke="white" stroke-width="4" stroke-linecap="round"/>
                    
                    <!-- Pharmacy (Right) -->
                    <circle cx="300" cy="150" r="25" fill="var(--navy-800)" stroke="var(--teal-500)" stroke-width="4"/>
                    <rect x="292" y="142" width="16" height="16" stroke="white" stroke-width="2"/>
                    <path d="M 300 142 v 5" stroke="white" stroke-width="2"/>
                    
                    <!-- Clinic (Top Center) -->
                    <circle cx="200" cy="80" r="20" fill="var(--navy-800)" stroke="var(--blue-500)" stroke-width="3"/>
                    <circle cx="200" cy="80" r="8" fill="white"/>
                    
                    <!-- Warehouse (Bottom Center) -->
                    <circle cx="200" cy="220" r="20" fill="var(--navy-800)" stroke="white" stroke-width="3"/>
                    <rect x="190" y="210" width="20" height="20" rx="2" fill="white"/>
                </svg>
            </div>
        </div>
        
        <!-- Right Panel -->
        <div class="auth-panel-right">
            <div class="auth-form-container">
                <div class="mb-4 text-center">
                    <h2 class="text-2xl font-bold text-navy-900 mb-2">Welcome Back</h2>
                    <p class="text-gray-600">Sign in to your MedRoute AI account</p>
                </div>
                
                <!-- Flash Messages -->
                <c:if test="${not empty param.error or not empty error}">
                    <div class="alert alert-danger mb-4">
                        <i class="bi bi-exclamation-triangle-fill me-2"></i> 
                        <c:choose>
                            <c:when test="${not empty error}">${error}</c:when>
                            <c:otherwise>Invalid email or password.</c:otherwise>
                        </c:choose>
                    </div>
                </c:if>
                <c:if test="${not empty param.logout}">
                    <div class="alert alert-success mb-4">
                        <i class="bi bi-check-circle-fill me-2"></i> You have been logged out.
                    </div>
                </c:if>
                <c:if test="${not empty param.expired}">
                    <div class="alert alert-warning mb-4">
                        <i class="bi bi-clock-history me-2"></i> Your session has expired.
                    </div>
                </c:if>
                <c:if test="${not empty param.verified}">
                    <div class="alert alert-success mb-4">
                        <i class="bi bi-check-circle-fill me-2"></i> Email verified! You can now log in.
                    </div>
                </c:if>
                
                <form id="loginForm" action="${pageContext.request.contextPath}/auth/login" method="POST">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    
                    <div class="form-group mb-3">
                        <label class="form-label">Email Address</label>
                        <div class="form-group-with-icon">
                            <i class="bi bi-envelope input-icon"></i>
                            <input type="email" id="email" name="email" class="form-control" placeholder="name@facility.com" required>
                        </div>
                        <div class="invalid-feedback" id="emailError">Please enter a valid email.</div>
                    </div>
                    
                    <div class="form-group mb-4">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <label class="form-label mb-0">Password</label>
                            <a href="${pageContext.request.contextPath}/auth/forgot-password" class="text-sm text-blue-500 text-decoration-none">Forgot password?</a>
                        </div>
                        <div class="form-group-with-icon">
                            <i class="bi bi-lock input-icon"></i>
                            <input type="password" id="password" name="password" class="form-control" placeholder="••••••••" required>
                            <button type="button" class="password-toggle" id="togglePassword">
                                <i class="bi bi-eye"></i>
                            </button>
                        </div>
                        <div class="invalid-feedback" id="passwordError">Password is required.</div>
                    </div>
                    
                    <div class="form-check mb-4">
                        <input class="form-check-input" type="checkbox" name="remember-me" id="rememberMe">
                        <label class="form-check-label text-sm text-gray-600" for="rememberMe">
                            Remember me for 30 days
                        </label>
                    </div>
                    
                    <button type="submit" class="btn btn-primary w-100" id="submitBtn">
                        <span class="btn-text">Sign In</span>
                        <span class="spinner-border spinner-border-sm ms-2 d-none" role="status" aria-hidden="true" id="submitSpinner"></span>
                    </button>
                    
                    <div class="text-center mt-4 text-sm text-gray-600">
                        Don't have an account? <a href="${pageContext.request.contextPath}/register" class="text-blue-500 font-medium text-decoration-none">Create an account</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
    
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            const loginForm = document.getElementById('loginForm');
            const emailInput = document.getElementById('email');
            const passwordInput = document.getElementById('password');
            const togglePassword = document.getElementById('togglePassword');
            const submitBtn = document.getElementById('submitBtn');
            const submitSpinner = document.getElementById('submitSpinner');
            const btnText = document.querySelector('.btn-text');
            
            // Password visibility toggle
            togglePassword.addEventListener('click', function() {
                const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
                passwordInput.setAttribute('type', type);
                this.querySelector('i').classList.toggle('bi-eye');
                this.querySelector('i').classList.toggle('bi-eye-slash');
            });
            
            // Form validation
            loginForm.addEventListener('submit', function(e) {
                let isValid = true;
                
                // Email validation
                const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                if (!emailPattern.test(emailInput.value)) {
                    emailInput.classList.add('is-invalid');
                    isValid = false;
                } else {
                    emailInput.classList.remove('is-invalid');
                }
                
                // Password validation
                if (passwordInput.value.trim() === '') {
                    passwordInput.classList.add('is-invalid');
                    isValid = false;
                } else {
                    passwordInput.classList.remove('is-invalid');
                }
                
                if (!isValid) {
                    e.preventDefault();
                } else {
                    // Loading state
                    submitBtn.disabled = true;
                    submitSpinner.classList.remove('d-none');
                    btnText.textContent = 'Signing in...';
                }
            });
            
            // Remove error state on input
            [emailInput, passwordInput].forEach(input => {
                input.addEventListener('input', function() {
                    this.classList.remove('is-invalid');
                });
            });
        });
    </script>
</body>
</html>
