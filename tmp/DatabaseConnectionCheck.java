import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Properties;

class DatabaseConnectionCheck {
    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("application-local.properties"))) {
            config.load(reader);
        }
        DriverManager.setLoginTimeout(10);
        try (var connection = DriverManager.getConnection(config.getProperty("DB_URL"),
                config.getProperty("DB_USERNAME"), config.getProperty("DB_PASSWORD"))) {
            connection.setReadOnly(true);
            System.out.println("CONNECTED: " + connection.getMetaData().getDatabaseProductName());
            try (var statement = connection.createStatement()) {
                statement.setQueryTimeout(10);
                try (var result = statement.executeQuery("SELECT table_schema, table_name FROM information_schema.tables WHERE table_type = 'BASE TABLE' AND table_schema NOT IN ('pg_catalog', 'information_schema') ORDER BY table_schema, table_name")) {
                    int count = 0;
                    while (result.next()) {
                        System.out.println("TABLE: " + result.getString(1) + "." + result.getString(2));
                        count++;
                    }
                    System.out.println("TABLE_COUNT: " + count);
                }
            }
        }
    }
}
