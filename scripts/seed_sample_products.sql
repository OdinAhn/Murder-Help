-- 상품 목록(GET /api/products) · 검색(GET /api/v1/products/search) API 동작 확인용 시드.
-- catalog.ts(프론트 로컬 카탈로그, 1,421건) 전체가 아니라, 5개 상위 카테고리 x 전체
-- 서브카테고리 x 3개 등급(yellow/purple/red) 조합마다 2건씩(총 174건) 발췌해 넣는다.
--
-- 실행 방법 (로컬 MySQL 또는 실제 AWS RDS에 직접 접속):
--   mysql -h <HOST> -P <PORT> -u <USER> -p <DATABASE> < scripts/seed_sample_products.sql
--
-- 이 스크립트는 애플리케이션 기동 시 자동 실행되지 않는다
-- (spring.sql.init.mode=never). 운영 DB에 실행하기 전에 반드시 내용을 검토할 것.
-- 이미 실행한 뒤 다시 실행해도 안전하도록 카테고리는 이름+부모, 상품은
-- product_code 기준으로 중복 삽입을 건너뛴다.

-- 1) 상위 카테고리
INSERT INTO categories (name, parent_id)
SELECT * FROM (SELECT 'Guns' AS name, NULL AS parent_id) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Guns' AND parent_id IS NULL);

INSERT INTO categories (name, parent_id)
SELECT * FROM (SELECT 'Weapons' AS name, NULL AS parent_id) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Weapons' AND parent_id IS NULL);

INSERT INTO categories (name, parent_id)
SELECT * FROM (SELECT 'Bombs' AS name, NULL AS parent_id) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Bombs' AND parent_id IS NULL);

INSERT INTO categories (name, parent_id)
SELECT * FROM (SELECT 'Gear' AS name, NULL AS parent_id) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Gear' AND parent_id IS NULL);

INSERT INTO categories (name, parent_id)
SELECT * FROM (SELECT 'Ammo' AS name, NULL AS parent_id) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Ammo' AND parent_id IS NULL);

-- 2) 하위 카테고리
INSERT INTO categories (name, parent_id)
SELECT 'Pistol', c.id FROM categories c
WHERE c.name = 'Guns' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Pistol' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Revolver', c.id FROM categories c
WHERE c.name = 'Guns' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Revolver' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Machine Pistol', c.id FROM categories c
WHERE c.name = 'Guns' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Machine Pistol' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Machine Gun', c.id FROM categories c
WHERE c.name = 'Guns' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Machine Gun' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'SMG', c.id FROM categories c
WHERE c.name = 'Guns' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'SMG' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Rifle', c.id FROM categories c
WHERE c.name = 'Guns' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Rifle' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Sniper Rifle', c.id FROM categories c
WHERE c.name = 'Guns' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Sniper Rifle' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Shotgun', c.id FROM categories c
WHERE c.name = 'Guns' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Shotgun' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Air Gun', c.id FROM categories c
WHERE c.name = 'Guns' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Air Gun' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Knife', c.id FROM categories c
WHERE c.name = 'Weapons' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Knife' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Sword', c.id FROM categories c
WHERE c.name = 'Weapons' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Sword' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Axe', c.id FROM categories c
WHERE c.name = 'Weapons' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Axe' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Baton', c.id FROM categories c
WHERE c.name = 'Weapons' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Baton' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Spear', c.id FROM categories c
WHERE c.name = 'Weapons' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Spear' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Smoke Grenade', c.id FROM categories c
WHERE c.name = 'Bombs' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Smoke Grenade' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Flash Bang', c.id FROM categories c
WHERE c.name = 'Bombs' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Flash Bang' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Frag Grenade', c.id FROM categories c
WHERE c.name = 'Bombs' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Frag Grenade' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Claymore', c.id FROM categories c
WHERE c.name = 'Bombs' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Claymore' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'C4', c.id FROM categories c
WHERE c.name = 'Bombs' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'C4' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Vest', c.id FROM categories c
WHERE c.name = 'Gear' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Vest' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Helmet', c.id FROM categories c
WHERE c.name = 'Gear' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Helmet' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Gloves', c.id FROM categories c
WHERE c.name = 'Gear' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Gloves' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Boots', c.id FROM categories c
WHERE c.name = 'Gear' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Boots' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Night Vision', c.id FROM categories c
WHERE c.name = 'Gear' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Night Vision' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT '6mm BB', c.id FROM categories c
WHERE c.name = 'Ammo' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = '6mm BB' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT '8mm BB', c.id FROM categories c
WHERE c.name = 'Ammo' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = '8mm BB' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'CO2 Cartridge', c.id FROM categories c
WHERE c.name = 'Ammo' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'CO2 Cartridge' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Gas Can', c.id FROM categories c
WHERE c.name = 'Ammo' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Gas Can' AND s.parent_id = c.id);

INSERT INTO categories (name, parent_id)
SELECT 'Tracer BB', c.id FROM categories c
WHERE c.name = 'Ammo' AND c.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM categories s WHERE s.name = 'Tracer BB' AND s.parent_id = c.id);

