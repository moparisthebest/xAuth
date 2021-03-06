package com.cypherx.xauth.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.cypherx.xauth.xAuthLog;
import com.cypherx.xauth.xAuthSettings;

public class Database {
	private static DBMS dbms = DBMS.H2;
	private static Connection connection = null;

	public static void connect() {
		String driver = "";
		String username = "sa";
		String password = "";

		switch (dbms) {
			case H2:
				driver = "org.h2.Driver";
				break;
			case MYSQL:
				driver = "com.mysql.jdbc.Driver";
				username = xAuthSettings.mysqlUser;
				password = xAuthSettings.mysqlPass;
				break;
		}

		try {
			Class.forName(driver);
			connection = DriverManager.getConnection(getConnString(), username, password);
			xAuthLog.info("Connection to database established!");
		} catch (ClassNotFoundException e) {
			xAuthLog.severe("Missing database library!");
		} catch (SQLException e) {
			xAuthLog.severe("Could not connect to database!", e);
		}
	}

	private static String getConnString() {
		switch (dbms) {
			case H2:
				return "jdbc:h2:plugins" + File.separator + "xAuth" + File.separator + "xAuth;IGNORECASE=TRUE";
			case MYSQL:
				return "jdbc:mysql://" + xAuthSettings.mysqlHost + ":" + xAuthSettings.mysqlPort + "/" + xAuthSettings.mysqlDb + "?zeroDateTimeBehavior=convertToNull";
			default:
				return "";
		}
	}

	public static ResultSet queryRead(String sql, Object... params) {
		if (!isConnected())
			connect();

		PreparedStatement stmt = null;

		try {
			stmt = connection.prepareStatement(sql);

			for (int i = 0; i < params.length; i++)
				stmt.setObject(i + 1, params[i]);

			return stmt.executeQuery();
		} catch (SQLException e) {
			xAuthLog.severe("SQL query failure [read] (" + sql + ")", e);
		}

		return null;
	}

	public static int queryWrite(String sql, Object... params) {
		if (!isConnected())
			connect();

		PreparedStatement stmt = null;
		int result = -1;

		try {
			stmt = connection.prepareStatement(sql);

			for (int i = 0; i < params.length; i++)
				stmt.setObject(i + 1, params[i]);

			result = stmt.executeUpdate();
		} catch (SQLException e) {
			xAuthLog.severe("SQL query failure [write] (" + sql + ")", e);
		}

		return result;
	}

	public static void queryBatch(PreparedStatement stmt) {
		try {
			stmt.executeBatch();
		} catch (SQLException e) {
			xAuthLog.severe("SQL query failure [batch]", e);
		}
	}

	public static int lastInsertId() {
		if (!isConnected())
			connect();

		int lastInsertId = -1;
		ResultSet rs = queryRead("SELECT LAST_INSERT_ID()");

		try {
			if (rs.next())
				lastInsertId = rs.getInt(1);
		} catch (SQLException e) {
			xAuthLog.severe("Could not retrieve last insert ID!", e);
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {}
		}

		return lastInsertId;
	}

	public static void printStats() {
		String sql = "SELECT" +
				" (SELECT COUNT(*) FROM `" + xAuthSettings.tblAccount + "`) AS accounts," +
				" (SELECT COUNT(*) FROM `" + xAuthSettings.tblSession + "`) AS sessions";
		ResultSet rs = queryRead(sql);

		try {
			if (rs.next())
				xAuthLog.info("Accounts: " + rs.getInt("accounts") + ", Sessions: " + rs.getInt("sessions"));
		} catch (SQLException e) {
			xAuthLog.severe("Could not fetch xAuth statistics!", e);
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {}
		}
	}

	public static void close() {
		try {
			if (connection != null)
				connection.close();
		} catch (SQLException e) {}
	}

	public static boolean isConnected() {
		try {
			if (connection == null || connection.isClosed())
				return false;
		} catch (SQLException e) {
			return false;
		}

		return true;
	}

	public static Connection getConnection() {
		return connection;
	}

	public static void setDBMS(DBMS newDBMS) {
		dbms = newDBMS;
	}

	public static DBMS getDBMS() {
		return dbms;
	}

	public enum DBMS {
		H2,
		MYSQL
	}
}