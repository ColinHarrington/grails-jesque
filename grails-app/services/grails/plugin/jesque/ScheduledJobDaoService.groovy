package grails.plugin.jesque

import redis.clients.jedis.Jedis

class ScheduledJobDaoService {

    def redisService

    void save( ScheduledJob scheduledJob ) {
        redisService.withRedis { Jedis redis ->
            save( redis, scheduledJob )
        }
    }

    void save( Jedis redis, ScheduledJob scheduledJob ) {
        redis.hmset(scheduledJob.redisKey, scheduledJob.toRedisHash())
        redis.sadd(ScheduledJob.JOB_INDEX, scheduledJob.name)
    }

    ScheduledJob findByName(String name) {
        redisService.withRedis { Jedis redis ->
            findByName(redis, name)
        } as ScheduledJob
    }

    ScheduledJob findByName(Jedis redis, String name) {
        def scheduledJob = ScheduledJob.fromRedisHash( redis.hgetAll( ScheduledJob.getRedisKeyForName(name) ) )
        scheduledJob.trigger = Trigger.fromRedisHash( redis.hgetAll( Trigger.getRedisKeyForJobName(name) ) )

        scheduledJob
    }

    List<ScheduledJob> getAll() {
        redisService.withRedis { Jedis redis ->
            redis.smembers(ScheduledJob.JOB_INDEX).collect{ jobName ->
                findByName(redis, jobName)
            }
        } as List<ScheduledJob>
    }
}

