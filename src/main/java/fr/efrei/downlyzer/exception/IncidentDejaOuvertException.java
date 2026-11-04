package fr.efrei.downlyzer.exception;

// on garde l'id de l'incident existant pour que le capteur puisse le suivre
public class IncidentDejaOuvertException extends ExceptionMetier {

    private final Long incidentExistantId;

    public IncidentDejaOuvertException(String referenceEquipement, Long incidentExistantId) {
        super("L'équipement « " + referenceEquipement + " » a déjà un incident en cours (n° " + incidentExistantId + ").");
        this.incidentExistantId = incidentExistantId;
    }

    public Long getIncidentExistantId() {
        return incidentExistantId;
    }
}
