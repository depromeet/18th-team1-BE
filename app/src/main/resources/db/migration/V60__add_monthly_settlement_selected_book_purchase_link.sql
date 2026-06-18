ALTER TABLE monthly_settlements
    ADD COLUMN IF NOT EXISTS selected_book_purchase_link TEXT;

UPDATE monthly_settlements
SET selected_book_purchase_link = books.aladin_link
FROM books
WHERE monthly_settlements.selected_book_id = books.id
  AND monthly_settlements.selected_book_purchase_link IS NULL;
