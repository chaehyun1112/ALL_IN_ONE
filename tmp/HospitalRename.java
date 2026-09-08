import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Properties;

class HospitalRename {
    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("application-local.properties"))) {
            config.load(reader);
        }
        DriverManager.setLoginTimeout(10);
        try (var connection = DriverManager.getConnection(config.getProperty("DB_URL"),
                config.getProperty("DB_USERNAME"), config.getProperty("DB_PASSWORD"))) {
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.prepareStatement(
                        "UPDATE public.tb_hospital SET hosp_nm = ? WHERE hosp_div_id = ?")) {
                    statement.setQueryTimeout(10);
                    statement.setString(1, "늘푸른요양병원");
                    statement.setString(2, "test");
                    if (statement.executeUpdate() != 1) {
                        throw new IllegalStateException("Expected exactly one hospital; changes rolled back.");
                    }
                }
                try (var statement = connection.prepareStatement(
                        "SELECT hosp_nm FROM public.tb_hospital WHERE hosp_div_id = ?")) {
                    statement.setQueryTimeout(10);
                    statement.setString(1, "test");
                    try (var result = statement.executeQuery()) {
                        if (!result.next() || !"늘푸른요양병원".equals(result.getString(1))) {
                            throw new IllegalStateException("Hospital name verification failed.");
                        }
                    }
                }
                connection.commit();
                System.out.println("UPDATED AND VERIFIED: hospital domain=test; requested hospital name saved.");
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
}
