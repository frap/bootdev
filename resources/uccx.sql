-- :name realtimestats :? :*
-- :doc RealTime ICD Statistics
SELECT *
FROM RtICDStatistics

-- :name loggedinagents :? :*
-- :doc  all loggedinagents from RT stats
SELECT loggedinagents AS agents
FROM RtICDStatistics

