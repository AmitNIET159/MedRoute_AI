-- ============================================
-- MedRoute AI - Database Schema
-- Version: 1.0
-- Database: medroute_ai (MySQL 8.x)
-- ============================================

CREATE DATABASE IF NOT EXISTS medroute_ai
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE medroute_ai;

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE facilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    facility_type ENUM('HOSPITAL','CLINIC','PHARMACY','NGO','WAREHOUSE') NOT NULL,
    registration_number VARCHAR(100),
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    is_active BOOLEAN DEFAULT TRUE,
    reliability_score DECIMAL(3,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    role ENUM('ADMIN','HOSPITAL','CLINIC','PHARMACY','NGO') NOT NULL,
    facility_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (facility_id) REFERENCES facilities(id) ON DELETE SET NULL
);

CREATE TABLE medicine_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE medicine_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    generic_name VARCHAR(255),
    category_id BIGINT NOT NULL,
    unit VARCHAR(50) NOT NULL,
    description TEXT,
    requires_cold_chain BOOLEAN DEFAULT FALSE,
    is_controlled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES medicine_categories(id)
);

CREATE TABLE inventory_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    facility_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    batch_number VARCHAR(100) NOT NULL,
    quantity INT NOT NULL CHECK(quantity >= 0),
    reserved_quantity INT DEFAULT 0,
    minimum_stock INT DEFAULT 0,
    target_stock INT DEFAULT 0,
    unit_price DECIMAL(10,2),
    manufacture_date DATE,
    expiry_date DATE NOT NULL,
    received_date DATE,
    supplier VARCHAR(255),
    status ENUM('ACTIVE','DEPLETED','EXPIRED','RECALLED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (facility_id) REFERENCES facilities(id),
    FOREIGN KEY (medicine_id) REFERENCES medicine_catalog(id)
);

CREATE TABLE inventory_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    facility_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    transaction_type ENUM('RECEIVED','DISPENSED','TRANSFERRED_OUT','TRANSFERRED_IN','ADJUSTMENT','EXPIRED','RETURNED','STOCK_IN','CONSUMPTION') NOT NULL,
    quantity INT NOT NULL,
    reference_id VARCHAR(100),
    reference_type VARCHAR(100),
    notes TEXT,
    performed_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (batch_id) REFERENCES inventory_batches(id),
    FOREIGN KEY (facility_id) REFERENCES facilities(id),
    FOREIGN KEY (medicine_id) REFERENCES medicine_catalog(id),
    FOREIGN KEY (performed_by) REFERENCES users(id)
);

CREATE TABLE stock_consumption (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    facility_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK(quantity >= 0),
    department VARCHAR(255),
    consumed_by VARCHAR(255),
    recorded_by BIGINT,
    reference_id VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (facility_id) REFERENCES facilities(id),
    FOREIGN KEY (medicine_id) REFERENCES medicine_catalog(id),
    FOREIGN KEY (recorded_by) REFERENCES users(id)
);

CREATE TABLE emergency_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requesting_facility_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    quantity_needed INT NOT NULL CHECK(quantity_needed > 0),
    quantity_fulfilled INT DEFAULT 0,
    urgency ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
    required_by_date DATE NOT NULL,
    reason TEXT,
    status ENUM('OPEN','PARTIALLY_FULFILLED','FULFILLED','EXPIRED','CANCELLED') NOT NULL DEFAULT 'OPEN',
    ai_priority_score DECIMAL(5,2),
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (requesting_facility_id) REFERENCES facilities(id),
    FOREIGN KEY (medicine_id) REFERENCES medicine_catalog(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE transfer_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_facility_id BIGINT NOT NULL,
    to_facility_id BIGINT NOT NULL,
    emergency_request_id BIGINT,
    requested_quantity INT NOT NULL DEFAULT 0,
    status ENUM('REQUESTED','OFFERED','ACCEPTED','SCHEDULED','IN_TRANSIT','RECEIVED','COMPLETED','REJECTED','CANCELLED','EXPIRED') NOT NULL DEFAULT 'REQUESTED',
    match_score DECIMAL(5,2),
    distance_km DECIMAL(8,2),
    notes TEXT,
    requested_by BIGINT,
    approved_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (from_facility_id) REFERENCES facilities(id),
    FOREIGN KEY (to_facility_id) REFERENCES facilities(id),
    FOREIGN KEY (emergency_request_id) REFERENCES emergency_requests(id),
    FOREIGN KEY (requested_by) REFERENCES users(id),
    FOREIGN KEY (approved_by) REFERENCES users(id)
);

