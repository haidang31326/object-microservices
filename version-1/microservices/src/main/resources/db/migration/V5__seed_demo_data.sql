INSERT INTO venue (id, name, address, total_capacity)
VALUES
    (1, 'Saigon Exhibition Center', 'Ho Chi Minh City', 5000),
    (2, 'Hanoi Opera House', 'Hanoi', 1200);

INSERT INTO event (id, name, venue_id, total_capacity, left_capacity, ticket_price)
VALUES
    (1, 'Java Microservices Summit', 1, 1000, 1000, 49.00),
    (2, 'Cloud Native Night', 2, 500, 500, 35.00);

INSERT INTO customer (id, name, email, address)
VALUES
    (1, 'Demo Customer', 'customer@example.com', 'Ho Chi Minh City'),
    (2, 'Demo Admin', 'admin@example.com', 'Hanoi');
