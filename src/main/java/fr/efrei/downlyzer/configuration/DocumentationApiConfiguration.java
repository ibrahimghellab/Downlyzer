package fr.efrei.downlyzer.configuration;

import fr.efrei.downlyzer.securite.CleApiFiltre;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// on déclare la clé pour que swagger affiche le bouton authorize
@Configuration
public class DocumentationApiConfiguration {

    private static final String SCHEMA_CLE = "cleApi";

    @Bean
    public OpenAPI documentationApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Downlyzer")
                        .version("1.0")
                        .description("Consultation des incidents et signalements des systèmes externes (capteurs). "
                                + "Chaque requête doit porter la clé d'API dans l'en-tête " + CleApiFiltre.EN_TETE + "."))
                .components(new Components().addSecuritySchemes(SCHEMA_CLE, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(CleApiFiltre.EN_TETE)))
                .addSecurityItem(new SecurityRequirement().addList(SCHEMA_CLE));
    }
}
