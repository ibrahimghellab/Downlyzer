package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.securite.UtilisateurConnecte;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Met l'utilisateur connecté à disposition de toutes les pages, pour l'en-tête « Bonjour [Prénom] ([profil]) » (F1)
@ControllerAdvice(basePackages = "fr.efrei.downlyzer.controleur.web")
public class UtilisateurConnecteModele {

    @ModelAttribute("utilisateurConnecte")
    public UtilisateurConnecte utilisateurConnecte(@AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return utilisateur;
    }
}