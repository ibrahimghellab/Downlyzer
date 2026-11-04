package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.NiveauAttention;

// Une part de la barre de répartition du tableau de bord : nombre d'incidents en cours à ce niveau,
// et pourcentage du total (0 quand il n'y a aucun incident en cours)
public record PartNiveau(NiveauAttention niveau, long nombre, double pourcentage) {
}
