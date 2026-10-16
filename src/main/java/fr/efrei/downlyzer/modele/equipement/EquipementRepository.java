package fr.efrei.downlyzer.modele.equipement;

import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.station.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipementRepository extends JpaRepository<Equipement, Long> {
    List<Equipement> findByStationId(Long id);

}
