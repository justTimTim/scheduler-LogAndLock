package com.aav.jdbc;

import static java.util.Objects.requireNonNull;

import javax.sql.DataSource;
import lombok.NonNull;

public class JdbcStorageClient {

  private static final String DEF_SCHEMA = "public";
  private static final String LOG_TABLE = "scheduler_log";
  private static final String LOCK_TABLE = "scheduler_lock";

  private final DataSource dataSource;
  private final String schema;
  private final String logTable;
  private final String lockTable;

  public JdbcStorageClient(@NonNull DataSource dataSource) {
    this(dataSource, DEF_SCHEMA, LOG_TABLE, LOCK_TABLE);
  }

  public JdbcStorageClient(@NonNull DataSource dataSource, @NonNull String schema) {
    this(dataSource, schema, LOG_TABLE, LOCK_TABLE);
  }

  public JdbcStorageClient(@NonNull DataSource dataSource, @NonNull String schema,
      @NonNull String logTable) {
    this(dataSource, schema, logTable, LOCK_TABLE);
  }

  public JdbcStorageClient(@NonNull DataSource dataSource, @NonNull String schema,
      @NonNull String logTable, @NonNull String lockTable) {
    requireNonNull(schema, "schema can not be null");
    requireNonNull(logTable, "logTable can not be null");
    requireNonNull(lockTable, "lockTable can not be null");
    this.dataSource = requireNonNull(dataSource, "dataSource can not be null");
    this.schema = schema;
    this.logTable = logTable;
    this.lockTable = lockTable;
  }

  public JdbcStorageAction init() {
    return new JdbcStorageAction(dataSource, schema, logTable, lockTable);
  }
}
