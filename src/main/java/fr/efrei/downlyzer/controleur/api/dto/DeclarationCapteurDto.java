package fr.efrei.downlyzer.controleur.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// le capteur connaît la référence de son équipement, pas son id en base
public record DeclarationCapteurDto(
        @NotBlank(message = "La référence de l'équipement est obligatoire.")
        String referenceEquipement,
        @NotBlank(message = "La description de la panne est obligatoire.")
        @Size(min = 10, max = 1000, message = "La description doit contenir entre 10 et 1000 caractères.")
        String description) {
}
