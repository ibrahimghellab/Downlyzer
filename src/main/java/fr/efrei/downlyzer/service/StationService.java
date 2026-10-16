package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.exception.StationIntrouvableException;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.equipement.EquipementRepository;
import fr.efrei.downlyzer.modele.station.Station;
import fr.efrei.downlyzer.modele.station.StationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationService {

    private final StationRepository stationRepository;
    private final EquipementRepository equipementRepository;

    public StationService(StationRepository stationRepository, EquipementRepository equipementRepository) {
        this.stationRepository = stationRepository;
        this.equipementRepository = equipementRepository;
    }

    public List<Station> listerStations() {
        return stationRepository.findAllByOrderByNomAsc();
    }

    public Station trouverStation(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new StationIntrouvableException(id));
    }

    public List<Equipement> listerEquipementsDeLaStation(Long stationId) {
        return equipementRepository.findByStationId(stationId);
    }
}