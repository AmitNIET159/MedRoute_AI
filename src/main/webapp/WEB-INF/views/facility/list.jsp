<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<div class="container-fluid p-0">
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
            <h2 class="h3 fw-bold text-light mb-1">
                <i class="bi bi-hospital text-primary me-2"></i>Healthcare Facilities Directory
            </h2>
            <p class="text-secondary small mb-0">Regional hospital and clinic telemetry nodes across the logistics network.</p>
        </div>
        <c:if test="${sessionScope.userRole == 'ADMIN' || sessionScope.ROLE == 'ADMIN'}">
            <a href="${pageContext.request.contextPath}/facilities/new" class="btn btn-primary">
                <i class="bi bi-plus-circle me-1"></i>Register New Facility
            </a>
        </c:if>
    </div>

    <div class="card bg-dark border-secondary shadow-lg">
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-dark table-hover align-middle mb-0">
                    <thead class="table-dark text-uppercase small text-secondary border-secondary">
                        <tr>
                            <th class="ps-4">Facility Name</th>
                            <th>Type</th>
                            <th>Registration</th>
                            <th>Phone</th>
                            <th>City</th>
                            <th>State</th>
                            <th class="text-end pe-4">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="facility" items="${facilities}">
                            <tr>
                                <td class="ps-4">
                                    <span class="text-light fw-semibold"><c:out value="${facility.name}"/></span>
                                </td>
                                <td>
                                    <span class="badge bg-secondary"><c:out value="${facility.facilityType}"/></span>
                                </td>
                                <td class="text-secondary font-monospace"><c:out value="${facility.registrationNumber}"/></td>
                                <td class="text-secondary"><c:out value="${facility.phone}"/></td>
                                <td class="text-light"><c:out value="${facility.city}"/></td>
                                <td class="text-secondary"><c:out value="${facility.state}"/></td>
                                <td class="text-end pe-4">
                                    <a href="${pageContext.request.contextPath}/facilities/${facility.id}" class="btn btn-sm btn-outline-primary me-1">
                                        <i class="bi bi-eye"></i> View
                                    </a>
                                    <c:if test="${sessionScope.userRole == 'ADMIN' || sessionScope.ROLE == 'ADMIN'}">
                                        <a href="${pageContext.request.contextPath}/facilities/edit/${facility.id}" class="btn btn-sm btn-outline-secondary">
                                            <i class="bi bi-pencil"></i> Edit
                                        </a>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty facilities}">
                            <tr>
                                <td colspan="7" class="text-center py-5 text-secondary">
                                    <i class="bi bi-building fs-2 d-block mb-2 text-secondary"></i>
                                    No facilities registered in the network.
                                </td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <!-- Pagination -->
    <c:if test="${totalPages > 1}">
        <nav aria-label="Facility pagination" class="mt-4">
            <ul class="pagination pagination-sm justify-content-center">
                <li class="page-item ${currentPage == 1 ? 'disabled' : ''}">
                    <a class="page-link bg-dark text-light border-secondary" href="?page=${currentPage - 1}">Previous</a>
                </li>
                <c:forEach begin="1" end="${totalPages}" var="i">
                    <li class="page-item ${currentPage == i ? 'active' : ''}">
                        <a class="page-link ${currentPage == i ? 'bg-primary text-white border-primary' : 'bg-dark text-light border-secondary'}" href="?page=${i}">${i}</a>
                    </li>
                </c:forEach>
                <li class="page-item ${currentPage == totalPages ? 'disabled' : ''}">
                    <a class="page-link bg-dark text-light border-secondary" href="?page=${currentPage + 1}">Next</a>
                </li>
            </ul>
        </nav>
    </c:if>
</div>
