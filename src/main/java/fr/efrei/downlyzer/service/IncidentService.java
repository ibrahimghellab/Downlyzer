package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.exception.ActionNonAutoriseeException;
import fr.efrei.downlyzer.exception.DonneeInvalideException;
import fr.efrei.downlyzer.exception.EquipementIntrouvableException;
import fr.efrei.downlyzer.exception.IncidentIntrouvableException;
import fr.efrei.downlyzer.exception.PrestataireIntrouvableException;
import fr.efrei.downlyzer.exception.TransitionInterditeException;
import fr.efrei.downlyzer.exception.UtilisateurIntrouvableException;
import fr.efrei.downlyzer.modele.EtatEquipement;
import fr.efrei.downlyzer.modele.Profil;
import fr.efrei.downlyzer.modele.StatutIncident;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.equipement.EquipementRepository;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.incident.IncidentRepository;
import fr.efrei.downlyzer.modele.prestataire.Prestataire;
import fr.efrei.downlyzer.modele.prestataire.PrestataireRepository;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import fr.efrei.downlyzer.modele.utilisateur.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final EquipementRepository equipementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PrestataireRepository prestataireRepository;
    private final Clock horloge;

    public IncidentService(IncidentRepository incidentRepository, EquipementRepository equipementRepository,
                           UtilisateurRepository utilisateurRepository, PrestataireRepository prestataireRepository,
                           Clock horloge) {
        this.incidentRepository = incidentRepository;
        this.equipementRepository = equipementRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.prestataireRepository = prestataireRepository;
        this.horloge = horloge;
    }

    // F4 : l'incident démarre non affecté, ouvert à l'instant présent (RG1), et l'équipement passe hors service.
    // Deux écritures (incident et équipement) : la transaction garantit qu'elles réussissent ensemble (RG10)
    @Transactional
    public Incident declarerIncident(Long equipementId, String description, String loginDeclarant) {
        Equipement equipement = equipementRepository.findById(equipementId)
                .orElseThrow(() -> new EquipementIntrouvableException(equipementId));
        Utilisateur declarant = utilisateurRepository.findByLogin(loginDeclarant)
                .orElseThrow(() -> new UtilisateurIntrouvableException(loginDeclarant));
        Incident incident = Incident.declarerManuellement(
                equipement, declarant, description, LocalDateTime.now(horloge));
        equipement.setEtat(EtatEquipement.HORS_SERVICE);
        return incidentRepository.save(incident);
    }

    // F5
    public Incident trouverIncident(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentIntrouvableException(id));
    }

    // F9 : RG7, du plus récent au plus ancien par date de résolution
    public List<Incident> listerIncidentsResolus() {
        return incidentRepository.findByStatutOrderByDateResolutionDesc(StatutIncident.RESOLU);
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

    // F7-F8 : réservées au responsable, et sans objet une fois l'incident résolu
    public boolean peutAffecterEtResoudre(Incident incident, Profil profil) {
        return profil == Profil.RESPONSABLE && !incident.estResolu();
    }

    // F7 : prestataires actifs spécialisés dans le type de l'équipement, sauf celui qui a déjà l'incident
    public List<Prestataire> listerPrestatairesAffectables(Incident incident) {
        Long typeEquipementId = incident.getEquipement().getTypeEquipement().getId();
        return prestataireRepository.trouverActifsSpecialisesEn(typeEquipementId).stream()
                .filter(prestataire -> !prestataire.equals(incident.getPrestataire()))
                .toList();
    }

    // F7 : affectation et réaffectation ; un incident résolu est refusé par Incident.affecter (RG5)
    @Transactional
    public Prestataire affecterPrestataire(Long id, Long prestataireId) {
        Incident incident = trouverIncident(id);
        Prestataire prestataire = prestataireRepository.findById(prestataireId)
                .orElseThrow(() -> new PrestataireIntrouvableException(prestataireId));
        if (!incident.estResolu() && !listerPrestatairesAffectables(incident).contains(prestataire)) {
            throw new DonneeInvalideException("Le prestataire « " + prestataire.getNom()
                    + " » ne peut pas être affecté à l'incident n° " + id
                    + " : il est inactif, n'intervient pas sur ce type d'équipement ou en est déjà chargé.");
        }
        incident.affecter(prestataire, LocalDateTime.now(horloge));
        return prestataire;
    }

    // F8 : l'incident et, s'il n'a plus d'incident en cours, son équipement changent ensemble (RG10)
    @Transactional
    public void resoudreIncident(Long id, String commentaire) {
        Incident incident = trouverIncident(id);
        // Un commentaire laissé vide est enregistré comme absent, et non comme une chaîne vide
        String commentaireSaisi = commentaire == null || commentaire.isBlank() ? null : commentaire.strip();
        incident.resoudre(LocalDateTime.now(horloge), commentaireSaisi);
        Equipement equipement = incident.getEquipement();
        if (!incidentRepository.existsByEquipementIdAndIdNotAndStatutNot(equipement.getId(), id, StatutIncident.RESOLU)) {
            equipement.setEtat(EtatEquipement.EN_SERVICE);
        }
    }
}
