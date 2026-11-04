package fr.efrei.downlyzer.controleur.api.dto;

import jakarta.validation.constraints.Size;

public record ResolutionDto(
        @Size(max = 1000, message = "Le commentaire ne doit pas dépasser 1000 caractères.")
        String commentaire) {
}
