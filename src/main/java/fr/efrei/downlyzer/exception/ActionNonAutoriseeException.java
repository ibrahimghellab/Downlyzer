package fr.efrei.downlyzer.exception;

// refusé à cause du profil et de l'état de l'incident (les routes interdites passent par spring security)
public class ActionNonAutoriseeException extends ExceptionMetier {

    public ActionNonAutoriseeException(String message) {
        super(message);
    }
}
