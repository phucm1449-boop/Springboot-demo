-- Baseline schema, reconstructed from the JPA entities that Hibernate's
-- ddl-auto=update was previously generating. This is the last schema version
-- that existing environments (seeded via ddl-auto) are assumed to already have;
-- see spring.flyway.baseline-version in application.yml.

CREATE TABLE roles (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    fullname            VARCHAR(255) COLLATE utf8mb4_unicode_ci,
    phone_number        VARCHAR(10) NOT NULL,
    address             VARCHAR(200),
    password            VARCHAR(200) NOT NULL,
    is_active           TINYINT(1) NOT NULL,
    date_of_birth       DATE,
    facebook_account_id INT NOT NULL,
    google_account_id   INT NOT NULL,
    role_id             BIGINT,
    created_at          DATETIME,
    updated_at          DATETIME,
    CONSTRAINT uq_users_phone_number UNIQUE (phone_number),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE categories (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(350) NOT NULL,
    price       FLOAT,
    thumbnail   VARCHAR(300),
    description VARCHAR(255),
    category_id BIGINT,
    created_at  DATETIME,
    updated_at  DATETIME,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_images (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT,
    image_url  VARCHAR(300),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE orders (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    fullname         VARCHAR(100),
    email            VARCHAR(100),
    phone_number     VARCHAR(100) NOT NULL,
    address          VARCHAR(100),
    note             VARCHAR(100),
    order_date       DATE,
    status           VARCHAR(255),
    total_money      FLOAT,
    shipping_method  VARCHAR(255),
    shipping_address VARCHAR(255),
    shipping_date    DATE,
    tracking_number  VARCHAR(255),
    payment_method   VARCHAR(255),
    active           TINYINT(1),
    user_id          BIGINT,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_details (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    price               FLOAT NOT NULL,
    number_of_products  INT NOT NULL,
    total_money         FLOAT NOT NULL,
    color               VARCHAR(255),
    order_id            BIGINT,
    product_id          BIGINT,
    CONSTRAINT fk_order_details_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_details_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    token           VARCHAR(255),
    token_type      VARCHAR(50),
    expiration_date DATETIME,
    revoked         TINYINT(1) NOT NULL,
    expired         TINYINT(1) NOT NULL,
    user_id         BIGINT,
    CONSTRAINT fk_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invalidated_token (
    id          VARCHAR(255) PRIMARY KEY,
    expiry_date DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE social_accounts (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider    VARCHAR(20) NOT NULL,
    provider_id VARCHAR(50) NOT NULL,
    name        VARCHAR(150),
    email       VARCHAR(150)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
