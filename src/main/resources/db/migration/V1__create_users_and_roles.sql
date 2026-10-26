CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE=InnoDB;

CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE users_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_users_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_users_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB;

INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_STUDENT'), ('ROLE_LECTURER'), ('ROLE_ADMIN');
