<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create Account - MedRoute AI</title>
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
            
            <h1 class="auth-tagline">Join the future of <span>medical logistics</span>.</h1>
            
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
                </svg>
            </div>
        </div>
        
        <!-- Right Panel -->
        <div class="auth-panel-right">
            <div class="auth-form-container" style="max-width: 500px;">
                <div class="mb-4 text-center">
                    <h2 class="text-2xl font-bold text-navy-900 mb-2">Create Account</h2>
                    <p class="text-gray-600">Register your facility with MedRoute AI</p>
                </div>
                
                <c:if test="${not empty errorMsg}">
                    <div class="alert alert-danger mb-4">
                        <i class="bi bi-exclamation-triangle-fill me-2"></i> <c:out value="${errorMsg}"/>
                    </div>
                </c:if>
                
                <form id="registerForm" action="${pageContext.request.contextPath}/auth/register" method="POST">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    
                    <div class="form-group mb-3">
                        <label class="form-label">Full Name</label>
                        <input type="text" id="fullName" name="fullName" class="form-control" value="${fn:escapeXml(param.fullName)}" required>
                    </div>
                    
                    <div class="row mb-3">
                        <div class="col-md-6 form-group">
                            <label class="form-label">Email Address</label>
                            <input type="email" id="email" name="email" class="form-control" placeholder="name@example.com" value="${fn:escapeXml(param.email)}" required>
                        </div>
                        <div class="col-md-6 form-group mt-3 mt-md-0">
                            <label class="form-label">Phone Number</label>
                            <div class="input-group">
                                <span class="input-group-text bg-light">+91</span>
                                <input type="tel" id="phone" name="phone" class="form-control" placeholder="9876543210" value="${fn:escapeXml(param.phone)}" required>
                            </div>
                        </div>
                    </div>
                    
                    <div class="row mb-3">
                        <div class="col-md-6 form-group">
                            <label class="form-label">Role</label>
                            <select id="role" name="role" class="form-control" required>
                                <option value="" disabled selected>Select Role</option>
                                <option value="HOSPITAL" ${param.role == 'HOSPITAL' ? 'selected' : ''}>Hospital Admin</option>
                                <option value="CLINIC" ${param.role == 'CLINIC' ? 'selected' : ''}>Clinic Admin</option>
                                <option value="PHARMACY" ${param.role == 'PHARMACY' ? 'selected' : ''}>Pharmacy Manager</option>
                                <option value="NGO" ${param.role == 'NGO' ? 'selected' : ''}>NGO Organizer</option>
                            </select>
                        </div>
                        <div class="col-md-6 form-group mt-3 mt-md-0">
                            <label class="form-label">Facility</label>
                            <select id="facilityId" name="facilityId" class="form-control" required>
                                <option value="" disabled selected>Select Facility</option>
                                <!-- In real app, populated via AJAX based on role or passed from model -->
                                <c:forEach items="${facilities}" var="facility">
                                    <option value="${facility.id}" data-type="${facility.facilityType}" ${param.facilityId == facility.id ? 'selected' : ''}>
                                        <c:out value="${facility.name}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    
                    <div class="row mb-4">
                        <div class="col-md-6 form-group">
                            <label class="form-label">Password</label>
                            <div class="form-group-with-icon">
                                <i class="bi bi-lock input-icon"></i>
                                <input type="password" id="password" name="password" class="form-control" required>
                            </div>
                            <div class="strength-meter">
                                <div class="strength-bar" id="strengthBar"></div>
                            </div>
                            <small class="text-muted mt-1 d-block" id="passwordHint">8+ chars, upper, lower, number</small>
                        </div>
                        <div class="col-md-6 form-group mt-3 mt-md-0">
                            <label class="form-label">Confirm Password</label>
                            <div class="form-group-with-icon">
                                <i class="bi bi-shield-lock input-icon"></i>
                                <input type="password" id="confirmPassword" name="confirmPassword" class="form-control" required>
                            </div>
                            <div class="invalid-feedback" id="matchError">Passwords do not match.</div>
                        </div>
                    </div>
                    
                    <div class="form-check mb-4">
                        <input class="form-check-input" type="checkbox" id="termsCheck" required>
                        <label class="form-check-label text-sm text-gray-600" for="termsCheck">
                            I agree to the <a href="#" class="text-blue-500 text-decoration-none">Terms of Service</a> and <a href="#" class="text-blue-500 text-decoration-none">Privacy Policy</a>
                        </label>
                    </div>
                    
                    <button type="submit" class="btn btn-primary w-100" id="submitBtn">
                        <span class="btn-text">Create Account</span>
                        <span class="spinner-border spinner-border-sm ms-2 d-none" role="status" aria-hidden="true" id="submitSpinner"></span>
                    </button>
                    
                    <div class="text-center mt-4 text-sm text-gray-600">
                        Already have an account? <a href="${pageContext.request.contextPath}/login" class="text-blue-500 font-medium text-decoration-none">Sign in</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
    
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            const form = document.getElementById('registerForm');
            const password = document.getElementById('password');
            const confirmPassword = document.getElementById('confirmPassword');
            const strengthBar = document.getElementById('strengthBar');
            const phone = document.getElementById('phone');
            const roleSelect = document.getElementById('role');
            const facilitySelect = document.getElementById('facilityId');
            
            // Basic Facility filtering mock
            roleSelect.addEventListener('change', function() {
                const role = this.value;
                const options = facilitySelect.options;
                for(let i = 1; i < options.length; i++) {
                    // if type matches role (simple heuristic for demo)
                    const type = options[i].getAttribute('data-type');
                    if(type === role) {
                        options[i].style.display = '';
                    } else {
                        options[i].style.display = 'none';
                    }
                }
                facilitySelect.value = "";
            });
            
            // Password strength
            password.addEventListener('input', function() {
                const val = this.value;
                let strength = 0;
                
                if (val.length >= 8) strength += 1;
                if (/[A-Z]/.test(val)) strength += 1;
                if (/[a-z]/.test(val)) strength += 1;
                if (/[0-9]/.test(val)) strength += 1;
                if (/[^A-Za-z0-9]/.test(val)) strength += 1;
                
                strengthBar.className = 'strength-bar';
                if (val.length === 0) {
                    strengthBar.style.width = '0';
                } else if (strength <= 2) {
                    strengthBar.style.width = '33%';
                    strengthBar.classList.add('strength-weak');
                } else if (strength <= 4) {
                    strengthBar.style.width = '66%';
                    strengthBar.classList.add('strength-medium');
                } else {
                    strengthBar.style.width = '100%';
                    strengthBar.classList.add('strength-strong');
                }
                checkMatch();
            });
            
            // Password match
            function checkMatch() {
                if (confirmPassword.value && password.value !== confirmPassword.value) {
                    confirmPassword.classList.add('is-invalid');
                    return false;
                } else {
                    confirmPassword.classList.remove('is-invalid');
                    return true;
                }
            }
            
            confirmPassword.addEventListener('input', checkMatch);
            
            // Form Submit
            form.addEventListener('submit', function(e) {
                let valid = true;
                
                // phone validation
                if (!/^[0-9]{10}$/.test(phone.value)) {
                    phone.classList.add('is-invalid');
                    valid = false;
                } else {
                    phone.classList.remove('is-invalid');
                }
                
                if (!checkMatch()) valid = false;
                
                // strong password requirement
                const pass = password.value;
                const strongRegex = new RegExp("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#\\$%\\^&\\*])(?=.{8,})");
                if(!strongRegex.test(pass)) {
                    password.classList.add('is-invalid');
                    valid = false;
                } else {
                    password.classList.remove('is-invalid');
                }
                
                if (!valid) {
                    e.preventDefault();
                } else {
                    document.getElementById('submitBtn').disabled = true;
                    document.getElementById('submitSpinner').classList.remove('d-none');
                    document.querySelector('.btn-text').textContent = 'Creating...';
                }
            });
        });
    </script>
</body>
</html>
