package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.exception.ActionNonAutoriseeException;
import fr.efrei.downlyzer.exception.EquipementIntrouvableException;
import fr.efrei.downlyzer.exception.IncidentIntrouvableException;
import fr.efrei.downlyzer.exception.TransitionInterditeException;
import fr.efrei.downlyzer.exception.UtilisateurIntrouvableException;
import fr.efrei.downlyzer.modele.Profil;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.equipement.EquipementRepository;
import fr.efrei.downlyzer.modele.evaluation.Evaluation;
import fr.efrei.downlyzer.modele.evaluation.EvaluationRepository;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.incident.IncidentRepository;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import fr.efrei.downlyzer.modele.utilisateur.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final EquipementRepository equipementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EvaluationRepository evaluationRepository;
    private final Clock horloge;

    public IncidentService(IncidentRepository incidentRepository, EquipementRepository equipementRepository,
                           UtilisateurRepository utilisateurRepository, EvaluationRepository evaluationRepository,
                           Clock horloge) {
        this.incidentRepository = incidentRepository;
        this.equipementRepository = equipementRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.evaluationRepository = evaluationRepository;
        this.horloge = horloge;
    }

    // F4 : l'incident démarre non affecté, ouvert à l'instant présent (RG1)
    public Incident declarerIncident(Long equipementId, String description, String loginDeclarant) {
        Equipement equipement = equipementRepository.findById(equipementId)
                .orElseThrow(() -> new EquipementIntrouvableException(equipementId));
        Utilisateur declarant = utilisateurRepository.findByLogin(loginDeclarant)
                .orElseThrow(() -> new UtilisateurIntrouvableException(loginDeclarant));
        Incident incident = Incident.declarerManuellement(
                equipement, declarant, description, LocalDateTime.now(horloge));
        return incidentRepository.save(incident);
    }

    // F5
    public Incident trouverIncident(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentIntrouvableException(id));
    }

    public Optional<Evaluation> trouverEvaluation(Long incidentId) {
        return evaluationRepository.findByIncidentId(incidentId);
    }

    public Duration calculerDureeOuverture(Incident incident) {
        return incident.dureeOuverture(LocalDateTime.now(horloge));
    }

    // F6 : seule la description est modifiable. L'agent ne peut plus y toucher dès qu'un prestataire
    // a pris l'incident en charge ; personne ne le peut une fois l'incident résolu
    public boolean peutModifierDescription(Incident incident, Profil profil) {
        return !incident.estResolu() && (profil == Profil.RESPONSABLE || !incident.estAffecte());
    }

    // Vérifié dès l'affichage du formulaire : une adresse saisie à la main ne doit pas ouvrir
    // un formulaire dont l'envoi serait refusé (RG11)
    public Incident trouverIncidentAModifier(Long id, Profil profil) {
        Incident incident = trouverIncident(id);
        if (incident.estResolu()) {
            throw new TransitionInterditeException(
                    "L'incident n° " + id + " est résolu : sa description ne peut plus être modifiée.");
        }
        if (!peutModifierDescription(incident, profil)) {
            throw new ActionNonAutoriseeException("L'incident n° " + id + " est pris en charge par un prestataire : "
                    + "seul un responsable d'exploitation peut encore modifier sa description.");
        }
        return incident;
    }

    // Transactionnel pour que la modification de l'entité chargée soit enregistrée à la fin de la méthode,
    // sans appel à save() ; le verrouillage optimiste (@Version) y est vérifié au même moment
    @Transactional
    public void modifierDescription(Long id, String nouvelleDescription, Profil profil) {
        trouverIncidentAModifier(id, profil).modifierDescription(nouvelleDescription);
    }
}
