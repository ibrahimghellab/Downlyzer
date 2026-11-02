package fr.efrei.downlyzer.exception;

// Donnée refusée par une règle métier que la validation du formulaire ne peut pas vérifier seule
// (par exemple un prestataire qui n'intervient pas sur ce type d'équipement)
public class DonneeInvalideException extends ExceptionMetier {

    public DonneeInvalideException(String message) {
        super(message);
    }
}
