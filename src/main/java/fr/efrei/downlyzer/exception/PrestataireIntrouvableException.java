package fr.efrei.downlyzer.exception;

public class PrestataireIntrouvableException extends RessourceIntrouvableException {

    public PrestataireIntrouvableException(Long id) {
        super("Le prestataire n° " + id + " n'existe pas.");
    }
}
