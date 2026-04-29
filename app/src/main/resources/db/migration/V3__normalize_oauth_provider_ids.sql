UPDATE users
SET provider_id = regexp_replace(provider_id, '^kakao_', '')
WHERE provider = 'KAKAO'
  AND provider_id LIKE 'kakao\_%' ESCAPE '\';

UPDATE users
SET provider_id = regexp_replace(provider_id, '^google_', '')
WHERE provider = 'GOOGLE'
  AND provider_id LIKE 'google\_%' ESCAPE '\';

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_provider_id_unique;

ALTER TABLE users
    ADD CONSTRAINT users_provider_provider_id_unique UNIQUE (provider, provider_id);
