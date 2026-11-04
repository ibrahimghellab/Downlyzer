package fr.efrei.downlyzer.exception;

// donnée refusée par une règle métier que le formulaire peut pas vérifier tout seul
public class DonneeInvalideException extends ExceptionMetier {

    public DonneeInvalideException(String message) {
        super(message);
    }
}
