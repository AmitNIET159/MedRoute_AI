# MedRoute AI - REST Endpoint Map

This document outlines the REST endpoints and web routes for the MedRoute AI application. The application is built using Spring MVC (non-Boot) running on Tomcat.

**Base Context Path:** `/MedRouteAI/`

All endpoints listed below are relative to this context path.

---

## Architecture Conventions

### Interceptors & Security
The application utilizes Spring MVC Interceptors for cross-cutting concerns:
- **`SessionInterceptor`**: Validates active user sessions for all secure endpoints.
- **`AuthorizationInterceptor`**: Enforces role-based access control (RBAC) on annotated methods or controllers.

### URL Pattern Conventions
- Web views (returning JSP/HTML) use clean paths: `/dashboard`, `/inventory/add`.
- REST APIs (returning JSON) are prefixed with `/api/`: `/api/dashboard/stats`, `/api/inventory/search`.
- Path variables are used for resource identification: `/inventory/{id}`.

### Request/Response Formats
- **View Endpoints**: Return a `ModelAndView` or `String` resolving to a JSP page. Use standard HTTP form-urlencoded requests.
- **API Endpoints**: Return `@ResponseBody` or `ResponseEntity<T>` producing `application/json`. Consume JSON or query parameters.

### CSRF Protection
All state-changing endpoints (POST, PUT, DELETE) require a CSRF token. 
- For web forms, it's included as a hidden field.
- For AJAX API requests, it must be included in the `X-CSRF-TOKEN` HTTP header.

### Error Response Format
API errors return a standardized JSON envelope:
```json
{
  "timestamp": "2026-08-16T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for field 'quantity'",
  "path": "/api/inventory/add"
}
```

### Pagination & Sorting
List APIs utilize query parameters for pagination:
- `page`: 0-indexed page number (default: 0)
- `size`: Number of records per page (default: 20)
- `sort`: Field to sort by (e.g., `createdAt,desc`)

### API Versioning
Currently, all APIs are considered v1. Future iterations will introduce versioning in the API path (e.g., `/api/v2/...`).

---

## Controller Endpoints

### 1. AuthController (`/auth`)
Manages user authentication and registration.
- **Interceptors**: None (Public access)
- **Roles**: All (Anonymous)

| HTTP Method | URL Pattern | Controller Method | Auth | Roles | Request Params/Body | Response | Description |
|---|---|---|---|---|---|---|---|
| GET | `/auth/login` | `showLogin` | No | Any | None | JSP View | Show login page |
| POST | `/auth/login` | `processLogin` | No | Any | `email`, `password` | Redirect/JSP | Process user login |
| GET | `/auth/register` | `showRegister` | No | Any | None | JSP View | Show registration page |
| POST | `/auth/register` | `processRegister` | No | Any | Registration Form | Redirect/JSP | Process new facility/user registration |
| POST | `/auth/verify-otp` | `verifyOtp` | No | Any | `email`, `otp` | JSON | Verify MFA/email OTP |
| POST | `/auth/resend-otp` | `resendOtp` | No | Any | `email` | JSON | Resend OTP code |
| POST | `/auth/forgot-password` | `forgotPassword` | No | Any | `email` | JSON | Initiate password reset process |
| POST | `/auth/reset-password` | `resetPassword` | No | Any | `token`, `newPassword` | JSON | Complete password reset |
| GET | `/auth/logout` | `logout` | Yes | Any | None | Redirect | Terminate session |

### 2. DashboardController (`/dashboard`)
Serves main application dashboards based on user roles.
- **Interceptors**: `SessionInterceptor`, `AuthorizationInterceptor`
- **Roles**: Dependent on specific endpoint

| HTTP Method | URL Pattern | Controller Method | Auth | Roles | Request Params/Body | Response | Description |
|---|---|---|---|---|---|---|---|
| GET | `/dashboard` | `routeDashboard` | Yes | Any | None | Redirect | Redirects to role-specific dashboard |
| GET | `/dashboard/admin` | `adminDashboard` | Yes | ADMIN | None | JSP View | Show admin dashboard |
| GET | `/dashboard/facility` | `facilityDashboard` | Yes | FACILITY | None | JSP View | Show facility dashboard |
| GET | `/api/dashboard/stats` | `getDashboardStats` | Yes | ADMIN, FACILITY | None | JSON | Overall stats for charts |
| GET | `/api/dashboard/alerts` | `getAlerts` | Yes | ADMIN, FACILITY | None | JSON | User-specific notifications |

### 3. InventoryController (`/inventory`)
Manages facility drug inventory and batch tracking.
- **Interceptors**: `SessionInterceptor`, `AuthorizationInterceptor`
- **Roles**: FACILITY, ADMIN

