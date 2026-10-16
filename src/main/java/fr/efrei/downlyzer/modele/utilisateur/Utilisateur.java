package fr.efrei.downlyzer.modele.utilisateur;

import fr.efrei.downlyzer.modele.Profil;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "utilisateur")
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "login", nullable = false, length = 50, unique = true)
    private String login;

    // Empreinte BCrypt (60 caractères) : le mot de passe en clair n'est jamais stocké
    @Column(name = "mot_de_passe_hash", nullable = false, length = 60)
    private String motDePasseHash;

    @Column(name = "prenom", nullable = false, length = 50)
    private String prenom;

    @Column(name = "nom", nullable = false, length = 50)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(name = "profil", nullable = false, length = 20)
    private Profil profil;

    @Column(name = "actif", nullable = false)
    private boolean actif;

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getMotDePasseHash() {
        return motDePasseHash;
    }

    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Profil getProfil() {
        return profil;
    }

    public void setProfil(Profil profil) {
        this.profil = profil;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}
