package fr.efrei.downlyzer.modele.incident;

import fr.efrei.downlyzer.modele.StatutIncident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByEquipementIdOrderByDateOuvertureDesc(Long equipementId);

    // F8 : l'équipement reste hors service tant qu'un autre de ses incidents n'est pas résolu
    boolean existsByEquipementIdAndIdNotAndStatutNot(Long equipementId, Long incidentId, StatutIncident statut);
}
