-- The column was sized for IPv4 while getRemoteAddr() only ever produced the
-- proxy's address. Now that CF-Connecting-IP carries the real visitor, IPv6
-- clients arrive as full-length literals, and one oversized value would roll
-- back the whole five-minute statistics batch.
ALTER TABLE request_statistics ALTER COLUMN ip TYPE VARCHAR(45);
