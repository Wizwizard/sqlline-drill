/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Modified BSD License
// (the "License"); you may not use this file except in compliance with
// the License. You may obtain a copy of the License at:
//
// http://opensource.org/licenses/BSD-3-Clause
*/
package sqlline;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import jline.console.UserInterruptException;
import jline.console.history.History;

/**
 * Collection of available commands.
 */
public class Commands {
  private static final String[] METHODS = {
    "allProceduresAreCallable",
    "allTablesAreSelectable",
    "dataDefinitionCausesTransactionCommit",
    "dataDefinitionIgnoredInTransactions",
    "doesMaxRowSizeIncludeBlobs",
    "getCatalogSeparator",
    "getCatalogTerm",
    "getDatabaseProductName",
    "getDatabaseProductVersion",
    "getDefaultTransactionIsolation",
    "getDriverMajorVersion",
    "getDriverMinorVersion",
    "getDriverName",
    "getDriverVersion",
    "getExtraNameCharacters",
    "getIdentifierQuoteString",
    "getMaxBinaryLiteralLength",
    "getMaxCatalogNameLength",
    "getMaxCharLiteralLength",
    "getMaxColumnNameLength",
    "getMaxColumnsInGroupBy",
    "getMaxColumnsInIndex",
    "getMaxColumnsInOrderBy",
    "getMaxColumnsInSelect",
    "getMaxColumnsInTable",
    "getMaxConnections",
    "getMaxCursorNameLength",
    "getMaxIndexLength",
    "getMaxProcedureNameLength",
    "getMaxRowSize",
    "getMaxSchemaNameLength",
    "getMaxStatementLength",
    "getMaxStatements",
    "getMaxTableNameLength",
    "getMaxTablesInSelect",
    "getMaxUserNameLength",
    "getNumericFunctions",
    "getProcedureTerm",
    "getSchemaTerm",
    "getSearchStringEscape",
    "getSQLKeywords",
    "getStringFunctions",
    "getSystemFunctions",
    "getTimeDateFunctions",
    "getURL",
    "getUserName",
    "isCatalogAtStart",
    "isReadOnly",
    "nullPlusNonNullIsNull",
    "nullsAreSortedAtEnd",
    "nullsAreSortedAtStart",
    "nullsAreSortedHigh",
    "nullsAreSortedLow",
    "storesLowerCaseIdentifiers",
    "storesLowerCaseQuotedIdentifiers",
    "storesMixedCaseIdentifiers",
    "storesMixedCaseQuotedIdentifiers",
    "storesUpperCaseIdentifiers",
    "storesUpperCaseQuotedIdentifiers",
    "supportsAlterTableWithAddColumn",
    "supportsAlterTableWithDropColumn",
    "supportsANSI92EntryLevelSQL",
    "supportsANSI92FullSQL",
    "supportsANSI92IntermediateSQL",
    "supportsBatchUpdates",
    "supportsCatalogsInDataManipulation",
    "supportsCatalogsInIndexDefinitions",
    "supportsCatalogsInPrivilegeDefinitions",
    "supportsCatalogsInProcedureCalls",
    "supportsCatalogsInTableDefinitions",
    "supportsColumnAliasing",
    "supportsConvert",
    "supportsCoreSQLGrammar",
    "supportsCorrelatedSubqueries",
    "supportsDataDefinitionAndDataManipulationTransactions",
    "supportsDataManipulationTransactionsOnly",
    "supportsDifferentTableCorrelationNames",
    "supportsExpressionsInOrderBy",
    "supportsExtendedSQLGrammar",
    "supportsFullOuterJoins",
    "supportsGroupBy",
    "supportsGroupByBeyondSelect",
    "supportsGroupByUnrelated",
    "supportsIntegrityEnhancementFacility",
    "supportsLikeEscapeClause",
    "supportsLimitedOuterJoins",
    "supportsMinimumSQLGrammar",
    "supportsMixedCaseIdentifiers",
    "supportsMixedCaseQuotedIdentifiers",
    "supportsMultipleResultSets",
    "supportsMultipleTransactions",
    "supportsNonNullableColumns",
    "supportsOpenCursorsAcrossCommit",
    "supportsOpenCursorsAcrossRollback",
    "supportsOpenStatementsAcrossCommit",
    "supportsOpenStatementsAcrossRollback",
    "supportsOrderByUnrelated",
    "supportsOuterJoins",
    "supportsPositionedDelete",
    "supportsPositionedUpdate",
    "supportsSchemasInDataManipulation",
    "supportsSchemasInIndexDefinitions",
    "supportsSchemasInPrivilegeDefinitions",
    "supportsSchemasInProcedureCalls",
    "supportsSchemasInTableDefinitions",
    "supportsSelectForUpdate",
    "supportsStoredProcedures",
    "supportsSubqueriesInComparisons",
    "supportsSubqueriesInExists",
    "supportsSubqueriesInIns",
    "supportsSubqueriesInQuantifieds",
    "supportsTableCorrelationNames",
    "supportsTransactions",
    "supportsUnion",
    "supportsUnionAll",
    "usesLocalFilePerTable",
    "usesLocalFiles",
  };

