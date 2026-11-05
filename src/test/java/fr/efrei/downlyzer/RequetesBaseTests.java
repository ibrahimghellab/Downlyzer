package fr.efrei.downlyzer;

import fr.efrei.downlyzer.modele.StatutIncident;
import fr.efrei.downlyzer.modele.evaluation.EvaluationRepository;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.incident.IncidentRepository;
import fr.efrei.downlyzer.modele.prestataire.PrestataireRepository;
import fr.efrei.downlyzer.service.EquipementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// vraies requêtes sur les données de db/create.sql, en lecture seule ; sautés s'il n'y a pas de base
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
class RequetesBaseTests {

    private static final Long ID_ESC_003 = 13L;
    private static final Long ID_STATION_ESC_003 = 5L;
    private static final Long ID_TYPE_ASCENSEUR = 1L;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private PrestataireRepository prestataireRepository;

    @Autowired
    private EquipementService equipementService;

    @Test
    void unIncidentResoluNApparaitPasParmiLesIncidentsEnCours() {
        List<Incident> enCours = incidentRepository.trouverEnCours(StatutIncident.RESOLU, null, null);

        assertThat(enCours).isNotEmpty().noneMatch(Incident::estResolu);
    }

    @Test
    void unIncidentResoluResteDansLHistoriqueDuPlusRecentAuPlusAncien() {
        List<LocalDateTime> dates = incidentRepository.findByStatutOrderByDateResolutionDesc(StatutIncident.RESOLU)
                .stream().map(Incident::getDateResolution).toList();

        assertThat(dates).hasSizeGreaterThan(1).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void lesIncidentsDUnEquipementSontTriesParDateDOuverture() {
        List<LocalDateTime> dates = incidentRepository.findByEquipementIdOrderByDateOuvertureDesc(ID_ESC_003)
                .stream().map(Incident::getDateOuverture).toList();

        assertThat(dates).hasSizeGreaterThan(1).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void leFiltreParStationNeGardeQueLesIncidentsDeCetteStation() {
        List<Incident> incidents = incidentRepository.trouverEnCours(StatutIncident.RESOLU, ID_STATION_ESC_003, null);

        assertThat(incidents).isNotEmpty().allMatch(
                incident -> incident.getEquipement().getStation().getId().equals(ID_STATION_ESC_003));
    }

    @Test
    void laRequeteNativeCalculeUnDelaiMoyen() {
        Double delaiEnMinutes = incidentRepository.calculerDelaiMoyenDeResolutionEnMinutes(
                LocalDateTime.now().minusDays(30));

        assertThat(delaiEnMinutes).isPositive();
    }

    @Test
    void lesNotesMoyennesDesPrestatairesSontComprisesEntre1Et5() {
        assertThat(evaluationRepository.calculerNotesMoyennes())
                .isNotEmpty().allMatch(note -> note.moyenne() >= 1 && note.moyenne() <= 5);
    }

    @Test
    void seulsLesPrestatairesActifsEtSpecialisesSontProposes() {
        assertThat(prestataireRepository.trouverActifsSpecialisesEn(ID_TYPE_ASCENSEUR))
                .isNotEmpty().allMatch(prestataire -> prestataire.isActif());
    }

    @Test
    void lesEquipementsFragilesSontTrouvesDansLaBase() {
        assertThat(equipementService.estFragile(ID_ESC_003)).isTrue();
    }
}
