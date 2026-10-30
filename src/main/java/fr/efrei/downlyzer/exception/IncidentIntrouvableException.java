package fr.efrei.downlyzer.exception;

public class IncidentIntrouvableException extends RessourceIntrouvableException {

    public IncidentIntrouvableException(Long id) {
        super("L'incident n° " + id + " n'existe pas.");
    }
}
