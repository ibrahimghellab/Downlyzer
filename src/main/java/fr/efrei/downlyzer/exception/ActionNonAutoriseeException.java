package fr.efrei.downlyzer.exception;

// Action refusée au profil connecté selon l'état de la donnée (RG11) ; les routes interdites
// à un profil sont, elles, bloquées en amont par Spring Security
public class ActionNonAutoriseeException extends ExceptionMetier {

    public ActionNonAutoriseeException(String message) {
        super(message);
    }
}
