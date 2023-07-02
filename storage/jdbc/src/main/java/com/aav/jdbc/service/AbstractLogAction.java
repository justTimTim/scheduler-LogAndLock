package com.aav.jdbc.service;

import static java.util.Objects.requireNonNull;

import com.aav.planner.model.ScheduleParams;
import com.aav.planner.service.log.LogAction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.NonNull;

public abstract class AbstractLogAction extends AbstractLockAction implements LogAction {

  private final String table;
  private final ObjectMapper objectMapper = new ObjectMapper();

  protected AbstractLogAction(@NonNull String schema, @NonNull String logTable,
      @NonNull String lockTable) {
    super(schema, lockTable);
    requireNonNull(schema, "schema can not be null");
    requireNonNull(logTable, "logTable can not be null");
    this.table = schema + "." + logTable;

    objectMapper.findAndRegisterModules();
  }

  @Override
  public boolean create(UUID id, String name) {
    String query = "INSERT INTO " + table + " (id, name, start) VALUES (?,?,?)";
    return executeQuery(query, statement -> {
      statement.setObject(1, id);
      statement.setString(2, name);
      statement.setTimestamp(3, Timestamp.from(Instant.now()));
      return statement.executeUpdate() > 0;
    }, this::insertExceptionWithOutStackTrace);
  }

  @Override
  public boolean update(UUID id, Map<String, Object> info) {
    String query = "UPDATE " + table + " SET finish = ?, info = (?::json) WHERE id = ?";
    return executeQuery(query, statement -> {
      statement.setTimestamp(1, Timestamp.from(Instant.now()));
      statement.setString(2, mapToString(info));
      statement.setObject(3, id);
      return statement.executeUpdate() > 0;
    }, this::updateExceptionWithOutStackTrace);
  }

  @Override
  public ScheduleParams getLastRow(String name) {
    String query = "SELECT * FROM " + table + " WHERE finish IS NOT NULL AND name = ?"
        + " ORDER BY finish DESC limit 1";
    return executeQuery(query, statement -> {
      statement.setString(1, name);
      return parseResultLog(statement.executeQuery());
    }, this::handleGetLastRowException);

  }

  @Override
  public Optional<UUID> replace(String name) {
    Optional<String> last = getLastRowId(name);

    return Optional.of(last)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(updateRow())
        .orElseGet(createRow(name));
  }

  private Supplier<Optional<UUID>> createRow(String name) {
    return () -> {
      UUID id = UUID.randomUUID();
      if (create(id, name)) {
        return Optional.of(id);
      } else {
        return Optional.empty();
      }
    };
  }

  private Function<String, Optional<UUID>> updateRow() {
    return el -> {
      UUID id = UUID.fromString(el);
      if (replaceLastRow(id)) {
        return Optional.of(id);
      }
      return Optional.empty();
    };
  }

  private boolean replaceLastRow(UUID id) {
    String query = "UPDATE " + table + " SET start = ?, finish = ?, info = ? "
        + "WHERE id = ?";
    return executeQuery(query, statement -> {
      statement.setTimestamp(1, Timestamp.from(Instant.now()));
      statement.setNull(2, java.sql.Types.NULL);
      statement.setNull(3, java.sql.Types.NULL);
      statement.setObject(4, id);
      return statement.executeUpdate() > 0;
    }, this::updateExceptionWithOutStackTrace);
  }

  private String mapToString(Map<String, Object> info) {
    if (info != null) {
      try {
        return objectMapper.writeValueAsString(info);
      } catch (JsonProcessingException e) {
        log.error("Map parse exception", e);
      }
    }
    return null;
  }

  private Optional<String> getLastRowId(String name) {
    var query = "SELECT id FROM " + table + " WHERE finish IS NOT NULL AND name = ?"
        + " ORDER BY finish DESC limit 1";
    return executeQuery(query, statement -> {
      statement.setString(1, name);
      return getIdFromResultSet(statement.executeQuery());
    }, this::handleGetIdException);
  }

  private Optional<String> getIdFromResultSet(ResultSet resultSet) throws SQLException {
    if (resultSet.next()) {
      return Optional.of(resultSet.getString(1));
    }

    return Optional.empty();
  }

  @SuppressWarnings(value = "unchecked")
  private ScheduleParams parseResultLog(ResultSet resultSet) throws SQLException {

    ScheduleParams response = new ScheduleParams();
    if (resultSet.next()) {

      response.setStart(resultSet.getTimestamp(3));
      response.setEnd(resultSet.getTimestamp(4));
      String info = resultSet.getString(5);
      if (info != null) {
        try {
          response.setInfo(objectMapper.readValue(info, HashMap.class));
        } catch (JsonProcessingException e) {
          log.error("Json parse exception", e);
        }
      }
    }

    return response;
  }

  ScheduleParams handleGetLastRowException(SQLException e) {
    log.error("unexpected exception when getting data about the last record in the database", e);
    return null;
  }

  Optional<String> handleGetIdException(SQLException e) {
    log.error("unexpected exception when getting id about the last record in the database", e);
    return Optional.empty();
  }

}
