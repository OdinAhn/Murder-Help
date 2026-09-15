-- 개발 환경 전용: 인기 상품 추천용 주문 완료 더미 데이터
--
-- 전제 조건
-- 1. orders.status 에 DELIVERED 상태가 있어야 한다.
-- 2. 각 상품 등급(yellow, purple, red, green)에 ON_SALE 상품이 최소 5개 있어야 한다.
--
-- 생성 범위
-- - 더미 회원 4명(등급별 1명)
-- - 등급별 DELIVERED 주문 30건, 총 120건
-- - 각 주문에 대응하는 주문상품 1건 및 COMPLETED 결제 1건
-- - 최근 90시간 안의 주문으로 생성하여 주간 랭킹 집계 대상에 포함
--
-- 재실행해도 DUMMY-RANK 전용 데이터만 삭제 후 다시 생성한다.
-- 실제 주문, 실제 회원, 상품 재고는 변경하지 않는다.

START TRANSACTION;

DELETE payment
FROM payments payment
JOIN orders `order` ON `order`.id = payment.order_id
WHERE `order`.order_number LIKE 'DUMMY-RANK-%';

DELETE order_item
FROM order_items order_item
JOIN orders `order` ON `order`.id = order_item.order_id
WHERE `order`.order_number LIKE 'DUMMY-RANK-%';

DELETE FROM orders
WHERE order_number LIKE 'DUMMY-RANK-%';

DELETE FROM members
WHERE email IN (
    'dummy-ranking-yellow@murderhelp.local',
    'dummy-ranking-purple@murderhelp.local',
    'dummy-ranking-red@murderhelp.local',
    'dummy-ranking-green@murderhelp.local'
);

INSERT INTO members (
    email,
    password,
    name,
    phone,
    grade,
    created_at,
    updated_at
) VALUES
    ('dummy-ranking-yellow@murderhelp.local', 'DUMMY_RANKING_ACCOUNT_NOT_FOR_LOGIN', '더미 옐로우', '010-0000-0101', 'YELLOW', NOW(6), NOW(6)),
    ('dummy-ranking-purple@murderhelp.local', 'DUMMY_RANKING_ACCOUNT_NOT_FOR_LOGIN', '더미 퍼플', '010-0000-0102', 'PURPLE', NOW(6), NOW(6)),
    ('dummy-ranking-red@murderhelp.local', 'DUMMY_RANKING_ACCOUNT_NOT_FOR_LOGIN', '더미 레드', '010-0000-0103', 'RED', NOW(6), NOW(6)),
    ('dummy-ranking-green@murderhelp.local', 'DUMMY_RANKING_ACCOUNT_NOT_FOR_LOGIN', '더미 그린', '010-0000-0104', 'GREEN', NOW(6), NOW(6));

CREATE TEMPORARY TABLE tmp_ranking_products AS
SELECT id, tier, name, price, tier_rank
FROM (
    SELECT
        product.id,
        product.tier,
        product.name,
        product.price,
        ROW_NUMBER() OVER (PARTITION BY product.tier ORDER BY product.id) AS tier_rank
    FROM products product
    WHERE product.status = 'ON_SALE'
      AND product.tier IN ('yellow', 'purple', 'red', 'green')
) ranked_product
WHERE tier_rank <= 5;

CREATE TEMPORARY TABLE tmp_ranking_orders AS
WITH RECURSIVE sequence_numbers AS (
    SELECT 1 AS sequence_number
    UNION ALL
    SELECT sequence_number + 1
    FROM sequence_numbers
    WHERE sequence_number < 30
)
SELECT
    member.id AS member_id,
    ranked_product.id AS product_id,
    ranked_product.name AS product_name,
    ranked_product.price AS unit_price,
    LOWER(member.grade) AS tier,
    sequence_numbers.sequence_number,
    DATE_SUB(NOW(6), INTERVAL sequence_numbers.sequence_number * 3 HOUR) AS ordered_at
