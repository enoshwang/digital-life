CREATE DATABASE HUIWEN_ONLINE;

USE HUIWEN_ONLINE;

CREATE TABLE User (
    ID INT PRIMARY KEY,
    Username VARCHAR(255) UNIQUE,
    Password VARCHAR(255),
    Email VARCHAR(255),
    Phone VARCHAR(20),
    FullName VARCHAR(255),
    Address VARCHAR(255),
    RegistrationDate DATE
);

/*
INSERT INTO User (ID, Username, Password, Email, Phone, FullName, Address, RegistrationDate)
VALUES (1, 'john', 'hashed_password', 'john@example.com', '123456789', 'John Doe', '123 Main St', '2023-06-30');
*/

-- SELECT * FROM User WHERE Username = 'john' AND Password = 'hashed_password';
