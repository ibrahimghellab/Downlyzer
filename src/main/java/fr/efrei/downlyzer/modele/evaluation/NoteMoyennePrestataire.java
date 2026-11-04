package fr.efrei.downlyzer.modele.evaluation;

// calculée par la base, jamais stockée
public record NoteMoyennePrestataire(Long prestataireId, Double moyenne, Long nombreEvaluations) {
}
