<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--
    MedRoute AI — Sidebar Navigation Fragment
    
    Role-aware navigation. Each section shows/hides
    based on the user's role stored in session.
    
    Session attributes used:
      - sessionScope.userId
      - sessionScope.userName
      - sessionScope.userEmail
      - sessionScope.userRole (ADMIN, HOSPITAL, CLINIC, PHARMACY, NGO)
      - sessionScope.facilityName
    
    Model attribute used:
      - activePage (string identifying current page for highlighting)
--%>

<c:set var="curRole" value="${not empty sessionScope.userRole ? sessionScope.userRole : sessionScope.ROLE}"/>
<c:set var="curName" value="${not empty sessionScope.userName ? sessionScope.userName : sessionScope.USER_NAME}"/>

<div class="sidebar">

    <%-- Brand --%>
    <div class="sidebar-brand">
        <div class="sidebar-brand-logo"><i class="bi bi-shield-plus"></i></div>
        <div class="sidebar-brand-text">
            <span class="sidebar-brand-name">MedRoute AI</span>
            <span class="sidebar-brand-tagline">Logistics Command</span>
        </div>
    </div>

    <%-- Navigation --%>
    <nav class="sidebar-nav" aria-label="Sidebar Navigation">

        <%-- Main --%>
        <div class="sidebar-nav-section">
            <div class="sidebar-nav-label">Operations</div>
            <ul style="list-style:none;padding:0;margin:0;">
                <li class="sidebar-nav-item">
                    <a href="${pageContext.request.contextPath}/demand/dashboard"
                       class="sidebar-nav-link ${activePage == 'dashboard' || activePage == 'demand-dashboard' ? 'active' : ''}">
                        <i class="bi bi-speedometer2 nav-icon"></i>
                        <span class="nav-label">Command Dashboard</span>
                    </a>
                </li>
            </ul>
        </div>

        <%-- Inventory & Supply (not for NGO) --%>
        <c:if test="${sessionScope.userRole != 'NGO'}">
            <div class="sidebar-nav-section">
                <div class="sidebar-nav-label">Inventory</div>
                <ul style="list-style:none;padding:0;margin:0;">
                    <li class="sidebar-nav-item">
                        <a href="${pageContext.request.contextPath}/inventory"
                           class="sidebar-nav-link ${activePage == 'inventory' && empty param.status && empty param.expiryRange ? 'active' : ''}">
                            <i class="bi bi-box-seam nav-icon"></i>
                            <span class="nav-label">Stock Overview</span>
                        </a>
                    </li>
                    <li class="sidebar-nav-item">
                        <a href="${pageContext.request.contextPath}/inventory?status=LOW"
                           class="sidebar-nav-link ${param.status == 'LOW' ? 'active' : ''}">
                            <i class="bi bi-exclamation-triangle nav-icon"></i>
                            <span class="nav-label">Low Stock</span>
                            <c:if test="${lowStockCount > 0}">
                                <span class="nav-badge">${lowStockCount}</span>
                            </c:if>
                        </a>
                    </li>
                    <li class="sidebar-nav-item">
                        <a href="${pageContext.request.contextPath}/inventory?status=CRITICAL"
                           class="sidebar-nav-link ${param.status == 'CRITICAL' ? 'active' : ''}">
                            <i class="bi bi-calendar-x nav-icon"></i>
                            <span class="nav-label">Critical Shortages</span>
                            <c:if test="${expiringCount > 0}">
                                <span class="nav-badge">${expiringCount}</span>
                            </c:if>
                        </a>
                    </li>
                </ul>
            </div>
        </c:if>

        <%-- Requests & Transfers (all roles) --%>
        <div class="sidebar-nav-section">
            <div class="sidebar-nav-label">Supply Chain</div>
            <ul style="list-style:none;padding:0;margin:0;">
                <li class="sidebar-nav-item">
                    <a href="${pageContext.request.contextPath}/requests"
                       class="sidebar-nav-link ${activePage == 'requests' ? 'active' : ''}">
                        <i class="bi bi-send nav-icon"></i>
                        <span class="nav-label">Emergency Requests</span>
                        <c:if test="${openRequestCount > 0}">
                            <span class="nav-badge">${openRequestCount}</span>
                        </c:if>
                    </a>
                </li>
                <li class="sidebar-nav-item">
                    <a href="${pageContext.request.contextPath}/transfers"
                       class="sidebar-nav-link ${activePage == 'transfers' ? 'active' : ''}">
                        <i class="bi bi-arrow-left-right nav-icon"></i>
                        <span class="nav-label">Transfers</span>
                    </a>
                </li>
            </ul>
        </div>

        <%-- Intelligence (all roles) --%>
        <div class="sidebar-nav-section">
            <div class="sidebar-nav-label">Intelligence</div>
            <ul style="list-style:none;padding:0;margin:0;">
                <li class="sidebar-nav-item">
                    <a href="${pageContext.request.contextPath}/demand/dashboard"
                       class="sidebar-nav-link ${activePage == 'demand-dashboard' ? 'active' : ''}">
                        <i class="bi bi-graph-up-arrow nav-icon"></i>
                        <span class="nav-label">Demand Intelligence</span>
                    </a>
                </li>
                <li class="sidebar-nav-item">
                    <a href="${pageContext.request.contextPath}/location/map"
                       class="sidebar-nav-link ${activePage == 'map' ? 'active' : ''}">
                        <i class="bi bi-geo-alt nav-icon"></i>
                        <span class="nav-label">Facility Map</span>
                    </a>
                </li>
                <li class="sidebar-nav-item">
                    <a href="${pageContext.request.contextPath}/ai/insights"
                       class="sidebar-nav-link ${activePage == 'ai-insights' ? 'active' : ''}">
                        <i class="bi bi-lightbulb nav-icon"></i>
                        <span class="nav-label">AI Insights</span>
                    </a>
                </li>
                <li class="sidebar-nav-item">
                    <a href="${pageContext.request.contextPath}/analytics"
                       class="sidebar-nav-link ${activePage == 'analytics' ? 'active' : ''}">
                        <i class="bi bi-bar-chart-line nav-icon"></i>
                        <span class="nav-label">Analytics</span>
                    </a>
                </li>
            </ul>
        </div>

        <%-- Network Directory --%>
        <div class="sidebar-nav-section">
            <div class="sidebar-nav-label">Network Directory</div>
            <ul style="list-style:none;padding:0;margin:0;">
                <li class="sidebar-nav-item">
                    <a href="${pageContext.request.contextPath}/facilities"
                       class="sidebar-nav-link ${activePage == 'facilities' ? 'active' : ''}">
                        <i class="bi bi-building nav-icon"></i>
                        <span class="nav-label">Facilities Network</span>
                    </a>
                </li>
                <li class="sidebar-nav-item">
                    <a href="${pageContext.request.contextPath}/medicines"
                       class="sidebar-nav-link ${activePage == 'medicines' ? 'active' : ''}">
                        <i class="bi bi-capsule nav-icon"></i>
                        <span class="nav-label">Medicines Catalog</span>
                    </a>
                </li>
            </ul>
        </div>

        <%-- Admin Section (ADMIN only) --%>
        <c:if test="${curRole == 'ADMIN'}">
            <div class="sidebar-nav-section">
                <div class="sidebar-nav-label">Administration</div>
                <ul style="list-style:none;padding:0;margin:0;">
                    <li class="sidebar-nav-item">
                        <a href="${pageContext.request.contextPath}/facilities"
                           class="sidebar-nav-link ${activePage == 'admin-facilities' ? 'active' : ''}">
                            <i class="bi bi-hospital nav-icon"></i>
                            <span class="nav-label">Facility Control</span>
                        </a>
                    </li>
                </ul>
            </div>
        </c:if>

    </nav>

    <%-- Sidebar Footer: User Info & Role Badge --%>
    <div class="sidebar-footer">
        <div class="sidebar-user">
            <div class="sidebar-user-avatar badge-role-${fn:toLowerCase(curRole)}">
                <c:out value="${fn:substring(curName != null ? curName : 'U', 0, 1)}"/>
            </div>
            <div class="sidebar-user-info">
                <div class="sidebar-user-name"><c:out value="${curName}"/></div>
                <div class="sidebar-user-role">
                    <span class="badge badge-role-${fn:toLowerCase(curRole)}" style="font-size: 9px; padding: 2px 6px;">
                        <c:out value="${curRole}"/>
                    </span>
                </div>
            </div>
            <a href="${pageContext.request.contextPath}/auth/logout" class="btn btn-sm btn-icon" title="Sign Out" style="color: var(--slate-400); text-decoration: none;">
                <i class="bi bi-box-arrow-right"></i>
            </a>
        </div>
    </div>
</div>
