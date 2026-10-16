package fr.efrei.downlyzer.exception;

public class EquipementIntrouvableException extends RessourceIntrouvableException {

    public EquipementIntrouvableException(Long id) {
        super("L'équipement n° " + id + " n'existe pas.");
    }
}