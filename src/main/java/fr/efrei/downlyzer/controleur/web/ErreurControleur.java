package fr.efrei.downlyzer.controleur.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

// Remplace la page « Whitelabel » de Spring Boot ; placé dans controleur.web pour que l'en-tête reçoive l'utilisateur connecté
@Controller
public class ErreurControleur implements ErrorController {

    @RequestMapping("/error")
    public String afficherErreur(HttpServletRequest requete, HttpServletResponse reponse, Model model) {
        Object code = requete.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statut = code instanceof Integer entier ? entier : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        reponse.setStatus(statut);

        switch (statut) {
            case HttpServletResponse.SC_NOT_FOUND -> {
                model.addAttribute("titre", "Introuvable");
                model.addAttribute("message", "Cette page n'existe pas.");
            }
            case HttpServletResponse.SC_FORBIDDEN -> {
                model.addAttribute("titre", "Accès refusé");
                model.addAttribute("message", "Votre profil ne permet pas d'accéder à cette page.");
            }
            case HttpServletResponse.SC_BAD_REQUEST -> {
                model.addAttribute("titre", "Requête invalide");
                model.addAttribute("message", "Les informations envoyées ne sont pas valides.");
            }
            default -> {
                model.addAttribute("titre", "Erreur inattendue");
                model.addAttribute("message", "Une erreur inattendue s'est produite. Réessayez plus tard.");
            }
        }
        return "erreur";
    }
}
