package fr.efrei.downlyzer.exception;

public class EquipementIntrouvableException extends RessourceIntrouvableException {

    public EquipementIntrouvableException(Long id) {
        super("L'équipement n° " + id + " n'existe pas.");
    }

    // pour les capteurs, qui donnent la référence et pas l'id
    public EquipementIntrouvableException(String reference) {
        super("Aucun équipement ne porte la référence « " + reference + " ».");
    }
}
