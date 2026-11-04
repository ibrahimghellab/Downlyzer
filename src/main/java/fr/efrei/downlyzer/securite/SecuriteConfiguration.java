package fr.efrei.downlyzer.securite;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// deux chaînes : une pour l'api, une pour les pages
@Configuration
public class SecuriteConfiguration {

    private static final Logger JOURNAL = LoggerFactory.getLogger(SecuriteConfiguration.class);

    // écrites à la main car spring security répond avant nos contrôleurs
    private static final String CORPS_NON_AUTHENTIFIE =
            "{\"title\":\"Non authentifié\",\"status\":401,"
                    + "\"detail\":\"Une clé d'API valide est requise dans l'en-tête X-API-KEY.\"}";
    private static final String CORPS_ACCES_REFUSE =
            "{\"title\":\"Accès refusé\",\"status\":403,"
                    + "\"detail\":\"L'API est réservée aux systèmes externes identifiés par une clé d'API.\"}";

    // api : pas de session ni de csrf, et un 401 en json au lieu de rediriger vers la connexion
    @Bean
    @Order(1)
    public SecurityFilterChain filtresApi(HttpSecurity http, @Value("${downlyzer.api.cle}") String cleApi) throws Exception {
        if (cleApi.isBlank()) {
            JOURNAL.warn("Aucune clé d'API configurée (DOWNLYZER_API_KEY) : l'API refusera toutes les requêtes.");
        }
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new CleApiFiltre(cleApi), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(acces -> acces.anyRequest().hasRole("SYSTEME_EXTERNE"))
                .exceptionHandling(erreurs -> erreurs
                        .authenticationEntryPoint((requete, reponse, erreur) ->
                                repondreEnJson(reponse, HttpServletResponse.SC_UNAUTHORIZED, CORPS_NON_AUTHENTIFIE))
                        // connecté autrement qu'avec la clé (ex : un utilisateur des pages)
                        .accessDeniedHandler((requete, reponse, refus) ->
                                repondreEnJson(reponse, HttpServletResponse.SC_FORBIDDEN, CORPS_ACCES_REFUSE)));
        return http.build();
    }

    private static void repondreEnJson(HttpServletResponse reponse, int statut, String corps) throws IOException {
        reponse.setStatus(statut);
        reponse.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        reponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        reponse.getWriter().write(corps);
    }

    // pages : connexion par formulaire et session
    @Bean
    @Order(2)
    public SecurityFilterChain filtresPagesWeb(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(acces -> acces
                        // sinon spring security renvoie vers /error juste après la connexion
                        .requestMatchers("/connexion", "/css/**", "/error", "/favicon.ico").permitAll()
                        // la doc est lisible sans compte, les appels demandent quand même la clé
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // réservé au responsable : un agent prend un 403 même en forgeant la requête
                        .requestMatchers(HttpMethod.POST, "/incidents/*/affectation", "/incidents/*/resolution",
                                "/incidents/*/evaluation")
                        .hasRole("RESPONSABLE")
                        .requestMatchers("/tableau-de-bord").hasRole("RESPONSABLE")
                        // seul l'onglet carte de /stations est réservé
                        .requestMatchers(requete -> requete.getRequestURI().equals(requete.getContextPath() + "/stations")
                                && "carte".equals(requete.getParameter("vue")))
                        .hasRole("RESPONSABLE")
                        .anyRequest().authenticated())
                .formLogin(connexion -> connexion
                        .loginPage("/connexion")
                        .defaultSuccessUrl("/")
                        .permitAll())
                .logout(deconnexion -> deconnexion
                        .logoutUrl("/deconnexion")
                        .logoutSuccessUrl("/connexion?logout")
                        .permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder encodeurMotDePasse() {
        return new BCryptPasswordEncoder();
    }
}
