package fr.efrei.downlyzer.modele.incident;

import fr.efrei.downlyzer.modele.StatutIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByEquipementIdOrderByDateOuvertureDesc(Long equipementId);

    List<Incident> findByStatutOrderByDateResolutionDesc(StatutIncident statut);

    List<Incident> findByStatutNot(StatutIncident statut);

    List<Incident> findAllByOrderByDateOuvertureDesc();

    List<Incident> findByStatutOrderByDateOuvertureDesc(StatutIncident statut);

    Optional<Incident> findFirstByEquipementIdAndStatutNot(Long equipementId, StatutIncident statut);

    // l'équipement reste hors service tant qu'il a un autre incident en cours
    boolean existsByEquipementIdAndIdNotAndStatutNot(Long equipementId, Long incidentId, StatutIncident statut);

    // un paramètre null veut dire pas de filtre
    @Query("""
            SELECT i FROM Incident i
            WHERE i.statut <> :resolu
              AND (:stationId IS NULL OR i.equipement.station.id = :stationId)
              AND (:typeEquipementId IS NULL OR i.equipement.typeEquipement.id = :typeEquipementId)
            """)
    List<Incident> trouverEnCours(@Param("resolu") StatutIncident resolu,
                                  @Param("stationId") Long stationId,
                                  @Param("typeEquipementId") Long typeEquipementId);

    // seule requête native du projet (analyse dans journal.md)
    @Query(value = """
            SELECT AVG(TIMESTAMPDIFF(MINUTE, date_ouverture, date_resolution))
            FROM incident
            WHERE statut = 'RESOLU' AND date_resolution >= :depuis
            """, nativeQuery = true)
    Double calculerDelaiMoyenDeResolutionEnMinutes(@Param("depuis") LocalDateTime depuis);
}
