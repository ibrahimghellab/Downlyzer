package fr.efrei.downlyzer.modele.incident;

import fr.efrei.downlyzer.modele.StatutIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByEquipementIdOrderByDateOuvertureDesc(Long equipementId);

    List<Incident> findByStatutOrderByDateResolutionDesc(StatutIncident statut);

    List<Incident> findByStatutNot(StatutIncident statut);

    // F8 : l'équipement reste hors service tant qu'un autre de ses incidents n'est pas résolu
    boolean existsByEquipementIdAndIdNotAndStatutNot(Long equipementId, Long incidentId, StatutIncident statut);

    // F11 : incidents en cours (RG6), filtrés par station et par type d'équipement quand ces filtres sont choisis ;
    // un paramètre null désactive son filtre
    @Query("""
            SELECT i FROM Incident i
            WHERE i.statut <> :resolu
              AND (:stationId IS NULL OR i.equipement.station.id = :stationId)
              AND (:typeEquipementId IS NULL OR i.equipement.typeEquipement.id = :typeEquipementId)
            """)
    List<Incident> trouverEnCours(@Param("resolu") StatutIncident resolu,
                                  @Param("stationId") Long stationId,
                                  @Param("typeEquipementId") Long typeEquipementId);

    // F11 : délai moyen de résolution, en minutes, des incidents résolus depuis une date.
    // Seule requête SQL native du projet (§7.6) : voir l'analyse critique dans journal.md
    @Query(value = """
            SELECT AVG(TIMESTAMPDIFF(MINUTE, date_ouverture, date_resolution))
            FROM incident
            WHERE statut = 'RESOLU' AND date_resolution >= :depuis
            """, nativeQuery = true)
    Double calculerDelaiMoyenDeResolutionEnMinutes(@Param("depuis") LocalDateTime depuis);
}
