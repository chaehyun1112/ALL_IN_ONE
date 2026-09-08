import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

class HospitalInsert {
    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("application-local.properties"))) {
            config.load(reader);
        }
        DriverManager.setLoginTimeout(10);
        try (var connection = DriverManager.getConnection(config.getProperty("DB_URL"), config.getProperty("DB_USERNAME"), config.getProperty("DB_PASSWORD"))) {
            connection.setAutoCommit(false);
            try {
                int inserted;
                try (var statement = connection.prepareStatement("INSERT INTO public.tb_hospital (hosp_div_id, hosp_nm) VALUES (?, ?) ON CONFLICT (hosp_div_id) DO NOTHING")) {
                    statement.setQueryTimeout(10);
                    statement.setString(1, "test");
                    statement.setString(2, "늘푸른병원");
                    inserted = statement.executeUpdate();
                }
                try (var statement = connection.prepareStatement("SELECT hosp_div_id, hosp_nm FROM public.tb_hospital WHERE hosp_div_id = ?")) {
                    statement.setQueryTimeout(10);
                    statement.setString(1, "test");
                    try (var result = statement.executeQuery()) {
                        if (!result.next() || !"늘푸른병원".equals(result.getString("hosp_nm"))) {
                            throw new IllegalStateException("Existing hospital ID has a different name; no changes committed.");
                        }
                    }
                }
                connection.commit();
                System.out.println("VERIFIED: hosp_div_id=test; hospital name matches requested value; inserted=" + inserted);
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
}