| HTTP Method | URL Pattern | Controller Method | Auth | Roles | Request Params/Body | Response | Description |
|---|---|---|---|---|---|---|---|
| GET | `/inventory` | `listInventory` | Yes | FACILITY | `page`, `size`, `filters` | JSP View | Paginated list of inventory |
| GET | `/inventory/add` | `showAddBatch` | Yes | FACILITY | None | JSP View | Form to add new inventory batch |
| POST | `/inventory/add` | `saveBatch` | Yes | FACILITY | Batch Form Data | Redirect/JSP | Save new batch to inventory |
| GET | `/inventory/{id}` | `batchDetails` | Yes | FACILITY | Path: `id` | JSP View | View specific batch details |
| POST | `/inventory/{id}/update` | `updateBatch` | Yes | FACILITY | Path: `id`, Form Data | Redirect/JSP | Update existing batch details |
| POST | `/inventory/{id}/dispense` | `dispense` | Yes | FACILITY | Path: `id`, `quantity` | Redirect/JSP | Record dispensing from batch |
| GET | `/inventory/expiring` | `expiringStock` | Yes | FACILITY | None | JSP View | View stock nearing expiry |
| GET | `/inventory/low-stock` | `lowStock` | Yes | FACILITY | None | JSP View | View items below par levels |
| GET | `/api/inventory/search` | `searchMedicines` | Yes | FACILITY | `q` (query) | JSON | AJAX autocomplete for medicines |
| GET | `/api/inventory/stats` | `getInventoryStats` | Yes | FACILITY | None | JSON | Inventory metrics for charts |
| GET | `/api/inventory/consumption/{medicineId}` | `consumptionData` | Yes | FACILITY | Path: `medicineId` | JSON | Historical consumption data |

### 4. RequestController (`/requests`)
Handles creation and tracking of emergency shortage requests.
- **Interceptors**: `SessionInterceptor`, `AuthorizationInterceptor`
- **Roles**: FACILITY, ADMIN

| HTTP Method | URL Pattern | Controller Method | Auth | Roles | Request Params/Body | Response | Description |
|---|---|---|---|---|---|---|---|
| GET | `/requests` | `listRequests` | Yes | FACILITY, ADMIN | `page`, `size`, `status` | JSP View | List emergency requests |
| GET | `/requests/create` | `showCreateRequest` | Yes | FACILITY | None | JSP View | Form to create a shortage request |
| POST | `/requests/create` | `submitRequest` | Yes | FACILITY | Request Form Data | Redirect/JSP | Submit new emergency request |
| GET | `/requests/{id}` | `requestDetails` | Yes | FACILITY, ADMIN | Path: `id` | JSP View | View details of a request |
| POST | `/requests/{id}/cancel` | `cancelRequest` | Yes | FACILITY | Path: `id`, `reason` | Redirect/JSP | Cancel an active request |
| GET | `/api/requests/matches/{id}` | `findMatches` | Yes | FACILITY | Path: `id` | JSON | Get AI-matched facilities with surplus |

### 5. TransferController (`/transfers`)
Manages the lifecycle of inter-facility drug transfers.
- **Interceptors**: `SessionInterceptor`, `AuthorizationInterceptor`
- **Roles**: FACILITY, ADMIN

| HTTP Method | URL Pattern | Controller Method | Auth | Roles | Request Params/Body | Response | Description |
|---|---|---|---|---|---|---|---|
| GET | `/transfers` | `listTransfers` | Yes | FACILITY, ADMIN | `page`, `status` | JSP View | List active and past transfers |
| GET | `/transfers/{id}` | `transferDetails` | Yes | FACILITY, ADMIN | Path: `id` | JSP View | Detailed view of a transfer |
| POST | `/transfers/initiate` | `initiateTransfer` | Yes | FACILITY | `requestId`, `providerId`, `quantity` | Redirect | Initiate transfer from a match |
| POST | `/transfers/{id}/accept` | `acceptTransfer` | Yes | FACILITY | Path: `id` | Redirect | Provider accepts transfer request |
| POST | `/transfers/{id}/reject` | `rejectTransfer` | Yes | FACILITY | Path: `id`, `reason` | Redirect | Provider rejects transfer request |
| POST | `/transfers/{id}/schedule` | `scheduleTransfer` | Yes | FACILITY | Path: `id`, `pickupTime` | Redirect | Schedule logistics for transfer |
| POST | `/transfers/{id}/dispatch` | `dispatchTransfer` | Yes | FACILITY | Path: `id`, `courierDetails`| Redirect | Mark transfer as in-transit |
| POST | `/transfers/{id}/receive` | `receiveTransfer` | Yes | FACILITY | Path: `id` | Redirect | Requester confirms receipt |
| POST | `/transfers/{id}/complete` | `completeTransfer` | Yes | FACILITY | Path: `id` | Redirect | Finalize transfer (moves to inventory) |
| POST | `/transfers/{id}/cancel` | `cancelTransfer` | Yes | FACILITY, ADMIN | Path: `id`, `reason` | Redirect | Cancel pending transfer |

### 6. FacilityController (`/facility`)
Manages facility profiles and geographic data.
- **Interceptors**: `SessionInterceptor`, `AuthorizationInterceptor`
- **Roles**: FACILITY, ADMIN

