/**
 * MedRoute AI — Global Application JavaScript
 * Version: 1.0
 *
 * Provides:
 * - CSRF token management
 * - AJAX/Fetch utilities
 * - Sidebar toggle
 * - Modal management
 * - Alert/Toast helpers
 * - Notification polling
 * - Form validation helpers
 * - Dropdown management
 * - Utility functions
 */

'use strict';

// ============================================
// NAMESPACE
// ============================================
const MedRoute = {
    config: {
        contextPath: '/MedRouteAI',
        csrfToken: null,
        csrfHeader: 'X-CSRF-TOKEN',
        notificationPollInterval: 60000, // 1 minute
        defaultPageSize: 20,
        dateFormat: 'en-IN',
    },

    // ============================================
    // INITIALIZATION
    // ============================================
    init() {
        this.csrf.init();
        this.sidebar.init();
        this.dropdown.init();
        this.modal.init();
        this.alerts.init();
        this.forms.init();
        document.addEventListener('DOMContentLoaded', () => {
            this.notifications.init();
        });
    },

    // ============================================
    // CSRF TOKEN MANAGEMENT
    // ============================================
    csrf: {
        init() {
            const meta = document.querySelector('meta[name="_csrf"]');
            const headerMeta = document.querySelector('meta[name="_csrf_header"]');
            if (meta) {
                MedRoute.config.csrfToken = meta.getAttribute('content');
            }
            if (headerMeta) {
                MedRoute.config.csrfHeader = headerMeta.getAttribute('content');
            }
        },

        getToken() {
            return MedRoute.config.csrfToken;
        },

        getHeader() {
            return MedRoute.config.csrfHeader;
        },

        /** Add CSRF token to a fetch headers object */
        applyHeaders(headers) {
            if (MedRoute.config.csrfToken) {
                headers[MedRoute.config.csrfHeader] = MedRoute.config.csrfToken;
            }
            return headers;
        }
    },

    // ============================================
    // FETCH / AJAX UTILITIES
    // ============================================
    api: {
        /**
         * Make a GET request returning JSON.
         * @param {string} url - Relative URL (appended to contextPath)
         * @param {object} params - Query parameters
         * @returns {Promise<object>}
         */
        async get(url, params = {}) {
            const queryString = new URLSearchParams(params).toString();
            const fullUrl = MedRoute.config.contextPath + url + (queryString ? '?' + queryString : '');

            const response = await fetch(fullUrl, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                },
                credentials: 'same-origin',
            });

            if (!response.ok) {
                throw await this._handleError(response);
            }
            return response.json();
        },

        /**
         * Make a POST request with JSON body.
         * @param {string} url - Relative URL
         * @param {object} data - Request body
         * @returns {Promise<object>}
         */
        async post(url, data = {}) {
            const fullUrl = MedRoute.config.contextPath + url;
            const headers = {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
            };
            MedRoute.csrf.applyHeaders(headers);

            const response = await fetch(fullUrl, {
                method: 'POST',
                headers,
                body: JSON.stringify(data),
                credentials: 'same-origin',
            });

            if (!response.ok) {
                throw await this._handleError(response);
            }
            return response.json();
        },

        /**
         * Make a POST request with form data.
         * @param {string} url - Relative URL
         * @param {FormData|HTMLFormElement} formData
         * @returns {Promise<object>}
         */
        async postForm(url, formData) {
            const fullUrl = MedRoute.config.contextPath + url;
            if (formData instanceof HTMLFormElement) {
                formData = new FormData(formData);
            }

            const headers = {};
            MedRoute.csrf.applyHeaders(headers);

            const response = await fetch(fullUrl, {
                method: 'POST',
                headers,
                body: formData,
                credentials: 'same-origin',
            });

            if (!response.ok) {
                throw await this._handleError(response);
            }
            return response.json();
        },

        /**
         * Handle non-OK responses.
         */
        async _handleError(response) {
            let error;
            try {
                error = await response.json();
            } catch {
                error = {
                    status: response.status,
                    message: response.statusText || 'An error occurred',
                };
            }
            error.httpStatus = response.status;

            // Session expired — redirect to login
            if (response.status === 401) {
                window.location.href = MedRoute.config.contextPath + '/auth/login?expired=true';
            }

            return error;
        }
    },

    // ============================================
    // SIDEBAR
    // ============================================
    sidebar: {
        init() {
            // Toggle button
            document.addEventListener('click', (e) => {
                const toggle = e.target.closest('.sidebar-toggle, .navbar-menu-toggle');
                if (toggle) {
                    this.toggle();
                }
            });

            // Click overlay to close on mobile
            document.addEventListener('click', (e) => {
                if (document.body.classList.contains('sidebar-open') &&
                    !e.target.closest('.app-sidebar') &&
                    !e.target.closest('.navbar-menu-toggle')) {
                    this.close();
                }
            });

            // Restore collapsed state
            const collapsed = localStorage.getItem('medroute_sidebar_collapsed');
            if (collapsed === 'true') {
                document.body.classList.add('sidebar-collapsed');
            }
        },

        toggle() {
            const body = document.body;
            const isMobile = window.innerWidth <= 1024;

            if (isMobile) {
                body.classList.toggle('sidebar-open');
            } else {
                body.classList.toggle('sidebar-collapsed');
                localStorage.setItem(
                    'medroute_sidebar_collapsed',
                    body.classList.contains('sidebar-collapsed')
                );
            }
        },

        close() {
            document.body.classList.remove('sidebar-open');
        }
    },

    // ============================================
    // DROPDOWNS
    // ============================================
    dropdown: {
        init() {
            document.addEventListener('click', (e) => {
                const trigger = e.target.closest('[data-dropdown]');

                if (trigger) {
                    e.stopPropagation();
                    const menuId = trigger.getAttribute('data-dropdown');
                    const menu = document.getElementById(menuId);
                    if (menu) {
                        // Close all other dropdowns
                        document.querySelectorAll('.dropdown-menu.show').forEach(m => {
                            if (m !== menu) m.classList.remove('show');
                        });
                        menu.classList.toggle('show');
                    }
                } else {
                    // Close all dropdowns on outside click
                    document.querySelectorAll('.dropdown-menu.show').forEach(m => {
                        m.classList.remove('show');
                    });
                }
            });
        }
    },

    // ============================================
    // MODALS
    // ============================================
    modal: {
        _activeModal: null,

        init() {
            // Close on backdrop click
            document.addEventListener('click', (e) => {
                if (e.target.classList.contains('modal-backdrop') && e.target.classList.contains('active')) {
                    this.close();
                }
            });

            // Close on Escape
            document.addEventListener('keydown', (e) => {
                if (e.key === 'Escape' && this._activeModal) {
                    this.close();
                }
            });

            // Close buttons
            document.addEventListener('click', (e) => {
                if (e.target.closest('.modal-close') || e.target.closest('[data-modal-close]')) {
                    this.close();
                }
            });

            // Open triggers
            document.addEventListener('click', (e) => {
                const trigger = e.target.closest('[data-modal-open]');
                if (trigger) {
                    const modalId = trigger.getAttribute('data-modal-open');
                    this.open(modalId);
                }
            });
        },

        /**
         * Open a modal by ID.
         * @param {string} modalId
         */
        open(modalId) {
            const modal = document.getElementById(modalId);
            const backdrop = document.getElementById(modalId + '-backdrop') ||
                             document.querySelector('.modal-backdrop');

            if (modal) {
                if (backdrop) backdrop.classList.add('active');
                modal.classList.add('active');
                this._activeModal = modal;
                document.body.style.overflow = 'hidden';

                // Focus first focusable element
                const focusable = modal.querySelector('input, select, textarea, button:not(.modal-close)');
                if (focusable) focusable.focus();
            }
        },

        /**
         * Close the active modal.
         */
        close() {
            if (this._activeModal) {
                const backdrop = document.querySelector('.modal-backdrop.active');
                if (backdrop) backdrop.classList.remove('active');
                this._activeModal.classList.remove('active');
                this._activeModal = null;
                document.body.style.overflow = '';
            }
        },

        /**
         * Show a confirmation dialog.
         * @param {object} options
         * @param {string} options.title
         * @param {string} options.message
         * @param {string} options.confirmText
         * @param {string} options.cancelText
         * @param {string} options.type - 'danger', 'warning', 'success'
         * @returns {Promise<boolean>}
         */
        confirm({ title = 'Confirm', message = 'Are you sure?', confirmText = 'Confirm', cancelText = 'Cancel', type = 'warning' }) {
            return new Promise((resolve) => {
                // Create or reuse confirm modal
                let modal = document.getElementById('confirmModal');
                let backdrop = document.getElementById('confirmModal-backdrop');

                if (!modal) {
                    backdrop = document.createElement('div');
                    backdrop.id = 'confirmModal-backdrop';
                    backdrop.className = 'modal-backdrop';
                    document.body.appendChild(backdrop);

                    modal = document.createElement('div');
                    modal.id = 'confirmModal';
                    modal.className = 'modal modal-sm';
                    modal.innerHTML = `
                        <div class="modal-body" style="padding-top:var(--space-8);text-align:center;">
                            <div class="modal-confirm-icon confirm-${type}" id="confirmIcon">
                                <i class="bi bi-exclamation-triangle"></i>
                            </div>
                            <h5 id="confirmTitle" class="mb-2">${title}</h5>
                            <p id="confirmMessage" class="text-secondary">${message}</p>
                        </div>
                        <div class="modal-footer">
                            <button class="btn btn-secondary" id="confirmCancel">${cancelText}</button>
                            <button class="btn btn-${type === 'danger' ? 'danger' : 'primary'}" id="confirmOk">${confirmText}</button>
                        </div>
                    `;
                    document.body.appendChild(modal);
                } else {
                    document.getElementById('confirmTitle').textContent = title;
                    document.getElementById('confirmMessage').textContent = message;
                    document.getElementById('confirmOk').textContent = confirmText;
                    document.getElementById('confirmCancel').textContent = cancelText;
                    const icon = document.getElementById('confirmIcon');
                    icon.className = `modal-confirm-icon confirm-${type}`;
                }

                backdrop.classList.add('active');
                modal.classList.add('active');
                document.body.style.overflow = 'hidden';

                const cleanup = (result) => {
                    backdrop.classList.remove('active');
                    modal.classList.remove('active');
                    document.body.style.overflow = '';
                    resolve(result);
                };

                document.getElementById('confirmOk').onclick = () => cleanup(true);
                document.getElementById('confirmCancel').onclick = () => cleanup(false);
                backdrop.onclick = () => cleanup(false);
            });
        }
    },

    // ============================================
    // ALERTS / TOASTS
    // ============================================
    alerts: {
        _container: null,

        init() {
            // Create toast container
            this._container = document.createElement('div');
            this._container.id = 'toastContainer';
            this._container.style.cssText = `
                position: fixed;
                top: calc(var(--navbar-height, 64px) + 16px);
                right: 16px;
                z-index: 1080;
                display: flex;
                flex-direction: column;
                gap: 8px;
                max-width: 400px;
            `;
            document.body.appendChild(this._container);

            // Dismiss inline alerts
            document.addEventListener('click', (e) => {
                const close = e.target.closest('.alert-close');
                if (close) {
                    const alert = close.closest('.alert');
                    if (alert) {
                        alert.style.opacity = '0';
                        alert.style.transform = 'translateY(-10px)';
                        setTimeout(() => alert.remove(), 200);
                    }
                }
            });
        },

        /**
         * Show a toast notification.
         * @param {string} message
         * @param {string} type - 'success', 'danger', 'warning', 'info'
         * @param {number} duration - ms
         */
        show(message, type = 'info', duration = 5000) {
            const icons = {
                success: 'bi-check-circle-fill',
                danger: 'bi-exclamation-circle-fill',
                warning: 'bi-exclamation-triangle-fill',
                info: 'bi-info-circle-fill',
            };

            const toast = document.createElement('div');
            toast.className = `alert alert-${type} alert-dismissible`;
            toast.style.cssText = `
                animation: toastSlideIn 0.3s ease;
                box-shadow: var(--shadow-lg);
                margin: 0;
            `;
            toast.innerHTML = `
                <i class="alert-icon bi ${icons[type] || icons.info}"></i>
                <div class="alert-content">
                    <div class="alert-message">${message}</div>
                </div>
                <button class="alert-close" aria-label="Close">&times;</button>
            `;

            this._container.appendChild(toast);

            // Auto-dismiss
            if (duration > 0) {
                setTimeout(() => {
                    toast.style.opacity = '0';
                    toast.style.transform = 'translateX(100%)';
                    toast.style.transition = 'all 0.3s ease';
                    setTimeout(() => toast.remove(), 300);
                }, duration);
            }
        },

        success(message, duration) { this.show(message, 'success', duration); },
        error(message, duration)   { this.show(message, 'danger', duration); },
        warning(message, duration) { this.show(message, 'warning', duration); },
        info(message, duration)    { this.show(message, 'info', duration); },
    },

    // ============================================
    // NOTIFICATIONS
    // ============================================
    notifications: {
        _pollTimer: null,

        init() {
            // Only poll if user is authenticated (check for session marker)
            if (document.body.dataset.authenticated === 'true') {
                this.poll();
                this._pollTimer = setInterval(
                    () => this.poll(),
                    MedRoute.config.notificationPollInterval
                );
            }
        },

        async poll() {
            try {
                const data = await MedRoute.api.get('/api/dashboard/alerts');
                this.updateBadge(data.unreadCount || 0);
            } catch {
                // Silently fail — non-critical
            }
        },

        updateBadge(count) {
            const dot = document.querySelector('.notification-dot');
            const badge = document.querySelector('.notification-count');

            if (dot) {
                dot.style.display = count > 0 ? 'block' : 'none';
            }
            if (badge) {
                badge.textContent = count > 99 ? '99+' : count;
                badge.style.display = count > 0 ? 'inline-flex' : 'none';
            }
        },

        destroy() {
            if (this._pollTimer) {
                clearInterval(this._pollTimer);
            }
        }
    },

    // ============================================
    // FORM HELPERS
    // ============================================
    forms: {
        init() {
            // Real-time validation on blur
            document.addEventListener('blur', (e) => {
                if (e.target.matches('.form-control[required], .form-select[required]')) {
                    this.validateField(e.target);
                }
            }, true);

            // Clear validation on input
            document.addEventListener('input', (e) => {
                if (e.target.matches('.form-control.is-invalid, .form-select.is-invalid')) {
                    this.clearFieldError(e.target);
                }
            });
        },

        /**
         * Validate a single form field.
         * @param {HTMLElement} field
         * @returns {boolean}
         */
        validateField(field) {
            const value = field.value.trim();

            if (field.required && !value) {
                this.setFieldError(field, 'This field is required');
                return false;
            }

            if (field.type === 'email' && value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
                this.setFieldError(field, 'Please enter a valid email address');
                return false;
            }

            if (field.minLength > 0 && value.length < field.minLength) {
                this.setFieldError(field, `Minimum ${field.minLength} characters required`);
                return false;
            }

            if (field.pattern && !new RegExp(field.pattern).test(value)) {
                this.setFieldError(field, field.title || 'Invalid format');
                return false;
            }

            this.setFieldValid(field);
            return true;
        },

        /**
         * Validate entire form.
         * @param {HTMLFormElement} form
         * @returns {boolean}
         */
        validateForm(form) {
            const fields = form.querySelectorAll('.form-control[required], .form-select[required]');
            let valid = true;
            fields.forEach(field => {
                if (!this.validateField(field)) {
                    valid = false;
                }
            });
            return valid;
        },

        setFieldError(field, message) {
            field.classList.remove('is-valid');
            field.classList.add('is-invalid');
            let feedback = field.parentElement.querySelector('.form-feedback-invalid');
            if (!feedback) {
                feedback = document.createElement('div');
                feedback.className = 'form-feedback form-feedback-invalid';
                field.parentElement.appendChild(feedback);
            }
            feedback.textContent = message;
        },

        setFieldValid(field) {
            field.classList.remove('is-invalid');
            field.classList.add('is-valid');
            const feedback = field.parentElement.querySelector('.form-feedback-invalid');
            if (feedback) feedback.remove();
        },

        clearFieldError(field) {
            field.classList.remove('is-invalid', 'is-valid');
            const feedback = field.parentElement.querySelector('.form-feedback-invalid');
            if (feedback) feedback.remove();
        },

        /**
         * Set a button to loading state.
         * @param {HTMLButtonElement} btn
         * @param {string} text - Loading text
         */
        setButtonLoading(btn, text = 'Loading...') {
            btn.dataset.originalText = btn.innerHTML;
            btn.classList.add('btn-loading');
            btn.disabled = true;
            btn.setAttribute('aria-busy', 'true');
        },

        /**
         * Reset a button from loading state.
         * @param {HTMLButtonElement} btn
         */
        resetButton(btn) {
            btn.innerHTML = btn.dataset.originalText || btn.innerHTML;
            btn.classList.remove('btn-loading');
            btn.disabled = false;
            btn.removeAttribute('aria-busy');
        }
    },

    // ============================================
    // TAB MANAGEMENT
    // ============================================
    tabs: {
        /**
         * Initialize tab navigation.
         * @param {string} containerSelector
         */
        init(containerSelector) {
            const container = document.querySelector(containerSelector);
            if (!container) return;

            container.querySelectorAll('.tab-item').forEach(tab => {
                tab.addEventListener('click', () => {
                    const target = tab.dataset.tab;
                    // Update tabs
                    container.querySelectorAll('.tab-item').forEach(t => t.classList.remove('active'));
                    tab.classList.add('active');
                    // Update panes
                    const paneContainer = container.closest('.card, .tab-container') || container.parentElement;
                    paneContainer.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
                    const pane = paneContainer.querySelector(`#${target}`);
                    if (pane) pane.classList.add('active');
                });
            });
        }
    },

    // ============================================
    // UTILITY FUNCTIONS
    // ============================================
    util: {
        /**
         * Format a date string.
         * @param {string} dateStr
         * @param {object} options - Intl.DateTimeFormat options
         * @returns {string}
         */
        formatDate(dateStr, options = {}) {
            if (!dateStr) return '-';
            const defaults = { year: 'numeric', month: 'short', day: 'numeric' };
            return new Intl.DateTimeFormat(MedRoute.config.dateFormat, { ...defaults, ...options })
                .format(new Date(dateStr));
        },

        /**
         * Format a number with commas.
         * @param {number} num
         * @returns {string}
         */
        formatNumber(num) {
            if (num == null) return '-';
            return new Intl.NumberFormat(MedRoute.config.dateFormat).format(num);
        },

        /**
         * Relative time string (e.g., "2 hours ago").
         * @param {string} dateStr
         * @returns {string}
         */
        timeAgo(dateStr) {
            const now = new Date();
            const then = new Date(dateStr);
            const seconds = Math.floor((now - then) / 1000);

            const intervals = [
                { label: 'year', seconds: 31536000 },
                { label: 'month', seconds: 2592000 },
                { label: 'day', seconds: 86400 },
                { label: 'hour', seconds: 3600 },
                { label: 'minute', seconds: 60 },
            ];

            for (const interval of intervals) {
                const count = Math.floor(seconds / interval.seconds);
                if (count >= 1) {
                    return `${count} ${interval.label}${count > 1 ? 's' : ''} ago`;
                }
            }
            return 'Just now';
        },

        /**
         * Debounce a function.
         * @param {Function} fn
         * @param {number} delay
         * @returns {Function}
         */
        debounce(fn, delay = 300) {
            let timer;
            return function (...args) {
                clearTimeout(timer);
                timer = setTimeout(() => fn.apply(this, args), delay);
            };
        },

        /**
         * Generate initials from a name.
         * @param {string} name
         * @returns {string}
         */
        getInitials(name) {
            if (!name) return '?';
            return name.split(' ')
                .filter(Boolean)
                .map(w => w[0].toUpperCase())
                .slice(0, 2)
                .join('');
        },

        /**
         * Copy text to clipboard.
         * @param {string} text
         */
        async copyToClipboard(text) {
            try {
                await navigator.clipboard.writeText(text);
                MedRoute.alerts.success('Copied to clipboard');
            } catch {
                MedRoute.alerts.error('Failed to copy');
            }
        },

        /**
         * Get risk level class and label from score.
         * @param {number} score
         * @returns {{ level: string, label: string, class: string }}
         */
        getRiskLevel(score) {
            if (score <= 30) return { level: 'low', label: 'Low', class: 'badge-risk-low' };
            if (score <= 60) return { level: 'moderate', label: 'Moderate', class: 'badge-risk-moderate' };
            if (score <= 80) return { level: 'high', label: 'High', class: 'badge-risk-high' };
            return { level: 'critical', label: 'Critical', class: 'badge-risk-critical' };
        }
    }
};

// Toast animation
const toastStyle = document.createElement('style');
toastStyle.textContent = `
    @keyframes toastSlideIn {
        from { opacity: 0; transform: translateX(100%); }
        to   { opacity: 1; transform: translateX(0); }
    }
`;
document.head.appendChild(toastStyle);

// Initialize
MedRoute.init();
