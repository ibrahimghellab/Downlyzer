package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.NiveauAttention;

// Ce que la liste et la carte des stations affichent ; niveau vaut null quand la station n'a aucun incident en cours.
// Un record plutôt que l'entité : il est transmis tel quel au JavaScript de la carte, sans relation JPA à sérialiser
public record SituationStation(Long id, String nom, String code, double latitude, double longitude,
                               NiveauAttention niveau, long incidentsEnCours) {
}
