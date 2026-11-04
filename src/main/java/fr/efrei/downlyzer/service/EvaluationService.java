package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.exception.DonneeInvalideException;
import fr.efrei.downlyzer.exception.IncidentIntrouvableException;
import fr.efrei.downlyzer.exception.TransitionInterditeException;
import fr.efrei.downlyzer.exception.UtilisateurIntrouvableException;
import fr.efrei.downlyzer.modele.Profil;
import fr.efrei.downlyzer.modele.evaluation.Evaluation;
import fr.efrei.downlyzer.modele.evaluation.EvaluationRepository;
import fr.efrei.downlyzer.modele.evaluation.NoteMoyennePrestataire;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.incident.IncidentRepository;
import fr.efrei.downlyzer.modele.utilisateur.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

// Évaluation des prestataires (F10), séparée d'IncidentService : ses règles (RG8) et ses données sont propres
@Service
public class EvaluationService {

    static final int NOTE_MINIMALE = 1;
    static final int NOTE_MAXIMALE = 5;

    private final EvaluationRepository evaluationRepository;
    private final IncidentRepository incidentRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final Clock horloge;

    public EvaluationService(EvaluationRepository evaluationRepository, IncidentRepository incidentRepository,
                             UtilisateurRepository utilisateurRepository, Clock horloge) {
        this.evaluationRepository = evaluationRepository;
        this.incidentRepository = incidentRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.horloge = horloge;
    }

    public Optional<Evaluation> trouverEvaluation(Long incidentId) {
        return evaluationRepository.findByIncidentId(incidentId);
    }

    // F9 : l'évaluation de chaque incident de l'historique, chargée en une seule requête
    public Map<Long, Evaluation> trouverEvaluationsParIncident() {
        return evaluationRepository.findAll().stream()
                .collect(Collectors.toMap(evaluation -> evaluation.getIncident().getId(), Function.identity()));
    }

    // F7 et F10 : la note d'un prestataire est consultée au moment de choisir à qui confier un incident
    public Map<Long, NoteMoyennePrestataire> calculerNotesMoyennesParPrestataire() {
        return evaluationRepository.calculerNotesMoyennes().stream()
                .collect(Collectors.toMap(NoteMoyennePrestataire::prestataireId, Function.identity()));
    }

    // RG8 : un incident résolu, pris en charge par un prestataire et pas encore évalué ; réservé au responsable
    public boolean peutEvaluer(Incident incident, boolean dejaEvalue, Profil profil) {
        return profil == Profil.RESPONSABLE && incident.estEvaluable() && !dejaEvalue;
    }

    public Evaluation evaluerIntervention(Long incidentId, int note, String commentaire, String loginAuteur) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentIntrouvableException(incidentId));
        if (!incident.estEvaluable()) {
            throw new TransitionInterditeException("L'incident n° " + incidentId
                    + " ne peut pas être évalué : seul un incident résolu par un prestataire peut l'être.");
        }
        if (evaluationRepository.existsByIncidentId(incidentId)) {
            throw new TransitionInterditeException("L'intervention sur l'incident n° " + incidentId
                    + " a déjà été évaluée.");
        }
        if (note < NOTE_MINIMALE || note > NOTE_MAXIMALE) {
            throw new DonneeInvalideException("La note doit être comprise entre "
                    + NOTE_MINIMALE + " et " + NOTE_MAXIMALE + ".");
        }
        Evaluation evaluation = new Evaluation();
        evaluation.setIncident(incident);
        evaluation.setAuteur(utilisateurRepository.findByLogin(loginAuteur)
                .orElseThrow(() -> new UtilisateurIntrouvableException(loginAuteur)));
        evaluation.setNote(note);
        evaluation.setCommentaire(commentaire == null || commentaire.isBlank() ? null : commentaire.strip());
        evaluation.setDateEvaluation(LocalDateTime.now(horloge));
        return evaluationRepository.save(evaluation);
    }
}
