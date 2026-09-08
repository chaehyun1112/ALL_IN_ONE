import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

class HospitalInspect {
    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("application-local.properties"))) {
            config.load(reader);
        }
        DriverManager.setLoginTimeout(10);
        try (var connection = DriverManager.getConnection(config.getProperty("DB_URL"), config.getProperty("DB_USERNAME"), config.getProperty("DB_PASSWORD"))) {
            connection.setReadOnly(true);
            try (var statement = connection.createStatement()) {
                statement.setQueryTimeout(10);
                for (String sql : new String[]{
                    "SELECT column_name, data_type, is_nullable, column_default FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'tb_hospital' ORDER BY ordinal_position",
                    "SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = 'public.tb_hospital'::regclass"
                }) {
                    try (var result = statement.executeQuery(sql)) {
                        while (result.next()) {
                            for (int i = 1; i <= result.getMetaData().getColumnCount(); i++) {
                                System.out.print(result.getString(i) + " | ");
                            }
                            System.out.println();
                        }
                    }
                }
            }
        }
    }
}
