-- SRH CinePass: Seed 105 shows and 20,370 seats
-- Safe to rerun: existing shows and seats are not duplicated.

START TRANSACTION;

INSERT INTO shows (movie_id, theater_id, show_time, ticket_price)
SELECT
    m.id,
    t.id,
    TIMESTAMP(
        GREATEST(
            COALESCE(m.release_date, CURDATE()),
            DATE_ADD(CURDATE(), INTERVAL 1 DAY)
        ),
        CASE MOD(m.id, 4)
            WHEN 0 THEN '10:00:00'
            WHEN 1 THEN '14:00:00'
            WHEN 2 THEN '18:00:00'
            ELSE '21:00:00'
        END
    ),
    180.00
FROM movie m
CROSS JOIN theatres t
WHERE m.id IN (3,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24)
AND t.id IN (1,5,4,3,23)
AND NOT EXISTS (
    SELECT 1
    FROM shows s
    WHERE s.movie_id = m.id
      AND s.theater_id = t.id
);

INSERT INTO seats (show_id, seat_number, booked)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 250
)
SELECT s.id, CONCAT('S', seq.n), FALSE
FROM shows s
JOIN theatres t ON t.id = s.theater_id
CROSS JOIN seq
WHERE s.movie_id IN (3,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24)
AND t.id IN (1,5,4,3,23)
AND seq.n <= t.total_seats
AND NOT EXISTS (
    SELECT 1
    FROM seats existing
    WHERE existing.show_id = s.id
      AND existing.seat_number = CONCAT('S', seq.n)
);

COMMIT;
