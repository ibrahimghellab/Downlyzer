package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.exception.EquipementIntrouvableException;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.equipement.EquipementRepository;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.incident.IncidentRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class EquipementService {

    private final EquipementRepository equipementRepository;
    private final IncidentRepository incidentRepository;
    private final PolitiqueFragilite politiqueFragilite;
    private final Clock horloge;

    public EquipementService(EquipementRepository equipementRepository, IncidentRepository incidentRepository,
                             PolitiqueFragilite politiqueFragilite, Clock horloge) {
        this.equipementRepository = equipementRepository;
        this.incidentRepository = incidentRepository;
        this.politiqueFragilite = politiqueFragilite;
        this.horloge = horloge;
    }

    public Equipement trouverEquipement(Long id) {
        return equipementRepository.findById(id)
                .orElseThrow(() -> new EquipementIntrouvableException(id));
    }

    public List<Incident> listerIncidentsDeLEquipement(Long equipementId) {
        return incidentRepository.findByEquipementIdOrderByDateOuvertureDesc(equipementId);
    }

    // une seule requête, le tableau de bord en a besoin pour toutes ses lignes
    public Set<Long> listerEquipementsFragiles() {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        List<Incident> incidents = incidentRepository.findByDateOuvertureGreaterThanEqual(
                politiqueFragilite.debutFenetre(maintenant));
        return politiqueFragilite.trouverEquipementsFragiles(incidents, maintenant);
    }

    public boolean estFragile(Long equipementId) {
        return listerEquipementsFragiles().contains(equipementId);
    }
}
