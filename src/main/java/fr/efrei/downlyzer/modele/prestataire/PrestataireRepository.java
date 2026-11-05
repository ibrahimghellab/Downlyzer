package fr.efrei.downlyzer.modele.prestataire;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrestataireRepository extends JpaRepository<Prestataire, Long> {

    @Query("""
            SELECT p FROM Prestataire p
            JOIN p.specialites s
            WHERE p.actif = true AND s.id = :typeEquipementId
            ORDER BY p.nom
            """)
    List<Prestataire> trouverActifsSpecialisesEn(@Param("typeEquipementId") Long typeEquipementId);
}
