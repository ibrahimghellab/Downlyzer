package fr.efrei.downlyzer.modele.equipement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipementRepository extends JpaRepository<Equipement, Long> {
    List<Equipement> findByStationId(Long id);

}
