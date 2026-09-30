<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<style>
    /* ==========================================================================
       Tactical Geospatial Command Center — Futuristic Map Styling
       ========================================================================== */
    .tactical-wrapper {
        position: relative;
        background: radial-gradient(circle at 10% 20%, rgba(13, 23, 42, 0.95), rgba(7, 13, 24, 0.98));
        border-radius: 12px;
        border: 1px solid rgba(56, 189, 248, 0.2);
        box-shadow: 0 10px 35px rgba(0, 0, 0, 0.7), inset 0 0 30px rgba(56, 189, 248, 0.03);
        overflow: hidden;
    }

    .tactical-header-hud {
        background: rgba(10, 18, 32, 0.85);
        backdrop-filter: blur(12px);
        border-bottom: 1px solid rgba(56, 189, 248, 0.15);
        padding: 14px 20px;
    }

    .tactical-map-container {
        height: 760px;
        width: 100%;
        position: relative;
        background-color: #070d18;
    }

    #map {
        height: 100%;
        width: 100%;
        background-color: #070d18;
    }

    /* Cyberpunk High-Contrast Dark Inversion for OSM */
    .cyberpunk-dark-tiles {
        filter: invert(100%) hue-rotate(185deg) brightness(85%) contrast(115%);
    }

    /* Floating Map Layers Control */
    .map-floating-layers {
        position: absolute;
        top: 16px;
        right: 16px;
        z-index: 1000;
        background: rgba(13, 23, 42, 0.88);
        backdrop-filter: blur(10px);
        border: 1px solid rgba(56, 189, 248, 0.25);
        border-radius: 30px;
        padding: 4px;
        display: flex;
        gap: 4px;
        box-shadow: 0 6px 20px rgba(0, 0, 0, 0.6);
    }

    .layer-btn {
        background: transparent;
        border: none;
        color: #94a3b8;
        font-size: 0.75rem;
        font-weight: 600;
        padding: 6px 14px;
        border-radius: 20px;
        cursor: pointer;
        transition: all 0.2s ease;
        display: flex;
        align-items: center;
        gap: 6px;
    }

    .layer-btn:hover {
        color: #f1f5f9;
        background: rgba(56, 189, 248, 0.12);
    }

    .layer-btn.active {
        color: #030712;
        background: #38bdf8;
        box-shadow: 0 0 12px rgba(56, 189, 248, 0.6);
    }

    /* Floating Tactical Tools */
    .map-floating-tools {
        position: absolute;
        top: 16px;
        left: 60px;
        z-index: 1000;
        display: flex;
        gap: 8px;
    }

    .tool-btn {
        background: rgba(13, 23, 42, 0.88);
        backdrop-filter: blur(10px);
        border: 1px solid rgba(56, 189, 248, 0.25);
        color: #cbd5e1;
        font-size: 0.78rem;
        font-weight: 600;
        padding: 7px 12px;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.2s ease;
        display: flex;
        align-items: center;
        gap: 6px;
        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.5);
    }

    .tool-btn:hover {
        background: rgba(56, 189, 248, 0.2);
        color: #38bdf8;
        border-color: #38bdf8;
    }

    .tool-btn.active {
        background: rgba(56, 189, 248, 0.25);
        color: #38bdf8;
        border-color: #38bdf8;
        box-shadow: 0 0 10px rgba(56, 189, 248, 0.4);
    }

    /* Custom Neon Markers & Radar Pulse */
    .facility-marker {
        display: flex;
        align-items: center;
        justify-content: center;
        position: relative;
    }

    .marker-core {
        width: 34px;
        height: 34px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #ffffff;
        font-size: 14px;
        font-weight: bold;
        box-shadow: 0 0 12px rgba(0, 0, 0, 0.8);
        transition: transform 0.2s ease;
        position: relative;
        z-index: 2;
    }

    .marker-core:hover {
        transform: scale(1.15);
    }

    /* Facility Type Colors */
    .core-hospital {
        background: linear-gradient(135deg, #0284c7, #0369a1);
        border: 2px solid #38bdf8;
        box-shadow: 0 0 14px rgba(56, 189, 248, 0.8);
    }

    .core-warehouse {
        background: linear-gradient(135deg, #d97706, #b45309);
        border: 2px solid #fbbf24;
        box-shadow: 0 0 14px rgba(251, 191, 36, 0.8);
    }

    .core-pharmacy {
        background: linear-gradient(135deg, #059669, #047857);
        border: 2px solid #34d399;
        box-shadow: 0 0 14px rgba(52, 211, 153, 0.8);
    }

    .core-clinic {
        background: linear-gradient(135deg, #7c3aed, #6d28d9);
        border: 2px solid #a78bfa;
        box-shadow: 0 0 14px rgba(167, 139, 250, 0.8);
    }

    .core-ngo {
        background: linear-gradient(135deg, #e11d48, #be123c);
        border: 2px solid #fb7185;
        box-shadow: 0 0 14px rgba(251, 113, 133, 0.8);
    }

    /* Radiating Radar Pulse for High Risk */
    .radar-pulse-ring {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        border-radius: 50%;
        pointer-events: none;
        z-index: 1;
    }

    .pulse-critical {
        width: 68px;
        height: 68px;
        border: 2px solid rgba(239, 68, 68, 0.8);
        background: rgba(239, 68, 68, 0.18);
        animation: radar-pulse 1.8s infinite cubic-bezier(0.2, 0.6, 0.35, 1);
    }

    .pulse-high {
        width: 58px;
        height: 58px;
        border: 2px solid rgba(245, 158, 11, 0.8);
        background: rgba(245, 158, 11, 0.15);
        animation: radar-pulse 2.2s infinite cubic-bezier(0.2, 0.6, 0.35, 1);
    }

    @keyframes radar-pulse {
        0% {
            transform: translate(-50%, -50%) scale(0.4);
            opacity: 1;
        }
        100% {
            transform: translate(-50%, -50%) scale(1.6);
            opacity: 0;
        }
    }

    /* Animated Supply Corridors */
    .corridor-in-transit {
        stroke: #38bdf8;
        stroke-width: 3.5;
        stroke-dasharray: 8, 8;
        animation: corridor-flow 1.5s linear infinite;
        filter: drop-shadow(0 0 6px rgba(56, 189, 248, 0.9));
    }

    .corridor-scheduled {
        stroke: #f59e0b;
        stroke-width: 3;
        stroke-dasharray: 6, 6;
        animation: corridor-flow 2.5s linear infinite;
        filter: drop-shadow(0 0 5px rgba(245, 158, 11, 0.8));
    }

    .corridor-accepted {
        stroke: #10b981;
        stroke-width: 2.5;
        stroke-dasharray: 4, 4;
        filter: drop-shadow(0 0 4px rgba(16, 185, 129, 0.6));
    }

    @keyframes corridor-flow {
        from {
            stroke-dashoffset: 32;
        }
        to {
            stroke-dashoffset: 0;
        }
    }

    /* Tactical HUD Popup */
    .leaflet-popup-content-wrapper {
        background: rgba(13, 23, 42, 0.95) !important;
        backdrop-filter: blur(14px) !important;
        color: #f1f5f9 !important;
        border: 1px solid rgba(56, 189, 248, 0.35) !important;
        border-radius: 12px !important;
        box-shadow: 0 12px 35px rgba(0, 0, 0, 0.8), 0 0 15px rgba(56, 189, 248, 0.15) !important;
        padding: 0 !important;
        overflow: hidden;
    }

    .leaflet-popup-content {
        margin: 0 !important;
        line-height: 1.4;
    }

    .leaflet-popup-tip {
        background: rgba(13, 23, 42, 0.95) !important;
        border: 1px solid rgba(56, 189, 248, 0.35) !important;
    }

    .popup-hud-header {
        background: linear-gradient(135deg, rgba(2, 132, 199, 0.25), rgba(15, 23, 42, 0.8));
        border-bottom: 1px solid rgba(56, 189, 248, 0.2);
        padding: 12px 16px;
    }

    .popup-hud-body {
        padding: 14px 16px;
    }

    /* Left Control Sidebar */
    .tactical-sidebar {
        background: rgba(10, 18, 32, 0.85);
        border-right: 1px solid rgba(56, 189, 248, 0.15);
        height: 760px;
        display: flex;
        flex-direction: column;
    }

    .sidebar-search-box {
        padding: 14px 16px;
        border-bottom: 1px solid rgba(56, 189, 248, 0.12);
    }

    .search-input-tactical {
        background: rgba(15, 23, 42, 0.8);
        border: 1px solid rgba(56, 189, 248, 0.25);
        color: #f1f5f9;
        border-radius: 8px;
        padding: 8px 12px 8px 36px;
        font-size: 0.82rem;
        width: 100%;
        transition: all 0.2s ease;
    }

    .search-input-tactical:focus {
        outline: none;
        border-color: #38bdf8;
        box-shadow: 0 0 10px rgba(56, 189, 248, 0.35);
        background: rgba(15, 23, 42, 0.95);
    }

    .facility-feed-scroll {
        flex: 1;
        overflow-y: auto;
        padding: 10px 12px;
    }

    .facility-feed-scroll::-webkit-scrollbar {
        width: 5px;
    }

    .facility-feed-scroll::-webkit-scrollbar-thumb {
        background: rgba(56, 189, 248, 0.3);
        border-radius: 4px;
    }

    .facility-card-hud {
        background: rgba(15, 23, 42, 0.7);
        border: 1px solid rgba(56, 189, 248, 0.12);
        border-radius: 8px;
        padding: 10px 12px;
        margin-bottom: 8px;
        cursor: pointer;
        transition: all 0.2s ease;
    }

    .facility-card-hud:hover, .facility-card-hud.active {
        background: rgba(56, 189, 248, 0.15);
        border-color: rgba(56, 189, 248, 0.5);
        transform: translateX(3px);
        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.4);
    }

    .pill-filter {
        font-size: 0.7rem;
        font-weight: 600;
        padding: 3px 8px;
        border-radius: 12px;
        border: 1px solid rgba(56, 189, 248, 0.2);
        background: rgba(15, 23, 42, 0.6);
        color: #94a3b8;
        cursor: pointer;
        transition: all 0.15s ease;
    }

    .pill-filter:hover {
        color: #f1f5f9;
        border-color: #38bdf8;
    }

    .pill-filter.active {
        background: #38bdf8;
        color: #030712;
        border-color: #38bdf8;
        box-shadow: 0 0 8px rgba(56, 189, 248, 0.4);
    }

    /* Live Telemetry Floating Bottom Widget */
    .floating-telemetry-hud {
        position: absolute;
        bottom: 16px;
        right: 16px;
        z-index: 1000;
        background: rgba(13, 23, 42, 0.92);
        backdrop-filter: blur(12px);
        border: 1px solid rgba(56, 189, 248, 0.3);
        border-radius: 10px;
        padding: 10px 16px;
        display: flex;
        gap: 16px;
        box-shadow: 0 8px 25px rgba(0, 0, 0, 0.6);
    }
</style>

<div class="container-fluid p-0 mb-4">
    <!-- Top HUD Title & Status -->
    <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
        <div>
            <h2 class="h3 fw-bold text-light mb-1 d-flex align-items-center gap-2">
                <i class="bi bi-radar text-primary"></i>
                Geospatial Tactical Command
            </h2>
            <p class="text-secondary small mb-0">Autonomous monitoring of regional healthcare nodes, stock depletion alerts, and transit corridors.</p>
        </div>
        <div class="d-flex align-items-center gap-3">
            <div class="badge bg-dark border border-primary text-primary px-3 py-2 d-flex align-items-center gap-2">
                <span class="spinner-grow spinner-grow-sm text-primary" role="status"></span>
                <span>GEO-TELEMETRY ONLINE</span>
            </div>
            <div class="badge bg-dark border border-secondary text-secondary px-3 py-2">
                <i class="bi bi-shield-check text-success me-1"></i> DELHI-NCR SECURE NETWORK
            </div>
        </div>
    </div>

    <!-- Main Tactical Map Frame -->
    <div class="tactical-wrapper">
        <div class="row g-0">
            <!-- Left Tactical Sidebar -->
            <div class="col-lg-3 col-md-4 tactical-sidebar">
                <!-- Search & Filters -->
                <div class="sidebar-search-box">
                    <div class="position-relative mb-3">
                        <i class="bi bi-search position-absolute top-50 translate-middle-y text-secondary" style="left: 12px;"></i>
                        <input type="text" id="facilitySearch" class="search-input-tactical" placeholder="Search Hospital, Depot, City...">
                    </div>

                    <!-- Type Filter Pills -->
                    <div class="mb-2">
                        <div class="text-secondary small fw-bold mb-1" style="font-size: 0.68rem; letter-spacing: 0.5px;">FACILITY CLASSIFICATION</div>
                        <div class="d-flex flex-wrap gap-1" id="typePills">
                            <span class="pill-filter active" data-type="ALL">All (12)</span>
                            <span class="pill-filter" data-type="HOSPITAL"><i class="bi bi-hospital me-1"></i>Hospital</span>
                            <span class="pill-filter" data-type="WAREHOUSE"><i class="bi bi-box-seam me-1"></i>Depot</span>
                            <span class="pill-filter" data-type="PHARMACY"><i class="bi bi-capsule me-1"></i>Pharmacy</span>
                            <span class="pill-filter" data-type="CLINIC"><i class="bi bi-bandaid me-1"></i>Clinic</span>
                        </div>
                    </div>

                    <!-- Risk Filter Pills -->
                    <div>
                        <div class="text-secondary small fw-bold mb-1" style="font-size: 0.68rem; letter-spacing: 0.5px;">INVENTORY RISK STATUS</div>
                        <div class="d-flex flex-wrap gap-1" id="riskPills">
                            <span class="pill-filter active" data-risk="ALL">All Status</span>
                            <span class="pill-filter text-danger" data-risk="CRITICAL"><i class="bi bi-exclamation-triangle-fill me-1"></i>Critical</span>
                            <span class="pill-filter text-warning" data-risk="HIGH"><i class="bi bi-exclamation-circle-fill me-1"></i>High</span>
                            <span class="pill-filter text-info" data-risk="MODERATE">Moderate</span>
                            <span class="pill-filter text-success" data-risk="LOW"><i class="bi bi-check-circle-fill me-1"></i>Stable</span>
                        </div>
                    </div>
                </div>

                <!-- Feed Header -->
                <div class="d-flex justify-content-between align-items-center px-3 py-2 bg-dark bg-opacity-50 border-bottom border-secondary border-opacity-25">
                    <span class="text-secondary small fw-bold" style="font-size: 0.72rem;">TACTICAL NODES (<span id="nodesCount">12</span>)</span>
                    <button class="btn btn-link btn-sm text-primary p-0 text-decoration-none" onclick="resetAllFilters()" style="font-size: 0.72rem;">
                        <i class="bi bi-arrow-counterclockwise me-1"></i>Reset
                    </button>
                </div>

                <!-- Interactive Facilities Scroll List -->
                <div class="facility-feed-scroll" id="facilitiesList">
                    <!-- Populated dynamically via JS -->
                    <div class="text-center text-secondary py-5">
                        <div class="spinner-border spinner-border-sm text-primary mb-2" role="status"></div>
                        <div class="small">Scanning regional nodes...</div>
                    </div>
                </div>

                <!-- Bottom Telemetry Stats Bar -->
                <div class="p-2 border-top border-secondary border-opacity-25 bg-dark bg-opacity-75">
                    <div class="row g-1 text-center">
                        <div class="col-4 border-end border-secondary border-opacity-25">
                            <div class="fw-bold text-light" id="statTotalNodes">12</div>
                            <div class="text-secondary" style="font-size: 0.65rem;">MONITORED</div>
                        </div>
                        <div class="col-4 border-end border-secondary border-opacity-25">
                            <div class="fw-bold text-danger" id="statCriticalNodes">1</div>
                            <div class="text-secondary" style="font-size: 0.65rem;">CRITICAL</div>
                        </div>
                        <div class="col-4">
                            <div class="fw-bold text-primary" id="statActiveRoutes">4</div>
                            <div class="text-secondary" style="font-size: 0.65rem;">CORRIDORS</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Main Map Canvas -->
            <div class="col-lg-9 col-md-8">
                <div class="tactical-map-container">
                    <div id="map"></div>

                    <!-- Floating Layer Switcher -->
                    <div class="map-floating-layers">
                        <button class="layer-btn active" onclick="switchLayer('dark')">
                            <i class="bi bi-moon-stars"></i> Tactical Dark
                        </button>
                        <button class="layer-btn" onclick="switchLayer('cyber')">
                            <i class="bi bi-cpu"></i> Cyberpunk
                        </button>
                        <button class="layer-btn" onclick="switchLayer('satellite')">
                            <i class="bi bi-globe"></i> Satellite
                        </button>
                        <button class="layer-btn" onclick="switchLayer('osm')">
                            <i class="bi bi-map"></i> Street Grid
                        </button>
                    </div>

                    <!-- Floating Tactical Tools -->
                    <div class="map-floating-tools">
                        <button class="tool-btn" onclick="recenterNetwork()" title="Fit all 12 Delhi-NCR facilities in view">
                            <i class="bi bi-arrows-fullscreen"></i> Recenter NCR
                        </button>
                        <button class="tool-btn active" id="btnToggleRoutes" onclick="toggleCorridors()" title="Toggle live transfer corridors">
                            <i class="bi bi-bezier2"></i> Supply Corridors
                        </button>
                        <button class="tool-btn" id="btnToggleBuffer" onclick="toggleMatchBuffer()" title="50km Donor Radius from AIIMS Apex Hospital">
                            <i class="bi bi-circle"></i> 50km Buffer
                        </button>
                    </div>

                    <!-- Floating Live Telemetry HUD (Bottom Right) -->
                    <div class="floating-telemetry-hud" id="weatherHUD" style="display: none;">
                        <div class="d-flex align-items-center gap-2">
                            <i class="bi bi-cloud-sun text-warning fs-5" id="hudWeatherIcon"></i>
                            <div>
                                <div class="text-light fw-bold small" id="hudTemp">-- °C</div>
                                <div class="text-secondary" style="font-size: 0.68rem;" id="hudWeatherCondition">Clear Sky</div>
                            </div>
                        </div>
                        <div class="border-start border-secondary border-opacity-50 ps-3">
                            <div class="text-light fw-bold small" id="hudWind">-- km/h</div>
                            <div class="text-secondary" style="font-size: 0.68rem;">WIND VELOCITY</div>
                        </div>
                        <div class="border-start border-secondary border-opacity-50 ps-3">
                            <span class="badge bg-success" id="hudDroneBadge" style="font-size: 0.68rem;">
                                <i class="bi bi-airplane-engines me-1"></i>DRONE VIABLE
                            </span>
                            <div class="text-secondary" style="font-size: 0.68rem;">AIR CORRIDOR</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    (function() {
        let map = null;
        let baseLayers = {};
        let currentLayerKey = 'dark';
        
        let allFacilitiesData = [];
        let allRoutesData = [];
        let markersMap = {};
        let corridorsGroup = null;
        let bufferCircle = null;

        let activeTypeFilter = 'ALL';
        let activeRiskFilter = 'ALL';
        let showCorridors = true;
        let showBuffer = false;

        const contextPath = '${pageContext.request.contextPath}';

        function initTacticalMap() {
            if (map) return;

            // Map init centered at Delhi-NCR
            map = L.map('map', {
                center: [28.5355, 77.2600],
                zoom: 11,
                zoomControl: false,
                attributionControl: false
            });

            // Re-position zoom control to bottom left
            L.control.zoom({ position: 'bottomleft' }).addTo(map);

            // Layer 1: Tactical Dark (Esri Dark Canvas — 100% Free, NO API Key, NO Watermark)
            const esriDarkBase = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}', {
                maxZoom: 16,
                attribution: 'Tiles &copy; Esri'
            });
            const esriDarkRef = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Reference/MapServer/tile/{z}/{y}/{x}', {
                maxZoom: 16
            });
            baseLayers['dark'] = L.layerGroup([esriDarkBase, esriDarkRef]).addTo(map);

            // Layer 2: Cyberpunk Inverted OSM (100% Free, Neon High-Contrast, NO API Key)
            baseLayers['cyber'] = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                className: 'cyberpunk-dark-tiles',
                attribution: '&copy; OpenStreetMap'
            });

            // Layer 3: Satellite Photogrammetry (Esri World Imagery — 100% Free, NO API Key)
            baseLayers['satellite'] = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                maxZoom: 18,
                attribution: 'Tiles &copy; Esri'
            });

            // Layer 4: Standard Street Grid (OpenStreetMap — 100% Free, NO API Key)
            baseLayers['osm'] = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                attribution: '&copy; OpenStreetMap'
            });

            corridorsGroup = L.layerGroup().addTo(map);

            // Attach search listener
            document.getElementById('facilitySearch').addEventListener('input', function(e) {
                renderFacilitiesList(e.target.value);
            });

            // Attach pill filters
            setupFilterPills();

            // Fetch telemetry data
            loadTacticalTelemetry();
        }

        window.switchLayer = function(layerKey) {
            if (currentLayerKey === layerKey) return;
            map.removeLayer(baseLayers[currentLayerKey]);
            baseLayers[layerKey].addTo(map);
            currentLayerKey = layerKey;

            document.querySelectorAll('.layer-btn').forEach(btn => {
                btn.classList.toggle('active', btn.getAttribute('onclick').includes(layerKey));
            });
        };

        function setupFilterPills() {
            document.querySelectorAll('#typePills .pill-filter').forEach(pill => {
                pill.addEventListener('click', function() {
                    document.querySelectorAll('#typePills .pill-filter').forEach(p => p.classList.remove('active'));
                    this.classList.add('active');
                    activeTypeFilter = this.getAttribute('data-type');
                    applyFilters();
                });
            });

            document.querySelectorAll('#riskPills .pill-filter').forEach(pill => {
                pill.addEventListener('click', function() {
                    document.querySelectorAll('#riskPills .pill-filter').forEach(p => p.classList.remove('active'));
                    this.classList.add('active');
                    activeRiskFilter = this.getAttribute('data-risk');
                    applyFilters();
                });
            });
        }

        window.resetAllFilters = function() {
            document.getElementById('facilitySearch').value = '';
            document.querySelectorAll('#typePills .pill-filter').forEach(p => p.classList.remove('active'));
            document.querySelector('#typePills .pill-filter[data-type="ALL"]').classList.add('active');
            document.querySelectorAll('#riskPills .pill-filter').forEach(p => p.classList.remove('active'));
            document.querySelector('#riskPills .pill-filter[data-risk="ALL"]').classList.add('active');
            activeTypeFilter = 'ALL';
            activeRiskFilter = 'ALL';
            applyFilters();
            recenterNetwork();
        };

        function loadTacticalTelemetry() {
            Promise.all([
                fetch(contextPath + '/api/location/facilities').then(res => res.json()),
                fetch(contextPath + '/api/location/routes').then(res => res.json()).catch(() => [])
            ]).then(([facilities, routes]) => {
                allFacilitiesData = facilities || [];
                allRoutesData = routes || [];
                
                updateStatsOverview();
                renderMarkers();
                renderFacilitiesList();
                if (showCorridors) renderCorridors();
                recenterNetwork();
            }).catch(err => {
                console.error("Telemetry error:", err);
                document.getElementById('facilitiesList').innerHTML = 
                    '<div class="text-danger small p-3 text-center"><i class="bi bi-exclamation-triangle me-1"></i> Telemetry offline</div>';
            });
        }

        function updateStatsOverview() {
            const total = allFacilitiesData.length;
            const critical = allFacilitiesData.filter(d => d.currentRisk === 'CRITICAL').length;
            const inTransitRoutes = allRoutesData.filter(r => r.status === 'IN_TRANSIT' || r.status === 'SCHEDULED' || r.status === 'ACCEPTED').length;

            document.getElementById('statTotalNodes').innerText = total;
            document.getElementById('statCriticalNodes').innerText = critical;
            document.getElementById('statActiveRoutes').innerText = inTransitRoutes;
        }

        function getFacilityIcon(type) {
            switch(type) {
                case 'HOSPITAL': return 'bi-hospital';
                case 'WAREHOUSE': return 'bi-box-seam';
                case 'PHARMACY': return 'bi-capsule';
                case 'CLINIC': return 'bi-bandaid';
                default: return 'bi-heart-pulse';
            }
        }

        function getCoreClass(type) {
            switch(type) {
                case 'HOSPITAL': return 'core-hospital';
                case 'WAREHOUSE': return 'core-warehouse';
                case 'PHARMACY': return 'core-pharmacy';
                case 'CLINIC': return 'core-clinic';
                default: return 'core-ngo';
            }
        }

        function renderMarkers() {
            // Clear existing
            Object.values(markersMap).forEach(m => map.removeLayer(m));
            markersMap = {};

            allFacilitiesData.forEach(dto => {
                const f = dto.facility;
                const risk = dto.currentRisk || 'LOW';

                if (!f.latitude || !f.longitude) return;

                // Pulsing radar ring for Critical/High
                let pulseHtml = '';
                if (risk === 'CRITICAL') {
                    pulseHtml = '<div class="radar-pulse-ring pulse-critical"></div>';
                } else if (risk === 'HIGH') {
                    pulseHtml = '<div class="radar-pulse-ring pulse-high"></div>';
                }

                const markerHtml = 
                    '<div class="facility-marker" id="marker-f-' + f.id + '">' +
                        pulseHtml +
                        '<div class="marker-core ' + getCoreClass(f.facilityType) + '">' +
                            '<i class="bi ' + getFacilityIcon(f.facilityType) + '"></i>' +
                        '</div>' +
                    '</div>';

                const customDivIcon = L.divIcon({
                    className: 'custom-tactical-div-icon',
                    html: markerHtml,
                    iconSize: [36, 36],
                    iconAnchor: [18, 18],
                    popupAnchor: [0, -20]
                });

                const marker = L.marker([f.latitude, f.longitude], { icon: customDivIcon }).addTo(map);

                // Tooltip on hover
                marker.bindTooltip(
                    '<strong>' + (f.name || 'Facility') + '</strong><br>' +
                    '<span class="text-secondary">' + (f.facilityType || '') + ' • ' + (f.city || '') + '</span>',
                    { direction: 'top', offset: [0, -18], className: 'tactical-tooltip' }
                );

                // Popup content
                marker.on('click', function() {
                    openFacilityPopup(dto, marker);
                    fetchLiveWeather(f.latitude, f.longitude, f.name);
                });

                markersMap[f.id] = marker;
            });
        }

        function openFacilityPopup(dto, marker) {
            const f = dto.facility;
            const risk = dto.currentRisk || 'LOW';
            const reliability = f.reliabilityScore ? Math.round(f.reliabilityScore * 100) : 95;

            let riskBadge = 'bg-success text-white';
            if (risk === 'CRITICAL') riskBadge = 'bg-danger text-white';
            else if (risk === 'HIGH') riskBadge = 'bg-warning text-dark';
            else if (risk === 'MODERATE') riskBadge = 'bg-info text-dark';

            const popupContent = 
                '<div style="width: 280px;">' +
                    '<div class="popup-hud-header">' +
                        '<div class="d-flex justify-content-between align-items-center mb-1">' +
                            '<span class="badge bg-dark border border-secondary text-primary px-2" style="font-size: 0.65rem;">' +
                                '<i class="bi ' + getFacilityIcon(f.facilityType) + ' me-1"></i>' + (f.facilityType || 'NODE') +
                            '</span>' +
                            '<span class="badge ' + riskBadge + '" style="font-size: 0.65rem;">' + risk + ' RISK</span>' +
                        '</div>' +
                        '<div class="fw-bold text-light" style="font-size: 0.95rem;">' + (f.name || 'Facility') + '</div>' +
                        '<div class="text-secondary small" style="font-size: 0.72rem;"><i class="bi bi-geo-alt me-1"></i>' + (f.city || 'NCR') + ', ' + (f.state || 'Delhi') + '</div>' +
                    '</div>' +
                    '<div class="popup-hud-body">' +
                        '<div class="mb-2">' +
                            '<div class="d-flex justify-content-between text-secondary" style="font-size: 0.7rem;">' +
                                '<span>Reliability Rating</span>' +
                                '<span class="text-light fw-bold">' + reliability + '%</span>' +
                            '</div>' +
                            '<div class="progress mt-1" style="height: 5px; background: rgba(255,255,255,0.1);">' +
                                '<div class="progress-bar bg-primary" role="progressbar" style="width: ' + reliability + '%;"></div>' +
                            '</div>' +
                        '</div>' +
                        '<div class="d-flex justify-content-between text-secondary mb-3" style="font-size: 0.72rem;">' +
                            '<span>Coordinates:</span>' +
                            '<span class="text-light font-monospace">' + f.latitude.toFixed(4) + '°, ' + f.longitude.toFixed(4) + '°</span>' +
                        '</div>' +
                        '<div class="d-grid gap-2">' +
                            '<a href="' + contextPath + '/facilities/' + f.id + '" class="btn btn-sm btn-outline-primary" style="font-size: 0.75rem;">' +
                                '<i class="bi bi-speedometer2 me-1"></i> View Telemetry Dossier' +
                            '</a>' +
                            '<a href="' + contextPath + '/demand/dashboard?facilityId=' + f.id + '" class="btn btn-sm btn-primary" style="font-size: 0.75rem;">' +
                                '<i class="bi bi-graph-up-arrow me-1"></i> Demand & Risk Analysis' +
                            '</a>' +
                        '</div>' +
                    '</div>' +
                '</div>';

            marker.bindPopup(popupContent).openPopup();
        }

        function renderFacilitiesList(searchQuery = '') {
            const container = document.getElementById('facilitiesList');
            const query = (searchQuery || '').trim().toLowerCase();

            const filtered = allFacilitiesData.filter(dto => {
                const f = dto.facility;
                const risk = dto.currentRisk || 'LOW';

                if (activeTypeFilter !== 'ALL' && f.facilityType !== activeTypeFilter) return false;
                if (activeRiskFilter !== 'ALL' && risk !== activeRiskFilter) return false;

                if (query) {
                    const matchName = f.name && f.name.toLowerCase().includes(query);
                    const matchCity = f.city && f.city.toLowerCase().includes(query);
                    const matchType = f.facilityType && f.facilityType.toLowerCase().includes(query);
                    return matchName || matchCity || matchType;
                }
                return true;
            });

            document.getElementById('nodesCount').innerText = filtered.length;

            if (filtered.length === 0) {
                container.innerHTML = '<div class="text-secondary small text-center py-4">No matching tactical nodes found</div>';
                return;
            }

            let html = '';
            filtered.forEach(dto => {
                const f = dto.facility;
                const risk = dto.currentRisk || 'LOW';

                let dotColor = '#10b981';
                if (risk === 'CRITICAL') dotColor = '#ef4444';
                else if (risk === 'HIGH') dotColor = '#f59e0b';
                else if (risk === 'MODERATE') dotColor = '#06b6d4';

                html += 
                    '<div class="facility-card-hud" onclick="focusFacility(' + f.id + ')">' +
                        '<div class="d-flex justify-content-between align-items-center mb-1">' +
                            '<span class="badge bg-dark border border-secondary text-secondary" style="font-size: 0.65rem;">' +
                                '<i class="bi ' + getFacilityIcon(f.facilityType) + ' text-primary me-1"></i>' + (f.facilityType || 'NODE') +
                            '</span>' +
                            '<span class="d-flex align-items-center gap-1" style="font-size: 0.68rem; color: ' + dotColor + ';">' +
                                '<span style="width: 7px; height: 7px; border-radius: 50%; background-color: ' + dotColor + '; display: inline-block;"></span>' +
                                '<span>' + risk + '</span>' +
                            '</span>' +
                        '</div>' +
                        '<div class="text-light fw-bold small text-truncate">' + (f.name || 'Facility') + '</div>' +
                        '<div class="d-flex justify-content-between text-secondary" style="font-size: 0.68rem;">' +
                            '<span><i class="bi bi-geo-alt me-1"></i>' + (f.city || 'Delhi-NCR') + '</span>' +
                            '<span>' + (f.reliabilityScore ? Math.round(f.reliabilityScore * 100) : 95) + '% Rel.</span>' +
                        '</div>' +
                    '</div>';
            });

            container.innerHTML = html;
        }

        window.focusFacility = function(facilityId) {
            const dto = allFacilitiesData.find(d => d.facility.id === facilityId);
            if (!dto || !dto.facility.latitude) return;

            const lat = dto.facility.latitude;
            const lng = dto.facility.longitude;

            map.flyTo([lat, lng], 14, {
                animate: true,
                duration: 1.2
            });

            const marker = markersMap[facilityId];
            if (marker) {
                setTimeout(() => {
                    openFacilityPopup(dto, marker);
                    fetchLiveWeather(lat, lng, dto.facility.name);
                }, 1200);
            }

            // Highlight card
            document.querySelectorAll('.facility-card-hud').forEach(card => card.classList.remove('active'));
            // Trigger visual highlight on marker
            const el = document.getElementById('marker-f-' + facilityId);
            if (el) {
                el.style.transform = 'scale(1.3)';
                setTimeout(() => { el.style.transform = ''; }, 1500);
            }
        };

        function applyFilters() {
            renderFacilitiesList(document.getElementById('facilitySearch').value);

            // Filter map markers
            allFacilitiesData.forEach(dto => {
                const f = dto.facility;
                const risk = dto.currentRisk || 'LOW';
                const marker = markersMap[f.id];
                if (!marker) return;

                const matchType = (activeTypeFilter === 'ALL' || f.facilityType === activeTypeFilter);
                const matchRisk = (activeRiskFilter === 'ALL' || risk === activeRiskFilter);

                if (matchType && matchRisk) {
                    if (!map.hasLayer(marker)) map.addLayer(marker);
                } else {
                    if (map.hasLayer(marker)) map.removeLayer(marker);
                }
            });
        }

        // =====================================================================
        // Live Animated Supply Corridors
        // =====================================================================
        function renderCorridors() {
            corridorsGroup.clearLayers();
            if (!showCorridors) return;

            const facCoords = {};
            allFacilitiesData.forEach(d => {
                if (d.facility.latitude && d.facility.longitude) {
                    facCoords[d.facility.id] = [d.facility.latitude, d.facility.longitude];
                }
            });

            allRoutesData.forEach(tr => {
                const fromCoord = facCoords[tr.fromFacilityId];
                const toCoord = facCoords[tr.toFacilityId];

                if (!fromCoord || !toCoord) return;

                let corridorClass = 'corridor-accepted';
                if (tr.status === 'IN_TRANSIT') corridorClass = 'corridor-in-transit';
                else if (tr.status === 'SCHEDULED') corridorClass = 'corridor-scheduled';
                else if (tr.status === 'COMPLETED') corridorClass = 'corridor-accepted';
                else if (tr.status === 'REJECTED' || tr.status === 'CANCELLED') return; // Do not draw cancelled

                const polyline = L.polyline([fromCoord, toCoord], {
                    className: corridorClass,
                    weight: (tr.status === 'IN_TRANSIT') ? 3.5 : 2.5
                }).addTo(corridorsGroup);

                // Corridor Tooltip
                const tooltipHtml = 
                    '<div class="p-1">' +
                        '<div class="fw-bold text-primary" style="font-size: 0.72rem;">TRANSFER #' + tr.id + ' • ' + tr.status + '</div>' +
                        '<div class="text-light small">' + (tr.fromFacilityName || 'Origin') + ' ➔ ' + (tr.toFacilityName || 'Destination') + '</div>' +
                        '<div class="text-secondary" style="font-size: 0.68rem;">Cargo: ' + tr.requestedQuantity + 'x ' + (tr.medicineName || 'Supplies') + ' | ' + (tr.distanceKm || '15') + ' km</div>' +
                    '</div>';

                polyline.bindTooltip(tooltipHtml, { sticky: true, className: 'tactical-tooltip' });
                
                polyline.on('click', function() {
                    window.location.href = contextPath + '/transfers/' + tr.id;
                });
            });
        }

        window.toggleCorridors = function() {
            showCorridors = !showCorridors;
            const btn = document.getElementById('btnToggleRoutes');
            btn.classList.toggle('active', showCorridors);
            if (showCorridors) {
                renderCorridors();
            } else {
                corridorsGroup.clearLayers();
            }
        };

        // =====================================================================
        // 50km Emergency Donor Matching Buffer Circle
        // =====================================================================
        window.toggleMatchBuffer = function() {
            showBuffer = !showBuffer;
            const btn = document.getElementById('btnToggleBuffer');
            btn.classList.toggle('active', showBuffer);

            if (showBuffer) {
                // Find AIIMS Apex Central Hospital (facility 1)
                const aiims = allFacilitiesData.find(d => d.facility.id === 1);
                const center = aiims ? [aiims.facility.latitude, aiims.facility.longitude] : [28.5672, 77.2100];

                bufferCircle = L.circle(center, {
                    radius: 50000, // 50 km in meters
                    color: '#38bdf8',
                    fillColor: '#0284c7',
                    fillOpacity: 0.08,
                    weight: 2,
                    dashArray: '6, 6'
                }).addTo(map);

                bufferCircle.bindTooltip("AIIMS 50 km Smart Matching Zone (All Donors Eligible)", { sticky: true });
                map.fitBounds(bufferCircle.getBounds(), { padding: [30, 30] });
            } else {
                if (bufferCircle) {
                    map.removeLayer(bufferCircle);
                    bufferCircle = null;
                }
            }
        };

        window.recenterNetwork = function() {
            const bounds = [];
            allFacilitiesData.forEach(d => {
                if (d.facility.latitude && d.facility.longitude) {
                    bounds.push([d.facility.latitude, d.facility.longitude]);
                }
            });

            if (bounds.length > 0) {
                map.fitBounds(bounds, { padding: [40, 40] });
            } else {
                map.setView([28.5355, 77.2600], 11);
            }
        };

        // =====================================================================
        // Live Weather Telemetry Context
        // =====================================================================
        function fetchLiveWeather(lat, lng, facilityName) {
            const hud = document.getElementById('weatherHUD');
            hud.style.display = 'flex';

            fetch(contextPath + '/api/location/weather?lat=' + lat + '&lng=' + lng)
                .then(res => res.json())
                .then(w => {
                    document.getElementById('hudTemp').innerText = Math.round(w.temperature) + ' °C';
                    document.getElementById('hudWeatherCondition').innerText = (w.weatherCondition || 'Clear Sky').toUpperCase();
                    document.getElementById('hudWind').innerText = Math.round(w.windSpeed || 12) + ' km/h';

                    const droneBadge = document.getElementById('hudDroneBadge');
                    if (w.isViableForDrone) {
                        droneBadge.className = 'badge bg-success';
                        droneBadge.innerHTML = '<i class="bi bi-airplane-engines me-1"></i>DRONE VIABLE';
                    } else {
                        droneBadge.className = 'badge bg-warning text-dark';
                        droneBadge.innerHTML = '<i class="bi bi-exclamation-triangle me-1"></i>WIND ADVISORY';
                    }
                })
                .catch(() => {
                    // Fallback simulated telemetry if external weather API throttles
                    document.getElementById('hudTemp').innerText = '28 °C';
                    document.getElementById('hudWeatherCondition').innerText = 'CLEAR SKY';
                    document.getElementById('hudWind').innerText = '14 km/h';
                });
        }

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', initTacticalMap);
        } else {
            initTacticalMap();
        }
    })();
</script>