  private final SqlLine sqlLine;

  Commands(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  public void metadata(String line, DispatchCallback callback) {
    sqlLine.debug(line);

    String[] parts = sqlLine.split(line);
    if (parts == null || parts.length == 0) {
      dbinfo("", callback);
      return;
    }

    List<Object> params = new LinkedList<Object>(Arrays.asList(parts));
    params.remove(0);
    params.remove(0);
    sqlLine.debug(params.toString());
    metadata(parts[1], params, callback);
  }

  public void metadata(
      String cmd, List<Object> argList, DispatchCallback callback) {
    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }

    try {
      Method[] methods = sqlLine.getDatabaseMetaData().getClass().getMethods();
      Set<String> methodNames = new TreeSet<String>();
      Set<String> methodNamesUpper = new TreeSet<String>();
      for (Method method : methods) {
        methodNames.add(method.getName());
        methodNamesUpper.add(method.getName().toUpperCase());
      }

      if (!methodNamesUpper.contains(cmd.toUpperCase())) {
        sqlLine.error(sqlLine.loc("no-such-method", cmd));
        sqlLine.error(sqlLine.loc("possible-methods"));
        for (String methodName : methodNames) {
          sqlLine.error("   " + methodName);
        }
        callback.setToFailure();
        return;
      }

      Object res = sqlLine.getReflector().invoke(sqlLine.getDatabaseMetaData(),
          DatabaseMetaData.class, cmd, argList);
      if (res instanceof ResultSet) {
        ResultSet rs = (ResultSet) res;
        try {
          sqlLine.print(rs, callback);
        } finally {
          rs.close();
        }
      } else if (res != null) {
        sqlLine.output(res.toString());
      }
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
    callback.setToSuccess();
  }

  public void history(String line, DispatchCallback callback) {
    int index = 1;
    for (History.Entry entry : sqlLine.getConsoleReader().getHistory()) {
      index++;
      sqlLine.output(
          sqlLine.getColorBuffer().pad(index + ".", 6)
              .append(entry.toString()));
    }
    callback.setToSuccess();
  }

  String arg1(String line, String paramName) {
    return arg1(line, paramName, null);
  }

  String arg1(String line, String paramName, String def) {
    String[] ret = sqlLine.split(line);

    if (ret == null || ret.length != 2) {
      if (def != null) {
        return def;
      }
      throw new IllegalArgumentException(
          sqlLine.loc("arg-usage",
              ret == null || ret.length == 0 ? "" : ret[0], paramName));
    }
    return ret[1];
  }

  /**
   * Constructs a list of string parameters for a metadata call.
   * <p/>
   * <p>The number of items is equal to the number of items in the
   * <tt>strings</tt> parameter, typically three (catalog, schema, table
   * name).
   * <p/>
   * <p>Parses the command line, and assumes that the the first word is
   * a compound identifier. If the compound identifier has fewer parts
   * than required, fills from the right.
   * <p/>
   * <p>The result is a mutable list of strings.
   *
   *
   * @param line          Command line
   * @param paramName     Name of parameter being read from command line
   * @param defaultValues Default values for each component of parameter
   * @return Mutable list of strings
   */
  private List<Object> buildMetadataArgs(
      String line,
      String paramName,
      String[] defaultValues) {
    final List<Object> list = new ArrayList<Object>();
    final String[][] ret = sqlLine.splitCompound(line);
    String[] compound;
    if (ret == null || ret.length != 2) {
      if (defaultValues[defaultValues.length - 1] == null) {
        throw new IllegalArgumentException(
            sqlLine.loc("arg-usage",
                ret == null || ret.length == 0 ? "" : ret[0][0], paramName));
      }
      compound = new String[0];
    } else {
      compound = ret[1];
    }
    if (compound.length <= defaultValues.length) {
      list.addAll(
          Arrays.asList(defaultValues).subList(
              0, defaultValues.length - compound.length));
      list.addAll(Arrays.asList(compound));
    } else {
      list.addAll(
          Arrays.asList(compound).subList(0, defaultValues.length));
    }
    return list;
  }

  public void indexes(String line, DispatchCallback callback) throws Exception {
    String[] strings = {sqlLine.getConnection().getCatalog(), null, "%"};
    List<Object> args = buildMetadataArgs(line, "table name", strings);
    args.add(Boolean.FALSE);
    args.add(Boolean.TRUE);
    metadata("getIndexInfo", args, callback);
  }

  public void primarykeys(String line, DispatchCallback callback)
      throws Exception {
    String[] strings = {sqlLine.getConnection().getCatalog(), null, "%"};
    List<Object> args = buildMetadataArgs(line, "table name", strings);
    metadata("getPrimaryKeys", args, callback);
  }

  public void exportedkeys(String line, DispatchCallback callback)
      throws Exception {
    String[] strings = {sqlLine.getConnection().getCatalog(), null, "%"};
    List<Object> args = buildMetadataArgs(line, "table name", strings);
    metadata("getExportedKeys", args, callback);
  }

  public void importedkeys(String line, DispatchCallback callback)
      throws Exception {
    String[] strings = {sqlLine.getConnection().getCatalog(), null, "%"};
    List<Object> args = buildMetadataArgs(line, "table name", strings);
    metadata("getImportedKeys", args, callback);
  }

  public void procedures(String line, DispatchCallback callback)
      throws Exception {
    String[] strings = {sqlLine.getConnection().getCatalog(), null, "%"};
    List<Object> args =
        buildMetadataArgs(line, "procedure name pattern", strings);
    metadata("getProcedures", args, callback);
  }

  public void tables(String line, DispatchCallback callback)
      throws SQLException {
    String[] strings = {sqlLine.getConnection().getCatalog(), null, "%"};
    List<Object> args = buildMetadataArgs(line, "table name", strings);
    args.add(null);
    metadata("getTables", args, callback);
  }

  public void typeinfo(String line, DispatchCallback callback)
      throws Exception {
    metadata("getTypeInfo", Collections.emptyList(), callback);
  }

  public void nativesql(String sql, DispatchCallback callback)
      throws Exception {
    if (sql.startsWith(SqlLine.COMMAND_PREFIX)) {
      sql = sql.substring(1);
    }

    if (sql.startsWith("native")) {
      sql = sql.substring("native".length() + 1);
    }

    String nat = sqlLine.getConnection().nativeSQL(sql);
    sqlLine.output(nat);
    callback.setToSuccess();
  }

  public void columns(String line, DispatchCallback callback)
      throws SQLException {
    String[] strings = {sqlLine.getConnection().getCatalog(), null, "%"};
    List<Object> args = buildMetadataArgs(line, "table name", strings);
    args.add("%");
    metadata("getColumns", args, callback);
  }

  public void dropall(String line, DispatchCallback callback) {
    DatabaseConnection databaseConnection = sqlLine.getDatabaseConnection();
    if (databaseConnection == null || databaseConnection.getUrl() == null) {
      sqlLine.error(sqlLine.loc("no-current-connection"));
      callback.setToFailure();
      return;
    }
    try {
      String question = sqlLine.loc("really-drop-all");
      if (!sqlLine.getConsoleReader().readLine(question).equals("y")) {
        sqlLine.error("abort-drop-all");
        callback.setToFailure();
        return;
      }

      List<String> cmds = new LinkedList<String>();
      ResultSet rs = sqlLine.getTables();
      try {
        while (rs.next()) {
          cmds.add("DROP TABLE "
              + rs.getString("TABLE_NAME") + ";");
        }
      } finally {
        try {
          rs.close();
        } catch (Exception e) {
          // ignore
        }
      }

      // run as a batch
      if (sqlLine.runCommands(cmds, callback) == cmds.size()) {
        callback.setToSuccess();
      } else {
        callback.setToFailure();
      }
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  public void reconnect(String line, DispatchCallback callback) {
    DatabaseConnection databaseConnection = sqlLine.getDatabaseConnection();
    if (databaseConnection == null || databaseConnection.getUrl() == null) {
      sqlLine.error(sqlLine.loc("no-current-connection"));
      callback.setToFailure();
      return;
    }

    sqlLine.info(sqlLine.loc("reconnecting", databaseConnection.getUrl()));
    try {
      databaseConnection.reconnect();
    } catch (Exception e) {
      sqlLine.error(e);
      callback.setToFailure();
      return;
    }

    callback.setToSuccess();
  }

  public void scan(String line, DispatchCallback callback)
      throws IOException {
    TreeSet<String> names = new TreeSet<String>();

    if (sqlLine.getDrivers() == null) {
      sqlLine.setDrivers(sqlLine.scanDrivers(line));
    }

    sqlLine.info(
        sqlLine.loc("drivers-found-count", sqlLine.getDrivers().size()));

    // unique the list
    for (Driver driver : sqlLine.getDrivers()) {
      names.add(driver.getClass().getName());
    }

    final ColorBuffer colorBuffer = sqlLine.getColorBuffer();
    sqlLine.output(colorBuffer
        .bold(colorBuffer.pad(sqlLine.loc("compliant"), 10).getMono())
        .bold(colorBuffer.pad(sqlLine.loc("jdbc-version"), 8).getMono())
        .bold(sqlLine.getColorBuffer(sqlLine.loc("driver-class")).getMono()));

    for (String name : names) {
      try {
        Driver driver = (Driver) Class.forName(name).newInstance();
        ColorBuffer msg =
            colorBuffer
                .pad(driver.jdbcCompliant() ? "yes" : "no", 10)
                .pad(driver.getMajorVersion() + "."
                    + driver.getMinorVersion(), 8)
                .append(name);
        if (driver.jdbcCompliant()) {
          sqlLine.output(msg);
        } else {
          sqlLine.output(colorBuffer.red(msg.getMono()));
        }
      } catch (Throwable t) {
        sqlLine.output(colorBuffer.red(name)); // error with driver
      }
    }

    callback.setToSuccess();
  }

  public void save(String line, DispatchCallback callback)
      throws IOException {
    sqlLine.info(
        sqlLine.loc("saving-options", sqlLine.getOpts().getPropertiesFile()));
    sqlLine.getOpts().save();
    callback.setToSuccess();
  }

  public void load(String line, DispatchCallback callback) throws IOException {
    sqlLine.getOpts().load();
    sqlLine.info(sqlLine.loc("loaded-options",
        sqlLine.getOpts().getPropertiesFile()));
    callback.setToSuccess();
  }

  public void config(String line, DispatchCallback callback) {
    try {
      Properties props = sqlLine.getOpts().toProperties();
      Set<String> keys = new TreeSet<String>(asMap(props).keySet());
      for (String key : keys) {
        sqlLine.output(sqlLine.getColorBuffer()
            .green(sqlLine.getColorBuffer()
                .pad(
                    key.substring(
                        SqlLineOpts.PROPERTY_PREFIX.length()), 20)
                .getMono())
            .append(props.getProperty(key)));
      }
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
      return;
    }

    callback.setToSuccess();
  }

  public void set(String line, DispatchCallback callback) {
    if (line == null || line.trim().equals("set")
        || line.length() == 0) {
      config(null, callback);
      return;
    }

    String[] parts = sqlLine.split(line, 3, "Usage: set <key> <value>");
    if (parts == null) {
      callback.setToFailure();
      return;
    }

    String key = parts[1];
    String value = parts[2];
    boolean success = sqlLine.getOpts().set(key, value, false);

    // if we autosave, then save
    if (success && sqlLine.getOpts().getAutoSave()) {
      try {
        sqlLine.getOpts().save();
      } catch (Exception saveException) {
        // ignore
      }
    }

    callback.setToSuccess();
  }

  private void reportResult(String action, long start, long end) {
    if (sqlLine.getOpts().getShowElapsedTime()) {
      sqlLine.info(action + " " + sqlLine.locElapsedTime(end - start));
    } else {
      sqlLine.info(action);
    }
  }

  public void commit(String line, DispatchCallback callback)
      throws SQLException {
    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }
    if (!(sqlLine.assertAutoCommit())) {
      callback.setToFailure();
      return;
    }

    try {
      long start = System.currentTimeMillis();
      sqlLine.getDatabaseConnection().connection.commit();
      long end = System.currentTimeMillis();
      sqlLine.showWarnings();
      reportResult(sqlLine.loc("commit-complete"), start, end);

      callback.setToSuccess();
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  public void rollback(String line, DispatchCallback callback)
      throws SQLException {
    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }
    if (!(sqlLine.assertAutoCommit())) {
      callback.setToFailure();
      return;
    }

    try {
      long start = System.currentTimeMillis();
      sqlLine.getDatabaseConnection().connection.rollback();
      long end = System.currentTimeMillis();
      sqlLine.showWarnings();
      reportResult(sqlLine.loc("rollback-complete"), start, end);
      callback.setToSuccess();
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  public void autocommit(String line, DispatchCallback callback)
      throws SQLException {
    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }

    if (line.endsWith("on")) {
      sqlLine.getDatabaseConnection().connection.setAutoCommit(true);
    } else if (line.endsWith("off")) {
      sqlLine.getDatabaseConnection().connection.setAutoCommit(false);
    }

    sqlLine.showWarnings();
    sqlLine.autocommitStatus(sqlLine.getDatabaseConnection().connection);
    callback.setToSuccess();
  }

  public void dbinfo(String line, DispatchCallback callback) {
    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }

    sqlLine.showWarnings();
    int padlen = 50;

    for (String method : METHODS) {
      try {
        sqlLine.output(
            sqlLine.getColorBuffer()
                .pad(method, padlen)
                .append("" + sqlLine.getReflector()
                    .invoke(sqlLine.getDatabaseMetaData(), method)));
      } catch (Exception e) {
        sqlLine.handleException(e);
      }
    }

    callback.setToSuccess();
  }

  public void verbose(String line, DispatchCallback callback) {
    sqlLine.info("verbose: on");
    set("set verbose true", callback);
  }

  public void outputformat(String line, DispatchCallback callback) {
    set("set " + line, callback);
  }

  public void brief(String line, DispatchCallback callback) {
    sqlLine.info("verbose: off");
    set("set verbose false", callback);
  }

  public void isolation(String line, DispatchCallback callback)
      throws SQLException {
    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }

    int i;

    if (line.endsWith("TRANSACTION_NONE")) {
      i = Connection.TRANSACTION_NONE;
    } else if (line.endsWith("TRANSACTION_READ_COMMITTED")) {
      i = Connection.TRANSACTION_READ_COMMITTED;
    } else if (line.endsWith("TRANSACTION_READ_UNCOMMITTED")) {
      i = Connection.TRANSACTION_READ_UNCOMMITTED;
    } else if (line.endsWith("TRANSACTION_REPEATABLE_READ")) {
      i = Connection.TRANSACTION_REPEATABLE_READ;
    } else if (line.endsWith("TRANSACTION_SERIALIZABLE")) {
      i = Connection.TRANSACTION_SERIALIZABLE;
    } else {
      callback.setToFailure();
      sqlLine.error(
          "Usage: isolation <TRANSACTION_NONE "
              + "| TRANSACTION_READ_COMMITTED "
              + "| TRANSACTION_READ_UNCOMMITTED "
              + "| TRANSACTION_REPEATABLE_READ "
              + "| TRANSACTION_SERIALIZABLE>");
      return;
    }

    Connection connection = sqlLine.getDatabaseConnection().getConnection();
    connection.setTransactionIsolation(i);

    int isol = connection.getTransactionIsolation();
    final String isolDesc;
    switch (i) {
    case Connection.TRANSACTION_NONE:
      isolDesc = "TRANSACTION_NONE";
      break;
    case Connection.TRANSACTION_READ_COMMITTED:
      isolDesc = "TRANSACTION_READ_COMMITTED";
      break;
    case Connection.TRANSACTION_READ_UNCOMMITTED:
      isolDesc = "TRANSACTION_READ_UNCOMMITTED";
      break;
    case Connection.TRANSACTION_REPEATABLE_READ:
      isolDesc = "TRANSACTION_REPEATABLE_READ";
      break;
    case Connection.TRANSACTION_SERIALIZABLE:
      isolDesc = "TRANSACTION_SERIALIZABLE";
      break;
    default:
      isolDesc = "UNKNOWN";
    }

    sqlLine.debug(sqlLine.loc("isolation-status", isolDesc));
    callback.setToSuccess();
  }

  public void batch(String line, DispatchCallback callback) {
    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }

    if (sqlLine.getBatch() == null) {
      sqlLine.setBatch(new LinkedList<String>());
      sqlLine.info(sqlLine.loc("batch-start"));
      callback.setToSuccess();
    } else {
      sqlLine.info(sqlLine.loc("running-batch"));
      try {
        sqlLine.runBatch(sqlLine.getBatch());
        callback.setToSuccess();
      } catch (Exception e) {
        callback.setToFailure();
        sqlLine.error(e);
      } finally {
        sqlLine.setBatch(null);
      }
    }
  }

  public void sql(String line, DispatchCallback callback) {
    execute(line, false, callback);
  }

  public void call(String line, DispatchCallback callback) {
    execute(line, true, callback);
  }

  private void execute(String line, boolean call, DispatchCallback callback) {
    if (line == null || line.length() == 0) {
      callback.setStatus(DispatchCallback.Status.FAILURE);
      return;
    }

    // ### FIXME:  doing the multi-line handling down here means
    // higher-level logic never sees the extra lines.  So,
    // for example, if a script is being saved, it won't include
    // the continuation lines!  This is logged as sf.net
    // bug 879518.

    // use multiple lines for statements not terminated by ";"
    try {
      while (!(line.trim().endsWith(";"))) {
        StringBuilder prompt = new StringBuilder(sqlLine.getPrompt());
        for (int i = 0; i < prompt.length() - 1; i++) {
          if (prompt.charAt(i) != '>') {
            prompt.setCharAt(i, i % 2 == 0 ? '.' : ' ');
          }
        }

        String extra = sqlLine.getConsoleReader().readLine(prompt.toString());
        if (null == extra) {
          break; // reader is at the end of data
        }
        if (!sqlLine.isComment(extra)) {
          line += SqlLine.getSeparator() + extra;
        }
      }
    } catch (UserInterruptException uie) {
      // CTRL-C'd out of the command. Note it, but don't call it an
      // error.
      callback.setStatus(DispatchCallback.Status.CANCELED);
      sqlLine.output(sqlLine.loc("command-canceled"));
      return;
    } catch (Exception e) {
      sqlLine.handleException(e);
    }

    if (line.trim().endsWith(";")) {
      line = line.trim();
      line = line.substring(0, line.length() - 1);
    }

    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }

    String sql = line;

    if (sql.startsWith(SqlLine.COMMAND_PREFIX)) {
      sql = sql.substring(1);
    }

    String prefix = call ? "call" : "sql";

    if (sql.startsWith(prefix)) {
      sql = sql.substring(prefix.length());
    }

    // batch statements?
    if (sqlLine.getBatch() != null) {
      sqlLine.getBatch().add(sql);
      callback.setToSuccess();
      return;
    }

    try {
      Statement stmnt = null;
      boolean hasResults;

      try {
        long start = System.currentTimeMillis();

        if (call) {
          stmnt = sqlLine.getDatabaseConnection().connection.prepareCall(sql);
          callback.trackSqlQuery(stmnt);
          hasResults = ((CallableStatement) stmnt).execute();
        } else {
          stmnt = sqlLine.createStatement();
          callback.trackSqlQuery(stmnt);
          hasResults = stmnt.execute(sql);
          callback.setToSuccess();
        }

        sqlLine.showWarnings();
        sqlLine.showWarnings(stmnt.getWarnings());

        if (hasResults) {
          do {
            ResultSet rs = stmnt.getResultSet();
            try {
              int count = sqlLine.print(rs, callback);
              long end = System.currentTimeMillis();

              reportResult(sqlLine.loc("rows-selected", count), start, end);
            } finally {
              rs.close();
            }
          } while (SqlLine.getMoreResults(stmnt));
        } else {
          int count = stmnt.getUpdateCount();
          long end = System.currentTimeMillis();
          reportResult(sqlLine.loc("rows-affected", count), start, end);
        }
      } finally {
        if (stmnt != null) {
          sqlLine.showWarnings(stmnt.getWarnings());
          stmnt.close();
        }
      }
    } catch (UserInterruptException uie) {
      // CTRL-C'd out of the command. Note it, but don't call it an
      // error.
      callback.setStatus(DispatchCallback.Status.CANCELED);
      sqlLine.output(sqlLine.loc("command-canceled"));
      return;
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
      return;
    }

    sqlLine.showWarnings();
    callback.setToSuccess();
  }

