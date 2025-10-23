

USE maintenance_db_test;

-- Alter table parts nambahin model column
ALTER TABLE parts ADD COLUMN model VARCHAR(255);


-- Rename description column to specification
ALTER TABLE parts CHANGE COLUMN description specification VARCHAR(255);

--Alter table parts ganti Supplier ke Manufacturer column
ALTER TABLE parts CHANGE COLUMN supplier manufacturer VARCHAR(255);


-- Insert multiple machine types
INSERT INTO machine_type (id, code, name) VALUES
('a1X9BcR7p2kQm4tYzFj8GdNwEoU5rV0', 'GI', 'General Item'),
('b2Y0DdS8q3lRn5uZaHk9HePxFpV6sW1', 'TJ', 'Tab Welding Machine'),
('c3Z1EeT9r4mSo6vAbJl0IfQyGqW7tX2', 'PC', 'Plate Surface Grinding Machine'),
('d4A2FfU0s5nTp7wBcKm1JgRzHrX8uY3', 'PU', 'Plate Automation UT Inspection Machine'),
('e5B3GgV1t6oUq8xCdLn2KhSaIsY9vZ4', 'EM', 'Plate Edge Milling Machine'),
('f6C4HhW2u7pVr9yDeMo3LiTbJtZ0wA5', 'EC', 'Plate Edge Crimping Machine'),
('g7D5IiX3v8qWs0zEfNp4MjUcKuA1xB6', 'PB', 'Press Bending Machine'),
('h8E6JjY4w9rXt1aFgOq5NkVdLvB2yC7', 'TW', 'Tack Welding'),
('i9F7KkZ5x0sYu2bGhPr6OlWeMwC3zD8', 'IW', 'Internal Welding Machine'),
('j0G8LlA6y1tZv3cHiQs7PmXfNxD4aE9', 'EW', 'External Welding Machine'),
('k1H9MmB7z2uAw4dIjRt8QnYgOyE5bF0', 'TR', 'Tab Removal Machine'),
('l2I0NnC8a3vBx5eJkSu9RoZhPzF6cG1', 'UT', 'Pipe Automatic UT'),
('m3J1OoD9b4wCy6fKlTv0SpAiQaG7dH2', 'RT', 'Pipe Radiographic Testing'),
('n4K2PpE0c5xDz7gLmUw1TqBjRbH8eI3', 'EF', 'Pipe Beveling'),
('o5L3QqF1d6yEa8hMnVx2UrCkScI9fJ4', 'ME', 'Mechanical Expansion Machine'),
('p6M4RrG2e7zFb9iNoWy3VsDlTdJ0gK5', 'PS', 'Pipe Sizing/Straightening Machine'),
('q7N5SsH3f8aGc0jOpXz4WtEmUeK1hL6', 'WG', 'Pipe End Grinding Machine'),
('r8O6TtI4g9bHd1kPqYa5XuFnVfL2iM7', 'HT', 'Hydrostatic Testing Machine'),
('s9P7UuJ5h0cIe2lQrZb6YvGoWgM3jN8', 'WM', 'Weight and Length Machine'),
('t0Q8VvK6i1dJf3mRsAc7ZwHpXhN4kO9', 'CT', 'Cooling Cycle Tower'),
('u1R9WwL7j2eKg4nStBd8AxIqYiO5lP0', 'AC', 'Air Compressor'),
('v2S0XxM8k3fLh5oTuCe9ByJrZjP6mQ1', 'OC', 'Overhead Crane'),
('w3T1YyN9l4gMi6pUvDf0CzKsAkQ7nR2', 'GC', 'Gantry Crane'),
('x4U2ZzO0m5hNj7qVwEg1DaLtBlR8oS3', 'DG', 'Generator'),
('y5V3AaP1n6iOk8rWxFh2EbMuCmS9pT4', 'SG', 'Slag Cleaning and Grinding Machine'),
('z6W4BbQ2o7jPl9sXyGi3FcNvDnT0qU5', 'RO', 'RO Plant'),
('A7X5CcR3p8kQm0tYzFj4GdNwEoU1vV6', 'FP', 'Four Column Press'),
('B8Y6DdS4q9lRn1uZaHk5HePxFpV2sW7', 'SF', 'Electric Stacker Forklift'),
('C9Z7EeT5r0mSo2vAbJl6IfQyGqW3tX8', 'FF', 'Fire Fighting System'),
('D0A8FfU6s1nTp3wBcKm7JgRzHrX4uY9', 'FT', 'Flatbed Truck'),
('E1B9GgV7t2oUq4xCdLn8KhSaIsY5vZ0', 'FD', 'Flux Dryer'),
('F2C0HhW8u3pVr5yDeMo9LiTbJtZ6wA1', 'XR', 'X Ray Photograph'),
('G3D1IiX9v4qWs6zEfNp0MjUcKuA7xB2', 'AW', 'Servo Hydraulic Universal Testing Machine'),
('H4E2JjY0w5rXt7aFgOq1NkVdLvB8yC3', 'ET', 'Electromechanical Universal Testing Machine'),
('I5F3KkZ1x6sYu8bGhPr2OlWeMwC9zD4', 'TM', 'Impact Machine'),
('J6G4LlA2y7tZv9cHiQs3PmXfNxD0aE5', 'DT', 'Drop Weight Tear Testing Machine'),
('K7H5MmB3z8uAw0dIjRt4QnYgOyE1bF6', 'WA', 'Microcomputer Servo Electric Hydraulic ServUniversal Tester'),
('L8I6NnC4a9vBx1eJkSu5RoZhPzF2cG7', 'SE', 'Stable Pressure Exploding Machine'),
('M9J7OoD5b0wCy2fKlTv6SpAiQaG3dH8', 'AM', 'Auto Groov Machine'),
('N0K8PpE6c1xDz3gLmUw7TqBjRbH4eI9', 'VM', 'Verticle Milling Tool'),
('O1L9QqF7d2yEa4hMnVx8UrCkScI5fJ0', 'GD', 'Grinding Machine'),
('P2M0RrG8e3zFb5iNoWy9VsDlTdJ6gK1', 'SM', 'Saw Machine'),
('Q3N1SsH9f4aGc6jOpXz0WtEmUeK7hL2', 'HM', 'Horizontal Milling Tool'),
('R4O2TtI0g5bHd7kPqYa1XuFnVfL8iM3', 'TD', 'Table Drill'),
('S5P3UuJ1h6cIe8lQrZb2YvGoWgM9jN4', 'LM', 'Lathe Machine'),
('T6Q4VvK2i7dJf9mRsAc3ZwHpXhN0kO5', 'FL', 'Forklifts'),
('U7R5WwL3j8eKg0nStBd4AxIqYiP1lP6', 'MC', 'Mobile Crane'),
('V8S6XxM4k9fLh1oTuCe5ByJrZjQ2mQ7', 'UM', 'Horizontal Universal Milling Machine'),
('W9T7YyN5l0gMi2pUvDf6CzKsAkR3nR8', 'BT', 'Bend Testing Machine'),
('X0U8ZzO6m1hNj3qVwEg7DaLtBlS4oS9', 'SC', 'Reach Stacker Crane'),
('Y1V9AaP7n2iOk4rWxFh8EbMuCmT5pT0', 'WC', 'Wire Cutting Machine'),
('Z2W0BbQ8o3jPl5sXyGi9FcNvDnU6qU1', 'GS', 'High Frequency Farique Testing Machine'),
('a3X1CcR9p4kQm6tYzFj0GdNwEoV7rV2', 'TT', 'Transfer Trolley'),
('b4Y2DdS0q5lRn7uZaHk1HePxFpW8sW3', 'GE', 'General ELECTRIACAL'),
('c5Z3EeT1r6mSo8vAbJl2IfQyGqX9tX4', 'GM', 'General MACHINCAL'),
('d6A4FfU2s7nTp9wBcKm3JgRzHrY0uY5', 'GH', 'General hydraulic');

