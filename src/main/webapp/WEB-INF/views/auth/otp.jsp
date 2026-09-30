<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Verify OTP - MedRoute AI</title>
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
    <div class="otp-layout">
        <div class="otp-card">
            <div class="mb-4">
                <i class="bi bi-shield-check text-blue-500" style="font-size: 3rem;"></i>
            </div>
            <h2 class="text-2xl font-bold text-navy-900 mb-2">Verify Your Email</h2>
            <p class="text-gray-600 mb-4">
                We've sent a 6-digit code to <br>
                <strong class="text-navy-900"><c:out value="${maskedEmail}"/></strong>
            </p>
            
            <c:if test="${not empty errorMsg}">
                <div class="alert alert-danger mb-4">
                    <i class="bi bi-exclamation-circle me-2"></i> <c:out value="${errorMsg}"/>
                </div>
            </c:if>
            <c:if test="${not empty successMsg}">
                <div class="alert alert-success mb-4">
                    <i class="bi bi-check-circle me-2"></i> <c:out value="${successMsg}"/>
                </div>
            </c:if>
            
            <form id="otpForm" action="${pageContext.request.contextPath}/auth/verify-otp" method="POST">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="email" value="${fn:escapeXml(email)}">
                <input type="hidden" name="purpose" value="${fn:escapeXml(purpose)}">
                <input type="hidden" id="otpCode" name="otp">
                
                <div class="otp-inputs" id="otpContainer">
                    <input type="text" class="otp-input" maxlength="1" pattern="[0-9]" inputmode="numeric" autocomplete="one-time-code" autofocus required>
                    <input type="text" class="otp-input" maxlength="1" pattern="[0-9]" inputmode="numeric" required>
                    <input type="text" class="otp-input" maxlength="1" pattern="[0-9]" inputmode="numeric" required>
                    <input type="text" class="otp-input" maxlength="1" pattern="[0-9]" inputmode="numeric" required>
                    <input type="text" class="otp-input" maxlength="1" pattern="[0-9]" inputmode="numeric" required>
                    <input type="text" class="otp-input" maxlength="1" pattern="[0-9]" inputmode="numeric" required>
                </div>
                
                <button type="submit" class="btn btn-primary w-100 mb-4" id="verifyBtn">
                    <span class="btn-text">Verify</span>
                    <span class="spinner-border spinner-border-sm ms-2 d-none" role="status" aria-hidden="true" id="verifySpinner"></span>
                </button>
            </form>
            
            <div class="text-sm text-gray-600">
                Didn't receive the code? 
                <span id="resendContainer">
                    <button type="button" class="btn btn-link p-0 text-blue-500 text-decoration-none" id="resendBtn" disabled>
                        Resend code (<span id="timer">60</span>s)
                    </button>
                </span>
            </div>
        </div>
    </div>
    
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            const inputs = document.querySelectorAll('.otp-input');
            const form = document.getElementById('otpForm');
            const hiddenOtpCode = document.getElementById('otpCode');
            const resendBtn = document.getElementById('resendBtn');
            const timerSpan = document.getElementById('timer');
            
            // OTP Input Logic
            inputs.forEach((input, index) => {
                // Focus next on input
                input.addEventListener('input', function(e) {
                    if (this.value.length === 1) {
                        if (index < inputs.length - 1) {
                            inputs[index + 1].focus();
                        } else {
                            // Auto submit when all filled
                            if (checkAllFilled()) {
                                submitForm();
                            }
                        }
                    }
                });
                
                // Backspace handling
                input.addEventListener('keydown', function(e) {
                    if (e.key === 'Backspace' && this.value === '') {
                        if (index > 0) {
                            inputs[index - 1].focus();
                        }
                    }
                });
                
                // Paste handling
                input.addEventListener('paste', function(e) {
                    e.preventDefault();
                    const pastedData = e.clipboardData.getData('text').slice(0, 6).replace(/[^0-9]/g, '');
                    if (pastedData) {
                        for (let i = 0; i < pastedData.length; i++) {
                            if (i < inputs.length) {
                                inputs[i].value = pastedData[i];
                            }
                        }
                        if (pastedData.length === 6) {
                            inputs[5].focus();
                            submitForm();
                        } else {
                            inputs[pastedData.length].focus();
                        }
                    }
                });
            });
            
            function checkAllFilled() {
                return Array.from(inputs).every(input => input.value.length === 1);
            }
            
            function submitForm() {
                const code = Array.from(inputs).map(input => input.value).join('');
                hiddenOtpCode.value = code;
                document.getElementById('verifyBtn').disabled = true;
                document.getElementById('verifySpinner').classList.remove('d-none');
                document.querySelector('.btn-text').textContent = 'Verifying...';
                form.submit();
            }
            
            form.addEventListener('submit', function(e) {
                if (!checkAllFilled()) {
                    e.preventDefault();
                    alert('Please enter all 6 digits.');
                } else {
                    const code = Array.from(inputs).map(input => input.value).join('');
                    hiddenOtpCode.value = code;
                }
            });
            
            // Resend Timer
            let timeLeft = 60;
            const timerId = setInterval(() => {
                timeLeft--;
                timerSpan.textContent = timeLeft;
                if (timeLeft <= 0) {
                    clearInterval(timerId);
                    resendBtn.disabled = false;
                    resendBtn.textContent = 'Resend code';
                }
            }, 1000);
            
            // Resend AJAX (mocked, would need CSRF and proper endpoint)
            resendBtn.addEventListener('click', function() {
                if (this.disabled) return;
                
                // In reality, you'd make a fetch request to /auth/resend-otp
                // Reset timer for now
                this.disabled = true;
                timeLeft = 60;
                this.innerHTML = 'Resend code (<span id="timer">60</span>s)';
                
                const newTimerSpan = document.getElementById('timer');
                const newTimerId = setInterval(() => {
                    timeLeft--;
                    newTimerSpan.textContent = timeLeft;
                    if (timeLeft <= 0) {
                        clearInterval(newTimerId);
                        this.disabled = false;
                        this.textContent = 'Resend code';
                    }
                }, 1000);
            });
        });
    </script>
</body>
</html>