  public void quit(String line, DispatchCallback callback) {
    sqlLine.setExit(true);
    close(null, callback);
  }

  /**
   * Closes all connections.
   *
   * @param line Command line
   * @param callback Callback for command status
   */
  public void closeall(String line, DispatchCallback callback) {
    close(null, callback);
    if (callback.isSuccess()) {
      while (callback.isSuccess()) {
        close(null, callback);
      }
      // the last "close" will set it to fail so reset it to success.
      callback.setToSuccess();
    }
    // probably a holdover of the old boolean returns.
    callback.setToFailure();
  }

  /**
   * Closes the current connection.
   *
   * @param line Command line
   * @param callback Callback for command status
   */
  public void close(String line, DispatchCallback callback) {
    DatabaseConnection databaseConnection = sqlLine.getDatabaseConnection();
    if (databaseConnection == null) {
      callback.setToFailure();
      return;
    }

    try {
      Connection connection = databaseConnection.getConnection();
      if (connection != null && !connection.isClosed()) {
        sqlLine.info(
            sqlLine.loc("closing", connection.getClass().getName()));
        connection.close();
      } else {
        sqlLine.info(sqlLine.loc("already-closed"));
      }
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
      return;
    }

    sqlLine.getDatabaseConnections().remove();
    callback.setToSuccess();
  }

