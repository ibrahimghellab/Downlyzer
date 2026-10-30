package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.exception.ActionNonAutoriseeException;
import fr.efrei.downlyzer.exception.IncidentIntrouvableException;
import fr.efrei.downlyzer.exception.TransitionInterditeException;
import fr.efrei.downlyzer.modele.Profil;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.equipement.EquipementRepository;
import fr.efrei.downlyzer.modele.evaluation.EvaluationRepository;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.incident.IncidentRepository;
import fr.efrei.downlyzer.modele.prestataire.Prestataire;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import fr.efrei.downlyzer.modele.utilisateur.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Règles de F5-F6 testées sans base de données : les repositories sont simulés et l'horloge est figée
class IncidentServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 10, 20, 12, 0);

    private IncidentRepository incidentRepository;
    private IncidentService incidentService;

    @BeforeEach
    void preparer() {
        incidentRepository = mock(IncidentRepository.class);
        Clock horloge = Clock.fixed(MAINTENANT.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        incidentService = new IncidentService(incidentRepository, mock(EquipementRepository.class),
                mock(UtilisateurRepository.class), mock(EvaluationRepository.class), horloge);
    }

    private Incident incidentEnregistre(Incident incident) {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        return incident;
    }

    private Incident incidentNonAffecte() {
        return Incident.declarerManuellement(new Equipement(), new Utilisateur(),
                "Ascenseur bloqué entre deux étages", MAINTENANT.minusHours(47));
    }

    private Incident incidentAffecte() {
        Incident incident = incidentNonAffecte();
        incident.affecter(new Prestataire(), MAINTENANT.minusHours(2));
        return incident;
    }

    private Incident incidentResolu() {
        Incident incident = incidentAffecte();
        incident.resoudre(MAINTENANT.minusHours(1), "Câble remplacé");
        return incident;
    }

    @Test
    void unAgentPeutModifierLaDescriptionDUnIncidentNonAffecte() {
        Incident incident = incidentEnregistre(incidentNonAffecte());

        incidentService.modifierDescription(1L, "Ascenseur bloqué au 2e étage", Profil.AGENT);

        assertThat(incident.getDescription()).isEqualTo("Ascenseur bloqué au 2e étage");
    }

    @Test
    void unAgentNePeutPlusModifierLaDescriptionDUnIncidentAffecte() {
        Incident incident = incidentEnregistre(incidentAffecte());

        assertThat(incidentService.peutModifierDescription(incident, Profil.AGENT)).isFalse();
        assertThatThrownBy(() -> incidentService.modifierDescription(1L, "Nouvelle description", Profil.AGENT))
                .isInstanceOf(ActionNonAutoriseeException.class);
        assertThat(incident.getDescription()).isEqualTo("Ascenseur bloqué entre deux étages");
    }

    @Test
    void unResponsablePeutModifierLaDescriptionDUnIncidentAffecte() {
        Incident incident = incidentEnregistre(incidentAffecte());

        incidentService.modifierDescription(1L, "Ascenseur bloqué au 2e étage", Profil.RESPONSABLE);

        assertThat(incident.getDescription()).isEqualTo("Ascenseur bloqué au 2e étage");
    }

    @Test
    void personneNePeutModifierLaDescriptionDUnIncidentResolu() {
        Incident incident = incidentEnregistre(incidentResolu());

        for (Profil profil : Profil.values()) {
            assertThat(incidentService.peutModifierDescription(incident, profil)).isFalse();
            assertThatThrownBy(() -> incidentService.modifierDescription(1L, "Nouvelle description", profil))
                    .isInstanceOf(TransitionInterditeException.class);
        }
        assertThat(incident.getDescription()).isEqualTo("Ascenseur bloqué entre deux étages");
    }

    @Test
    void unIncidentInexistantEstSignale() {
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.trouverIncident(999L))
                .isInstanceOf(IncidentIntrouvableException.class)
                .hasMessage("L'incident n° 999 n'existe pas.");
    }

    @Test
    void laDureeDOuvertureDUnIncidentEnCoursVaJusquAMaintenant() {
        assertThat(incidentService.calculerDureeOuverture(incidentNonAffecte())).isEqualTo(Duration.ofHours(47));
    }

    @Test
    void laDureeDOuvertureDUnIncidentResoluSArreteASaResolution() {
        assertThat(incidentService.calculerDureeOuverture(incidentResolu())).isEqualTo(Duration.ofHours(46));
    }
}
