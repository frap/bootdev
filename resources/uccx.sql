-- :name realtimestats :? :*
-- :doc RealTime ICD Statistics
SELECT *
FROM RtICDStatistics

-- :name loggedinagents :? :*
-- :doc  all loggedinagents from RT stats
SELECT loggedinagents AS agents
FROM RtICDStatistics

-- :name csqlist :? :*
-- :doc UCCX CSQ list
select CSQNAME from contactservicequeue where active = 'T' and queuetype = 0

-- :name realtime-queues :? :*
-- :doc select all data from RealTime CSQ Summary
select *,(callshandled + callsdequeued) as callshandeq,(callsabandoned - callsdequeued) as callsabdeq
FROM RtCSQsSummary

-- :name agenttoqueues :? :*
-- :doc select all agents assigned to queues
SELECT r.resourceLoginId,csq.csqname FROM resource AS r
INNER JOIN resourceskillmapping AS rsm ON rsm.resourceskillmapid=r.resourceskillmapid
INNER JOIN skillgroup AS s ON rsm.skillid=s.skillid
INNER JOIN contactservicequeue AS csq ON csq.skillgroupid=s.skillgroupid
WHERE rsm.active='t' AND csq.active = 't' AND r.active = 't' AND s.active='t'
UNION SELECT r.resourceLoginId,csq.csqname FROM resource AS r
INNER JOIN resourcegroup AS rg ON rg.resourcegroupid=r.resourcegroupid
INNER JOIN contactservicequeue AS csq ON csq.resourcegroupid=rg.resourcegroupid
WHERE rg.active='t' AND csq.active = 't' AND r.active = 't'

-- :name agenttoqueue :? :*
-- :doc select all agent data from queues
SELECT r.resourceLoginId,csq.csqname FROM resource AS r
INNER JOIN resourceskillmapping AS rsm ON rsm.resourceskillmapid=r.resourceskillmapid
INNER JOIN skillgroup AS s ON rsm.skillid=s.skillid
INNER JOIN contactservicequeue AS csq ON csq.skillgroupid=s.skillgroupid
WHERE rsm.active='t' AND csq.active = 't' AND r.active = 't' AND s.active='t'


-- :name resource :? :*
-- :doc select active resources
SELECT r.resourceLoginId FROM resource AS r
WHERE r.active = 't'

-- :name directin :? :*
-- :doc select number of direct inward calls per resource
select count(*) as callsdirectin,R.resourceloginid  from ContactCallDetail CCD join Resource R on (R.resourceid = CCD.destinationid and R.profileid = CCD.profileid) where CCD.applicationid = -1 AND CCD.startdatetime > :starttime AND CCD.origprotocolcallref != '' group by R.resourceloginid

-- :name directout :? :*
-- :doc select number of direct outward calls per resource
select count(*) as callsdirectout,R.resourceloginid  from ContactCallDetail CCD join Resource R on (R.resourceid = CCD.originatorid and R.profileid = CCD.profileid) where CCD.applicationid = -1 AND CCD.startdatetime > :starttime group by R.resourceloginid


-- :name gos :? :*
-- :doc Get GoS from starttime (midnight local converted to UTC)
select csq.csqname,cqd.metservicelevel,cqd.disposition, count(*) as numsl from contactqueuedetail cqd join contactservicequeue csq on (csq.recordID = cqd.targetid) join contactcalldetail cdd on (cdd.sessionid = cqd.sessionid)  where cdd.startdatetime > :starttime and cdd.contacttype != 5 and cdd.sessionseqnum = 0 group by csq.csqname,cqd.metservicelevel,cqd.disposition


-- :name sp-abandoned :? :*
-- :doc Abandoned Calls stored procedure
call sp_abandoned_calls_activity(:yesterday , :now ,'0',null)

-- :name sp-csq-activity
-- :doc All CSQ activity via stored procedure
call sp_csq_activity( :yesterday, :now, '0', null)

-- :name sp-csq-activity-csq
-- :doc per CSQ activity via stored procedure
call sp_csq_activity( :yesterday, :now, '0', :csq, null)
