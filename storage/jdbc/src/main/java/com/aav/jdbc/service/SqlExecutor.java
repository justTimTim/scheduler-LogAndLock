package com.aav.jdbc.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SqlExecutor {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected abstract <T> T executeQuery(String query, SqlFunction<PreparedStatement, T> body,
      Function<SQLException, T> exceptionHandler
  );

  boolean insertExceptionWithOutStackTrace(SQLException e) {
    log.error("exception while saving to db");
    log.error(e.getMessage());
    return false;
  }

  boolean insertException(SQLException e) {
    log.error("exception while saving to db", e);
    return false;
  }

  boolean deleteExceptionWithOutStackTrace(SQLException e) {
    log.error("exception on deletion from database");
    log.error(e.getMessage());
    return false;
  }

  boolean updateExceptionWithOutStackTrace(SQLException e) {
    log.error("exception when trying to update data in database");
    log.error(e.getMessage());
    return false;
  }
}
