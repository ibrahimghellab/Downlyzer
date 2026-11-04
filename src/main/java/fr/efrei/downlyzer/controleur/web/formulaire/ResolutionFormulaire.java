package fr.efrei.downlyzer.controleur.web.formulaire;

import jakarta.validation.constraints.Size;

// le commentaire est facultatif, seule la date est obligatoire
public class ResolutionFormulaire {

    @Size(max = 1000, message = "Le commentaire ne doit pas dépasser 1000 caractères.")
    private String commentaire;

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
}
