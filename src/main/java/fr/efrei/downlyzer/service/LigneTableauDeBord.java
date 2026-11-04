package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.NiveauAttention;
import fr.efrei.downlyzer.modele.incident.Incident;

import java.time.Duration;

// Un incident en cours et ce qui est calculé pour lui à l'instant de l'affichage (RG3, jamais stocké)
public record LigneTableauDeBord(Incident incident, NiveauAttention niveau, Duration dureeOuverture) {
}
