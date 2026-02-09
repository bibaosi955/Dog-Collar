package com.acme.collar;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class FlywayMigrationResourceTest {

  @Test
  void v1InitSqlExistsAndEnablesPostgis() throws Exception {
    try (InputStream in =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("db/migration/V1__init.sql")) {
      assertNotNull(in, "classpath 中应存在 db/migration/V1__init.sql");

      String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(
          sql.contains("CREATE EXTENSION IF NOT EXISTS postgis"),
          "V1__init.sql 必须包含 CREATE EXTENSION IF NOT EXISTS postgis");
    }
  }
}
