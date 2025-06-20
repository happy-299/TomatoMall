INSERT INTO stockpile (amount, frozen, product_id, version)
SELECT 
    FLOOR(RAND() * 100) + 1 AS amount,  -- 随机初始化 1–100 件
    0                   AS frozen,      -- 冻结库存全部设为 0
    id                  AS product_id,
    0                   AS version
FROM product;
