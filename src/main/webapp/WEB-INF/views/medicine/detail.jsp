<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<div class="container-fluid p-0">
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
            <h2 class="h3 fw-bold text-light mb-1">
                <i class="bi bi-capsule text-primary me-2"></i><c:out value="${medicine.name}"/>
            </h2>
            <p class="text-secondary small mb-0">Formulary specifications, active chemical entity, and dispensing profile.</p>
        </div>
        <div class="d-flex gap-2">
            <a href="${pageContext.request.contextPath}/medicines" class="btn btn-outline-secondary">
                <i class="bi bi-arrow-left me-1"></i>Back to Catalog
            </a>
            <c:if test="${sessionScope.userRole == 'ADMIN' || sessionScope.ROLE == 'ADMIN'}">
                <a href="${pageContext.request.contextPath}/medicines/edit/${medicine.id}" class="btn btn-primary">
                    <i class="bi bi-pencil me-1"></i>Edit Medicine
                </a>
            </c:if>
        </div>
    </div>

    <div class="row justify-content-center">
        <div class="col-lg-8">
            <div class="card bg-dark border-secondary shadow-lg">
                <div class="card-header bg-dark border-secondary py-3 d-flex justify-content-between align-items-center">
                    <h5 class="mb-0 text-light"><i class="bi bi-info-circle text-primary me-2"></i>Medicine Specifications</h5>
                    <span class="badge ${medicine.controlled ? 'bg-warning text-dark' : (medicine.requiresColdChain ? 'bg-info text-dark' : 'bg-success')}">
                        ${medicine.controlled ? 'CONTROLLED SUBSTANCE' : (medicine.requiresColdChain ? 'COLD CHAIN REQUIRED' : 'ACTIVE FORMULARY')}
                    </span>
                </div>
                <div class="card-body p-4">
                    <table class="table table-dark table-borderless align-middle mb-0">
                        <tbody>
                            <tr>
                                <td class="text-secondary" style="width: 200px;">Trade / Brand Name</td>
                                <td class="text-light fw-bold fs-5"><c:out value="${medicine.name}"/></td>
                            </tr>
                            <tr>
                                <td class="text-secondary">Generic Entity</td>
                                <td class="text-info fw-semibold"><c:out value="${medicine.genericName}"/></td>
                            </tr>
                            <tr>
                                <td class="text-secondary">Therapeutic Category</td>
                                <td><span class="badge bg-secondary"><c:out value="${not empty medicine.categoryName ? medicine.categoryName : medicine.categoryId}"/></span></td>
                            </tr>
                            <tr>
                                <td class="text-secondary">Standard Unit</td>
                                <td class="text-light"><c:out value="${medicine.unit}"/></td>
                            </tr>
                            <c:if test="${not empty medicine.description}">
                                <tr>
                                    <td class="text-secondary align-top">Formulary Notes</td>
                                    <td class="text-secondary fst-italic"><c:out value="${medicine.description}"/></td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
