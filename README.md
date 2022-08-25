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

~~~xml

<dependency>
  <groupId>com.aav</groupId>
  <artifactId>jdbc</artifactId>
  <version>1.0.1</version>
</dependency>
~~~

The first dependency contains the main logic of work, the second dependency indicates which storage
will be used.

then you need to add the "@SchedulerLogAndLock" annotation to the method that will run according to
the schedule.

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

the "SchedulerLogAndLock" annotation takes three parameters as input. of which:
cron - accepts a standard cron expression (required)
lock - enables blocking between hosts (optional)
lockUntil - specifies the time to hold the lock (optional)

"lock Until" accepts as a valid value a string like "5m" where the valid unit is s (seconds), m (
minutes), h(hours).

Then you need to create a configuration to work with.

~~~java
import com.aav.jdbc.JdbcStorageAction;
import com.aav.planner.service.SchedulerPostProcessor;
import com.aav.planner.service.lock.LockAction;
import com.aav.planner.service.log.LogAction;
import javax.sql.DataSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class CustomConfig {

  @Bean
  public JdbcStorageAction jdbcStorageAction(DataSource dataSource) {
    return new JdbcStorageAction(dataSource);
  }

  @Bean
  public SchedulerPostProcessor postProcessor(
      ThreadPoolTaskScheduler threadPoolTaskScheduler,
      LogAction logAction,
      LockAction lockAction
  ) {
    return new SchedulerPostProcessor(threadPoolTaskScheduler, logAction, lockAction);
  }
~~~

You can run the application.