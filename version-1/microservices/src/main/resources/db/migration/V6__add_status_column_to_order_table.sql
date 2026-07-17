-- Thêm cột status vào bảng order, mặc định trạng thái ban đầu là 'PENDING'
ALTER TABLE `order` ADD COLUMN status VARCHAR(32) DEFAULT 'PENDING' NOT NULL;