package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.incident.Incident;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// seul endroit avec le seuil et la fenêtre ; rien n'est stocké, on recalcule à chaque consultation
@Component
public class PolitiqueFragilite {

    static final int SEUIL_INCIDENTS = 3;
    static final Duration FENETRE = Duration.ofDays(30);

    public LocalDateTime debutFenetre(LocalDateTime maintenant) {
        return maintenant.minus(FENETRE);
    }

    // tous les statuts comptent, on regarde juste la date d'ouverture
    public Set<Long> trouverEquipementsFragiles(List<Incident> incidents, LocalDateTime maintenant) {
        LocalDateTime debut = debutFenetre(maintenant);
        return incidents.stream()
                .filter(incident -> !incident.getDateOuverture().isBefore(debut))
                .collect(Collectors.groupingBy(incident -> incident.getEquipement().getId(), Collectors.counting()))
                .entrySet().stream()
                .filter(equipement -> equipement.getValue() >= SEUIL_INCIDENTS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
