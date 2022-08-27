package com.aav.jdbc;

import static java.util.Objects.requireNonNull;

import com.aav.jdbc.service.AbstractLogAction;
import com.aav.jdbc.service.SqlFunction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Function;
import javax.sql.DataSource;
import lombok.NonNull;

public class JdbcStorageAction extends AbstractLogAction {

  private final DataSource dataSource;

  public JdbcStorageAction(@NonNull DataSource dataSource, @NonNull String schema,
      @NonNull String logTable, @NonNull String lockTable) {
    super(schema, logTable, lockTable);
    this.dataSource = requireNonNull(dataSource, "dataSource can not be null");
  }

  @Override
  protected <T> T executeQuery(String query, SqlFunction<PreparedStatement, T> body,
      Function<SQLException, T> exceptionHandler) {
    try (Connection connection = dataSource.getConnection()) {
      try (PreparedStatement statement = connection.prepareStatement(query)) {
        return body.apply(statement);
      } catch (SQLException e) {
        return exceptionHandler.apply(e);
      } finally {
        if (!connection.getAutoCommit()) {
          connection.commit();
        }
      }
    } catch (SQLException e) {
      return exceptionHandler.apply(e);
    }
  }
}