  /**
   * Connects to the database defined in the specified properties file.
   *
   * @param line Command line
   * @param callback Callback for command status
   * @throws Exception on error
   */
  public void properties(String line, DispatchCallback callback)
      throws Exception {
    String example = "";
    example += "Usage: properties <properties file>" + SqlLine.getSeparator();

    String[] parts = sqlLine.split(line);
    if (parts.length < 2) {
      callback.setToFailure();
      sqlLine.error(example);
      return;
    }

    int successes = 0;

    for (int i = 1; i < parts.length; i++) {
      Properties props = new Properties();
      props.load(new FileInputStream(parts[i]));
      connect(props, callback);
      if (callback.isSuccess()) {
        successes++;
      }
    }

    if (successes != parts.length - 1) {
      callback.setToFailure();
    } else {
      callback.setToSuccess();
    }
  }

  public void connect(String line, DispatchCallback callback) throws Exception {
    String example = "Usage: connect <url> <username> <password> [driver]"
        + SqlLine.getSeparator();

    String[] parts = sqlLine.split(line);
    if (parts == null) {
      callback.setToFailure();
      return;
    }

    if (parts.length < 2) {
      callback.setToFailure();
      sqlLine.error(example);
      return;
    }

    String url = parts.length < 2 ? null : parts[1];
    String user = parts.length < 3 ? null : parts[2];
    String pass = parts.length < 4 ? null : parts[3];
    String driver = parts.length < 5 ? null : parts[4];

    Properties props = new Properties();
    if (url != null) {
      props.setProperty("url", url);
    }
    if (driver != null) {
      props.setProperty("driver", driver);
    }
    if (user != null) {
      props.setProperty("user", user);
    }
    if (pass != null) {
      props.setProperty("password", pass);
    }

    connect(props, callback);
  }

