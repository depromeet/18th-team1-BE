UPDATE tags
SET label = btrim(regexp_replace(label, '\s*순간$', ''))
WHERE type = 'EMOTION'
  AND label ~ '\s*순간$';