-- Insert multiple categories
INSERT INTO category (id, code, name) VALUES
('A1b2C3d4E5f6G7h8I9j0K1l2M3n4O5', 'CB', 'Miniature Circuit Breaker MCB'),
('B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6', 'LS', 'Limit Switch'),
('C3d4E5f6G7h8I9j0K1l2M3n4O5p6Q7', 'MC', 'Magnet Contactor'),
('D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8', 'IS', 'Inductive Sensor'),
('E5f6G7h8I9j0K1l2M3n4O5p6Q7r8S9', 'DI', 'Digital Input Module'),
('F6g7H8i9J0k1L2m3N4o5P6q7R8s9T0', 'DO', 'Digital Output Module'),
('G7h8I9j0K1l2M3n4O5p6Q7r8S9t0U1', 'AI', 'Analog Input Module'),
('H8i9J0k1L2m3N4o5P6q7R8s9T0u1V2', 'AO', 'Analog Output Module'),
('I9j0K1l2M3n4O5p6Q7r8S9t0U1v2W3', 'MD', 'Mixed Digital IO'),
('J0k1L2m3N4o5P6q7R8s9T0u1V2w3X4', 'MA', 'Mixed Analog IO'),
('K1l2M3n4O5p6Q7r8S9t0U1v2W3x4Y5', 'FM', 'Counter Module'),
('L2m3N4o5P6q7R8s9T0u1V2w3X4y5Z6', 'CU', 'PLC CPU'),
('M3n4O5p6Q7r8S9t0U1v2W3x4Y5z6A7', 'CM', 'Communication Module'),
('N4o5P6q7R8s9T0u1V2w3X4y5Z6a7B8', 'RC', 'PLC Rack'),
('O5p6Q7r8S9t0U1v2W3x4Y5z6A7b8C9', 'PS', 'Power Supply'),
('P6q7R8s9T0u1V2w3X4y5Z6a7B8c9D0', 'AD', 'AC Drives/Inverters/Frequency Drives'),
('Q7r8S9t0U1v2W3x4Y5z6A7b8C9d0E1', 'CT', 'Controller'),
('R8s9T0u1V2w3X4y5Z6a7B8c9D0e1F2', 'EN', 'Encoder'),
('S9t0U1v2W3x4Y5z6A7b8C9d0E1f2G3', 'LU', 'Lugs'),
('T0u1V2w3X4y5Z6a7B8c9D0e1F2g3H4', 'MO', 'Motor'),
('U1v2W3x4Y5z6A7b8C9d0E1f2G3h4I5', 'MP', 'Motor Protection Circuit Breaker'),
('V2w3X4y5Z6a7B8c9D0e1F2g3H4i5J6', 'OL', 'Over Load Relay'),
('W3x4Y5z6A7b8C9d0E1f2G3h4I5j6K7', 'AC', 'Auxiliary Contact'),
('X4y5Z6a7B8c9D0e1F2g3H4i5J6k7L8', 'CR', 'Control Relay'),
('Y5z6A7b8C9d0E1f2G3h4I5j6K7l8M9', 'GB', 'Gear Box'),
('Z6a7B8c9D0e1F2g3H4i5J6k7L8m9N0', 'BR', 'Bearing'),
('a7B8c9D0e1F2g3H4i5J6k7L8m9N0o1', 'SL', 'Slider'),
('b8C9d0E1f2G3h4I5j6K7l8M9n0O1p2', 'GD', 'Guide'),
('c9D0e1F2g3H4i5J6k7L8m9N0o1P2q3', 'BL', 'Bolts'),
('d0E1f2G3h4I5j6K7l8M9n0O1p2Q3r4', 'NT', 'Nuts'),
('e1F2g3H4i5J6k7L8m9N0o1P2q3R4s5', 'SW', 'Spring Washers'),
('f2G3h4I5j6K7l8M9n0O1p2Q3r4S5t6', 'PW', 'Plain Washers'),
('g3H4i5J6k7L8m9N0o1P2q3R4s5T6u7', 'OS', 'Oil Seal'),
('h4I5j6K7l8M9n0O1p2Q3r4S5t6U7v8', 'HS', 'Hydraulic Seal'),
('i5J6k7L8m9N0o1P2q3R4s5T6u7V8w9', 'OR', 'O-Ring'),
('j6K7l8M9n0O1p2Q3r4S5t6U7v8W9x0', 'MB', 'Molded Case Circuit Breaker'),
('k7L8m9N0o1P2q3R4s5T6u7V8w9X0y1', 'NO', 'Normally Open Contact'),
('l8M9n0O1p2Q3r4S5t6U7v8W9x0Y1z2', 'NC', 'Normally Close Contact'),
('m9N0o1P2q3R4s5T6u7V8w9X0y1Z2a3', 'RR', 'Radio Remote'),
('n0O1p2Q3r4S5t6U7v8W9x0Y1z2A3b4', 'AB', 'Air Circuit Breaker'),
('o1P2q3R4s5T6u7V8w9X0y1Z2a3B4c5', 'BC', 'Brush Carbon'),
('p2Q3r4S5t6U7v8W9x0Y1z2A3b4C5d6', 'RB', 'Residual Current Circuit Breaker');

