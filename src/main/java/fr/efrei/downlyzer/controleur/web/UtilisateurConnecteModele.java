package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.securite.UtilisateurConnecte;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// met l'utilisateur connecté dans toutes les pages pour l'en-tête
@ControllerAdvice(basePackages = "fr.efrei.downlyzer.controleur.web")
public class UtilisateurConnecteModele {

    @ModelAttribute("utilisateurConnecte")
    public UtilisateurConnecte utilisateurConnecte(@AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return utilisateur;
    }
}
