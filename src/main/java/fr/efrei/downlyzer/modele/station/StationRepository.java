package fr.efrei.downlyzer.modele.station;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StationRepository extends JpaRepository<Station, Long> {
    List <Station> findAllByOrderByNomAsc();
}
