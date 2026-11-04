package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.NiveauAttention;
import fr.efrei.downlyzer.modele.StatutIncident;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.equipement.EquipementRepository;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.incident.IncidentRepository;
import fr.efrei.downlyzer.modele.prestataire.Prestataire;
import fr.efrei.downlyzer.modele.station.Station;
import fr.efrei.downlyzer.modele.station.StationRepository;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Piste 4 : la situation de chaque station, avec la vraie politique d'urgence et une horloge figée
class StationServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 10, 14, 12, 0);

    private StationRepository stationRepository;
    private IncidentRepository incidentRepository;
    private StationService stationService;

    @BeforeEach
    void preparer() {
        stationRepository = mock(StationRepository.class);
        incidentRepository = mock(IncidentRepository.class);
        Clock horloge = Clock.fixed(MAINTENANT.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        stationService = new StationService(stationRepository, mock(EquipementRepository.class), incidentRepository,
                new PolitiqueUrgence(), horloge);
    }

    private static Station station(String nom) {
        Station station = new Station();
        station.setNom(nom);
        return station;
    }

    private static Incident incidentOuvertDepuis(Station station, long heures) {
        Equipement equipement = new Equipement();
        equipement.setStation(station);
        return Incident.declarerManuellement(equipement, new Utilisateur(), "Panne", MAINTENANT.minusHours(heures));
    }

    private static Incident incidentPrisEnCharge(Station station) {
        Incident incident = incidentOuvertDepuis(station, 80);
        incident.affecter(new Prestataire(), MAINTENANT.minusHours(10));
        return incident;
    }

    private List<SituationStation> situationsAvec(List<Station> stations, List<Incident> incidentsEnCours) {
        when(stationRepository.findAllByOrderByNomAsc()).thenReturn(stations);
        when(incidentRepository.findByStatutNot(StatutIncident.RESOLU)).thenReturn(incidentsEnCours);
        return stationService.listerSituationsDesStations();
    }

    @Test
    void uneStationPrendLeNiveauDeSonIncidentLePlusUrgent() {
        Station gare = station("Gare Centrale");

        List<SituationStation> situations = situationsAvec(List.of(gare),
                List.of(incidentOuvertDepuis(gare, 2), incidentOuvertDepuis(gare, 72), incidentPrisEnCharge(gare)));

        assertThat(situations.getFirst().niveau()).isEqualTo(NiveauAttention.ROUGE);
        assertThat(situations.getFirst().incidentsEnCours()).isEqualTo(3);
    }

    @Test
    void unIncidentEnAttenteDAffectationPasseAvantUnIncidentPrisEnCharge() {
        Station gare = station("Gare Centrale");

        List<SituationStation> situations = situationsAvec(List.of(gare),
                List.of(incidentPrisEnCharge(gare), incidentOuvertDepuis(gare, 2)));

        assertThat(situations.getFirst().niveau()).isEqualTo(NiveauAttention.VERT);
    }

    @Test
    void uneStationDontTousLesIncidentsSontAffectesEstPriseEnCharge() {
        Station gare = station("Gare Centrale");

        assertThat(situationsAvec(List.of(gare), List.of(incidentPrisEnCharge(gare))).getFirst().niveau())
                .isEqualTo(NiveauAttention.PRIS_EN_CHARGE);
    }

    @Test
    void uneStationSansIncidentEnCoursNAPasDeNiveau() {
        Station gare = station("Gare Centrale");
        Station port = station("Port Fluvial");

        List<SituationStation> situations = situationsAvec(List.of(gare, port), List.of(incidentOuvertDepuis(gare, 50)));

        assertThat(situations).extracting(SituationStation::nom).containsExactly("Gare Centrale", "Port Fluvial");
        assertThat(situations.get(0).niveau()).isEqualTo(NiveauAttention.ORANGE);
        assertThat(situations.get(1).niveau()).isNull();
        assertThat(situations.get(1).incidentsEnCours()).isZero();
    }
}
