package fr.efrei.downlyzer.controleur.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;

// remplace la page whitelabel de spring ; dans controleur.web pour avoir l'utilisateur dans l'en-tête
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

    // pour un client qui attend du json on renvoie pas de page html
    @RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProblemDetail> afficherErreurJson(HttpServletRequest requete) {
        Object code = requete.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus statut = code instanceof Integer entier ? HttpStatus.resolve(entier) : null;
        HttpStatus statutConnu = statut == null ? HttpStatus.INTERNAL_SERVER_ERROR : statut;
        boolean introuvable = statutConnu == HttpStatus.NOT_FOUND;
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(statutConnu,
                introuvable ? "Cette adresse n'existe pas." : "Une erreur inattendue s'est produite. Réessayez plus tard.");
        probleme.setTitle(introuvable ? "Introuvable" : "Erreur inattendue");
        // on met l'adresse demandée et pas /error
        if (requete.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) instanceof String adresse) {
            probleme.setInstance(URI.create(adresse));
        }
        return ResponseEntity.status(statutConnu).body(probleme);
    }
}
