package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.exception.EquipementIntrouvableException;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.equipement.EquipementRepository;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.incident.IncidentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EquipementService {

    private final EquipementRepository equipementRepository;
    private final IncidentRepository incidentRepository;

    public EquipementService(EquipementRepository equipementRepository, IncidentRepository incidentRepository) {
        this.equipementRepository = equipementRepository;
        this.incidentRepository = incidentRepository;
    }

    public Equipement trouverEquipement(Long id){
        return equipementRepository.findById(id)
                .orElseThrow(() -> new EquipementIntrouvableException(id));
    }

    public List<Incident> listerIncidentsDeLEquipement(Long equipementId) {
        return incidentRepository.findByEquipementIdOrderByDateOuvertureDesc(equipementId);
    }

}
