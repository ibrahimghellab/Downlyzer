package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.securite.UtilisateurConnecte;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConnexionControleur {

    @GetMapping("/connexion")
    public String afficherConnexion() {
        return "connexion";
    }

    // le responsable arrive sur le tableau de bord, l'agent sur les stations
    @GetMapping("/")
    public String accueil(@AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return utilisateur.getProfil().peutSuperviser() ? "redirect:/tableau-de-bord" : "redirect:/stations";
    }
}