CREATE TABLE transfer_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK(quantity > 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transfer_id) REFERENCES transfer_requests(id),
    FOREIGN KEY (batch_id) REFERENCES inventory_batches(id),
    FOREIGN KEY (medicine_id) REFERENCES medicine_catalog(id)
);

CREATE TABLE ai_insights (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    facility_id BIGINT,
    medicine_id BIGINT,
    insight_type ENUM('SHORTAGE_RISK','EXPIRY_ALERT','DEMAND_FORECAST','TREND_ANALYSIS','LOGISTICS_RECOMMENDATION') NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    severity ENUM('INFO','WARNING','CRITICAL') NOT NULL,
    risk_score DECIMAL(5,2),
    metadata JSON,
    request_hash VARCHAR(64) UNIQUE,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    FOREIGN KEY (facility_id) REFERENCES facilities(id),
    FOREIGN KEY (medicine_id) REFERENCES medicine_catalog(id)
);

CREATE TABLE ai_usage_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    request_type VARCHAR(100) NOT NULL,
    model_used VARCHAR(100) NOT NULL,
    input_tokens INT,
    output_tokens INT,
    response_time_ms INT,
    is_cached BOOLEAN DEFAULT FALSE,
    status ENUM('SUCCESS','FAILED','TIMEOUT','FALLBACK') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE geocoding_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    query_hash VARCHAR(64) NOT NULL UNIQUE,
    query_text VARCHAR(255) NOT NULL,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    display_name VARCHAR(500),
    raw_response JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL
);

CREATE TABLE weather_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    location_hash VARCHAR(64) NOT NULL UNIQUE,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    temperature DECIMAL(5,2),
    humidity DECIMAL(5,2),
    rain_probability DECIMAL(5,2),
    weather_data JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL
);

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    facility_id BIGINT,
    type ENUM('OTP','LOW_STOCK','CRITICAL_STOCK','EXPIRY_WARNING','TRANSFER_REQUEST','TRANSFER_UPDATE','SYSTEM') NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    reference_id VARCHAR(100),
    reference_type VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (facility_id) REFERENCES facilities(id)
);

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    old_value JSON,
    new_value JSON,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE otp_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    purpose ENUM('REGISTRATION','LOGIN','PASSWORD_RESET') NOT NULL,
    attempts INT DEFAULT 0,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL
);

CREATE TABLE system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    description TEXT,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

SET FOREIGN_KEY_CHECKS = 1;

CREATE INDEX idx_facilities_city ON facilities(city);
CREATE INDEX idx_facilities_location ON facilities(latitude, longitude);
CREATE INDEX idx_inv_batch_facility ON inventory_batches(facility_id);
CREATE INDEX idx_inv_batch_medicine ON inventory_batches(medicine_id);
CREATE INDEX idx_inv_batch_expiry ON inventory_batches(expiry_date);
CREATE INDEX idx_emerg_req_status ON emergency_requests(status);
CREATE INDEX idx_trans_req_status ON transfer_requests(status);
CREATE INDEX idx_inv_batch_status ON inventory_batches(status);
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(user_id, is_read);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_otp_email ON otp_verifications(email);
CREATE INDEX idx_consumption_facility_medicine ON stock_consumption(facility_id, medicine_id);
CREATE INDEX idx_consumption_date ON stock_consumption(consumption_date);
CREATE INDEX idx_transfer_from ON transfer_requests(from_facility_id);
CREATE INDEX idx_transfer_to ON transfer_requests(to_facility_id);
