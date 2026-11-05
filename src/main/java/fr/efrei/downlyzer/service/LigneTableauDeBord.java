package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.NiveauAttention;
import fr.efrei.downlyzer.modele.incident.Incident;

import java.time.Duration;

public record LigneTableauDeBord(Incident incident, NiveauAttention niveau, Duration dureeOuverture,
                                 boolean equipementFragile) {
}
