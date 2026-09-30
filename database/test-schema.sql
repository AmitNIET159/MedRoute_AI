CREATE TABLE facilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    facility_type VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(20),
    phone VARCHAR(20),
    email VARCHAR(255)
);

CREATE TABLE medicine_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE medicine_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL,
    unit VARCHAR(50) NOT NULL,
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
    expiry_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    FOREIGN KEY (facility_id) REFERENCES facilities(id),
    FOREIGN KEY (medicine_id) REFERENCES medicine_catalog(id)
);

CREATE TABLE inventory_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    facility_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    notes TEXT,
    performed_by BIGINT,
    FOREIGN KEY (batch_id) REFERENCES inventory_batches(id)
);

CREATE TABLE stock_consumption (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    facility_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    department VARCHAR(255),
    recorded_by BIGINT,
    FOREIGN KEY (facility_id) REFERENCES facilities(id),
    FOREIGN KEY (medicine_id) REFERENCES medicine_catalog(id)
);
