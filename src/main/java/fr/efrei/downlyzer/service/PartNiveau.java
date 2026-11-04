package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.NiveauAttention;

// pourcentage à 0 quand il n'y a aucun incident en cours
public record PartNiveau(NiveauAttention niveau, long nombre, double pourcentage) {
}
