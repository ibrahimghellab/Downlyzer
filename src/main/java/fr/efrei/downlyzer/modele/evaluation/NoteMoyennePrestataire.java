package fr.efrei.downlyzer.modele.evaluation;

// Résultat d'agrégation calculé par la base : la note moyenne n'est jamais stockée
public record NoteMoyennePrestataire(Long prestataireId, Double moyenne, Long nombreEvaluations) {
}
