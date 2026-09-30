<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<div class="container-fluid p-0">
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
            <h2 class="h3 fw-bold text-light mb-1">
                <i class="bi bi-hospital text-primary me-2"></i>${not empty facility.id ? 'Edit Facility Telemetry' : 'Register New Network Node'}
            </h2>
            <p class="text-secondary small mb-0">Configure hospital/clinic operational parameters, contact points, and geospatial addresses.</p>
        </div>
        <a href="${pageContext.request.contextPath}/facilities" class="btn btn-outline-secondary">
            <i class="bi bi-arrow-left me-1"></i>Back to Directory
        </a>
    </div>

    <div class="row justify-content-center">
        <div class="col-lg-8">
            <div class="card bg-dark border-secondary shadow-lg">
                <div class="card-header bg-dark border-secondary py-3">
                    <h5 class="mb-0 text-light"><i class="bi bi-geo-alt text-primary me-2"></i>Facility Profile</h5>
                </div>
                <div class="card-body p-4">
                    <form action="${pageContext.request.contextPath}/facilities/${not empty facility.id ? 'edit/' : 'new'}${not empty facility.id ? facility.id : ''}" method="post">
                        <c:if test="${not empty _csrf}">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        </c:if>
                        <c:if test="${not empty facility.id}">
                            <input type="hidden" name="id" value="<c:out value="${facility.id}"/>">
                        </c:if>
                        
                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <label for="name" class="form-label text-light fw-semibold">Facility Name <span class="text-danger">*</span></label>
                                <input type="text" class="form-control bg-dark text-light border-secondary" id="name" name="name" value="<c:out value="${facility.name}"/>" required>
                            </div>
                            <div class="col-md-6">
                                <label for="facilityType" class="form-label text-light fw-semibold">Facility Type <span class="text-danger">*</span></label>
                                <select class="form-select bg-dark text-light border-secondary" id="facilityType" name="facilityType" required>
                                    <option value="">Select Type...</option>
                                    <option value="HOSPITAL" ${facility.facilityType == 'HOSPITAL' ? 'selected' : ''}>Hospital</option>
                                    <option value="CLINIC" ${facility.facilityType == 'CLINIC' ? 'selected' : ''}>Clinic</option>
                                    <option value="WAREHOUSE" ${facility.facilityType == 'WAREHOUSE' ? 'selected' : ''}>Warehouse</option>
                                    <option value="PHARMACY" ${facility.facilityType == 'PHARMACY' ? 'selected' : ''}>Pharmacy</option>
                                    <option value="NGO" ${facility.facilityType == 'NGO' ? 'selected' : ''}>NGO</option>
                                </select>
                            </div>
                        </div>
                        
                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <label for="registrationNumber" class="form-label text-light fw-semibold">Registration Number <span class="text-danger">*</span></label>
                                <input type="text" class="form-control bg-dark text-light border-secondary" id="registrationNumber" name="registrationNumber" value="<c:out value="${facility.registrationNumber}"/>" required>
                            </div>
                            <div class="col-md-6">
                                <label for="phone" class="form-label text-light fw-semibold">Phone <span class="text-danger">*</span></label>
                                <input type="tel" class="form-control bg-dark text-light border-secondary" id="phone" name="phone" value="<c:out value="${facility.phone}"/>" required>
                            </div>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md-12">
                                <label for="email" class="form-label text-light fw-semibold">Contact Email <span class="text-danger">*</span></label>
                                <input type="email" class="form-control bg-dark text-light border-secondary" id="email" name="email" value="<c:out value="${facility.email}"/>" required>
                            </div>
                        </div>

                        <div class="mb-3">
                            <label for="address" class="form-label text-light fw-semibold">Street Address <span class="text-danger">*</span></label>
                            <textarea class="form-control bg-dark text-light border-secondary" id="address" name="address" rows="3" required><c:out value="${facility.address}"/></textarea>
                        </div>

                        <div class="row g-3 mb-4">
                            <div class="col-md-4">
                                <label for="city" class="form-label text-light fw-semibold">City <span class="text-danger">*</span></label>
                                <input type="text" class="form-control bg-dark text-light border-secondary" id="city" name="city" value="<c:out value="${facility.city}"/>" required>
                            </div>
                            <div class="col-md-4">
                                <label for="state" class="form-label text-light fw-semibold">State <span class="text-danger">*</span></label>
                                <input type="text" class="form-control bg-dark text-light border-secondary" id="state" name="state" value="<c:out value="${facility.state}"/>" required>
                            </div>
                            <div class="col-md-4">
                                <label for="pincode" class="form-label text-light fw-semibold">Pincode <span class="text-danger">*</span></label>
                                <input type="text" class="form-control bg-dark text-light border-secondary" id="pincode" name="pincode" value="<c:out value="${facility.pincode}"/>" required>
                            </div>
                        </div>

                        <hr class="border-secondary my-4">
                        <div class="d-flex justify-content-end gap-2">
                            <a href="${pageContext.request.contextPath}/facilities" class="btn btn-outline-secondary">Cancel</a>
                            <button type="submit" class="btn btn-primary px-4">
                                <i class="bi bi-check-circle me-1"></i>Save Facility
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>
