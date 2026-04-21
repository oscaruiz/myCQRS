-- Drop the legacy single-author column. No production data exists.
ALTER TABLE book_entity DROP COLUMN author;

-- Join table. No FK to author_entity by design — cross-aggregate consistency
-- lives in events, not in the schema.
CREATE TABLE book_authors (
    book_id UUID NOT NULL,
    author_id UUID NOT NULL,
    PRIMARY KEY (book_id, author_id),
    CONSTRAINT fk_book_authors_book FOREIGN KEY (book_id)
        REFERENCES book_entity(id) ON DELETE CASCADE
);

CREATE INDEX idx_book_authors_author ON book_authors (author_id);
