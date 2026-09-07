-- Record why a request was classified as bot traffic, not only that it was.
-- BotDetector now sums weighted header signals instead of matching the
-- User-Agent alone, and the weights can only be tuned by reading the score
-- distribution these columns capture.
--
-- Both nullable and unbackfilled on purpose: the headers behind a past decision
-- were never stored, so an invented score would be indistinguishable from a
-- measured one. is_bot keeps its meaning and every statistics query that
-- filters on it is unaffected.
ALTER TABLE request_statistics ADD COLUMN bot_score INTEGER;
ALTER TABLE request_statistics ADD COLUMN bot_signals VARCHAR(128);
