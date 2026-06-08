INSERT INTO quote_candidates (
    book_id,
    content,
    source,
    status,
    created_at,
    updated_at
)
SELECT
    q.book_id,
    q.content,
    'EXISTING_QUOTE',
    'PENDING',
    q.created_at,
    q.updated_at
FROM quotes q
WHERE q.deleted_at IS NULL
ON CONFLICT (book_id, content) DO NOTHING;
