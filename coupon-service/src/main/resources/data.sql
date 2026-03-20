INSERT INTO coupons (id, code, usage_limit, current_usage, target_country, created_at, version)
VALUES
(gen_random_uuid(), 'WIOSNA2026', 100, 0, 'PL', NOW(), 0),
(gen_random_uuid(), 'ALOHA-HAWAII', 10, 5, 'US', NOW(), 0),
(gen_random_uuid(), 'EMPIK-PROMO', 500, 499, 'PL', NOW(), 0);