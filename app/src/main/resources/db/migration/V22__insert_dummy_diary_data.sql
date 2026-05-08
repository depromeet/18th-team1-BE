INSERT INTO users (
    id,
    provider,
    provider_id,
    email,
    nickname,
    profile_image_id,
    status,
    last_login_at,
    deleted_at,
    created_at,
    updated_at
) OVERRIDING SYSTEM VALUE VALUES (
    9001,
    'KAKAO',
    'dummy-kakao-provider-id',
    'dummy@example.com',
    '더미사용자',
    NULL,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

INSERT INTO images (
    id,
    url,
    created_at
) OVERRIDING SYSTEM VALUE VALUES (
    9002,
    'https://cdn.example.com/dummy-diary-image.png',
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

INSERT INTO diaries (
    id,
    user_id,
    quote_id,
    emotion_intensity,
    content,
    created_at,
    updated_at,
    deleted_at
) OVERRIDING SYSTEM VALUE VALUES (
    9003,
    9001,
    1,
    80,
    '더미 일기 데이터입니다.',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
) ON CONFLICT DO NOTHING;

INSERT INTO image_owners (
    image_id,
    owner_type,
    owner_id,
    sort_order
) VALUES (
    9002,
    'DIARY',
    9003,
    0
) ON CONFLICT DO NOTHING;

SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users), true);
SELECT setval('images_id_seq', (SELECT COALESCE(MAX(id), 1) FROM images), true);
SELECT setval('diaries_id_seq', (SELECT COALESCE(MAX(id), 1) FROM diaries), true);
