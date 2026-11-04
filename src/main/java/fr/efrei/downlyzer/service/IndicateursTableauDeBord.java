package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.NiveauAttention;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

// F11 : indicateurs synthétiques sur l'ensemble du réseau. La répartition contient un élément par niveau,
// du plus urgent au moins urgent ; delaiMoyenDeResolution vaut null quand rien n'a été résolu sur la période.
// calculesLe est l'instant du calcul, affiché pour situer les niveaux d'attention, qui changent avec l'heure
public record IndicateursTableauDeBord(LocalDateTime calculesLe, long incidentsEnCours, List<PartNiveau> repartition,
                                       long equipementsHorsService, Duration delaiMoyenDeResolution) {

    public long nombre(NiveauAttention niveau) {
        return repartition.stream()
                .filter(part -> part.niveau() == niveau)
                .mapToLong(PartNiveau::nombre)
                .sum();
    }

    public long rouges() {
        return nombre(NiveauAttention.ROUGE);
    }

    public long oranges() {
        return nombre(NiveauAttention.ORANGE);
    }

    public long prisEnCharge() {
        return nombre(NiveauAttention.PRIS_EN_CHARGE);
    }

    public long enAttenteDAffectation() {
        return incidentsEnCours - prisEnCharge();
    }
}
