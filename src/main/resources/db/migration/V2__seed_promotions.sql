-- Seed minimal active promotions
INSERT INTO CPRS_PROMOTIONS (PROMO_ID, NAME, DESCRIPTION, START_DATE, EXPIRY_DATE, ELIGIBILITY_RULES, REWARD_FORMULA, STATUS)
VALUES (
        'PROMO_PCT10',
        '10% Cashback 50+',
        '10% cashback for merchants 1001/1002 on amounts >= 50',
        SYSTIMESTAMP - NUMTODSINTERVAL(30,'DAY'),
        SYSTIMESTAMP + NUMTODSINTERVAL(365,'DAY'),
        '{"minAmount":50,"merchantIds":[1001,1002]}',
        '{"cashbackPercent":10,"maxCashback":20}',
        'ACTIVE'
);

INSERT INTO CPRS_PROMOTIONS (PROMO_ID, NAME, DESCRIPTION, START_DATE, EXPIRY_DATE, ELIGIBILITY_RULES, REWARD_FORMULA, STATUS)
VALUES (
        'PROMO_PCT5',
        '5% Cashback Any',
        '5% cashback any merchant amount >= 20',
        SYSTIMESTAMP - NUMTODSINTERVAL(10,'DAY'),
        SYSTIMESTAMP + NUMTODSINTERVAL(180,'DAY'),
        '{"minAmount":20}',
        '{"cashbackPercent":5,"maxCashback":15}',
        'ACTIVE'
);

INSERT INTO CPRS_PROMOTIONS (PROMO_ID, NAME, DESCRIPTION, START_DATE, EXPIRY_DATE, ELIGIBILITY_RULES, REWARD_FORMULA, STATUS)
VALUES (
        'PROMO_MERCHANT1003',
        'Flat 8% Merchant 1003',
        '8% cashback only for merchant 1003 amounts >= 30',
        SYSTIMESTAMP - NUMTODSINTERVAL(5,'DAY'),
        SYSTIMESTAMP + NUMTODSINTERVAL(60,'DAY'),
        '{"minAmount":30,"merchantIds":[1003]}',
        '{"cashbackPercent":8,"maxCashback":25}',
        'ACTIVE'
);