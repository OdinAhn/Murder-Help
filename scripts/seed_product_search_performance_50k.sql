-- 상품 검색 v1/v2 성능 비교용 50,000건 더미 데이터.
--
-- 실행 방법 (로컬 MySQL에서만 실행):
--   mysql --default-character-set=utf8mb4 -h localhost -P 3306 -u killer_admin -p killer_mall < scripts/seed_product_search_performance_50k.sql
--
-- 이 스크립트는 애플리케이션 기동 시 자동 실행되지 않는다.
-- 기존 데이터 중 product_code가 PERF-로 시작하는 성능 테스트 상품만 삭제한 뒤
-- 정확히 50,000건을 다시 적재한다.

INSERT INTO categories (name, parent_id)
SELECT 'Performance Test', NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE name = 'Performance Test' AND parent_id IS NULL
);

SET @performance_root_category_id = (
    SELECT id
    FROM categories
    WHERE name = 'Performance Test' AND parent_id IS NULL
    ORDER BY id
    LIMIT 1
);

INSERT INTO categories (name, parent_id)
SELECT 'Search Products', @performance_root_category_id
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE name = 'Search Products' AND parent_id = @performance_root_category_id
);

SET @performance_category_id = (
    SELECT id
    FROM categories
    WHERE name = 'Search Products' AND parent_id = @performance_root_category_id
    ORDER BY id
    LIMIT 1
);

DROP PROCEDURE IF EXISTS seed_product_search_performance_data;

DELIMITER //

CREATE PROCEDURE seed_product_search_performance_data()
BEGIN
    DECLARE product_number INT DEFAULT 1;

    WHILE product_number <= 50000 DO
        INSERT INTO products (
            product_code,
            category_id,
            category,
            name,
            description,
            image_url,
            price,
            stock_quantity,
            tier,
            status
        ) VALUES (
            CONCAT('PERF-', LPAD(product_number, 6, '0')),
            @performance_category_id,
            'Performance Test',
            CONCAT(
                CASE MOD(product_number, 5)
                    WHEN 0 THEN '권총'
                    WHEN 1 THEN '소총'
                    WHEN 2 THEN '방탄조끼'
                    WHEN 3 THEN '탄약'
                    ELSE '전술 장비'
                END,
                ' 성능 테스트 상품 ', product_number
            ),
            CONCAT('상품 검색 성능 테스트용 더미 데이터 #', product_number),
            'https://example.com/images/performance-product.png',
            10000 + MOD(product_number, 100) * 1000,
            100 + MOD(product_number, 50),
            CASE MOD(product_number, 4)
                WHEN 0 THEN 'yellow'
                WHEN 1 THEN 'purple'
                WHEN 2 THEN 'red'
                ELSE 'green'
            END,
            'ON_SALE'
        );

        SET product_number = product_number + 1;
    END WHILE;
END //

DELIMITER ;

START TRANSACTION;

DELETE FROM products WHERE product_code LIKE 'PERF-%';

CALL seed_product_search_performance_data();
COMMIT;

DROP PROCEDURE seed_product_search_performance_data;

SELECT
    COUNT(*) AS performance_product_count,
    SUM(name LIKE '%권총%') AS pistol_keyword_count,
    SUM(name LIKE '%소총%') AS rifle_keyword_count,
    SUM(name LIKE '%방탄조끼%') AS armor_keyword_count,
    SUM(name LIKE '%탄약%') AS ammo_keyword_count,
    SUM(name LIKE '%전술 장비%') AS tactical_keyword_count
FROM products
WHERE product_code LIKE 'PERF-%';
