package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.EtatEquipement;
import fr.efrei.downlyzer.modele.NiveauAttention;
import fr.efrei.downlyzer.modele.StatutIncident;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.equipement.EquipementRepository;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.incident.IncidentRepository;
import fr.efrei.downlyzer.modele.prestataire.Prestataire;
import fr.efrei.downlyzer.modele.station.Station;
import fr.efrei.downlyzer.modele.typeequipement.TypeEquipementRepository;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TableauDeBordServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 10, 14, 12, 0);

    private IncidentRepository incidentRepository;
    private EquipementRepository equipementRepository;
    private EquipementService equipementService;
    private TableauDeBordService tableauDeBordService;

    private final Station gare = station("Gare Centrale");
    private final Station port = station("Port Fluvial");
    private final Incident rouge100h = ouvertDepuis(port, 100);
    private final Incident rouge80h = ouvertDepuis(gare, 80);
    private final Incident orange50h = ouvertDepuis(port, 50);
    private final Incident vert2h = ouvertDepuis(gare, 2);
    private final Incident prisEnCharge = prisEnCharge(gare);

    @BeforeEach
    void preparer() {
        incidentRepository = mock(IncidentRepository.class);
        equipementRepository = mock(EquipementRepository.class);
        equipementService = mock(EquipementService.class);
        Clock horloge = Clock.fixed(MAINTENANT.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        tableauDeBordService = new TableauDeBordService(incidentRepository, equipementRepository,
                mock(TypeEquipementRepository.class), new PolitiqueUrgence(), equipementService, horloge);
        when(incidentRepository.trouverEnCours(eq(StatutIncident.RESOLU), any(), any()))
                .thenReturn(List.of(vert2h, prisEnCharge, orange50h, rouge80h, rouge100h));
    }

    @Test
    void uneLigneDitSiSonEquipementEstFragile() {
        Equipement fragile = new Equipement();
        ReflectionTestUtils.setField(fragile, "id", 1L);
        fragile.setStation(gare);
        Equipement solide = new Equipement();
        ReflectionTestUtils.setField(solide, "id", 2L);
        solide.setStation(gare);
        Incident surFragile = Incident.declarerManuellement(fragile, new Utilisateur(), "Panne", MAINTENANT.minusHours(1));
        Incident surSolide = Incident.declarerManuellement(solide, new Utilisateur(), "Panne", MAINTENANT.minusHours(2));
        when(incidentRepository.trouverEnCours(eq(StatutIncident.RESOLU), any(), any()))
                .thenReturn(List.of(surFragile, surSolide));
        when(equipementService.listerEquipementsFragiles()).thenReturn(Set.of(1L));

        List<LigneTableauDeBord> lignes = tableauDeBordService.listerIncidentsEnCours(null, null, null, null);

        assertThat(lignes).filteredOn(ligne -> ligne.incident() == surFragile)
                .singleElement().extracting(LigneTableauDeBord::equipementFragile).isEqualTo(true);
        assertThat(lignes).filteredOn(ligne -> ligne.incident() == surSolide)
                .singleElement().extracting(LigneTableauDeBord::equipementFragile).isEqualTo(false);
    }

    private static Station station(String nom) {
        Station station = new Station();
        station.setNom(nom);
        return station;
    }

    private static Incident ouvertDepuis(Station station, long heures) {
        Equipement equipement = new Equipement();
        equipement.setStation(station);
        return Incident.declarerManuellement(equipement, new Utilisateur(), "Panne", MAINTENANT.minusHours(heures));
    }

    private static Incident prisEnCharge(Station station) {
        Incident incident = ouvertDepuis(station, 200);
        incident.affecter(new Prestataire(), MAINTENANT.minusHours(5));
        return incident;
    }

    private List<Incident> incidentsAffiches(NiveauAttention niveau, OrdreTableauDeBord ordre) {
        return tableauDeBordService.listerIncidentsEnCours(null, null, niveau, ordre).stream()
                .map(LigneTableauDeBord::incident)
                .toList();
    }

    @Test
    void parDefautLesIncidentsSontTriesDuPlusUrgentAuMoinsUrgentEtLesPlusAnciensDAbord() {
        assertThat(incidentsAffiches(null, null))
                .containsExactly(rouge100h, rouge80h, orange50h, vert2h, prisEnCharge);
    }

    @Test
    void unIncidentPrisEnChargePasseApresLesIncidentsEnAttenteMemeOuvertDepuisPlusLongtemps() {
        List<LigneTableauDeBord> lignes = tableauDeBordService.listerIncidentsEnCours(null, null, null, null);

        assertThat(lignes.getLast().niveau()).isEqualTo(NiveauAttention.PRIS_EN_CHARGE);
        assertThat(lignes.getLast().dureeOuverture()).isEqualTo(Duration.ofHours(200));
    }

    @Test
    void lesIncidentsPeuventEtreTriesParStationPuisParUrgence() {
        assertThat(incidentsAffiches(null, OrdreTableauDeBord.STATION))
                .containsExactly(rouge80h, vert2h, prisEnCharge, rouge100h, orange50h);
    }

    @Test
    void lesIncidentsPeuventEtreTriesDuPlusRecentAuPlusAncien() {
        assertThat(incidentsAffiches(null, OrdreTableauDeBord.PLUS_RECENTS))
                .containsExactly(vert2h, orange50h, rouge80h, rouge100h, prisEnCharge);
    }

    @Test
    void leFiltreParNiveauNeGardeQueLesIncidentsDeCeNiveau() {
        assertThat(incidentsAffiches(NiveauAttention.ROUGE, null)).containsExactly(rouge100h, rouge80h);
        assertThat(incidentsAffiches(NiveauAttention.PRIS_EN_CHARGE, null)).containsExactly(prisEnCharge);
    }

    @Test
    void lesFiltresParStationEtParTypeSontTransmisALaRequete() {
        when(incidentRepository.trouverEnCours(StatutIncident.RESOLU, 3L, 2L)).thenReturn(List.of(orange50h));

        assertThat(tableauDeBordService.listerIncidentsEnCours(3L, 2L, null, null))
                .extracting(LigneTableauDeBord::incident).containsExactly(orange50h);
    }

    @Test
    void leRegroupementDonneUneColonneParNiveauDansLOrdreDUrgenceEnGardantLeTri() {
        List<LigneTableauDeBord> lignes = tableauDeBordService.listerIncidentsEnCours(null, null, null,
                OrdreTableauDeBord.PLUS_RECENTS);

        Map<NiveauAttention, List<LigneTableauDeBord>> colonnes = tableauDeBordService.regrouperParNiveau(lignes);

        assertThat(colonnes.keySet()).containsExactly(NiveauAttention.ROUGE, NiveauAttention.ORANGE,
                NiveauAttention.VERT, NiveauAttention.PRIS_EN_CHARGE);
        assertThat(colonnes.get(NiveauAttention.ROUGE)).extracting(LigneTableauDeBord::incident)
                .containsExactly(rouge80h, rouge100h);
        assertThat(colonnes.get(NiveauAttention.VERT)).extracting(LigneTableauDeBord::incident).containsExactly(vert2h);
    }

    @Test
    void uneColonneSansIncidentEstPresenteEtVide() {
        assertThat(tableauDeBordService.regrouperParNiveau(List.of())).hasSize(4)
                .allSatisfy((niveau, lignes) -> assertThat(lignes).isEmpty());
    }

    @Test
    void lesIndicateursResumentLaSituationDuReseau() {
        when(equipementRepository.countByEtat(EtatEquipement.HORS_SERVICE)).thenReturn(9L);
        when(incidentRepository.calculerDelaiMoyenDeResolutionEnMinutes(MAINTENANT.minusDays(30))).thenReturn(1530.4);

        IndicateursTableauDeBord indicateurs = tableauDeBordService.calculerIndicateurs();

        assertThat(indicateurs.incidentsEnCours()).isEqualTo(5);
        assertThat(indicateurs.enAttenteDAffectation()).isEqualTo(4);
        assertThat(indicateurs.rouges()).isEqualTo(2);
        assertThat(indicateurs.prisEnCharge()).isEqualTo(1);
        assertThat(indicateurs.equipementsHorsService()).isEqualTo(9);
        assertThat(indicateurs.delaiMoyenDeResolution()).isEqualTo(Duration.ofMinutes(1530));
    }

    @Test
    void laRepartitionDonneLeNombreEtLaPartDeChaqueNiveauDuPlusUrgentAuMoinsUrgent() {
        List<PartNiveau> repartition = tableauDeBordService.calculerIndicateurs().repartition();

        assertThat(repartition).extracting(PartNiveau::niveau).containsExactly(NiveauAttention.ROUGE,
                NiveauAttention.ORANGE, NiveauAttention.VERT, NiveauAttention.PRIS_EN_CHARGE);
        assertThat(repartition).extracting(PartNiveau::nombre).containsExactly(2L, 1L, 1L, 1L);
        assertThat(repartition).extracting(PartNiveau::pourcentage).containsExactly(40.0, 20.0, 20.0, 20.0);
    }

    @Test
    void sansIncidentEnCoursLaRepartitionEstVideSansDivisionParZero() {
        when(incidentRepository.trouverEnCours(eq(StatutIncident.RESOLU), any(), any())).thenReturn(List.of());

        IndicateursTableauDeBord indicateurs = tableauDeBordService.calculerIndicateurs();

        assertThat(indicateurs.incidentsEnCours()).isZero();
        assertThat(indicateurs.repartition()).allSatisfy(part -> assertThat(part.pourcentage()).isZero());
    }

    @Test
    void sansResolutionSurLaPeriodeLeDelaiMoyenEstAbsent() {
        when(incidentRepository.calculerDelaiMoyenDeResolutionEnMinutes(any())).thenReturn(null);

        assertThat(tableauDeBordService.calculerIndicateurs().delaiMoyenDeResolution()).isNull();
    }

    @Test
    void sansFiltreLaRequeteNeFiltreNiParStationNiParType() {
        when(incidentRepository.trouverEnCours(eq(StatutIncident.RESOLU), isNull(), isNull())).thenReturn(List.of(vert2h));

        assertThat(tableauDeBordService.listerIncidentsEnCours(null, null, null, null)).hasSize(1);
    }
}
