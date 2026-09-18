CREATE DATABASE IF NOT EXISTS portfolio
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'portfolio'@'localhost' IDENTIFIED BY 'portfolio';
CREATE USER IF NOT EXISTS 'portfolio'@'%' IDENTIFIED BY 'portfolio';

-- Keep existing local installations in sync with application.properties.
-- CREATE USER IF NOT EXISTS does not change an existing account's password.
ALTER USER 'portfolio'@'localhost' IDENTIFIED BY 'portfolio';
ALTER USER 'portfolio'@'%' IDENTIFIED BY 'portfolio';

GRANT ALL PRIVILEGES ON portfolio.* TO 'portfolio'@'localhost';
GRANT ALL PRIVILEGES ON portfolio.* TO 'portfolio'@'%';

FLUSH PRIVILEGES;
