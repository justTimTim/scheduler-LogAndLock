package com.aav.jdbc.service;

import java.sql.SQLException;

@FunctionalInterface
public interface SqlFunction<T, R> {

  R apply(T t) throws SQLException;

}