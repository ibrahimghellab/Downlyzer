package fr.efrei.downlyzer.controleur.web.formulaire;

import jakarta.validation.constraints.NotNull;

public class AffectationFormulaire {

    @NotNull(message = "Choisissez un prestataire.")
    private Long prestataireId;

    public Long getPrestataireId() {
        return prestataireId;
    }

    public void setPrestataireId(Long prestataireId) {
        this.prestataireId = prestataireId;
    }
}
