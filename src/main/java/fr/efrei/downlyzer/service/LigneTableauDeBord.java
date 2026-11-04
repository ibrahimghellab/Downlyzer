package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.NiveauAttention;
import fr.efrei.downlyzer.modele.incident.Incident;

import java.time.Duration;

// le niveau est calculé à l'affichage, jamais stocké
public record LigneTableauDeBord(Incident incident, NiveauAttention niveau, Duration dureeOuverture) {
}
