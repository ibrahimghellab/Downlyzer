package fr.efrei.downlyzer.securite;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class CleApiFiltre extends OncePerRequestFilter {

    public static final String EN_TETE = "X-API-KEY";

    private final byte[] cleAttendue;

    public CleApiFiltre(String cleAttendue) {
        this.cleAttendue = cleAttendue.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain suite)
            throws ServletException, IOException {
        String cleRecue = requete.getHeader(EN_TETE);
        if (cleRecue != null && estValide(cleRecue)) {
            SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    "systeme-externe", null, AuthorityUtils.createAuthorityList("ROLE_SYSTEME_EXTERNE")));
        }
        suite.doFilter(requete, reponse);
    }

    // on compare en temps constant pour pas qu'on devine la clé
    private boolean estValide(String cleRecue) {
        return cleAttendue.length > 0 && MessageDigest.isEqual(cleAttendue, cleRecue.getBytes(StandardCharsets.UTF_8));
    }
}
