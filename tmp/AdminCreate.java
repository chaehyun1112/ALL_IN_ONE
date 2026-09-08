import java.nio.file.*;
import java.sql.*;
import java.util.Properties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdminCreate {
    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("application-local.properties"))) { config.load(reader); }
        var encoder = new BCryptPasswordEncoder();
        DriverManager.setLoginTimeout(10);
        try (var connection = DriverManager.getConnection(config.getProperty("DB_URL"), config.getProperty("DB_USERNAME"), config.getProperty("DB_PASSWORD"))) {
            connection.setAutoCommit(false);
            try {
                try (var query = connection.prepareStatement("SELECT emp_id FROM public.tb_emp WHERE emp_id = ?")) {
                    query.setQueryTimeout(10);
                    query.setString(1, "admin");
                    try (var result = query.executeQuery()) {
                        if (result.next()) throw new IllegalStateException("Account admin already exists; no changes made.");
                    }
                }
                try (var query = connection.prepareStatement("INSERT INTO public.tb_emp (emp_id, emp_pw, hosp_div_id, ward_id, emp_nm, role_cd, auth_st) VALUES (?, ?, ?, NULL, ?, ?, ?)")) {
                    query.setQueryTimeout(10);
                    query.setString(1, "admin");
                    query.setString(2, encoder.encode("1234"));
                    query.setString(3, "test");
                    query.setString(4, "관리자");
                    query.setString(5, "ADMIN");
                    query.setString(6, "APPROVED");
                    if (query.executeUpdate() != 1) throw new IllegalStateException("Insert failed");
                }
                try (var query = connection.prepareStatement("SELECT emp_pw, hosp_div_id, role_cd, auth_st FROM public.tb_emp WHERE emp_id = ?")) {
                    query.setQueryTimeout(10);
                    query.setString(1, "admin");
                    try (var result = query.executeQuery()) {
                        if (!result.next() || !encoder.matches("1234", result.getString("emp_pw"))
                                || !"test".equals(result.getString("hosp_div_id"))
                                || !"ADMIN".equals(result.getString("role_cd"))
                                || !"APPROVED".equals(result.getString("auth_st"))) {
                            throw new IllegalStateException("Verification failed");
                        }
                    }
                }
                connection.commit();
                System.out.println("CREATED AND VERIFIED: admin; hospital=test; role=ADMIN; auth=APPROVED; BCrypt password verified.");
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
}
