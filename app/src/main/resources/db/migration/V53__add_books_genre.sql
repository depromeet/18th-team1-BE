ALTER TABLE books
    ADD COLUMN IF NOT EXISTS genre TEXT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'books'
          AND column_name = 'aladin_category_name'
    ) THEN
        UPDATE books
        SET genre =
            CASE
                WHEN aladin_category_name LIKE '%과학소설(SF)%' THEN 'SF'
                WHEN aladin_category_name LIKE '%추리/미스터리소설%' THEN '추리･미스터리'
                WHEN aladin_category_name LIKE '%호러.공포소설%'
                    OR aladin_category_name LIKE '%액션/스릴러소설%' THEN '공포･스릴러'
                WHEN aladin_category_name LIKE '%판타지/환상문학%' THEN '판타지'
                WHEN aladin_category_name LIKE '%로맨스소설%' THEN '로맨스'
                WHEN aladin_category_name LIKE '%역사소설%' THEN '역사'
                WHEN aladin_category_name LIKE '%무협소설%' THEN '무협'
                WHEN aladin_category_name LIKE '%>시>%'
                    OR aladin_category_name LIKE '%에세이%'
                    OR aladin_category_name LIKE '%>우리나라 옛글>산문%'
                    OR aladin_category_name LIKE '%>우리나라 옛글>시가%' THEN '시･에세이'
                ELSE '일반문학'
            END
        WHERE genre IS NULL;
    ELSE
        UPDATE books
        SET genre = '일반문학'
        WHERE genre IS NULL;
    END IF;
END $$;

ALTER TABLE books
    ALTER COLUMN genre SET NOT NULL;
