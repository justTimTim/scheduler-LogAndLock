scheduler-LogAndLock
========
![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)

"scheduler-LogAndLock" provides the following capabilities:
- create a schedule for running methods
- pass parameters to these methods
- save execution results
- keep a log of launches
- block simultaneous execution of a task on more than one application instance.
<br>
At the moment, only SQL databases are implemented as storage.

### Get Started

Install the project and add the following dependencies to your project.

~~~xml

<dependency>
  <groupId>com.aav</groupId>
  <artifactId>planner</artifactId>
  <version>1.0.3</version>
</dependency>
~~~

This dependency implements the work of the scheduler.

You then need to add the "@SchedulerLogAndLock" annotation to the method, which will run
according to your schedule.

Example

~~~java
import com.aav.planner.annotation.SchedulerLogAndLock;
import com.aav.planner.model.ScheduleParams;
...

@SchedulerLogAndLock(cron = "0 0/1 * * * *", lock = true, lockUntil = "10m")
public Map<String, Object> example(ScheduleParams params){

    //any logic

    HashMap<String, Object> map=new HashMap<>();
    map.put("stamp",System.currentTimeMillis());
    return map;
    }
~~~

The "SchedulerLogAndLock" annotation takes three parameters as input. <br>
of which: <br>
cron - accepts a standard cron expression (required) <br>
lock - enables locking between hosts (optional) <br>
lockUntil - specifies the time to hold the lock (optional)<br>
replace - overwrite last entry for running method (optional)

cron can also accept the "-" parameter, in which case the schedule will not be created.
If you pass parameters through application.yml you can disable your job at any time.
cron = "${app.scheduler.test}" - an example of passing cron expressions through application file
"lockUntil" accepts as a valid value a string like "5m", where the valid unit is s (seconds), m (
minutes), h(hours).

"ScheduleParams" returns the parameters of the last run. Contains information about the beginning
and end of work. And there is a Map with useful data. When the work is finished, you can add any
information in the Map that you need the next time you run it (such as the timestamp).

### storage

#### jdbc

When working with a SQL database, you need to create a table for the job log and for the lock.

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

You need to add a dependency.

~~~xml

<dependency>
  <groupId>com.aav</groupId>
  <artifactId>jdbc</artifactId>
  <version>1.0.2</version>
</dependency>
~~~

By default, the following schema and table names are set.

~~~java
    SCHEMA="public";
    LOG_TABLE="scheduler_log";
    LOCK_TABLE="scheduler_lock";
~~~

But you can specify your values through the "JdbcStorageClient" constructor when creating the bean.
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
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class CustomConfig {

  @Bean
  public JdbcStorageAction jdbcStorageAction(DataSource dataSource) {
    //return new JdbcStorageClient(dataSource, "my_schema").init(); 
    return new JdbcStorageClient(dataSource).init();
  }

  @Bean
  public SchedulerPostProcessor postProcessor(
      ThreadPoolTaskScheduler threadPoolTaskScheduler,
      LogAction logAction,
      LockAction lockAction,
      Environment env
  ) {
    return new SchedulerPostProcessor(threadPoolTaskScheduler, logAction, lockAction, env);
  }
}
~~~

By default, "ThreadPoolTaskScheduler" has a value of 1. Accordingly, you must understand the number of tasks,
which you run at the same time, since they will be executed sequentially unless you increase
the number of available threads in the pool. <br>
For example, if you have 4 jobs that run at the same time,
but at the same time, 2 instances of your service are guaranteed to be raised, you can set the number of threads equal to 2.
In this case, 2 tasks will be simultaneously executed on each instance.

Setting example<br>
setPoolSize - sets the number of threads in the pool

~~~java
  @Bean
public ThreadPoolTaskScheduler threadPoolTaskScheduler(){
    ThreadPoolTaskScheduler threadPoolTaskScheduler
    =new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.setPoolSize(10);
    threadPoolTaskScheduler.setThreadNamePrefix("ThreadPoolTaskScheduler");
    return threadPoolTaskScheduler;
    }
~~~

You can run the application.