  private String getProperty(Properties props, String... keys) {
    for (String key : keys) {
      String val = props.getProperty(key);
      if (val != null) {
        return val;
      }
    }

    for (String key : asMap(props).keySet()) {
      for (String key1 : keys) {
        if (key.endsWith(key1)) {
          return props.getProperty(key);
        }
      }
    }

    return null;
  }

  public void connect(Properties props, DispatchCallback callback)
      throws IOException {
    String url = getProperty(props,
        "url",
        "javax.jdo.option.ConnectionURL",
        "ConnectionURL");
    String driver = getProperty(props,
        "driver",
        "javax.jdo.option.ConnectionDriverName",
        "ConnectionDriverName");
    String username = getProperty(props,
        "user",
        "javax.jdo.option.ConnectionUserName",
        "ConnectionUserName");
    String password = getProperty(props,
        "password",
        "javax.jdo.option.ConnectionPassword",
        "ConnectionPassword");

    if (url == null || url.length() == 0) {
      callback.setToFailure();
      sqlLine.error("Property \"url\" is required");
      return;
    }
    if (driver == null || driver.length() == 0) {
      if (!sqlLine.scanForDriver(url)) {
        callback.setToFailure();
        sqlLine.error(sqlLine.loc("no-driver", url));
        return;
      }
    }

    sqlLine.debug("Connecting to " + url);

    if (username == null) {
      username = sqlLine.getConsoleReader()
          .readLine("Enter username for " + url + ": ");
    }
    if (password == null) {
      password = sqlLine.getConsoleReader()
          .readLine("Enter password for " + url + ": ", '*');
    }

    try {
      sqlLine.getDatabaseConnections().setConnection(
          new DatabaseConnection(sqlLine, driver, url, username, password));
      sqlLine.getDatabaseConnection().getConnection();

      sqlLine.setCompletions();
      callback.setToSuccess();
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  public void rehash(String line, DispatchCallback callback) {
    try {
      if (!sqlLine.assertConnection()) {
        callback.setToFailure();
      }

      if (sqlLine.getDatabaseConnection() != null) {
        sqlLine.getDatabaseConnection().setCompletions(false);
      }

      callback.setToSuccess();
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  /**
   * Lists the current connections.
   *
   * @param line Command line
   * @param callback Callback for command status
   */
  public void list(String line, DispatchCallback callback) {
    int index = 0;
    DatabaseConnections databaseConnections = sqlLine.getDatabaseConnections();
    sqlLine.info(
        sqlLine.loc("active-connections", databaseConnections.size()));

    for (DatabaseConnection databaseConnection : databaseConnections) {
      boolean closed;
      try {
        closed = databaseConnection.connection.isClosed();
      } catch (Exception e) {
        closed = true;
      }

      sqlLine.output(
          sqlLine.getColorBuffer()
              .pad(" #" + index++ + "", 5)
              .pad(closed ? sqlLine.loc("closed") : sqlLine.loc("open"), 9)
              .append(databaseConnection.getUrl()));
    }

    callback.setToSuccess();
  }

  public void all(String line, DispatchCallback callback) {
    int index = sqlLine.getDatabaseConnections().getIndex();
    boolean success = true;

    for (int i = 0; i < sqlLine.getDatabaseConnections().size(); i++) {
      sqlLine.getDatabaseConnections().setIndex(i);
      sqlLine.output(
          sqlLine.loc("executing-con", sqlLine.getDatabaseConnection()));

      // ### FIXME:  this is broken for multi-line SQL
      sql(line.substring("all ".length()), callback);
      success = callback.isSuccess() && success;
    }

    // restore index
    sqlLine.getDatabaseConnections().setIndex(index);
    if (success) {
      callback.setToSuccess();
    } else {
      callback.setToFailure();
    }
  }

  public void go(String line, DispatchCallback callback) {
    String[] parts = sqlLine.split(line, 2, "Usage: go <connection index>");
    if (parts == null) {
      callback.setToFailure();
      return;
    }

    int index = Integer.parseInt(parts[1]);
    if (!sqlLine.getDatabaseConnections().setIndex(index)) {
      sqlLine.error(sqlLine.loc("invalid-connection", "" + index));
      list("", callback); // list the current connections
      callback.setToFailure();
      return;
    }

    callback.setToSuccess();
  }

  /**
   * Starts or stops saving a script to a file.
   *
   * @param line Command line
   * @param callback Callback for command status
   */
  public void script(String line, DispatchCallback callback) {
    if (sqlLine.getScriptOutputFile() == null) {
      startScript(line, callback);
    } else {
      stopScript(line, callback);
    }
  }

  /**
   * Stop writing to the script file and close the script.
   */
  private void stopScript(String line, DispatchCallback callback) {
    try {
      sqlLine.getScriptOutputFile().close();
    } catch (Exception e) {
      sqlLine.handleException(e);
    }

    sqlLine.output(sqlLine.loc("script-closed", sqlLine.getScriptOutputFile()));
    sqlLine.setScriptOutputFile(null);
    callback.setToSuccess();
  }

  /**
   * Start writing to the specified script file.
   */
  private void startScript(String line, DispatchCallback callback) {
    OutputFile outFile = sqlLine.getScriptOutputFile();
    if (outFile != null) {
      callback.setToFailure();
      sqlLine.error(sqlLine.loc("script-already-running", outFile));
      return;
    }

    String[] parts = sqlLine.split(line, 2, "Usage: script <filename>");
    if (parts == null) {
      callback.setToFailure();
      return;
    }

    try {
      outFile = new OutputFile(parts[1]);
      sqlLine.setScriptOutputFile(outFile);
      sqlLine.output(sqlLine.loc("script-started", outFile));
      callback.setToSuccess();
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  /**
   * Runs a script from the specified file.
   *
   * @param line Command line
   * @param callback Callback for command status
   */
  public void run(String line, DispatchCallback callback) {
    String[] parts = sqlLine.split(line, 2, "Usage: run <scriptfile>");
    if (parts == null) {
      callback.setToFailure();
      return;
    }

    List<String> cmds = new LinkedList<String>();

    try {
      BufferedReader reader =
          new BufferedReader(new FileReader(parts[1]));
      try {
        // ### NOTE: fix for sf.net bug 879427
        StringBuilder cmd = null;
        for (;;) {
          String scriptLine = reader.readLine();

          if (scriptLine == null) {
            break;
          }

          String trimmedLine = scriptLine.trim();
          if (sqlLine.getOpts().getTrimScripts()) {
            scriptLine = trimmedLine;
          }

          if (cmd != null) {
            // we're continuing an existing command
            cmd.append(" \n");
            cmd.append(scriptLine);
            if (trimmedLine.endsWith(";")) {
              // this command has terminated
              cmds.add(cmd.toString());
              cmd = null;
            }
          } else {
            // we're starting a new command
            if (sqlLine.needsContinuation(scriptLine)) {
              // multi-line
              cmd = new StringBuilder(scriptLine);
            } else {
              // single-line
              cmds.add(scriptLine);
            }
          }
        }

        if (cmd != null) {
          // ### REVIEW: oops, somebody left the last command
          // unterminated; should we fix it for them or complain?
          // For now be nice and fix it.
          cmd.append(";");
          cmds.add(cmd.toString());
        }
      } finally {
        reader.close();
      }

      // success only if all the commands were successful
      if (sqlLine.runCommands(cmds, callback) == cmds.size()) {
        callback.setToSuccess();
      } else {
        callback.setToFailure();
      }
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  /**
   * Starts or stops saving all output to a file.
   *
   * @param line Command line
   * @param callback Callback for command status
   */
  public void record(String line, DispatchCallback callback) {
    if (sqlLine.getRecordOutputFile() == null) {
      startRecording(line, callback);
    } else {
      stopRecording(line, callback);
    }
  }

  /**
   * Stop writing output to the record file.
   */
  private void stopRecording(String line, DispatchCallback callback) {
    try {
      sqlLine.getRecordOutputFile().close();
    } catch (Exception e) {
      sqlLine.handleException(e);
    }

    sqlLine.output(sqlLine.loc("record-closed", sqlLine.getRecordOutputFile()));
    sqlLine.setRecordOutputFile(null);
    callback.setToSuccess();
  }

  /**
   * Start writing to the specified record file.
   */
  private void startRecording(String line, DispatchCallback callback) {
    OutputFile outputFile = sqlLine.getRecordOutputFile();
    if (outputFile != null) {
      callback.setToFailure();
      sqlLine.error(sqlLine.loc("record-already-running", outputFile));
      return;
    }

    String[] parts = sqlLine.split(line, 2, "Usage: record <filename>");
    if (parts == null) {
      callback.setToFailure();
      return;
    }

    try {
      outputFile = new OutputFile(parts[1]);
      sqlLine.setRecordOutputFile(outputFile);
      sqlLine.output(sqlLine.loc("record-started", outputFile));
      callback.setToSuccess();
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  public void describe(String line, DispatchCallback callback)
      throws SQLException {
    String[][] cmd = sqlLine.splitCompound(line);
    if (cmd.length != 2) {
      sqlLine.error("Usage: describe <table name>");
      callback.setToFailure();
      return;
    }

    if (cmd[1].length == 1
        && cmd[1][0] != null
        && cmd[1][0].equalsIgnoreCase("tables")) {
      tables("tables", callback);
    } else {
      columns(line, callback);
    }
  }

  public void help(String line, DispatchCallback callback) {
    String[] parts = sqlLine.split(line);
    String cmd = parts.length > 1 ? parts[1] : "";
    TreeSet<ColorBuffer> clist = new TreeSet<ColorBuffer>();

    for (CommandHandler commandHandler : sqlLine.commandHandlers) {
      if (cmd.length() == 0
          || commandHandler.getNames().contains(cmd)) {
        String help = commandHandler.getHelpText();
        if (!help.contains("\n")) {
          // Do not wrap if text appears to be pre-formatted (e.g. '!help set')
          help = sqlLine.wrap(help, 60, 28);
        }
        clist.add(sqlLine.getColorBuffer()
            .pad("!" + commandHandler.getName(), 28)
            .append(help));
      }
    }

    for (ColorBuffer c : clist) {
      sqlLine.output(c);
    }

    if (cmd.length() == 0) {
      sqlLine.output("");
      sqlLine.output(
          sqlLine.loc("comments", SqlLine.getApplicationContactInformation()));
    }

    callback.setToSuccess();
  }

  public void manual(String line, DispatchCallback callback)
      throws IOException {
    InputStream in = SqlLine.class.getResourceAsStream("manual.txt");
    if (in == null) {
      callback.setToFailure();
      sqlLine.error(sqlLine.loc("no-manual"));
      return;
    }

    BufferedReader breader =
        new BufferedReader(new InputStreamReader(in));
    String man;
    int index = 0;
    while ((man = breader.readLine()) != null) {
      index++;
      sqlLine.output(man);

      // silly little pager
      if (index % (sqlLine.getOpts().getMaxHeight() - 1) == 0) {
        String prompt = sqlLine.loc("enter-for-more");
        String ret = sqlLine.getConsoleReader().readLine(prompt);
        if (ret != null && ret.startsWith("q")) {
          break;
        }
      }
    }

    breader.close();

    callback.setToSuccess();
  }

  static Map<String, String> asMap(Properties properties) {
    //noinspection unchecked
    return (Map) properties;
  }
}

// End Commands.java
