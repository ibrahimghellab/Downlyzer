package fr.efrei.downlyzer.controleur.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConnexionControleur {

    @GetMapping("/connexion")
    public String afficherConnexion() {
        return "connexion";
    }

    @GetMapping("/")
    public String accueil() {
        return "redirect:/stations";
    }
}