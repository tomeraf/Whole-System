package Tests.AcceptanceTests;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = com.halilovindustries.Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        args = "--spring.profiles.active=default"
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConfigTests extends BaseAcceptanceTests {

    @Autowired
    private Environment env;

    @Resource
    private DataSource dataSource;


    /*
    This test checks that your custom startup section is parsed and usable — important for your system manager logic
     */
    @Test
    public void mainConfig_shouldLoadStartupDefaults() {
        assertThat(env.getProperty("startup.defaultSystemManager.name")).isEqualTo("idan");
        assertThat(env.getProperty("startup.defaultSystemManager.password")).isEqualTo("1234");
    }

    /*
    This one validates that your app knows what external URL it should connect to (e.g. microservice endpoint or API gateway).
     */
    @Test
    public void mainConfig_shouldLoadExternalUrl() {
        assertThat(env.getProperty("external.externalUrl"))
                .isEqualTo("https://damp-lynna-wsep-1984852e.koyeb.app/");
    }
}
