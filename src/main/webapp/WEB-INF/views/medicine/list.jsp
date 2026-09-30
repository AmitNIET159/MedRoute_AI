<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<div class="container-fluid p-0">
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
            <h2 class="h3 fw-bold text-light mb-1">
                <i class="bi bi-capsule text-primary me-2"></i>Medicine & Formulary Catalog
            </h2>
            <p class="text-secondary small mb-0">Standardized pharmaceutical inventory catalog and unit definitions.</p>
        </div>
        <c:if test="${sessionScope.userRole == 'ADMIN' || sessionScope.ROLE == 'ADMIN'}">
            <a href="${pageContext.request.contextPath}/medicines/new" class="btn btn-primary">
                <i class="bi bi-plus-circle me-1"></i>Add Medicine
            </a>
        </c:if>
    </div>

    <%-- Filter Bar --%>
    <div class="card bg-dark border-secondary shadow-lg mb-4">
        <div class="card-body p-3">
            <form action="${pageContext.request.contextPath}/medicines" method="get" class="row g-2 align-items-center">
                <div class="col-md-5">
                    <div class="input-group input-group-sm">
                        <span class="input-group-text bg-dark text-secondary border-secondary"><i class="bi bi-search"></i></span>
                        <input type="text" class="form-control bg-dark text-light border-secondary" name="search"
                               placeholder="Search medicine by trade or generic name..." value="<c:out value="${param.search}"/>">
                    </div>
                </div>
                <div class="col-md-4">
                    <select class="form-select form-select-sm bg-dark text-light border-secondary" name="category">
                        <option value="">All Categories</option>
                        <c:forEach var="cat" items="${categories}">
                            <option value="${cat}" ${param.category == cat ? 'selected' : ''}><c:out value="${cat}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-3 d-flex gap-2">
                    <button type="submit" class="btn btn-outline-primary btn-sm w-100">
                        <i class="bi bi-funnel me-1"></i>Filter
                    </button>
                    <a href="${pageContext.request.contextPath}/medicines" class="btn btn-outline-secondary btn-sm">Reset</a>
                </div>
            </form>
        </div>
    </div>

    <div class="card bg-dark border-secondary shadow-lg">
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-dark table-hover align-middle mb-0">
                    <thead class="table-dark text-uppercase small text-secondary border-secondary">
                        <tr>
                            <th class="ps-4">Medicine Name</th>
                            <th>Generic Formula</th>
                            <th>Category</th>
                            <th>Unit</th>
                            <th>Formulary Status</th>
                            <th class="text-end pe-4">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="med" items="${medicines}">
                            <tr>
                                <td class="ps-4">
                                    <span class="text-light fw-semibold"><c:out value="${med.name}"/></span>
                                </td>
                                <td class="text-secondary small"><c:out value="${med.genericName}"/></td>
                                <td><span class="badge bg-secondary"><c:out value="${not empty med.categoryName ? med.categoryName : med.categoryId}"/></span></td>
                                <td class="text-light"><c:out value="${med.unit}"/></td>
                                <td>
                                    <span class="badge ${med.controlled ? 'bg-warning text-dark' : (med.requiresColdChain ? 'bg-info text-dark' : 'bg-success')}">
                                        ${med.controlled ? 'CONTROLLED' : (med.requiresColdChain ? 'COLD CHAIN' : 'ACTIVE')}
                                    </span>
                                </td>
                                <td class="text-end pe-4">
                                    <a href="${pageContext.request.contextPath}/medicines/${med.id}" class="btn btn-sm btn-outline-primary me-1">
                                        <i class="bi bi-eye"></i> View
                                    </a>
                                    <c:if test="${sessionScope.userRole == 'ADMIN' || sessionScope.ROLE == 'ADMIN'}">
                                        <a href="${pageContext.request.contextPath}/medicines/edit/${med.id}" class="btn btn-sm btn-outline-secondary">
                                            <i class="bi bi-pencil"></i> Edit
                                        </a>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty medicines}">
                            <tr>
                                <td colspan="6" class="text-center py-5 text-secondary">
                                    <i class="bi bi-capsule fs-2 d-block mb-2 text-secondary"></i>
                                    No medicines matched the search criteria.
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
        <nav aria-label="Medicine pagination" class="mt-4">
            <ul class="pagination pagination-sm justify-content-center">
                <li class="page-item ${currentPage == 1 ? 'disabled' : ''}">
                    <a class="page-link bg-dark text-light border-secondary" href="?page=${currentPage - 1}&search=<c:out value='${param.search}'/>&category=<c:out value='${param.category}'/>">Previous</a>
                </li>
                <c:forEach begin="1" end="${totalPages}" var="i">
                    <li class="page-item ${currentPage == i ? 'active' : ''}">
                        <a class="page-link ${currentPage == i ? 'bg-primary text-white border-primary' : 'bg-dark text-light border-secondary'}" href="?page=${i}&search=<c:out value='${param.search}'/>&category=<c:out value='${param.category}'/>">${i}</a>
                    </li>
                </c:forEach>
                <li class="page-item ${currentPage == totalPages ? 'disabled' : ''}">
                    <a class="page-link bg-dark text-light border-secondary" href="?page=${currentPage + 1}&search=<c:out value='${param.search}'/>&category=<c:out value='${param.category}'/>">Next</a>
                </li>
            </ul>
        </nav>
    </c:if>
</div>
