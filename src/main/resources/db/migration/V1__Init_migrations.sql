CREATE EXTENSION IF NOT EXISTS "vector";

CREATE TABLE wiki_stem_corpus
(
    embedding_id UUID NOT NULL PRIMARY KEY,
    embedding    VECTOR(4096),
    text         TEXT,
    metadata     JSONB
);