FROM members member
CROSS JOIN sequence_numbers
JOIN tmp_ranking_products ranked_product
  ON ranked_product.tier = LOWER(member.grade)
 AND ranked_product.tier_rank = CASE
    WHEN sequence_numbers.sequence_number <= 10 THEN 1
    WHEN sequence_numbers.sequence_number <= 18 THEN 2
    WHEN sequence_numbers.sequence_number <= 24 THEN 3
    WHEN sequence_numbers.sequence_number <= 28 THEN 4
    ELSE 5
 END
WHERE member.email IN (
    'dummy-ranking-yellow@murderhelp.local',
    'dummy-ranking-purple@murderhelp.local',
    'dummy-ranking-red@murderhelp.local',
    'dummy-ranking-green@murderhelp.local'
);

INSERT INTO orders (
    member_id,
    order_number,
    status,
    total_amount,
    receiver_name,
    receiver_phone,
    delivery_address,
    delivery_request,
    created_at,
    updated_at
)
SELECT
    member_id,
    CONCAT('DUMMY-RANK-', UPPER(tier), '-', LPAD(sequence_number, 3, '0')),
    'DELIVERED',
    unit_price,
    '더미 주문 수령인',
    '010-0000-0000',
    '서울특별시 테스트구 랭킹로 120',
    '인기 상품 추천 테스트용 주문',
    ordered_at,
    ordered_at
FROM tmp_ranking_orders;

INSERT INTO order_items (
    order_id,
    product_id,
    product_name,
    unit_price,
    quantity,
    created_at,
    updated_at
)
SELECT
    `order`.id,
    ranking_order.product_id,
    ranking_order.product_name,
    ranking_order.unit_price,
    1,
    ranking_order.ordered_at,
    ranking_order.ordered_at
FROM tmp_ranking_orders ranking_order
JOIN orders `order`
  ON `order`.order_number = CONCAT(
      'DUMMY-RANK-',
      UPPER(ranking_order.tier),
      '-',
      LPAD(ranking_order.sequence_number, 3, '0')
  );

INSERT INTO payments (
    order_id,
    portone_payment_id,
    amount,
    pg_amount,
    status,
    paid_at,
    created_at,
    updated_at
)
SELECT
    `order`.id,
    CONCAT('dummy_pay_rank_', ranking_order.tier, '_', LPAD(ranking_order.sequence_number, 3, '0')),
    ranking_order.unit_price,
    ranking_order.unit_price,
    'COMPLETED',
    ranking_order.ordered_at,
    ranking_order.ordered_at,
    ranking_order.ordered_at
FROM tmp_ranking_orders ranking_order
JOIN orders `order`
  ON `order`.order_number = CONCAT(
      'DUMMY-RANK-',
      UPPER(ranking_order.tier),
      '-',
      LPAD(ranking_order.sequence_number, 3, '0')
  );

COMMIT;

SELECT
    product.tier,
    COUNT(DISTINCT `order`.id) AS delivered_order_count,
    COUNT(order_item.id) AS order_item_count,
    SUM(order_item.quantity) AS sold_quantity
FROM orders `order`
JOIN order_items order_item ON order_item.order_id = `order`.id
JOIN products product ON product.id = order_item.product_id
WHERE `order`.order_number LIKE 'DUMMY-RANK-%'
GROUP BY product.tier
ORDER BY product.tier;

SELECT
    product.tier,
    product.id AS product_id,
    product.name AS product_name,
    SUM(order_item.quantity) AS sold_quantity
FROM orders `order`
JOIN order_items order_item ON order_item.order_id = `order`.id
JOIN products product ON product.id = order_item.product_id
WHERE `order`.order_number LIKE 'DUMMY-RANK-%'
GROUP BY product.tier, product.id, product.name
ORDER BY product.tier, sold_quantity DESC, product.id;

DROP TEMPORARY TABLE tmp_ranking_orders;
DROP TEMPORARY TABLE tmp_ranking_products;