-- 3) 상품 (카테고리 x 서브카테고리 x 등급마다 2건, 총 174건 — catalog.ts 실제 데이터 발췌)
INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'P001', cat.id, 'Colt M1911 — Spring Type', 'Colt M1911 — Spring Type의 엔트리 사양입니다. 스프링 수동 코킹 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 권총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1580426263555-40cb477078ce?w=320&h=240&fit=crop&auto=format', 250, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Pistol' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'P001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'P002', cat.id, 'Beretta M92F — Spring Type', 'Beretta M92F — Spring Type의 엔트리 사양입니다. 스프링 수동 코킹 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 권총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1700774606158-a4fa926f0bee?w=320&h=240&fit=crop&auto=format', 380, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Pistol' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'P002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'P003', cat.id, 'Glock 17 — Gas Blowback Full Frame', 'Glock 17 — Gas Blowback Full Frame의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 권총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1713643562617-d5010f826966?w=320&h=240&fit=crop&auto=format', 1850, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Pistol' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'P003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'P020', cat.id, 'Sig Sauer P226 — Gas Blowback', 'Sig Sauer P226 — Gas Blowback의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 권총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1580865767741-37cd59206d74?w=320&h=240&fit=crop&auto=format', 2589, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Pistol' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'P020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'P004', cat.id, 'Colt M1911 Government — Gold Edition', 'Colt M1911 Government — Gold Edition의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 권총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1581955957646-b5a446b6100a?w=320&h=240&fit=crop&auto=format', 4200, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Pistol' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'P004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'P035', cat.id, 'Sig Sauer P226 — Full Metal Elite', 'Sig Sauer P226 — Full Metal Elite의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 권총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1681679972234-cae673eb5c34?w=320&h=240&fit=crop&auto=format', 4044, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Pistol' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'P035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'RV001', cat.id, 'S&W M36 Chief''s Special — Spring', 'S&W M36 Chief''s Special — Spring의 엔트리 사양입니다. 스프링 수동 코킹 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 리볼버이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1595590426346-a9d0348d802f?w=320&h=240&fit=crop&auto=format', 320, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Revolver' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'RV001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'RV002', cat.id, 'Colt Detective Special — Spring', 'Colt Detective Special — Spring의 엔트리 사양입니다. 스프링 수동 코킹 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 리볼버이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1718729611703-26c40ced4ed1?w=320&h=240&fit=crop&auto=format', 470, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Revolver' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'RV002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'RV003', cat.id, 'Colt Python .357 Magnum — Gas', 'Colt Python .357 Magnum — Gas의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 리볼버이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1681680060792-f5719ea2c9a8?w=320&h=240&fit=crop&auto=format', 2150, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Revolver' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'RV003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'RV020', cat.id, 'Ruger GP100 — Gas Blowback', 'Ruger GP100 — Gas Blowback의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 리볼버이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1595590424283-b8f17842773f?w=320&h=240&fit=crop&auto=format', 2956, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Revolver' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'RV020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'RV004', cat.id, 'Mateba Autorevolver — Full Metal', 'Mateba Autorevolver — Full Metal의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 리볼버이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1681680122225-a8bf46251b6d?w=320&h=240&fit=crop&auto=format', 4450, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Revolver' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'RV004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'RV035', cat.id, 'Ruger GP100 — Full Metal Elite', 'Ruger GP100 — Full Metal Elite의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 리볼버이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1595590424283-b8f17842773f?w=320&h=240&fit=crop&auto=format', 4409, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Revolver' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'RV035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'MP001', cat.id, 'IMI Micro Uzi — Spring Type', 'IMI Micro Uzi — Spring Type의 엔트리 사양입니다. 스프링 수동 코킹 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 기관권총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1595590426346-a9d0348d802f?w=320&h=240&fit=crop&auto=format', 400, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Machine Pistol' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'MP001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'MP002', cat.id, 'Glock 18C — Spring Type', 'Glock 18C — Spring Type의 엔트리 사양입니다. 스프링 수동 코킹 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 기관권총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1742349533542-dcf54f88f4da?w=320&h=240&fit=crop&auto=format', 620, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Machine Pistol' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'MP002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'MP003', cat.id, 'Ingram MAC-10 — Full Auto Silver', 'Ingram MAC-10 — Full Auto Silver의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 기관권총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1588449799312-e9b8c0353a8f?w=320&h=240&fit=crop&auto=format', 2400, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Machine Pistol' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'MP003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'MP020', cat.id, 'Beretta 93R — Gas Blowback', 'Beretta 93R — Gas Blowback의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 기관권총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1681680012700-ef9b162f2f33?w=320&h=240&fit=crop&auto=format', 1324, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Machine Pistol' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'MP020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'MP004', cat.id, 'Skorpion vz. 61 — Titanium Frame', 'Skorpion vz. 61 — Titanium Frame의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 기관권총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1587205419020-1ff788f8ca54?w=320&h=240&fit=crop&auto=format', 4800, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Machine Pistol' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'MP004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'MP035', cat.id, 'Beretta 93R — Full Metal Elite', 'Beretta 93R — Full Metal Elite의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 기관권총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1700774606158-a4fa926f0bee?w=320&h=240&fit=crop&auto=format', 4774, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Machine Pistol' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'MP035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'MG001', cat.id, 'RPD — Plastic Body Electric', 'RPD — Plastic Body Electric의 엔트리 사양입니다. 전동 (AEG) 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 기관총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1633591325602-eb52ee0a60b2?w=320&h=240&fit=crop&auto=format', 700, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Machine Gun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'MG001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'MG002', cat.id, 'M60 — Spring Trainer', 'M60 — Spring Trainer의 엔트리 사양입니다. 스프링 수동 코킹 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 기관총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1636922861058-ac8d49cb5147?w=320&h=240&fit=crop&auto=format', 900, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Machine Gun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'MG002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'MG003', cat.id, 'FN M249 Para — Light Support', 'FN M249 Para — Light Support의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 기관총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1759203977900-2f83bf81dcaa?w=320&h=240&fit=crop&auto=format', 2600, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Machine Gun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'MG003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'MG020', cat.id, 'PKM — Gas Blowback', 'PKM — Gas Blowback의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 기관총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1610057998992-6182af268499?w=320&h=240&fit=crop&auto=format', 1691, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Machine Gun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'MG020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'MG004', cat.id, 'M134 Minigun — Collector''s Edition', 'M134 Minigun — Collector''s Edition의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 기관총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1662642038068-acce8df45ee1?w=320&h=240&fit=crop&auto=format', 5000, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Machine Gun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'MG004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'MG035', cat.id, 'PKM — Full Metal Elite', 'PKM — Full Metal Elite의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 기관총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1610057998992-6182af268499?w=320&h=240&fit=crop&auto=format', 3139, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Machine Gun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'MG035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SM001', cat.id, 'H&K MP5A5 — Spring Type', 'H&K MP5A5 — Spring Type의 엔트리 사양입니다. 스프링 수동 코킹 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 기관단총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1664432242592-93ac43e446b1?w=320&h=240&fit=crop&auto=format', 550, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'SMG' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SM001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SM002', cat.id, 'Ingram MAC-11 — Spring Type', 'Ingram MAC-11 — Spring Type의 엔트리 사양입니다. 스프링 수동 코킹 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 기관단총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1676496649290-8e643822f0d5?w=320&h=240&fit=crop&auto=format', 680, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'SMG' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SM002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SM003', cat.id, 'H&K UMP45 — Tactical Electric', 'H&K UMP45 — Tactical Electric의 중급 사양입니다. 전동 (AEG) 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 기관단총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1539164986234-d51d1fe3a629?w=320&h=240&fit=crop&auto=format', 1950, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'SMG' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SM003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SM020', cat.id, 'Thompson M1928 — Gas Blowback', 'Thompson M1928 — Gas Blowback의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 기관단총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1696996129520-83ebfffbd22a?w=320&h=240&fit=crop&auto=format', 2058, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'SMG' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SM020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SM004', cat.id, 'FN P90 — CNC Full Metal', 'FN P90 — CNC Full Metal의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 기관단총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1681782876653-f9e43e328e93?w=320&h=240&fit=crop&auto=format', 4300, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'SMG' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SM004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SM035', cat.id, 'Thompson M1928 — Full Metal Elite', 'Thompson M1928 — Full Metal Elite의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 기관단총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1696996129520-83ebfffbd22a?w=320&h=240&fit=crop&auto=format', 3504, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'SMG' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SM035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'RF001', cat.id, 'AK-47 — Plastic Body Electric', 'AK-47 — Plastic Body Electric의 엔트리 사양입니다. 전동 (AEG) 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 소총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1610165539774-791ae89d0574?w=320&h=240&fit=crop&auto=format', 800, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Rifle' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'RF001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'RF002', cat.id, 'Colt M16A1 — Spring Type', 'Colt M16A1 — Spring Type의 엔트리 사양입니다. 스프링 수동 코킹 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 소총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1669489890724-2088c1162f5e?w=320&h=240&fit=crop&auto=format', 860, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Rifle' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'RF002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'RF003', cat.id, 'Colt M4A1 CQB — Full Metal', 'Colt M4A1 CQB — Full Metal의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 소총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1602901248692-06c8935adac0?w=320&h=240&fit=crop&auto=format', 2300, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Rifle' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'RF003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'RF020', cat.id, 'AR-15 — Gas Blowback', 'AR-15 — Gas Blowback의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 소총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1610165539827-5d3ed2512958?w=320&h=240&fit=crop&auto=format', 2425, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Rifle' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'RF020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'RF004', cat.id, 'H&K HK416 — Elite CNC Full Metal', 'H&K HK416 — Elite CNC Full Metal의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 소총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1602432140723-b88c8b054b26?w=320&h=240&fit=crop&auto=format', 4650, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Rifle' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'RF004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'RF035', cat.id, 'AR-15 — Full Metal Elite', 'AR-15 — Full Metal Elite의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 소총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1610165539827-5d3ed2512958?w=320&h=240&fit=crop&auto=format', 3869, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Rifle' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'RF035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SN001', cat.id, 'L96A1 — Spring Bolt Action', 'L96A1 — Spring Bolt Action의 엔트리 사양입니다. 스프링 수동 코킹 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 저격소총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1637091043246-8156a913dede?w=320&h=240&fit=crop&auto=format', 950, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Sniper Rifle' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SN001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SN002', cat.id, 'VSR-10 — Entry Spring', 'VSR-10 — Entry Spring의 엔트리 사양입니다. 스프링 수동 코킹 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 저격소총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1627816300432-2cf20f239a65?w=320&h=240&fit=crop&auto=format', 990, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Sniper Rifle' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SN002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SN003', cat.id, 'SVD Dragunov — Marksman Gas', 'SVD Dragunov — Marksman Gas의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 저격소총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1591123253780-af288771206f?w=320&h=240&fit=crop&auto=format', 2750, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Sniper Rifle' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SN003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SN020', cat.id, 'Remington 700 — Gas Blowback', 'Remington 700 — Gas Blowback의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 저격소총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1521727215876-9bfe173be82f?w=320&h=240&fit=crop&auto=format', 2792, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Sniper Rifle' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SN020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SN004', cat.id, 'Barrett M82A1 — Full Metal', 'Barrett M82A1 — Full Metal의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 저격소총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1637091042526-af42c546b640?w=320&h=240&fit=crop&auto=format', 4900, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Sniper Rifle' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SN004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SN035', cat.id, 'Remington 700 — Full Metal Elite', 'Remington 700 — Full Metal Elite의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 저격소총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1521727215876-9bfe173be82f?w=320&h=240&fit=crop&auto=format', 4234, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Sniper Rifle' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SN035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SG001', cat.id, 'Mossberg M500 — Spring Pump', 'Mossberg M500 — Spring Pump의 엔트리 사양입니다. 펌프 가압식 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 산탄총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1639064569728-14f279104fa7?w=320&h=240&fit=crop&auto=format', 650, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Shotgun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SG001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SG002', cat.id, 'Remington M870 — Spring Type', 'Remington M870 — Spring Type의 엔트리 사양입니다. 스프링 수동 코킹 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 산탄총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1681680122225-a8bf46251b6d?w=320&h=240&fit=crop&auto=format', 720, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Shotgun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SG002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SG003', cat.id, 'Franchi SPAS-12 — Tri-Shot', 'Franchi SPAS-12 — Tri-Shot의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 산탄총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1517651468335-5164984ba2c5?w=320&h=240&fit=crop&auto=format', 2050, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Shotgun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SG003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SG020', cat.id, 'Benelli M4 — Gas Blowback', 'Benelli M4 — Gas Blowback의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 산탄총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1547343052-1f5e556d53d6?w=320&h=240&fit=crop&auto=format', 1160, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Shotgun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SG020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SG004', cat.id, 'AA-12 Atchisson — Full Metal', 'AA-12 Atchisson — Full Metal의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 산탄총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1643538034194-895138d3d61d?w=320&h=240&fit=crop&auto=format', 4400, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Shotgun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SG004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SG035', cat.id, 'Benelli M4 — Full Metal Elite', 'Benelli M4 — Full Metal Elite의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 산탄총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1547343052-1f5e556d53d6?w=320&h=240&fit=crop&auto=format', 4599, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Shotgun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SG035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'AG001', cat.id, 'Daisy Red Ryder — Spring', 'Daisy Red Ryder — Spring의 엔트리 사양입니다. 스프링 수동 코킹 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 공기총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1514124242452-68a335539134?w=320&h=240&fit=crop&auto=format', 150, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Air Gun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'AG001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'AG002', cat.id, 'Crosman 760 Pumpmaster', 'Crosman 760 Pumpmaster의 엔트리 사양입니다. 펌프 가압식 방식으로 작동하며 ABS 플라스틱 바디로 마감했습니다. 실내 사격장과 자세 연습용 입문 모델입니다. 6mm BB탄을 사용하는 에어소프트 공기총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1748444355580-278e972f660b?w=320&h=240&fit=crop&auto=format', 340, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Air Gun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'AG002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'AG003', cat.id, 'Gamo CO2 Air Rifle — Precision', 'Gamo CO2 Air Rifle — Precision의 중급 사양입니다. CO2 카트리지 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 공기총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1518219870542-9cc78377f953?w=320&h=240&fit=crop&auto=format', 1700, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Air Gun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'AG003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'AG020', cat.id, 'Umarex Steel Storm — Gas Blowback', 'Umarex Steel Storm — Gas Blowback의 중급 사양입니다. 가스 블로우백 방식으로 작동하며 알루미늄 + 강화 폴리머 바디로 마감했습니다. 필드 게임에서 무리 없이 쓸 수 있는 실사용 등급입니다. 6mm BB탄을 사용하는 에어소프트 공기총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1514124242452-68a335539134?w=320&h=240&fit=crop&auto=format', 1527, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Air Gun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'AG020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'AG004', cat.id, 'FX PCP Match Air Rifle — Competition', 'FX PCP Match Air Rifle — Competition의 VIP 한정판 사양입니다. PCP 사전 압축 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 공기총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1627816300432-2cf20f239a65?w=320&h=240&fit=crop&auto=format', 3900, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Air Gun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'AG004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'AG035', cat.id, 'Umarex Steel Storm — Full Metal Elite', 'Umarex Steel Storm — Full Metal Elite의 VIP 한정판 사양입니다. 전동 (AEG) 방식으로 작동하며 CNC 알루미늄 풀메탈 바디로 마감했습니다. 수집과 전시를 함께 고려한 한정 생산 모델입니다. 6mm BB탄을 사용하는 에어소프트 공기총이며 실탄은 발사되지 않습니다.', 'https://images.unsplash.com/photo-1676496649280-46a58ff76d2c?w=320&h=240&fit=crop&auto=format', 4964, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Air Gun' AND parent.name = 'Guns'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'AG035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'KN001', cat.id, 'Training Rubber Knife', 'Training Rubber Knife. 고무 · 폴리우레탄 재질의 엔트리 나이프입니다. 타격력이 없는 훈련용이라 CQB 연습에 안전하게 씁니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1606976467874-31e4342935f4?w=320&h=240&fit=crop&auto=format', 100, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Knife' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'KN001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'KN002', cat.id, 'Folding Utility Knife', 'Folding Utility Knife. 고무 · 폴리우레탄 재질의 엔트리 나이프입니다. 타격력이 없는 훈련용이라 CQB 연습에 안전하게 씁니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1588597574944-5e581eeef359?w=320&h=240&fit=crop&auto=format', 260, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Knife' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'KN002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'KN003', cat.id, 'Tanto Combat Knife — Full Tang', 'Tanto Combat Knife — Full Tang. 스테인리스 스틸 재질의 중급 나이프입니다. 실물 무게중심을 맞춘 실사용 등급입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1589728473894-4fb97b4dbb88?w=320&h=240&fit=crop&auto=format', 1450, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Knife' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'KN003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'KN020', cat.id, 'Karambit Trainer', 'Karambit Trainer. 스테인리스 스틸 재질의 중급 나이프입니다. 실물 무게중심을 맞춘 실사용 등급입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1606976467874-31e4342935f4?w=320&h=240&fit=crop&auto=format', 1894, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Knife' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'KN020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'KN004', cat.id, 'Damascus Steel Combat Knife', 'Damascus Steel Combat Knife. 다마스커스 적층강 재질의 VIP 한정판 나이프입니다. 장인이 단조한 한정 수량 모델로 소장용입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1593618229012-8aaad1cfefc3?w=320&h=240&fit=crop&auto=format', 3600, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Knife' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'KN004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'KN035', cat.id, 'Karambit Trainer', 'Karambit Trainer. 다마스커스 적층강 재질의 VIP 한정판 나이프입니다. 장인이 단조한 한정 수량 모델로 소장용입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1777562120942-bcebdf1b9fa4?w=320&h=240&fit=crop&auto=format', 3329, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Knife' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'KN035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SW001', cat.id, 'Foam Practice Sword', 'Foam Practice Sword. 고무 · 폴리우레탄 재질의 엔트리 도검입니다. 타격력이 없는 훈련용이라 CQB 연습에 안전하게 씁니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1589578527966-fdac0f44566c?w=320&h=240&fit=crop&auto=format', 180, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Sword' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SW001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SW002', cat.id, 'Wooden Bokken', 'Wooden Bokken. 고무 · 폴리우레탄 재질의 엔트리 도검입니다. 타격력이 없는 훈련용이라 CQB 연습에 안전하게 씁니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1645835870529-0bdbe31c5553?w=320&h=240&fit=crop&auto=format', 300, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Sword' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SW002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SW003', cat.id, 'Carbon Steel Katana — Practice', 'Carbon Steel Katana — Practice. 스테인리스 스틸 재질의 중급 도검입니다. 실물 무게중심을 맞춘 실사용 등급입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1542403764-c26462c4697e?w=320&h=240&fit=crop&auto=format', 2200, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Sword' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SW003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SW020', cat.id, 'Longsword Replica', 'Longsword Replica. 스테인리스 스틸 재질의 중급 도검입니다. 실물 무게중심을 맞춘 실사용 등급입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1687567571600-4b5ad80665b8?w=320&h=240&fit=crop&auto=format', 2261, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Sword' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SW020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SW004', cat.id, 'Hand-Forged Katana — Limited', 'Hand-Forged Katana — Limited. 다마스커스 적층강 재질의 VIP 한정판 도검입니다. 장인이 단조한 한정 수량 모델로 소장용입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1600081729801-fd151cba450f?w=320&h=240&fit=crop&auto=format', 4950, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Sword' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SW004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SW035', cat.id, 'Longsword Replica', 'Longsword Replica. 다마스커스 적층강 재질의 VIP 한정판 도검입니다. 장인이 단조한 한정 수량 모델로 소장용입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1687567571600-4b5ad80665b8?w=320&h=240&fit=crop&auto=format', 3694, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Sword' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SW035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'AX001', cat.id, 'Camp Hatchet — Basic', 'Camp Hatchet — Basic. 고무 · 폴리우레탄 재질의 엔트리 도끼입니다. 타격력이 없는 훈련용이라 CQB 연습에 안전하게 씁니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1656027102478-d85e59f593e2?w=320&h=240&fit=crop&auto=format', 220, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Axe' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'AX001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'AX002', cat.id, 'Rubber Training Tomahawk', 'Rubber Training Tomahawk. 고무 · 폴리우레탄 재질의 엔트리 도끼입니다. 타격력이 없는 훈련용이라 CQB 연습에 안전하게 씁니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1627468623506-0858a332509e?w=320&h=240&fit=crop&auto=format', 290, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Axe' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'AX002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'AX003', cat.id, 'Tactical Tomahawk — Steel', 'Tactical Tomahawk — Steel. 스테인리스 스틸 재질의 중급 도끼입니다. 실물 무게중심을 맞춘 실사용 등급입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1667608162353-d07f4387958d?w=320&h=240&fit=crop&auto=format', 1600, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Axe' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'AX003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'AX020', cat.id, 'Viking Axe Replica', 'Viking Axe Replica. 스테인리스 스틸 재질의 중급 도끼입니다. 실물 무게중심을 맞춘 실사용 등급입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1620303008348-afff3cc06891?w=320&h=240&fit=crop&auto=format', 2628, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Axe' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'AX020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'AX004', cat.id, 'Breaching Axe — Titanium Head', 'Breaching Axe — Titanium Head. 다마스커스 적층강 재질의 VIP 한정판 도끼입니다. 장인이 단조한 한정 수량 모델로 소장용입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1667928916594-9907f259a9ed?w=320&h=240&fit=crop&auto=format', 3800, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Axe' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'AX004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'AX035', cat.id, 'Viking Axe Replica', 'Viking Axe Replica. 다마스커스 적층강 재질의 VIP 한정판 도끼입니다. 장인이 단조한 한정 수량 모델로 소장용입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1620303008348-afff3cc06891?w=320&h=240&fit=crop&auto=format', 4059, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Axe' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'AX035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'BT001', cat.id, 'Rubber Training Baton', 'Rubber Training Baton. 고무 · 폴리우레탄 재질의 엔트리 바톤입니다. 타격력이 없는 훈련용이라 CQB 연습에 안전하게 씁니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1648834806369-2e1200ba2355?w=320&h=240&fit=crop&auto=format', 130, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Baton' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'BT001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'BT002', cat.id, 'Telescopic Baton — Basic', 'Telescopic Baton — Basic. 고무 · 폴리우레탄 재질의 엔트리 바톤입니다. 타격력이 없는 훈련용이라 CQB 연습에 안전하게 씁니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1625480859702-919bc0c5820d?w=320&h=240&fit=crop&auto=format', 350, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Baton' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'BT002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'BT003', cat.id, 'Expandable Steel Baton', 'Expandable Steel Baton. 스테인리스 스틸 재질의 중급 바톤입니다. 실물 무게중심을 맞춘 실사용 등급입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1584921465774-1be8bcd0c6c3?w=320&h=240&fit=crop&auto=format', 1250, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Baton' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'BT003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'BT020', cat.id, 'Riot Baton Trainer', 'Riot Baton Trainer. 스테인리스 스틸 재질의 중급 바톤입니다. 실물 무게중심을 맞춘 실사용 등급입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1625480859702-919bc0c5820d?w=320&h=240&fit=crop&auto=format', 2995, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Baton' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'BT020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'BT004', cat.id, 'Tactical Baton — Carbon Grip', 'Tactical Baton — Carbon Grip. 다마스커스 적층강 재질의 VIP 한정판 바톤입니다. 장인이 단조한 한정 수량 모델로 소장용입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1709110401582-9b39e60e3f7a?w=320&h=240&fit=crop&auto=format', 3300, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Baton' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'BT004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'BT035', cat.id, 'Riot Baton Trainer', 'Riot Baton Trainer. 다마스커스 적층강 재질의 VIP 한정판 바톤입니다. 장인이 단조한 한정 수량 모델로 소장용입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1661432432355-8f262a143280?w=320&h=240&fit=crop&auto=format', 4424, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Baton' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'BT035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SP001', cat.id, 'Training Foam Spear', 'Training Foam Spear. 고무 · 폴리우레탄 재질의 엔트리 창입니다. 타격력이 없는 훈련용이라 CQB 연습에 안전하게 씁니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1686747503915-109d6315635e?w=320&h=240&fit=crop&auto=format', 200, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Spear' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SP001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SP002', cat.id, 'Wooden Practice Spear', 'Wooden Practice Spear. 고무 · 폴리우레탄 재질의 엔트리 창입니다. 타격력이 없는 훈련용이라 CQB 연습에 안전하게 씁니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1787435966994-988fedfd2f5e?w=320&h=240&fit=crop&auto=format', 330, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Spear' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SP002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SP003', cat.id, 'Tactical Survival Spear', 'Tactical Survival Spear. 스테인리스 스틸 재질의 중급 창입니다. 실물 무게중심을 맞춘 실사용 등급입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1726103845323-3a8c4146ca36?w=320&h=240&fit=crop&auto=format', 1900, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Spear' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SP003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SP020', cat.id, 'Naginata Trainer', 'Naginata Trainer. 스테인리스 스틸 재질의 중급 창입니다. 실물 무게중심을 맞춘 실사용 등급입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1774100063415-67286b1adb08?w=320&h=240&fit=crop&auto=format', 1363, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Spear' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SP020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SP004', cat.id, 'Damascus Tipped Spear — Limited', 'Damascus Tipped Spear — Limited. 다마스커스 적층강 재질의 VIP 한정판 창입니다. 장인이 단조한 한정 수량 모델로 소장용입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1774100063415-67286b1adb08?w=320&h=240&fit=crop&auto=format', 4100, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Spear' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SP004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SP035', cat.id, 'Naginata Trainer', 'Naginata Trainer. 다마스커스 적층강 재질의 VIP 한정판 창입니다. 장인이 단조한 한정 수량 모델로 소장용입니다. 날은 서지 않은 상태로 출고됩니다.', 'https://images.unsplash.com/photo-1726103845323-3a8c4146ca36?w=320&h=240&fit=crop&auto=format', 4789, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Spear' AND parent.name = 'Weapons'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SP035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SK001', cat.id, 'Smoke Grenade — Red (6-pack)', 'Smoke Grenade — Red (6-pack). 핀 수동 격발 방식의 엔트리 연막탄입니다. 연습용으로 반복 사용할 수 있습니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1612812706163-b89351960ad2?w=320&h=240&fit=crop&auto=format', 100, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Smoke Grenade' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SK001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SK002', cat.id, 'Smoke Grenade — Blue (6-pack)', 'Smoke Grenade — Blue (6-pack). 핀 수동 격발 방식의 엔트리 연막탄입니다. 연습용으로 반복 사용할 수 있습니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1603103112382-a65eb0c26a3d?w=320&h=240&fit=crop&auto=format', 140, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Smoke Grenade' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SK002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SK003', cat.id, 'Pyro Smoke — Competition Grade', 'Pyro Smoke — Competition Grade. 충격 격발 방식의 중급 연막탄입니다. 필드 게임 규정에 맞춘 실사용 등급입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1612812725779-5577101c96ba?w=320&h=240&fit=crop&auto=format', 1550, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Smoke Grenade' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SK003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SK020', cat.id, 'Cold Smoke Canister', 'Cold Smoke Canister. 충격 격발 방식의 중급 연막탄입니다. 필드 게임 규정에 맞춘 실사용 등급입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1519066473994-a7506988851d?w=320&h=240&fit=crop&auto=format', 1730, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Smoke Grenade' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SK020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SK004', cat.id, 'Pro Smoke Set — Tournament', 'Pro Smoke Set — Tournament. 리모트 · 타이머 격발 방식의 VIP 한정판 연막탄입니다. 대회 운영용 풀세트 구성입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1612813092731-0f4dfc059942?w=320&h=240&fit=crop&auto=format', 3450, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Smoke Grenade' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SK004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'SK035', cat.id, 'Cold Smoke Canister', 'Cold Smoke Canister. 리모트 · 타이머 격발 방식의 VIP 한정판 연막탄입니다. 대회 운영용 풀세트 구성입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1519066473994-a7506988851d?w=320&h=240&fit=crop&auto=format', 3154, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Smoke Grenade' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'SK035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'FB001', cat.id, 'Training Flash Bang — Reusable', 'Training Flash Bang — Reusable. 핀 수동 격발 방식의 엔트리 섬광탄입니다. 연습용으로 반복 사용할 수 있습니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1774866380572-f72f6c08eedc?w=320&h=240&fit=crop&auto=format', 240, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Flash Bang' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'FB001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'FB002', cat.id, 'Sound Only Bang — Practice', 'Sound Only Bang — Practice. 핀 수동 격발 방식의 엔트리 섬광탄입니다. 연습용으로 반복 사용할 수 있습니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1517179574326-467498102408?w=320&h=240&fit=crop&auto=format', 310, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Flash Bang' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'FB002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'FB003', cat.id, 'Tactical Flash Bang — Training', 'Tactical Flash Bang — Training. 충격 격발 방식의 중급 섬광탄입니다. 필드 게임 규정에 맞춘 실사용 등급입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1788328348239-d634d3ae87cc?w=320&h=240&fit=crop&auto=format', 1800, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Flash Bang' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'FB003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'FB020', cat.id, 'Training Bang Device', 'Training Bang Device. 충격 격발 방식의 중급 섬광탄입니다. 필드 게임 규정에 맞춘 실사용 등급입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1788328348239-d634d3ae87cc?w=320&h=240&fit=crop&auto=format', 2097, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Flash Bang' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'FB020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'FB004', cat.id, 'Pro Flash Device — Competition', 'Pro Flash Device — Competition. 리모트 · 타이머 격발 방식의 VIP 한정판 섬광탄입니다. 대회 운영용 풀세트 구성입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1773839420967-b50018fc0505?w=320&h=240&fit=crop&auto=format', 4000, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Flash Bang' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'FB004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'FB035', cat.id, 'Training Bang Device', 'Training Bang Device. 리모트 · 타이머 격발 방식의 VIP 한정판 섬광탄입니다. 대회 운영용 풀세트 구성입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1774866380572-f72f6c08eedc?w=320&h=240&fit=crop&auto=format', 3519, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Flash Bang' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'FB035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'FG001', cat.id, 'Dummy Frag Grenade', 'Dummy Frag Grenade. 핀 수동 격발 방식의 엔트리 파편탄입니다. 연습용으로 반복 사용할 수 있습니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1784612195960-8c9af732f473?w=320&h=240&fit=crop&auto=format', 160, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Frag Grenade' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'FG001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'FG002', cat.id, 'BB Shower Grenade — Basic', 'BB Shower Grenade — Basic. 핀 수동 격발 방식의 엔트리 파편탄입니다. 연습용으로 반복 사용할 수 있습니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1678773887720-0c9a9f970a25?w=320&h=240&fit=crop&auto=format', 420, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Frag Grenade' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'FG002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'FG003', cat.id, 'Impact Frag Grenade — Gas', 'Impact Frag Grenade — Gas. 충격 격발 방식의 중급 파편탄입니다. 필드 게임 규정에 맞춘 실사용 등급입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1633591324611-55ee4caa790f?w=320&h=240&fit=crop&auto=format', 2000, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Frag Grenade' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'FG003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'FG020', cat.id, 'BB Burst Grenade', 'BB Burst Grenade. 충격 격발 방식의 중급 파편탄입니다. 필드 게임 규정에 맞춘 실사용 등급입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1612812706163-b89351960ad2?w=320&h=240&fit=crop&auto=format', 2464, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Frag Grenade' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'FG020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'FG004', cat.id, 'Pro Frag System — Full Metal', 'Pro Frag System — Full Metal. 리모트 · 타이머 격발 방식의 VIP 한정판 파편탄입니다. 대회 운영용 풀세트 구성입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1612812706163-b89351960ad2?w=320&h=240&fit=crop&auto=format', 4550, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Frag Grenade' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'FG004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'FG035', cat.id, 'BB Burst Grenade', 'BB Burst Grenade. 리모트 · 타이머 격발 방식의 VIP 한정판 파편탄입니다. 대회 운영용 풀세트 구성입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1633591324611-55ee4caa790f?w=320&h=240&fit=crop&auto=format', 3884, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Frag Grenade' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'FG035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'CM001', cat.id, 'Dummy Claymore — Display', 'Dummy Claymore — Display. 핀 수동 격발 방식의 엔트리 클레이모어입니다. 연습용으로 반복 사용할 수 있습니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1729284045177-7a40238e4cd0?w=320&h=240&fit=crop&auto=format', 280, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Claymore' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'CM001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'CM002', cat.id, 'Tripwire Claymore — Basic', 'Tripwire Claymore — Basic. 핀 수동 격발 방식의 엔트리 클레이모어입니다. 연습용으로 반복 사용할 수 있습니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1580741569354-08feedd159f9?w=320&h=240&fit=crop&auto=format', 560, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Claymore' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'CM002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'CM003', cat.id, 'BB Claymore — Directional', 'BB Claymore — Directional. 충격 격발 방식의 중급 클레이모어입니다. 필드 게임 규정에 맞춘 실사용 등급입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1684183471939-a1d8b41f7efd?w=320&h=240&fit=crop&auto=format', 2450, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Claymore' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'CM003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'CM020', cat.id, 'Directional Mine Trainer', 'Directional Mine Trainer. 충격 격발 방식의 중급 클레이모어입니다. 필드 게임 규정에 맞춘 실사용 등급입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1580741569354-08feedd159f9?w=320&h=240&fit=crop&auto=format', 2831, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Claymore' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'CM020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'CM004', cat.id, 'Pro Claymore Set — Tournament', 'Pro Claymore Set — Tournament. 리모트 · 타이머 격발 방식의 VIP 한정판 클레이모어입니다. 대회 운영용 풀세트 구성입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1673026002416-ee8788859ff3?w=320&h=240&fit=crop&auto=format', 4700, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Claymore' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'CM004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'CM035', cat.id, 'Directional Mine Trainer', 'Directional Mine Trainer. 리모트 · 타이머 격발 방식의 VIP 한정판 클레이모어입니다. 대회 운영용 풀세트 구성입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1684117336673-052c6a726330?w=320&h=240&fit=crop&auto=format', 4249, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Claymore' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'CM035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'C4001', cat.id, 'Dummy C4 Prop', 'Dummy C4 Prop. 핀 수동 격발 방식의 엔트리 폭약입니다. 연습용으로 반복 사용할 수 있습니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1633591324611-55ee4caa790f?w=320&h=240&fit=crop&auto=format', 190, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'C4' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'C4001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'C4002', cat.id, 'Timer Charge — Practice', 'Timer Charge — Practice. 핀 수동 격발 방식의 엔트리 폭약입니다. 연습용으로 반복 사용할 수 있습니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1633591324611-55ee4caa790f?w=320&h=240&fit=crop&auto=format', 480, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'C4' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'C4002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'C4003', cat.id, 'Remote Charge — Training', 'Remote Charge — Training. 충격 격발 방식의 중급 폭약입니다. 필드 게임 규정에 맞춘 실사용 등급입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1784612195960-8c9af732f473?w=320&h=240&fit=crop&auto=format', 2850, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'C4' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'C4003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'C4020', cat.id, 'Dummy Demolition Charge', 'Dummy Demolition Charge. 충격 격발 방식의 중급 폭약입니다. 필드 게임 규정에 맞춘 실사용 등급입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1633591324611-55ee4caa790f?w=320&h=240&fit=crop&auto=format', 1199, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'C4' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'C4020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'C4004', cat.id, 'Pro Demolition Kit — Full Set', 'Pro Demolition Kit — Full Set. 리모트 · 타이머 격발 방식의 VIP 한정판 폭약입니다. 대회 운영용 풀세트 구성입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1679767878560-3d8c16c01fe1?w=320&h=240&fit=crop&auto=format', 4850, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'C4' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'C4004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'C4035', cat.id, 'Dummy Demolition Charge', 'Dummy Demolition Charge. 리모트 · 타이머 격발 방식의 VIP 한정판 폭약입니다. 대회 운영용 풀세트 구성입니다. 화약을 쓰지 않으며 실내 사용은 금지됩니다.', 'https://images.unsplash.com/photo-1774365433818-a10ee9163c64?w=320&h=240&fit=crop&auto=format', 4614, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'C4' AND parent.name = 'Bombs'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'C4035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'VS001', cat.id, 'Basic Chest Rig', 'Basic Chest Rig. 600D 폴리에스터 소재의 엔트리 조끼입니다. 가볍고 저렴해 입문용으로 적당합니다.', 'https://images.unsplash.com/photo-1767597278612-ab382ab01c2d?w=320&h=240&fit=crop&auto=format', 340, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Vest' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'VS001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'VS002', cat.id, 'Lightweight Mesh Vest', 'Lightweight Mesh Vest. 600D 폴리에스터 소재의 엔트리 조끼입니다. 가볍고 저렴해 입문용으로 적당합니다.', 'https://images.unsplash.com/photo-1769430534842-b249cd217ecc?w=320&h=240&fit=crop&auto=format', 520, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Vest' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'VS002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'VS003', cat.id, 'Plate Carrier — MOLLE', 'Plate Carrier — MOLLE. 1000D 코듀라 소재의 중급 조끼입니다. 장시간 게임에도 견디는 실사용 등급입니다.', 'https://images.unsplash.com/photo-1666873574507-c77105c5071b?w=320&h=240&fit=crop&auto=format', 1700, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Vest' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'VS003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'VS020', cat.id, 'Recon Chest Rig', 'Recon Chest Rig. 1000D 코듀라 소재의 중급 조끼입니다. 장시간 게임에도 견디는 실사용 등급입니다.', 'https://images.unsplash.com/photo-1666873575194-fdccbdb9888d?w=320&h=240&fit=crop&auto=format', 1566, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Vest' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'VS020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'VS004', cat.id, 'Elite Plate Carrier — Laser Cut', 'Elite Plate Carrier — Laser Cut. 레이저컷 코듀라 + 카본 소재의 VIP 한정판 조끼입니다. 무게를 줄이고 마감을 끌어올린 최상급입니다.', 'https://images.unsplash.com/photo-1771241137247-6d00a277c2ac?w=320&h=240&fit=crop&auto=format', 3700, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Vest' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'VS004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'VS035', cat.id, 'Recon Chest Rig', 'Recon Chest Rig. 레이저컷 코듀라 + 카본 소재의 VIP 한정판 조끼입니다. 무게를 줄이고 마감을 끌어올린 최상급입니다.', 'https://images.unsplash.com/photo-1666873575194-fdccbdb9888d?w=320&h=240&fit=crop&auto=format', 4979, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Vest' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'VS035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'HM001', cat.id, 'Bump Helmet — Entry', 'Bump Helmet — Entry. 600D 폴리에스터 소재의 엔트리 헬멧입니다. 가볍고 저렴해 입문용으로 적당합니다.', 'https://images.unsplash.com/photo-1569242838421-c6f57b9a02df?w=320&h=240&fit=crop&auto=format', 380, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Helmet' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'HM001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'HM002', cat.id, 'PASGT Replica Helmet', 'PASGT Replica Helmet. 600D 폴리에스터 소재의 엔트리 헬멧입니다. 가볍고 저렴해 입문용으로 적당합니다.', 'https://images.unsplash.com/photo-1700774607136-7e730e9e5dd0?w=320&h=240&fit=crop&auto=format', 640, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Helmet' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'HM002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'HM003', cat.id, 'FAST Helmet — Rail System', 'FAST Helmet — Rail System. 1000D 코듀라 소재의 중급 헬멧입니다. 장시간 게임에도 견디는 실사용 등급입니다.', 'https://images.unsplash.com/photo-1677030041879-24c9e2cc55e5?w=320&h=240&fit=crop&auto=format', 2250, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Helmet' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'HM003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'HM020', cat.id, 'MICH Helmet Basic', 'MICH Helmet Basic. 1000D 코듀라 소재의 중급 헬멧입니다. 장시간 게임에도 견디는 실사용 등급입니다.', 'https://images.unsplash.com/photo-1700774606243-e7b44df0db9f?w=320&h=240&fit=crop&auto=format', 1933, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Helmet' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'HM020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'HM004', cat.id, 'Ballistic-Style Helmet — Full Kit', 'Ballistic-Style Helmet — Full Kit. 레이저컷 코듀라 + 카본 소재의 VIP 한정판 헬멧입니다. 무게를 줄이고 마감을 끌어올린 최상급입니다.', 'https://images.unsplash.com/photo-1586246934424-0e5c6767a2c3?w=320&h=240&fit=crop&auto=format', 4150, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Helmet' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'HM004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'HM035', cat.id, 'MICH Helmet Basic', 'MICH Helmet Basic. 레이저컷 코듀라 + 카본 소재의 VIP 한정판 헬멧입니다. 무게를 줄이고 마감을 끌어올린 최상급입니다.', 'https://images.unsplash.com/photo-1700774606243-e7b44df0db9f?w=320&h=240&fit=crop&auto=format', 3344, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Helmet' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'HM035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'GL001', cat.id, 'Half-Finger Tactical Gloves', 'Half-Finger Tactical Gloves. 600D 폴리에스터 소재의 엔트리 장갑입니다. 가볍고 저렴해 입문용으로 적당합니다.', 'https://images.unsplash.com/photo-1778495351699-91a5ed2651ce?w=320&h=240&fit=crop&auto=format', 120, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Gloves' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'GL001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'GL002', cat.id, 'Mechanic Gloves — Basic', 'Mechanic Gloves — Basic. 600D 폴리에스터 소재의 엔트리 장갑입니다. 가볍고 저렴해 입문용으로 적당합니다.', 'https://images.unsplash.com/photo-1549396555-3d107fd70c85?w=320&h=240&fit=crop&auto=format', 230, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Gloves' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'GL002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'GL003', cat.id, 'Hard Knuckle Gloves — Goatskin', 'Hard Knuckle Gloves — Goatskin. 1000D 코듀라 소재의 중급 장갑입니다. 장시간 게임에도 견디는 실사용 등급입니다.', 'https://images.unsplash.com/photo-1788328348239-d634d3ae87cc?w=320&h=240&fit=crop&auto=format', 1100, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Gloves' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'GL003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'GL020', cat.id, 'Full Finger Combat Gloves', 'Full Finger Combat Gloves. 1000D 코듀라 소재의 중급 장갑입니다. 장시간 게임에도 견디는 실사용 등급입니다.', 'https://images.unsplash.com/photo-1687349150019-003d3ea38d79?w=320&h=240&fit=crop&auto=format', 2300, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Gloves' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'GL020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'GL004', cat.id, 'Elite Gloves — Kevlar Lined', 'Elite Gloves — Kevlar Lined. 레이저컷 코듀라 + 카본 소재의 VIP 한정판 장갑입니다. 무게를 줄이고 마감을 끌어올린 최상급입니다.', 'https://images.unsplash.com/photo-1687349150019-003d3ea38d79?w=320&h=240&fit=crop&auto=format', 3150, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Gloves' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'GL004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'GL035', cat.id, 'Full Finger Combat Gloves', 'Full Finger Combat Gloves. 레이저컷 코듀라 + 카본 소재의 VIP 한정판 장갑입니다. 무게를 줄이고 마감을 끌어올린 최상급입니다.', 'https://images.unsplash.com/photo-1788328348239-d634d3ae87cc?w=320&h=240&fit=crop&auto=format', 3709, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Gloves' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'GL035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'BO001', cat.id, 'Canvas Tactical Boots', 'Canvas Tactical Boots. 600D 폴리에스터 소재의 엔트리 부츠입니다. 가볍고 저렴해 입문용으로 적당합니다.', 'https://images.unsplash.com/photo-1664214386711-ad61bed14771?w=320&h=240&fit=crop&auto=format', 460, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Boots' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'BO001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'BO002', cat.id, 'Lightweight Trainer Boots', 'Lightweight Trainer Boots. 600D 폴리에스터 소재의 엔트리 부츠입니다. 가볍고 저렴해 입문용으로 적당합니다.', 'https://images.unsplash.com/photo-1769203905757-5aaf2dcba5fa?w=320&h=240&fit=crop&auto=format', 590, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Boots' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'BO002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'BO003', cat.id, 'Side-Zip Combat Boots', 'Side-Zip Combat Boots. 1000D 코듀라 소재의 중급 부츠입니다. 장시간 게임에도 견디는 실사용 등급입니다.', 'https://images.unsplash.com/photo-1645538097249-7dd6a4ca59ac?w=320&h=240&fit=crop&auto=format', 1650, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Boots' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'BO003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'BO020', cat.id, 'Jungle Combat Boots', 'Jungle Combat Boots. 1000D 코듀라 소재의 중급 부츠입니다. 장시간 게임에도 견디는 실사용 등급입니다.', 'https://images.unsplash.com/photo-1571370421770-70c96e7317cc?w=320&h=240&fit=crop&auto=format', 2667, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Boots' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'BO020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'BO004', cat.id, 'Elite Combat Boots — Gore Lined', 'Elite Combat Boots — Gore Lined. 레이저컷 코듀라 + 카본 소재의 VIP 한정판 부츠입니다. 무게를 줄이고 마감을 끌어올린 최상급입니다.', 'https://images.unsplash.com/photo-1713075693930-b593e701b6d8?w=320&h=240&fit=crop&auto=format', 3550, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Boots' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'BO004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'BO035', cat.id, 'Jungle Combat Boots', 'Jungle Combat Boots. 레이저컷 코듀라 + 카본 소재의 VIP 한정판 부츠입니다. 무게를 줄이고 마감을 끌어올린 최상급입니다.', 'https://images.unsplash.com/photo-1571370421770-70c96e7317cc?w=320&h=240&fit=crop&auto=format', 4074, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Boots' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'BO035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'NV001', cat.id, 'Monocular Scope — Digital', 'Monocular Scope — Digital. 600D 폴리에스터 소재의 엔트리 야시장비입니다. 가볍고 저렴해 입문용으로 적당합니다.', 'https://images.unsplash.com/photo-1611810738212-d1dea6720ff3?w=320&h=240&fit=crop&auto=format', 880, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Night Vision' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'NV001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'NV002', cat.id, 'IR Illuminator — Basic', 'IR Illuminator — Basic. 600D 폴리에스터 소재의 엔트리 야시장비입니다. 가볍고 저렴해 입문용으로 적당합니다.', 'https://images.unsplash.com/photo-1700774607136-7e730e9e5dd0?w=320&h=240&fit=crop&auto=format', 940, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Night Vision' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'NV002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'NV003', cat.id, 'Gen 2 Night Vision Monocular', 'Gen 2 Night Vision Monocular. 1000D 코듀라 소재의 중급 야시장비입니다. 장시간 게임에도 견디는 실사용 등급입니다.', 'https://images.unsplash.com/photo-1771241137141-38d484c6fee7?w=320&h=240&fit=crop&auto=format', 2900, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Night Vision' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'NV003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'NV020', cat.id, 'PVS-14 Style Monocular', 'PVS-14 Style Monocular. 1000D 코듀라 소재의 중급 야시장비입니다. 장시간 게임에도 견디는 실사용 등급입니다.', 'https://images.unsplash.com/photo-1773839421072-bc9dbc4b8934?w=320&h=240&fit=crop&auto=format', 1035, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Night Vision' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'NV020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'NV004', cat.id, 'Dual Tube NVG — Gen 3', 'Dual Tube NVG — Gen 3. 레이저컷 코듀라 + 카본 소재의 VIP 한정판 야시장비입니다. 무게를 줄이고 마감을 끌어올린 최상급입니다.', 'https://images.unsplash.com/photo-1778495351270-f60bc42c51bf?w=320&h=240&fit=crop&auto=format', 5000, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Night Vision' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'NV004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'NV035', cat.id, 'PVS-14 Style Monocular', 'PVS-14 Style Monocular. 레이저컷 코듀라 + 카본 소재의 VIP 한정판 야시장비입니다. 무게를 줄이고 마감을 끌어올린 최상급입니다.', 'https://images.unsplash.com/photo-1773839421072-bc9dbc4b8934?w=320&h=240&fit=crop&auto=format', 4439, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Night Vision' AND parent.name = 'Gear'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'NV035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'A6001', cat.id, '6mm BB 0.20g (3000ea)', '6mm BB 0.20g (3000ea). 일반 등급의 6mm BB탄입니다. 연습 사격용 대용량 구성입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1780404421811-99d3bf478096?w=320&h=240&fit=crop&auto=format', 100, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = '6mm BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'A6001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'A6002', cat.id, '6mm BB 0.25g (3000ea)', '6mm BB 0.25g (3000ea). 일반 등급의 6mm BB탄입니다. 연습 사격용 대용량 구성입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1605092262243-28c74cfc74c7?w=320&h=240&fit=crop&auto=format', 130, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = '6mm BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'A6002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'A6003', cat.id, '6mm Precision BB 0.28g (2000ea)', '6mm Precision BB 0.28g (2000ea). 정밀 연마 등급의 6mm BB탄입니다. 탄도가 안정적이라 중거리 교전에 적합합니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1633164530923-309fd4c02969?w=320&h=240&fit=crop&auto=format', 1050, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = '6mm BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'A6003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'A6020', cat.id, '6mm BB 0.23g (2500ea)', '6mm BB 0.23g (2500ea). 정밀 연마 등급의 6mm BB입니다. 탄도가 안정적이라 중거리 교전에 적합합니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1683308743789-4910c3d16dae?w=320&h=240&fit=crop&auto=format', 1402, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = '6mm BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'A6020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'A6004', cat.id, '6mm Match Grade BB 0.40g (1000ea)', '6mm Match Grade BB 0.40g (1000ea). 매치 그레이드 등급의 6mm BB탄입니다. 편차를 최소화한 대회용 등급입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1673245886490-81c2173270f0?w=320&h=240&fit=crop&auto=format', 3050, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = '6mm BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'A6004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'A6035', cat.id, '6mm BB 0.23g (2500ea)', '6mm BB 0.23g (2500ea). 매치 그레이드 등급의 6mm BB입니다. 편차를 최소화한 대회용 등급입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1732568282682-c4c2bd8eaf02?w=320&h=240&fit=crop&auto=format', 4804, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = '6mm BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'A6035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'A8001', cat.id, '8mm BB 0.34g (1000ea)', '8mm BB 0.34g (1000ea). 일반 등급의 8mm BB탄입니다. 연습 사격용 대용량 구성입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1776463156345-b1a3e00ef289?w=320&h=240&fit=crop&auto=format', 170, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = '8mm BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'A8001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'A8002', cat.id, '8mm BB 0.45g (1000ea)', '8mm BB 0.45g (1000ea). 일반 등급의 8mm BB탄입니다. 연습 사격용 대용량 구성입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1676653323136-7c83027321b5?w=320&h=240&fit=crop&auto=format', 210, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = '8mm BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'A8002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'A8003', cat.id, '8mm Precision BB 0.52g (800ea)', '8mm Precision BB 0.52g (800ea). 정밀 연마 등급의 8mm BB탄입니다. 탄도가 안정적이라 중거리 교전에 적합합니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1595613483608-d9d04e74db72?w=320&h=240&fit=crop&auto=format', 1300, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = '8mm BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'A8003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'A8020', cat.id, '8mm BB 0.40g (1000ea)', '8mm BB 0.40g (1000ea). 정밀 연마 등급의 8mm BB입니다. 탄도가 안정적이라 중거리 교전에 적합합니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1732568282682-c4c2bd8eaf02?w=320&h=240&fit=crop&auto=format', 1769, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = '8mm BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'A8020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'A8004', cat.id, '8mm Match Grade BB (500ea)', '8mm Match Grade BB (500ea). 매치 그레이드 등급의 8mm BB탄입니다. 편차를 최소화한 대회용 등급입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1687933086196-cbdbbbaeaf62?w=320&h=240&fit=crop&auto=format', 3250, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = '8mm BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'A8004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'A8035', cat.id, '8mm BB 0.40g (1000ea)', '8mm BB 0.40g (1000ea). 매치 그레이드 등급의 8mm BB입니다. 편차를 최소화한 대회용 등급입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1683308743837-e6ba8cdeb60a?w=320&h=240&fit=crop&auto=format', 3169, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = '8mm BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'A8035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'CO001', cat.id, 'CO2 12g Cartridge (10ea)', 'CO2 12g Cartridge (10ea). 일반 등급의 CO2 카트리지입니다. 연습 사격용 대용량 구성입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1662738412406-d929c3b25796?w=320&h=240&fit=crop&auto=format', 150, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'CO2 Cartridge' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'CO001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'CO002', cat.id, 'CO2 12g Cartridge (25ea)', 'CO2 12g Cartridge (25ea). 일반 등급의 CO2 카트리지입니다. 연습 사격용 대용량 구성입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1576723162358-918012da3544?w=320&h=240&fit=crop&auto=format', 390, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'CO2 Cartridge' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'CO002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'CO003', cat.id, 'CO2 88g Cartridge (5ea)', 'CO2 88g Cartridge (5ea). 정밀 연마 등급의 CO2 카트리지입니다. 탄도가 안정적이라 중거리 교전에 적합합니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1733151340989-e181b326616b?w=320&h=240&fit=crop&auto=format', 1500, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'CO2 Cartridge' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'CO003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'CO020', cat.id, 'CO2 12g Cartridge (5ea)', 'CO2 12g Cartridge (5ea). 정밀 연마 등급의 CO2 Cartridge입니다. 탄도가 안정적이라 중거리 교전에 적합합니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1786690511605-28e5120c0062?w=320&h=240&fit=crop&auto=format', 2136, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'CO2 Cartridge' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'CO020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'CO004', cat.id, 'CO2 Bulk Pack — Competition (50ea)', 'CO2 Bulk Pack — Competition (50ea). 매치 그레이드 등급의 CO2 카트리지입니다. 편차를 최소화한 대회용 등급입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1786690511605-28e5120c0062?w=320&h=240&fit=crop&auto=format', 3400, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'CO2 Cartridge' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'CO004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'CO035', cat.id, 'CO2 12g Cartridge (5ea)', 'CO2 12g Cartridge (5ea). 매치 그레이드 등급의 CO2 Cartridge입니다. 편차를 최소화한 대회용 등급입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1733151340989-e181b326616b?w=320&h=240&fit=crop&auto=format', 3534, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'CO2 Cartridge' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'CO035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'GS001', cat.id, 'Green Gas 600ml', 'Green Gas 600ml. 일반 등급의 가스입니다. 연습 사격용 대용량 구성입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1667281725157-75424c253932?w=320&h=240&fit=crop&auto=format', 200, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Gas Can' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'GS001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'GS002', cat.id, 'Green Gas 1000ml', 'Green Gas 1000ml. 일반 등급의 가스입니다. 연습 사격용 대용량 구성입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1768146336032-c1ead0b55d34?w=320&h=240&fit=crop&auto=format', 430, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Gas Can' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'GS002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'GS003', cat.id, 'Red Gas — High Power 1000ml', 'Red Gas — High Power 1000ml. 정밀 연마 등급의 가스입니다. 탄도가 안정적이라 중거리 교전에 적합합니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1771946428593-0d40cdf487e3?w=320&h=240&fit=crop&auto=format', 1400, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Gas Can' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'GS003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'GS020', cat.id, 'Red Gas 600ml', 'Red Gas 600ml. 정밀 연마 등급의 Gas Can입니다. 탄도가 안정적이라 중거리 교전에 적합합니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1768146336032-c1ead0b55d34?w=320&h=240&fit=crop&auto=format', 2503, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Gas Can' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'GS020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'GS004', cat.id, 'Pro Gas Set — Winter Grade (6ea)', 'Pro Gas Set — Winter Grade (6ea). 매치 그레이드 등급의 가스입니다. 편차를 최소화한 대회용 등급입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1688202960533-a2b837dfb7bd?w=320&h=240&fit=crop&auto=format', 3650, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Gas Can' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'GS004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'GS035', cat.id, 'Red Gas 600ml', 'Red Gas 600ml. 매치 그레이드 등급의 Gas Can입니다. 편차를 최소화한 대회용 등급입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1684073889576-b30ef02ff19b?w=320&h=240&fit=crop&auto=format', 3899, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Gas Can' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'GS035');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'TR001', cat.id, 'Tracer BB 0.20g Green (2000ea)', 'Tracer BB 0.20g Green (2000ea). 일반 등급의 트레이서 BB탄입니다. 연습 사격용 대용량 구성입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1592514808335-0fe829647d9d?w=320&h=240&fit=crop&auto=format', 240, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Tracer BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'TR001');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'TR002', cat.id, 'Tracer BB 0.25g Red (2000ea)', 'Tracer BB 0.25g Red (2000ea). 일반 등급의 트레이서 BB탄입니다. 연습 사격용 대용량 구성입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1598390367635-55152e31ec07?w=320&h=240&fit=crop&auto=format', 370, 100, 'yellow', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Tracer BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'TR002');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'TR003', cat.id, 'Tracer BB Precision 0.28g (1500ea)', 'Tracer BB Precision 0.28g (1500ea). 정밀 연마 등급의 트레이서 BB탄입니다. 탄도가 안정적이라 중거리 교전에 적합합니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1699016804671-a86aac664695?w=320&h=240&fit=crop&auto=format', 1150, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Tracer BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'TR003');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'TR020', cat.id, 'Tracer BB 0.20g Red (2000ea)', 'Tracer BB 0.20g Red (2000ea). 정밀 연마 등급의 Tracer BB입니다. 탄도가 안정적이라 중거리 교전에 적합합니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1598390367635-55152e31ec07?w=320&h=240&fit=crop&auto=format', 2870, 100, 'purple', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Tracer BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'TR020');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'TR004', cat.id, 'Tracer BB Match Grade (1000ea)', 'Tracer BB Match Grade (1000ea). 매치 그레이드 등급의 트레이서 BB탄입니다. 편차를 최소화한 대회용 등급입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1783572954399-353cccd8de82?w=320&h=240&fit=crop&auto=format', 3900, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Tracer BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'TR004');

INSERT INTO products (product_code, category_id, name, description, image_url, price, stock_quantity, tier, status)
SELECT 'TR035', cat.id, 'Tracer BB 0.20g Red (2000ea)', 'Tracer BB 0.20g Red (2000ea). 매치 그레이드 등급의 Tracer BB입니다. 편차를 최소화한 대회용 등급입니다. 개봉 후에는 습기를 피해 보관하십시오.', 'https://images.unsplash.com/photo-1605092262243-28c74cfc74c7?w=320&h=240&fit=crop&auto=format', 4264, 100, 'red', 'ON_SALE'
FROM categories cat JOIN categories parent ON cat.parent_id = parent.id
WHERE cat.name = 'Tracer BB' AND parent.name = 'Ammo'
  AND NOT EXISTS (SELECT 1 FROM products WHERE product_code = 'TR035');

