package com.aav.jdbc.service;

import static java.util.Objects.requireNonNull;

import com.aav.planner.model.LockParam;
import com.aav.planner.service.lock.LockAction;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import lombok.NonNull;

public abstract class AbstractLockAction extends SqlExecutor implements LockAction {

  private final String table;

  protected AbstractLockAction(@NonNull String schema, @NonNull String lockTableName) {
    requireNonNull(schema, "schema can not be null");
    requireNonNull(lockTableName, "lockTableName can not be null");
    this.table = schema + "." + lockTableName;
  }

  @Override
  public boolean lock(LockParam param) {
    String query = "INSERT INTO " + table + " (name, lock_until, host) VALUES (?,?,?)";
    return executeQuery(query, statement -> {
      statement.setString(1, param.getName());
      statement.setTimestamp(2, Timestamp.from(param.getLockUntil()));
      statement.setString(3, param.getHost());
      return statement.executeUpdate() > 0;
    }, this::insertExceptionWithOutStackTrace);
  }

  @Override
  public boolean updateLock(LockParam param) {
    return false;
  }

  @Override
  public boolean unlock(LockParam param) {
    String query = "DELETE FROM " + table + " WHERE name = ?";
    return executeQuery(query, statement -> {
      statement.setString(1, param.getName());
      return statement.executeUpdate() > 0;
    }, this::deleteExceptionWithOutStackTrace);
  }

  @Override
  public LockParam getLockInfo(LockParam param) {
    String query = "SELECT * FROM " + table + " WHERE name = ?";
    return executeQuery(query, statement -> {
      statement.setString(1, param.getName());
      return parseResult(statement.executeQuery());
    }, this::handleQueryLockInfoException);
  }

  private LockParam parseResult(ResultSet resultSet) throws SQLException {
    LockParam response = new LockParam();
    if (resultSet.next()) {
      response.setName(resultSet.getString(1));
      Timestamp timestamp = resultSet.getTimestamp(2);
      response.setLockUntil(timestamp.toInstant());
      response.setHost(resultSet.getString(3));
    }
    return response;
  }


  private LockParam handleQueryLockInfoException(SQLException e) {
    log.error(
        "An exception occurred while getting data from the database about the current lock", e);
    return null;
  }
}
