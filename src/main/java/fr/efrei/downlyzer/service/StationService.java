package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.exception.StationIntrouvableException;
import fr.efrei.downlyzer.modele.NiveauAttention;
import fr.efrei.downlyzer.modele.StatutIncident;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.equipement.EquipementRepository;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.incident.IncidentRepository;
import fr.efrei.downlyzer.modele.station.Station;
import fr.efrei.downlyzer.modele.station.StationRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StationService {

    private final StationRepository stationRepository;
    private final EquipementRepository equipementRepository;
    private final IncidentRepository incidentRepository;
    private final PolitiqueUrgence politiqueUrgence;
    private final Clock horloge;

    public StationService(StationRepository stationRepository, EquipementRepository equipementRepository,
                          IncidentRepository incidentRepository, PolitiqueUrgence politiqueUrgence, Clock horloge) {
        this.stationRepository = stationRepository;
        this.equipementRepository = equipementRepository;
        this.incidentRepository = incidentRepository;
        this.politiqueUrgence = politiqueUrgence;
        this.horloge = horloge;
    }

    public List<Station> listerStations() {
        return stationRepository.findAllByOrderByNomAsc();
    }

    // une station prend le niveau de son incident le plus urgent
    public List<SituationStation> listerSituationsDesStations() {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        Map<Station, List<Incident>> incidentsEnCoursParStation =
                incidentRepository.findByStatutNot(StatutIncident.RESOLU).stream()
                        .collect(Collectors.groupingBy(incident -> incident.getEquipement().getStation()));
        return listerStations().stream()
                .map(station -> {
                    List<Incident> incidentsEnCours = incidentsEnCoursParStation.getOrDefault(station, List.of());
                    NiveauAttention niveau = incidentsEnCours.stream()
                            .map(incident -> politiqueUrgence.evaluer(incident, maintenant))
                            .min(Comparator.naturalOrder())
                            .orElse(null);
                    return new SituationStation(station.getId(), station.getNom(), station.getCode(),
                            station.getLatitude(), station.getLongitude(), niveau, incidentsEnCours.size());
                })
                .toList();
    }

    public Station trouverStation(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new StationIntrouvableException(id));
    }

    public List<Equipement> listerEquipementsDeLaStation(Long stationId) {
        return equipementRepository.findByStationId(stationId);
    }
}