| HTTP Method | URL Pattern | Controller Method | Auth | Roles | Request Params/Body | Response | Description |
|---|---|---|---|---|---|---|---|
| GET | `/facility/profile` | `viewProfile` | Yes | FACILITY | None | JSP View | View facility profile |
| POST | `/facility/profile/update` | `updateProfile` | Yes | FACILITY | Profile Form Data | Redirect/JSP | Update facility information |
| GET | `/facility/map` | `mapView` | Yes | FACILITY, ADMIN | None | JSP View | Geographical map of network |
| GET | `/api/facility/nearby` | `getNearby` | Yes | FACILITY, ADMIN | `lat`, `lng`, `radius` | JSON | Fetch facilities within radius |
| GET | `/api/facility/search` | `searchFacilities` | Yes | FACILITY, ADMIN | `q` (query) | JSON | Search facilities by name |

### 7. AIController (`/ai`)
Exposes Gemini/Vertex AI capabilities for predictive insights.
- **Interceptors**: `SessionInterceptor`, `AuthorizationInterceptor`
- **Roles**: FACILITY, ADMIN

| HTTP Method | URL Pattern | Controller Method | Auth | Roles | Request Params/Body | Response | Description |
|---|---|---|---|---|---|---|---|
| GET | `/ai/insights` | `insightsDashboard` | Yes | FACILITY, ADMIN | None | JSP View | Main AI insights view |
| POST | `/api/ai/analyze-risk` | `analyzeRisk` | Yes | FACILITY | `facilityId`, `days` | JSON | AI analysis of inventory risk |
| POST | `/api/ai/explain-shortage` | `explainShortage` | Yes | FACILITY | `medicineId` | JSON | Generate LLM explanation for shortage |
| POST | `/api/ai/classify-request` | `classifyRequest` | Yes | FACILITY | `requestText` | JSON | NLP classification of emergency severity |
| POST | `/api/ai/logistics-recommendation`| `getLogistics` | Yes | FACILITY | `sourceId`, `destId` | JSON | Route/Courier optimization recommendation |
| GET | `/api/ai/usage-stats` | `usageStats` | Yes | ADMIN | None | JSON | AI token/API usage statistics |

### 8. AnalyticsController (`/analytics`)
Data visualization and reporting endpoints.
- **Interceptors**: `SessionInterceptor`, `AuthorizationInterceptor`
- **Roles**: ADMIN, FACILITY (limited)

| HTTP Method | URL Pattern | Controller Method | Auth | Roles | Request Params/Body | Response | Description |
|---|---|---|---|---|---|---|---|
| GET | `/analytics` | `analyticsDashboard`| Yes | ADMIN, FACILITY | None | JSP View | Analytics UI |
| GET | `/api/analytics/inventory-trends` | `inventoryTrends` | Yes | ADMIN, FACILITY | `timeframe` | JSON | Network-wide inventory trends |
| GET | `/api/analytics/transfer-metrics` | `transferMetrics` | Yes | ADMIN, FACILITY | `startDate`, `endDate` | JSON | Success rates, fulfillment times |
| GET | `/api/analytics/facility-performance`| `facilityPerf` | Yes | ADMIN | None | JSON | Facility response times and scores |
| GET | `/api/analytics/demand-patterns` | `demandPatterns` | Yes | ADMIN | None | JSON | Regional demand hotspots |
| GET | `/api/analytics/risk-heatmap` | `riskHeatmap` | Yes | ADMIN | None | JSON | GeoJSON for stockout risk heatmap |

### 9. AdminController (`/admin`)
System administration, configuration, and monitoring.
- **Interceptors**: `SessionInterceptor`, `AuthorizationInterceptor`
- **Roles**: ADMIN

| HTTP Method | URL Pattern | Controller Method | Auth | Roles | Request Params/Body | Response | Description |
|---|---|---|---|---|---|---|---|
| GET | `/admin` | `adminPanel` | Yes | ADMIN | None | JSP View | Main admin control panel |
| GET | `/admin/users` | `manageUsers` | Yes | ADMIN | `page`, `role` | JSP View | User management list |
| POST | `/admin/users/{id}/toggle-status`| `toggleUser` | Yes | ADMIN | Path: `id` | Redirect | Enable/disable user account |
| GET | `/admin/facilities` | `manageFacilities` | Yes | ADMIN | `page`, `status` | JSP View | Facility management list |
| POST | `/admin/facilities/{id}/toggle-status`| `toggleFacility`| Yes | ADMIN | Path: `id` | Redirect | Suspend/activate facility |
| GET | `/admin/audit-logs` | `viewLogs` | Yes | ADMIN | `page`, `user`, `action`| JSP View | View system audit trail |
| GET | `/admin/system-health` | `systemHealth` | Yes | ADMIN | None | JSP View | UI for system status |
| GET | `/admin/settings` | `viewSettings` | Yes | ADMIN | None | JSP View | View global system settings |
| POST | `/admin/settings` | `updateSettings` | Yes | ADMIN | Settings Form Data | Redirect/JSP | Save global configuration |
| GET | `/api/admin/api-health` | `apiHealthCheck` | Yes | ADMIN | None | JSON | System/DB/Integrations health check |
