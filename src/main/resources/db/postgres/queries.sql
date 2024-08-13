-- :name count-wiki-stem-items :query :one
-- :doc Counts number of ingested items of Wiki STEM Corpus
SELECT COUNT(*)
FROM wiki_stem_corpus;