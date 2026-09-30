<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<div class="container-fluid p-0">
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
            <h2 class="h3 fw-bold text-light mb-1">
                <i class="bi bi-hospital text-primary me-2"></i><c:out value="${facility.name}"/>
            </h2>
            <p class="text-secondary small mb-0">Operational node telemetry, environmental context, and inventory connections.</p>
        </div>
        <div class="d-flex gap-2">
            <a href="${pageContext.request.contextPath}/facilities" class="btn btn-outline-secondary">
                <i class="bi bi-arrow-left me-1"></i>Back
            </a>
            <c:if test="${sessionScope.userRole == 'ADMIN' || sessionScope.ROLE == 'ADMIN'}">
                <a href="${pageContext.request.contextPath}/facilities/edit/${facility.id}" class="btn btn-primary">
                    <i class="bi bi-pencil me-1"></i>Edit Facility
                </a>
            </c:if>
        </div>
    </div>

    <div class="row g-4">
        <%-- Main Facility Card --%>
        <div class="col-lg-8">
            <div class="card bg-dark border-secondary shadow-lg mb-4">
                <div class="card-header bg-dark border-secondary py-3 d-flex justify-content-between align-items-center">
                    <h5 class="mb-0 text-light"><i class="bi bi-info-circle text-primary me-2"></i>Node Parameters</h5>
                    <span class="badge bg-primary fs-6"><c:out value="${facility.facilityType}"/></span>
                </div>
                <div class="card-body p-4">
                    <table class="table table-dark table-borderless align-middle mb-0">
                        <tbody>
                            <tr>
                                <td class="text-secondary" style="width: 180px;">Registration No.</td>
                                <td class="text-light fw-bold font-monospace"><c:out value="${facility.registrationNumber}"/></td>
                            </tr>
                            <tr>
                                <td class="text-secondary">Phone Number</td>
                                <td class="text-light"><c:out value="${facility.phone}"/></td>
                            </tr>
                            <tr>
                                <td class="text-secondary">Contact Email</td>
                                <td class="text-light"><c:out value="${facility.email}"/></td>
                            </tr>
                            <tr>
                                <td class="text-secondary">Physical Address</td>
                                <td class="text-light">
                                    <c:out value="${facility.address}"/><br>
                                    <span class="text-secondary"><c:out value="${facility.city}"/>, <c:out value="${facility.state}"/> &mdash; <c:out value="${facility.pincode}"/></span>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <%-- Environmental Context --%>
            <c:if test="${not empty facility.latitude}">
                <div class="card bg-dark border-secondary shadow-lg">
                    <div class="card-header bg-dark border-secondary py-3">
                        <h6 class="mb-0 text-light"><i class="bi bi-cloud-sun text-primary me-2"></i>Environmental Logistics Telemetry</h6>
                    </div>
                    <div class="card-body p-4">
                        <div id="weatherContext" class="small text-secondary p-3 rounded bg-black bg-opacity-25 border border-secondary">
                            <span class="spinner-border spinner-border-sm text-primary me-2" role="status"></span>
                            Acquiring live meteorological telemetry...
                        </div>
                        <div class="small mt-2 text-secondary opacity-75">Weather telemetry stream provided via Open-Meteo API.</div>
                    </div>
                </div>
            </c:if>
        </div>

        <%-- Geolocation Intelligence Sidebar --%>
        <div class="col-lg-4">
            <div class="card bg-dark border-secondary shadow-lg">
                <div class="card-header bg-dark border-secondary py-3">
                    <h6 class="mb-0 text-light"><i class="bi bi-geo-alt text-primary me-2"></i>Location Telemetry</h6>
                </div>
                <div class="card-body p-4">
                    <div class="mb-3">
                        <label class="text-secondary small text-uppercase fw-semibold">Spatial Coordinates</label>
                        <div id="coordDisplay" class="fs-6 font-monospace mt-1">
                            <c:choose>
                                <c:when test="${not empty facility.latitude}">
                                    <span class="text-success"><c:out value="${facility.latitude}"/>, <c:out value="${facility.longitude}"/></span>
                                </c:when>
                                <c:otherwise>
                                    <span class="text-warning fst-italic">Coordinates unmapped</span>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                    <button class="btn btn-outline-primary w-100 py-2"
                            onclick="geocodeAddress(${facility.id}, '${facility.address}, ${facility.city}, ${facility.state} ${facility.pincode}')">
                        <i class="bi bi-crosshair me-1"></i>Geocode Facility Address
                    </button>
                    <p class="text-secondary small mt-2 mb-0">Resolves latitude and longitude coordinates and updates routing graph.</p>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    function geocodeAddress(id, addr) {
        if (!confirm('Execute geocoding lookup and calibrate coordinates for this facility?')) return;
        const formData = new URLSearchParams();
        formData.append('facilityId', id);
        formData.append('address', addr);
        
        fetch('${pageContext.request.contextPath}/api/location/geocode', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                alert('Facility coordinates calibrated successfully.');
                location.reload();
            } else {
                alert('Geocoding failed: ' + data.message);
            }
        })
        .catch(e => alert('Geocoding API error: ' + e));
    }
    
    <c:if test="${not empty facility.latitude}">
    (function() {
        const lat = ${facility.latitude};
        const lng = ${facility.longitude};
        const weatherContext = document.getElementById("weatherContext");
        if (!weatherContext) return;
        
        fetch('${pageContext.request.contextPath}/api/location/weather?lat=' + lat + '&lng=' + lng)
            .then(res => {
                if (!res.ok) throw new Error('Weather unavailable');
                return res.json();
            })
            .then(data => {
                const temp = data.temperature != null ? data.temperature + '°C' : 'N/A';
                const wind = data.windSpeed != null ? data.windSpeed + ' km/h' : 'N/A';
                const precip = data.rainProbability != null ? data.rainProbability + '%' : 'N/A';
                const conditions = data.logisticsContext || 'Clear Routing Conditions';
                
                weatherContext.innerHTML =
                    '<div class="text-light fw-bold fs-6 mb-1">' + temp + ' &bull; ' + conditions + '</div>' +
                    '<div class="text-secondary">Surface Wind: <span class="text-light">' + wind + '</span> &bull; Precipitation Risk: <span class="text-light">' + precip + '</span></div>';
            })
            .catch(err => {
                weatherContext.innerHTML = '<span class="text-warning"><i class="bi bi-exclamation-triangle me-1"></i> Environmental weather telemetry currently unreachable</span>';
                console.error('Weather error:', err);
            });
    })();
    </c:if>
</script>
