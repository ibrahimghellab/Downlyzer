package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.exception.ActionNonAutoriseeException;
import fr.efrei.downlyzer.exception.DonneeInvalideException;
import fr.efrei.downlyzer.exception.EquipementIntrouvableException;
import fr.efrei.downlyzer.exception.IncidentDejaOuvertException;
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

    // deux écritures (incident + équipement) donc transaction
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

    // un capteur peut renvoyer la même panne, on évite les doublons
    @Transactional
    public Incident declarerParCapteur(String referenceEquipement, String description) {
        Equipement equipement = equipementRepository.findByReference(referenceEquipement)
                .orElseThrow(() -> new EquipementIntrouvableException(referenceEquipement));
        incidentRepository.findFirstByEquipementIdAndStatutNot(equipement.getId(), StatutIncident.RESOLU)
                .ifPresent(existant -> {
                    throw new IncidentDejaOuvertException(referenceEquipement, existant.getId());
                });
        Incident incident = Incident.declarerParCapteur(equipement, description, LocalDateTime.now(horloge));
        equipement.setEtat(EtatEquipement.HORS_SERVICE);
        return incidentRepository.save(incident);
    }

    public List<Incident> listerIncidents(StatutIncident statut) {
        return statut == null ? incidentRepository.findAllByOrderByDateOuvertureDesc()
                : incidentRepository.findByStatutOrderByDateOuvertureDesc(statut);
    }

    public Incident trouverIncident(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentIntrouvableException(id));
    }

    public List<Incident> listerIncidentsResolus() {
        return incidentRepository.findByStatutOrderByDateResolutionDesc(StatutIncident.RESOLU);
    }

    public Duration calculerDureeOuverture(Incident incident) {
        return incident.dureeOuverture(LocalDateTime.now(horloge));
    }

    // seule la description est modifiable : l'agent tant que c'est pas affecté, personne après la résolution
    public boolean peutModifierDescription(Incident incident, Profil profil) {
        return !incident.estResolu() && (profil == Profil.RESPONSABLE || !incident.estAffecte());
    }

    // vérifié dès l'affichage pour pas ouvrir un formulaire qui sera refusé
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

    // la transaction suffit pour enregistrer la modif, pas besoin de save()
    @Transactional
    public void modifierDescription(Long id, String nouvelleDescription, Profil profil) {
        trouverIncidentAModifier(id, profil).modifierDescription(nouvelleDescription);
    }

    public boolean peutAffecterEtResoudre(Incident incident, Profil profil) {
        return profil == Profil.RESPONSABLE && !incident.estResolu();
    }

    public List<Prestataire> listerPrestatairesAffectables(Incident incident) {
        Long typeEquipementId = incident.getEquipement().getTypeEquipement().getId();
        return prestataireRepository.trouverActifsSpecialisesEn(typeEquipementId).stream()
                .filter(prestataire -> !prestataire.equals(incident.getPrestataire()))
                .toList();
    }

    // un incident résolu est refusé directement par Incident.affecter
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

    // l'incident et l'équipement changent ensemble donc transaction
    @Transactional
    public void resoudreIncident(Long id, String commentaire) {
        Incident incident = trouverIncident(id);
        // un commentaire vide est enregistré à null
        String commentaireSaisi = commentaire == null || commentaire.isBlank() ? null : commentaire.strip();
        incident.resoudre(LocalDateTime.now(horloge), commentaireSaisi);
        Equipement equipement = incident.getEquipement();
        if (!incidentRepository.existsByEquipementIdAndIdNotAndStatutNot(equipement.getId(), id, StatutIncident.RESOLU)) {
            equipement.setEtat(EtatEquipement.EN_SERVICE);
        }
    }
}
