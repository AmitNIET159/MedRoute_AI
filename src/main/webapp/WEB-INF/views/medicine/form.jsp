<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<div class="container-fluid p-0">
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
            <h2 class="h3 fw-bold text-light mb-1">
                <i class="bi bi-capsule text-primary me-2"></i>${not empty medicine.id ? 'Edit Formulary Definition' : 'Register New Medicine'}
            </h2>
            <p class="text-secondary small mb-0">Define trade names, active chemical generic formulas, units, and inventory categories.</p>
        </div>
        <a href="${pageContext.request.contextPath}/medicines" class="btn btn-outline-secondary">
            <i class="bi bi-arrow-left me-1"></i>Back to Catalog
        </a>
    </div>

    <div class="row justify-content-center">
        <div class="col-lg-8">
            <div class="card bg-dark border-secondary shadow-lg">
                <div class="card-header bg-dark border-secondary py-3">
                    <h5 class="mb-0 text-light"><i class="bi bi-file-earmark-medical text-primary me-2"></i>Pharmaceutical Parameters</h5>
                </div>
                <div class="card-body p-4">
                    <form action="${pageContext.request.contextPath}/medicines/${not empty medicine.id ? 'edit/' : 'new'}${not empty medicine.id ? medicine.id : ''}" method="post">
                        <c:if test="${not empty _csrf}">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        </c:if>
                        <c:if test="${not empty medicine.id}">
                            <input type="hidden" name="id" value="<c:out value="${medicine.id}"/>">
                        </c:if>
                        
                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <label for="name" class="form-label text-light fw-semibold">Trade / Brand Name <span class="text-danger">*</span></label>
                                <input type="text" class="form-control bg-dark text-light border-secondary" id="name" name="name" value="<c:out value="${medicine.name}"/>" required placeholder="e.g. Paracetamol 500mg">
                            </div>
                            <div class="col-md-6">
                                <label for="genericName" class="form-label text-light fw-semibold">Generic / Active Chemical <span class="text-danger">*</span></label>
                                <input type="text" class="form-control bg-dark text-light border-secondary" id="genericName" name="genericName" value="<c:out value="${medicine.genericName}"/>" required placeholder="e.g. Acetaminophen">
                            </div>
                        </div>
                        
                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <label for="category" class="form-label text-light fw-semibold">Therapeutic Category <span class="text-danger">*</span></label>
                                <input type="text" class="form-control bg-dark text-light border-secondary" id="category" name="category" value="<c:out value="${medicine.category}"/>" required placeholder="e.g. Analgesic, Antibiotic">
                            </div>
                            <div class="col-md-6">
                                <label for="unit" class="form-label text-light fw-semibold">Dispensing Unit <span class="text-danger">*</span></label>
                                <input type="text" class="form-control bg-dark text-light border-secondary" id="unit" name="unit" value="<c:out value="${medicine.unit}"/>" required placeholder="e.g. Tablets, Vials, Boxes">
                            </div>
                        </div>

                        <div class="mb-3">
                            <label for="description" class="form-label text-light fw-semibold">Formulary Notes / Specifications</label>
                            <textarea class="form-control bg-dark text-light border-secondary" id="description" name="description" rows="3" placeholder="Storage temperature, shelf-life characteristics..."><c:out value="${medicine.description}"/></textarea>
                        </div>

                        <div class="mb-4 form-check">
                            <input type="checkbox" class="form-check-input" id="active" name="active" value="true" ${medicine.active or empty medicine.id ? 'checked' : ''}>
                            <label class="form-check-label text-light" for="active">Active in Formulary (Eligible for Allocation)</label>
                        </div>

                        <hr class="border-secondary my-4">
                        <div class="d-flex justify-content-end gap-2">
                            <a href="${pageContext.request.contextPath}/medicines" class="btn btn-outline-secondary">Cancel</a>
                            <button type="submit" class="btn btn-primary px-4">
                                <i class="bi bi-check-circle me-1"></i>Save Medicine
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>
