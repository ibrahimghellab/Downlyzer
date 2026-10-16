package fr.efrei.downlyzer.exception;

public class StationIntrouvableException extends RessourceIntrouvableException {

    public StationIntrouvableException(Long id) {
        super("La station n° " + id + " n'existe pas.");
    }
}
