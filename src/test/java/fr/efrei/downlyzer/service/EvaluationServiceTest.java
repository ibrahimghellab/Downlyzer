package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.exception.DonneeInvalideException;
import fr.efrei.downlyzer.exception.TransitionInterditeException;
import fr.efrei.downlyzer.modele.Profil;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.evaluation.Evaluation;
import fr.efrei.downlyzer.modele.evaluation.EvaluationRepository;
import fr.efrei.downlyzer.modele.evaluation.NoteMoyennePrestataire;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.incident.IncidentRepository;
import fr.efrei.downlyzer.modele.prestataire.Prestataire;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import fr.efrei.downlyzer.modele.utilisateur.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EvaluationServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 10, 20, 12, 0);

    private EvaluationRepository evaluationRepository;
    private IncidentRepository incidentRepository;
    private EvaluationService evaluationService;
    private Utilisateur claire;

    @BeforeEach
    void preparer() {
        evaluationRepository = mock(EvaluationRepository.class);
        incidentRepository = mock(IncidentRepository.class);
        UtilisateurRepository utilisateurRepository = mock(UtilisateurRepository.class);
        Clock horloge = Clock.fixed(MAINTENANT.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        evaluationService = new EvaluationService(evaluationRepository, incidentRepository, utilisateurRepository, horloge);

        claire = new Utilisateur();
        when(utilisateurRepository.findByLogin("claire.martin")).thenReturn(Optional.of(claire));
        when(evaluationRepository.save(any())).thenAnswer(appel -> appel.getArgument(0));
    }

    private Incident incidentEnregistre(Incident incident) {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        return incident;
    }

    private Incident incidentNonResolu() {
        return Incident.declarerManuellement(new Equipement(), new Utilisateur(), "Ascenseur bloqué",
                MAINTENANT.minusDays(3));
    }

    private Incident incidentResoluParUnPrestataire() {
        Incident incident = incidentNonResolu();
        incident.affecter(new Prestataire(), MAINTENANT.minusDays(2));
        incident.resoudre(MAINTENANT.minusDays(1), "Câble remplacé");
        return incident;
    }

    private Incident incidentResoluSansPrestataire() {
        Incident incident = incidentNonResolu();
        incident.resoudre(MAINTENANT.minusDays(1), "Fausse alerte");
        return incident;
    }

    @Test
    void unIncidentResoluParUnPrestataireEstEvalueParLeResponsableConnecte() {
        Incident incident = incidentEnregistre(incidentResoluParUnPrestataire());

        Evaluation evaluation = evaluationService.evaluerIntervention(1L, 4, "  Intervention rapide.  ", "claire.martin");

        assertThat(evaluation.getIncident()).isSameAs(incident);
        assertThat(evaluation.getAuteur()).isSameAs(claire);
        assertThat(evaluation.getNote()).isEqualTo(4);
        assertThat(evaluation.getCommentaire()).isEqualTo("Intervention rapide.");
        assertThat(evaluation.getDateEvaluation()).isEqualTo(MAINTENANT);
    }

    @Test
    void unIncidentNonResoluNePeutPasEtreEvalue() {
        incidentEnregistre(incidentNonResolu());

        assertThatThrownBy(() -> evaluationService.evaluerIntervention(1L, 4, null, "claire.martin"))
                .isInstanceOf(TransitionInterditeException.class);
        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void unIncidentResoluSansPrestataireNePeutPasEtreEvalue() {
        incidentEnregistre(incidentResoluSansPrestataire());

        assertThatThrownBy(() -> evaluationService.evaluerIntervention(1L, 4, null, "claire.martin"))
                .isInstanceOf(TransitionInterditeException.class);
    }

    @Test
    void unIncidentNePeutEtreEvalueQuUneFois() {
        incidentEnregistre(incidentResoluParUnPrestataire());
        when(evaluationRepository.existsByIncidentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> evaluationService.evaluerIntervention(1L, 4, null, "claire.martin"))
                .isInstanceOf(TransitionInterditeException.class)
                .hasMessageContaining("déjà été évaluée");
        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void laNoteEstUnEntierDeUnACinq() {
        incidentEnregistre(incidentResoluParUnPrestataire());

        for (int noteInvalide : new int[]{0, 6}) {
            assertThatThrownBy(() -> evaluationService.evaluerIntervention(1L, noteInvalide, null, "claire.martin"))
                    .isInstanceOf(DonneeInvalideException.class);
        }
        for (int noteValide : new int[]{1, 5}) {
            assertThat(evaluationService.evaluerIntervention(1L, noteValide, null, "claire.martin").getNote())
                    .isEqualTo(noteValide);
        }
    }

    @Test
    void unCommentaireVideEstEnregistreCommeAbsent() {
        incidentEnregistre(incidentResoluParUnPrestataire());

        assertThat(evaluationService.evaluerIntervention(1L, 3, "   ", "claire.martin").getCommentaire()).isNull();
    }

    @Test
    void seulLeResponsablePeutEvaluerUnIncidentEvaluablePasEncoreEvalue() {
        Incident evaluable = incidentResoluParUnPrestataire();

        assertThat(evaluationService.peutEvaluer(evaluable, false, Profil.RESPONSABLE)).isTrue();
        assertThat(evaluationService.peutEvaluer(evaluable, false, Profil.AGENT)).isFalse();
        assertThat(evaluationService.peutEvaluer(evaluable, true, Profil.RESPONSABLE)).isFalse();
        assertThat(evaluationService.peutEvaluer(incidentResoluSansPrestataire(), false, Profil.RESPONSABLE)).isFalse();
        assertThat(evaluationService.peutEvaluer(incidentNonResolu(), false, Profil.RESPONSABLE)).isFalse();
    }

    @Test
    void lesNotesMoyennesSontIndexeesParPrestataire() {
        NoteMoyennePrestataire ascenso = new NoteMoyennePrestataire(1L, 4.5, 2L);
        when(evaluationRepository.calculerNotesMoyennes()).thenReturn(List.of(ascenso));

        assertThat(evaluationService.calculerNotesMoyennesParPrestataire()).containsEntry(1L, ascenso).hasSize(1);
    }
}
