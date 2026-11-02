package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.exception.ActionNonAutoriseeException;
import fr.efrei.downlyzer.exception.DonneeInvalideException;
import fr.efrei.downlyzer.exception.RessourceIntrouvableException;
import fr.efrei.downlyzer.exception.TransitionInterditeException;
import fr.efrei.downlyzer.securite.UtilisateurConnecte;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Traduit les exceptions métier en une page d'erreur lisible, avec le code HTTP adapté.
// Limité aux pages : l'API aura son propre gestionnaire, qui répondra en JSON
@ControllerAdvice(basePackages = "fr.efrei.downlyzer.controleur.web")
public class GestionnaireExceptionsWeb {

    @ExceptionHandler(RessourceIntrouvableException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String ressourceIntrouvable(RessourceIntrouvableException exception,
                                       @AuthenticationPrincipal UtilisateurConnecte utilisateur, Model model) {
        return afficherErreur(model, utilisateur, "Introuvable", exception.getMessage());
    }

    @ExceptionHandler(ActionNonAutoriseeException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String actionNonAutorisee(ActionNonAutoriseeException exception,
                                     @AuthenticationPrincipal UtilisateurConnecte utilisateur, Model model) {
        return afficherErreur(model, utilisateur, "Action non autorisée", exception.getMessage());
    }

    @ExceptionHandler(TransitionInterditeException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String transitionInterdite(TransitionInterditeException exception,
                                      @AuthenticationPrincipal UtilisateurConnecte utilisateur, Model model) {
        return afficherErreur(model, utilisateur, "Opération impossible", exception.getMessage());
    }

    @ExceptionHandler(DonneeInvalideException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String donneeInvalide(DonneeInvalideException exception,
                                 @AuthenticationPrincipal UtilisateurConnecte utilisateur, Model model) {
        return afficherErreur(model, utilisateur, "Donnée invalide", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String identifiantInvalide(@AuthenticationPrincipal UtilisateurConnecte utilisateur, Model model) {
        return afficherErreur(model, utilisateur, "Introuvable", "Cette page n'existe pas.");
    }

    // Levée à l'enregistrement quand la version de l'incident (@Version) a changé entre-temps
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String modificationConcurrente(@AuthenticationPrincipal UtilisateurConnecte utilisateur, Model model) {
        return afficherErreur(model, utilisateur, "Modification concurrente",
                "Ces informations viennent d'être modifiées par quelqu'un d'autre. Rechargez la page et recommencez.");
    }

    // Le modèle d'une page d'erreur part vide : l'utilisateur connecté y est remis pour l'en-tête (F1)
    private String afficherErreur(Model model, UtilisateurConnecte utilisateur, String titre, String message) {
        model.addAttribute("utilisateurConnecte", utilisateur);
        model.addAttribute("titre", titre);
        model.addAttribute("message", message);
        return "erreur";
    }
}
