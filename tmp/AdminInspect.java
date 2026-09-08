import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

class AdminInspect {
    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("application-local.properties"))) { config.load(reader); }
        DriverManager.setLoginTimeout(10);
        try (var connection = DriverManager.getConnection(config.getProperty("DB_URL"), config.getProperty("DB_USERNAME"), config.getProperty("DB_PASSWORD"))) {
            connection.setReadOnly(true);
            for (String sql : new String[]{
                "SELECT table_name, column_name, data_type, character_maximum_length, is_nullable, column_default FROM information_schema.columns WHERE table_schema='public' AND table_name IN ('tb_emp','tb_ward') ORDER BY table_name, ordinal_position",
                "SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid IN ('public.tb_emp'::regclass, 'public.tb_ward'::regclass)",
                "SELECT extname FROM pg_extension"
            }) {
                try (var statement = connection.createStatement()) {
                    statement.setQueryTimeout(10);
                    try (var result = statement.executeQuery(sql)) {
                        while (result.next()) {
                            for (int i=1; i<=result.getMetaData().getColumnCount(); i++) System.out.print(result.getString(i) + " | ");
                            System.out.println();
                        }
                    }
                }
            }
        }
    }
}
