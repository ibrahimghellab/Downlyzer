package fr.efrei.downlyzer.securite;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecuriteConfiguration {

    // Les règles d'accès par profil (RG11) s'ajouteront ici au fil des fonctionnalités réservées au responsable
    @Bean
    public SecurityFilterChain filtresPagesWeb(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(acces -> acces
                        // /error et /favicon.ico libres : sinon Spring Security les mémorise comme page demandée
                        // et y renvoie l'utilisateur juste après la connexion
                        .requestMatchers("/connexion", "/css/**", "/error", "/favicon.ico").permitAll()
                        .anyRequest().authenticated())
                .formLogin(connexion -> connexion
                        .loginPage("/connexion")
                        .defaultSuccessUrl("/stations")
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