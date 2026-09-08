import com.aio.hospitalsafety.HospitalSafetyApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DomainFlowCheck {
    public static void main(String[] args) throws Exception {
        try (var context = SpringApplication.run(HospitalSafetyApplication.class,
                "--server.port=0", "--spring.main.banner-mode=off", "--logging.level.root=ERROR")) {
            var mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).build();
            var session = new MockHttpSession();
            mvc.perform(get("/"))
                    .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("/domain")));
            mvc.perform(post("/domain").param("hospitalDomain", "test").session(session))
                    .andExpect(redirectedUrl("/login"));
            mvc.perform(get("/login").session(session))
                    .andExpect(status().isOk()).andExpect(view().name("html/login"))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("늘푸른병원")));
            mvc.perform(post("/domain").param("hospitalDomain", "__unknown_domain_check__"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("등록되지 않은 병원 도메인")));
            System.out.println("PASS: actual DB test domain redirects to rendered login; unknown domain displays error.");
        }
    }
}
