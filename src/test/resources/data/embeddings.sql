CREATE OR REPLACE FUNCTION generate_random_vector_4096()
    RETURNS VECTOR(4096) AS
$$
DECLARE
    result VECTOR(4096);
BEGIN
    SELECT ARRAY(
                   SELECT random()
                   FROM generate_series(1, 4096)
           )::VECTOR(4096)
    INTO result;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

INSERT INTO wiki_stem_corpus (embedding_id, embedding, text, metadata)
VALUES ('725af2f1-10f5-4aea-a119-349a34fdf000'::UUID,
        generate_random_vector_4096(),
        'Nanomaterials are materials with structural features smaller than 100 nanometers.',
        '{
          "page_title": "Introduction to Nanomaterials"
        }'),

       ('725af2f1-10f5-4aea-a119-349a34fdf001'::UUID,
        generate_random_vector_4096(),
        'Two-dimensional (2D) nanomaterials, like graphene, have unique properties.',
        '{
          "page_title": "Two dimensional (2D) nanomaterials"
        }'),

       ('725af2f1-10f5-4aea-a119-349a34fdf002'::UUID,
        generate_random_vector_4096(),
        'Graphene is a single layer of carbon atoms arranged in a hexagonal lattice.',
        '{
          "page_title": "Properties of Graphene"
        }'),

       ('725af2f1-10f5-4aea-a119-349a34fdf003'::UUID,
        generate_random_vector_4096(),
        'Quantum dots are semiconductor nanocrystals that possess quantum mechanical properties.',
        '{
          "page_title": "Quantum Dots and Their Applications"
        }'),

       ('725af2f1-10f5-4aea-a119-349a34fdf004'::UUID,
        generate_random_vector_4096(),
        'Nanotechnology involves the manipulation of matter on an atomic, molecular, and supramolecular scale.',
        '{
          "page_title": "Overview of Nanotechnology"
        }');
