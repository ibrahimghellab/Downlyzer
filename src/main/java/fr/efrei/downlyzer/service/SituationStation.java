package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.NiveauAttention;

// niveau null si aucun incident en cours
public record SituationStation(Long id, String nom, String code, double latitude, double longitude,
                               NiveauAttention niveau, long incidentsEnCours) {
}
