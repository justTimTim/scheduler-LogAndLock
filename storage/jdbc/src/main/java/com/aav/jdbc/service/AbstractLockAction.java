package com.aav.jdbc.service;

import com.aav.planner.model.LockParam;
import com.aav.planner.service.lock.LockAction;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.lang.NonNull;

public abstract class AbstractLockAction extends SqlExecutor implements LockAction {

  private final String table;

  protected AbstractLockAction(@org.springframework.lang.NonNull String schema,
      @NonNull String lockTable) {
    this.table = schema + "." + lockTable;
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
    String query = "UPDATE " + table + " SET lock_until = ? WHERE name = ? AND host = ?";
    return executeQuery(query, statement -> {
      statement.setTimestamp(1, Timestamp.from(param.getLockUntil()));
      statement.setString(2, param.getName());
      statement.setString(3, param.getHost());
      return statement.executeUpdate() > 0;
    }, this::updateExceptionWithOutStackTrace);
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
