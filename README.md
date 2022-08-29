scheduler-LogAndLock
========
![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)

"scheduler-LogAndLock" provides the ability to create your own schedule, keep a log of launches. It
also allows you to block the simultaneous launch of a job on more than one host.
<br>
At the moment, only SQL databases are implemented as storage.

### Get Started

Install the project and add the following dependencies to your project.

~~~xml

<dependency>
  <groupId>com.aav</groupId>
  <artifactId>planner</artifactId>
  <version>1.0.1</version>
</dependency>
~~~

This dependency implements the work of the scheduler.

Then you need to add the "@SchedulerLogAndLock" annotation to the method that will run according to
the schedule.

Example

~~~java
import com.aav.planner.annotation.SchedulerLogAndLock;
import com.aav.planner.model.ScheduleParams;
...

@SchedulerLogAndLock(cron = "0 0/1 * * * *", lock = true, lockUntil = "10m")
public Map<String, Object> example(ScheduleParams params){

    //any logic

    Map<String, Object> map=new HashMap<>();
    map.put("stamp",System.currentTimeMillis());
    map.put("id",12345);
    return map;
    }
~~~

The "SchedulerLogAndLock" annotation takes three parameters as input. <br>
of which: <br>
cron - accepts a standard cron expression (required) <br>
lock - enables blocking between hosts (optional) <br>
lockUntil - specifies the time to hold the lock (optional)

"lock Until" accepts as a valid value a string like "5m" where the valid unit is s (seconds), m (
minutes), h(hours).

"ScheduleParams" returns the parameters of the last run. Contains information about the beginning
and end of the job. And there is a map with useful data. When the job is finished, you can add any
information you need to the map to the map when you start it again.

### storage

#### jdbc

When working with a SQL database, you must create a table for logging the execution of a job and a
table for locking.

~~~sql
create table public.scheduler_log
(
    id     uuid not null
        constraint scheduler_log_pk
            primary key,
    name   varchar(100),
    start  timestamp with time zone,
    finish timestamp with time zone,
    info   json
);
comment on table public.scheduler_log is 'job execution log';

create table public.scheduler_lock
(
    name       varchar(100) not null
        constraint scheduler_lock_pk
            primary key,
    lock_until timestamp,
    host       varchar(100)
);
comment on table public.scheduler_lock is 'job lock journal';
~~~

And dependency

~~~xml

<dependency>
  <groupId>com.aav</groupId>
  <artifactId>jdbc</artifactId>
  <version>1.0.1</version>
</dependency>
~~~

By default, the following schema and table names are set.

~~~java
    SCHEMA="public";
    LOG_TABLE="scheduler_log";
    LOCK_TABLE="scheduler_lock";
~~~

But you can specify your own values through the "JdbcStorageClient" constructor when creating the
bean.
______________________________________

Then you need to create a configuration to work with.

~~~java
import com.aav.jdbc.JdbcStorageAction;
import com.aav.jdbc.JdbcStorageClient;
import com.aav.planner.service.SchedulerPostProcessor;
import com.aav.planner.service.lock.LockAction;
import com.aav.planner.service.log.LogAction;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class CustomConfig {

  @Bean
  public JdbcStorageAction jdbcStorageAction(DataSource dataSource) {
    return new JdbcStorageClient(dataSource, "my_schema").init();
  }

  @Bean
  public SchedulerPostProcessor postProcessor(
      ThreadPoolTaskScheduler threadPoolTaskScheduler,
      LogAction logAction,
      LockAction lockAction
  ) {
    return new SchedulerPostProcessor(threadPoolTaskScheduler, logAction, lockAction);
  }
}
~~~

By default, "ThreadPoolTaskScheduler" has a value of 1. Accordingly, you must understand how many
tasks you run at the same time, as they will be executed sequentially if you do not increase the
number of available threads in the pool.

Example
~~~java
  @Bean
  public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
    ThreadPoolTaskScheduler threadPoolTaskScheduler
        = new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.setPoolSize(10);
    threadPoolTaskScheduler.setThreadNamePrefix("ThreadPoolTaskScheduler");
    return threadPoolTaskScheduler;
  }
~~~

You can run the application.