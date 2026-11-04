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

    // Page d'arrivée après la connexion : le responsable commence par la supervision (F11), l'agent par les stations
    @GetMapping("/")
    public String accueil(@AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return utilisateur.getProfil().peutSuperviser() ? "redirect:/tableau-de-bord" : "redirect:/stations";
    }
}
