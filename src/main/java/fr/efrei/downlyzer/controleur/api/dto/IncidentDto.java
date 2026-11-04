package fr.efrei.downlyzer.controleur.api.dto;

import fr.efrei.downlyzer.modele.incident.Incident;

import java.time.LocalDateTime;

// on renvoie un dto et pas l'entité pour pas exposer les champs internes
public record IncidentDto(
        Long id,
        String referenceEquipement,
        String station,
        String description,
        String source,
        String statut,
        LocalDateTime dateOuverture,
        String prestataire,
        LocalDateTime dateAffectation,
        LocalDateTime dateResolution,
        String commentaireResolution) {

    public static IncidentDto depuis(Incident incident) {
        return new IncidentDto(
                incident.getId(),
                incident.getEquipement().getReference(),
                incident.getEquipement().getStation().getNom(),
                incident.getDescription(),
                incident.getSource().name(),
                incident.getStatut().name(),
                incident.getDateOuverture(),
                incident.getPrestataire() == null ? null : incident.getPrestataire().getNom(),
                incident.getDateAffectation(),
                incident.getDateResolution(),
                incident.getCommentaireResolution());
    }
}
