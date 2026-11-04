package fr.efrei.downlyzer.modele.equipement;

import fr.efrei.downlyzer.modele.EtatEquipement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipementRepository extends JpaRepository<Equipement, Long> {
    List<Equipement> findByStationId(Long id);

    long countByEtat(EtatEquipement etat);

}
