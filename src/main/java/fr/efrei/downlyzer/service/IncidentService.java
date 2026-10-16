package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.exception.EquipementIntrouvableException;
import fr.efrei.downlyzer.exception.UtilisateurIntrouvableException;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.equipement.EquipementRepository;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.incident.IncidentRepository;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import fr.efrei.downlyzer.modele.utilisateur.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final EquipementRepository equipementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final Clock horloge;

    public IncidentService(IncidentRepository incidentRepository, EquipementRepository equipementRepository,
                           UtilisateurRepository utilisateurRepository, Clock horloge) {
        this.incidentRepository = incidentRepository;
        this.equipementRepository = equipementRepository;
        this.utilisateurRepository = utilisateurRepository;
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
}