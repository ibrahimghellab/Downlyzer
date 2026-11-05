package fr.efrei.downlyzer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

// démarre toute l'appli donc il faut une base ; sauté sans elle pour que mvn verify passe partout
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
class DownlyzerApplicationTests {

    @Test
    void contextLoads() {
    }

}
