package fr.efrei.downlyzer.controleur.api;

import fr.efrei.downlyzer.exception.DonneeInvalideException;
import fr.efrei.downlyzer.exception.IncidentDejaOuvertException;
import fr.efrei.downlyzer.exception.RessourceIntrouvableException;
import fr.efrei.downlyzer.exception.TransitionInterditeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

// comme pour les pages mais en json ; @ResponseStatus pour que swagger affiche les codes
@RestControllerAdvice(basePackages = "fr.efrei.downlyzer.controleur.api")
public class GestionnaireExceptionsApi {

    @ExceptionHandler(RessourceIntrouvableException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail ressourceIntrouvable(RessourceIntrouvableException exception) {
        return probleme(HttpStatus.NOT_FOUND, "Introuvable", exception.getMessage());
    }

    @ExceptionHandler(IncidentDejaOuvertException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail incidentDejaOuvert(IncidentDejaOuvertException exception) {
        ProblemDetail probleme = probleme(HttpStatus.CONFLICT, "Incident déjà en cours", exception.getMessage());
        probleme.setProperty("incidentExistant", exception.getIncidentExistantId());
        return probleme;
    }

    @ExceptionHandler({TransitionInterditeException.class, ObjectOptimisticLockingFailureException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail transitionInterdite(RuntimeException exception) {
        String message = exception instanceof TransitionInterditeException ? exception.getMessage()
                : "L'incident vient d'être modifié par quelqu'un d'autre. Relisez-le puis recommencez.";
        return probleme(HttpStatus.CONFLICT, "Opération impossible", message);
    }

    @ExceptionHandler(DonneeInvalideException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail donneeInvalide(DonneeInvalideException exception) {
        return probleme(HttpStatus.BAD_REQUEST, "Donnée invalide", exception.getMessage());
    }

    // on renvoie toutes les erreurs d'un coup avec le champ concerné
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail donneesInvalides(MethodArgumentNotValidException exception) {
        String erreurs = exception.getBindingResult().getFieldErrors().stream()
                .map(erreur -> erreur.getField() + " : " + erreur.getDefaultMessage())
                .collect(Collectors.joining(" ; "));
        return probleme(HttpStatus.BAD_REQUEST, "Données invalides", erreurs);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail corpsIllisible() {
        return probleme(HttpStatus.BAD_REQUEST, "Données invalides", "Le corps de la requête n'est pas un JSON valide.");
    }

    // ici c'est la requête qui est fausse donc 400 (côté pages c'est un 404)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail parametreInvalide(MethodArgumentTypeMismatchException exception) {
        return probleme(HttpStatus.BAD_REQUEST, "Données invalides",
                "La valeur « " + exception.getValue() + " » n'est pas valide pour " + exception.getName() + ".");
    }

    private static ProblemDetail probleme(HttpStatus statut, String titre, String message) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(statut, message);
        probleme.setTitle(titre);
        return probleme;
    }
}
