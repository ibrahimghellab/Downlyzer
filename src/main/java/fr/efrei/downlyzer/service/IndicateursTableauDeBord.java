package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.NiveauAttention;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

// delaiMoyenDeResolution est null si rien n'a été résolu sur la période
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
