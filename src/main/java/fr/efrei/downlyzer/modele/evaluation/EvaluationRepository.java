package fr.efrei.downlyzer.modele.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findByIncidentId(Long incidentId);

    boolean existsByIncidentId(Long incidentId);

    // F10 : la note d'un prestataire est la moyenne des notes des incidents qu'il a traités
    @Query("""
            SELECT new fr.efrei.downlyzer.modele.evaluation.NoteMoyennePrestataire(
                i.prestataire.id, AVG(e.note), COUNT(e))
            FROM Evaluation e
            JOIN e.incident i
            GROUP BY i.prestataire.id
            """)
    List<NoteMoyennePrestataire> calculerNotesMoyennes();
}
