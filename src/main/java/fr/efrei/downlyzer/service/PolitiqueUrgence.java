package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.NiveauAttention;
import fr.efrei.downlyzer.modele.incident.Incident;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

// seul endroit avec les seuils ; l'heure est passée en paramètre pour pouvoir tester
@Component
public class PolitiqueUrgence {

    static final Duration SEUIL_ORANGE = Duration.ofHours(48);
    static final Duration SEUIL_ROUGE = Duration.ofHours(72);

    public NiveauAttention evaluer(Incident incident, LocalDateTime maintenant) {
        if (incident.estResolu()) {
            throw new IllegalArgumentException("Un incident résolu n'a plus de niveau d'attention (RG6).");
        }
        if (incident.estAffecte()) {
            return NiveauAttention.PRIS_EN_CHARGE;
        }
        Duration dureeOuverture = incident.dureeOuverture(maintenant);
        if (dureeOuverture.compareTo(SEUIL_ROUGE) >= 0) {
            return NiveauAttention.ROUGE;
        }
        if (dureeOuverture.compareTo(SEUIL_ORANGE) >= 0) {
            return NiveauAttention.ORANGE;
        }
        return NiveauAttention.VERT;
    }
}
