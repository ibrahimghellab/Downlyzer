package fr.efrei.downlyzer.securite;

import fr.efrei.downlyzer.modele.Profil;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

import java.util.List;

public class UtilisateurConnecte implements UserDetails {

    private final String login;
    private final String motDePasseHash;
    private final String prenom;
    private final Profil profil;
    private final boolean actif;

    public UtilisateurConnecte(Utilisateur utilisateur) {
        this.login = utilisateur.getLogin();
        this.motDePasseHash = utilisateur.getMotDePasseHash();
        this.prenom = utilisateur.getPrenom();
        this.profil = utilisateur.getProfil();
        this.actif = utilisateur.isActif();
    }

    // Le préfixe ROLE_ permet d'écrire hasRole("RESPONSABLE") dans les règles d'accès
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + profil.name()));
    }

    @Override
    public String getPassword() {
        return motDePasseHash;
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isEnabled() {
        return actif;
    }

    public String getPrenom() {
        return prenom;
    }

    public Profil getProfil() {
        return profil;
    }
}