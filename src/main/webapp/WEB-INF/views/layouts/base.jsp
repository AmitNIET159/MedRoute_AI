<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">

    <!-- SEO -->
    <title><c:out value="${pageTitle}" default="MedRoute AI"/> — MedRoute AI</title>
    <meta name="description" content="${fn:escapeXml(pageDescription != null ? pageDescription : 'Intelligent Healthcare Medicine & Medical Supply Logistics Platform')}">
    <meta name="robots" content="noindex, nofollow">

    <!-- CSRF -->
    <c:if test="${not empty _csrf}">
        <meta name="_csrf" content="${_csrf.token}">
        <meta name="_csrf_header" content="${_csrf.headerName}">
    </c:if>

    <!-- Favicon -->
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/assets/icons/favicon.svg">

    <!-- Google Fonts: Inter -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">

    <!-- Bootstrap Icons -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">

    <!-- Leaflet CSS (conditionally loaded) -->
    <c:if test="${includeMap}">
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
              integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin="">
    </c:if>

    <!-- MedRoute Design System -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/main.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/layout.css">

    <!-- Page-specific CSS -->
    <c:if test="${not empty pageCss}">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/${pageCss}">
    </c:if>
</head>
<body data-authenticated="${(not empty sessionScope.userId || not empty sessionScope.USER_ID) ? 'true' : 'false'}"
      data-role="${fn:escapeXml(not empty sessionScope.userRole ? sessionScope.userRole : sessionScope.ROLE)}">

    <c:choose>
        <%-- Authenticated layout: sidebar + navbar + content --%>
        <c:when test="${not empty sessionScope.userId || not empty sessionScope.USER_ID}">
            <div class="app-layout">

                <%-- Sidebar --%>
                <aside class="app-sidebar" role="navigation" aria-label="Main Navigation">
                    <jsp:include page="/WEB-INF/views/layouts/sidebar.jsp"/>
                </aside>

                <%-- Main content area --%>
                <div class="app-main">

                    <%-- Top Navbar --%>
                    <header class="app-navbar">
                        <nav class="navbar" role="banner">
                            <div class="navbar-left">
                                <button class="navbar-menu-toggle" aria-label="Toggle menu">
                                    <i class="bi bi-list"></i>
                                </button>

                                <div class="live-beacon d-none d-md-inline-flex">
                                    <span class="live-beacon-dot"></span>
                                    <span>OPERATIONS ONLINE &bull; REALTIME DISPATCH</span>
                                </div>

                                <c:if test="${not empty breadcrumbs}">
                                    <nav aria-label="Breadcrumb">
                                        <ol class="breadcrumb">
                                            <li class="breadcrumb-item">
                                                <a href="${pageContext.request.contextPath}/demand/dashboard">
                                                    <i class="bi bi-house-door"></i>
                                                </a>
                                            </li>
                                            <c:forEach var="crumb" items="${breadcrumbs}" varStatus="status">
                                                <li class="breadcrumb-item ${status.last ? 'active' : ''}">
                                                    <c:choose>
                                                        <c:when test="${status.last}">
                                                            <c:out value="${crumb.label}"/>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <a href="${pageContext.request.contextPath}${crumb.url}">
                                                                <c:out value="${crumb.label}"/>
                                                            </a>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </li>
                                            </c:forEach>
                                        </ol>
                                    </nav>
                                </c:if>
                            </div>

                            <div class="navbar-right">
                                <%-- Role Badge --%>
                                <span class="badge badge-role-${fn:toLowerCase(not empty sessionScope.userRole ? sessionScope.userRole : sessionScope.ROLE)}">
                                    <c:out value="${not empty sessionScope.userRole ? sessionScope.userRole : sessionScope.ROLE}"/>
                                </span>

                                <%-- Notifications --%>
                                <div class="dropdown">
                                    <button class="navbar-notification"
                                            data-dropdown="notificationDropdown"
                                            aria-label="Notifications"
                                            data-tooltip="Notifications">
                                        <i class="bi bi-bell"></i>
                                        <span class="notification-dot" style="display:none;"></span>
                                    </button>
                                    <div id="notificationDropdown" class="dropdown-menu notification-dropdown">
                                        <div class="notification-dropdown-header">
                                            <h6>Notifications</h6>
                                            <a href="#" class="text-sm">Mark all read</a>
                                        </div>
                                        <div class="notification-dropdown-list" id="notificationList">
                                            <div class="empty-state-compact">
                                                <div class="empty-state-icon" style="width:40px;height:40px;font-size:var(--text-lg);">
                                                    <i class="bi bi-bell-slash"></i>
                                                </div>
                                                <p class="text-sm text-muted">No notifications</p>
                                            </div>
                                        </div>
                                        <div class="notification-dropdown-footer">
                                            <a href="${pageContext.request.contextPath}/notifications">View all</a>
                                        </div>
                                    </div>
                                </div>

                                <%-- User Menu --%>
                                <div class="dropdown">
                                    <button class="navbar-user" data-dropdown="userDropdown" aria-label="User menu">
                                        <div class="navbar-user-avatar badge-role-${fn:toLowerCase(not empty sessionScope.userRole ? sessionScope.userRole : sessionScope.ROLE)}">
                                            <c:out value="${fn:substring(not empty sessionScope.userName ? sessionScope.userName : (not empty sessionScope.USER_NAME ? sessionScope.USER_NAME : 'U'), 0, 1)}"/>
                                        </div>
                                        <span class="navbar-user-name">
                                            <c:out value="${not empty sessionScope.userName ? sessionScope.userName : sessionScope.USER_NAME}"/>
                                        </span>
                                        <i class="bi bi-chevron-down text-xs text-muted"></i>
                                    </button>
                                    <div id="userDropdown" class="dropdown-menu">
                                        <div style="padding:var(--space-2) var(--space-3);">
                                            <div class="text-sm font-semibold"><c:out value="${not empty sessionScope.userName ? sessionScope.userName : sessionScope.USER_NAME}"/></div>
                                            <div class="text-xs text-muted"><c:out value="${not empty sessionScope.userEmail ? sessionScope.userEmail : sessionScope.USER_EMAIL}"/></div>
                                        </div>
                                        <div class="dropdown-divider"></div>
                                        <a href="${pageContext.request.contextPath}/facilities" class="dropdown-item">
                                            <i class="bi bi-building"></i> Facilities
                                        </a>
                                        <a href="${pageContext.request.contextPath}/medicines" class="dropdown-item">
                                            <i class="bi bi-capsule"></i> Medicines Catalog
                                        </a>
                                        <div class="dropdown-divider"></div>
                                        <a href="${pageContext.request.contextPath}/auth/logout" class="dropdown-item dropdown-item-danger">
                                            <i class="bi bi-box-arrow-right"></i> Sign Out
                                        </a>
                                    </div>
                                </div>
                            </div>
                        </nav>
                    </header>

                    <%-- Page Content --%>
                    <main class="app-content" role="main">
                        <%-- Flash messages --%>
                        <c:if test="${not empty successMessage}">
                            <div class="alert alert-success alert-dismissible" role="alert">
                                <i class="alert-icon bi bi-check-circle-fill"></i>
                                <div class="alert-content">
                                    <div class="alert-message"><c:out value="${successMessage}"/></div>
                                </div>
                                <button class="alert-close" aria-label="Close">&times;</button>
                            </div>
                        </c:if>

                        <c:if test="${not empty errorMessage}">
                            <div class="alert alert-danger alert-dismissible" role="alert">
                                <i class="alert-icon bi bi-exclamation-circle-fill"></i>
                                <div class="alert-content">
                                    <div class="alert-message"><c:out value="${errorMessage}"/></div>
                                </div>
                                <button class="alert-close" aria-label="Close">&times;</button>
                            </div>
                        </c:if>

                        <%-- Page body injected here --%>
                        <jsp:include page="${contentPage}"/>
                    </main>
                </div>
            </div>
        </c:when>

        <%-- Unauthenticated layout: auth pages --%>
        <c:otherwise>
            <div class="auth-layout">
                <div class="app-main">
                    <main role="main">
                        <jsp:include page="${contentPage}"/>
                    </main>
                </div>
            </div>
        </c:otherwise>
    </c:choose>

    <!-- Chart.js (conditionally loaded) -->
    <c:if test="${includeCharts}">
        <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js"></script>
    </c:if>

    <!-- Leaflet JS (conditionally loaded) -->
    <c:if test="${includeMap}">
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"
                integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>
    </c:if>

    <!-- MedRoute Global JS -->
    <script src="${pageContext.request.contextPath}/assets/js/app.js"></script>

    <!-- Page-specific JS -->
    <c:if test="${not empty pageJs}">
        <script src="${pageContext.request.contextPath}/assets/js/${pageJs}"></script>
    </c:if>
</body>
</html>
