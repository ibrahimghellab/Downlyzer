package fr.efrei.downlyzer.modele.evaluation;

import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation")
public class Evaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // RG8 : une seule évaluation par incident, d'où la colonne unique
    @OneToOne(optional = false)
    @JoinColumn(name = "incident_id", nullable = false, unique = true)
    private Incident incident;

    @ManyToOne(optional = false)
    @JoinColumn(name = "auteur_id", nullable = false)
    private Utilisateur auteur;

    @Column(name = "note", nullable = false)
    private int note;

    @Column(name = "commentaire", length = 1000)
    private String commentaire;

    @Column(name = "date_evaluation", nullable = false)
    private LocalDateTime dateEvaluation;

    public Long getId() {
        return id;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public Utilisateur getAuteur() {
        return auteur;
    }

    public void setAuteur(Utilisateur auteur) {
        this.auteur = auteur;
    }

    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public LocalDateTime getDateEvaluation() {
        return dateEvaluation;
    }

    public void setDateEvaluation(LocalDateTime dateEvaluation) {
        this.dateEvaluation = dateEvaluation;
    }
}