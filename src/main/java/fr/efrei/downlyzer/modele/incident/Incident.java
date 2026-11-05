package fr.efrei.downlyzer.modele.incident;

import fr.efrei.downlyzer.exception.TransitionInterditeException;
import fr.efrei.downlyzer.modele.SourceIncident;
import fr.efrei.downlyzer.modele.StatutIncident;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.prestataire.Prestataire;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

// pas de setters pour l'état : il change seulement via declarer, affecter et resoudre
@Entity
@Table(name = "incident")
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "equipement_id", nullable = false)
    private Equipement equipement;

    // vide si l'incident vient d'un capteur
    @ManyToOne
    @JoinColumn(name = "declarant_id")
    private Utilisateur declarant;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private SourceIncident source;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "date_ouverture", nullable = false)
    private LocalDateTime dateOuverture;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutIncident statut;

    @ManyToOne
    @JoinColumn(name = "prestataire_id")
    private Prestataire prestataire;

    @Column(name = "date_affectation")
    private LocalDateTime dateAffectation;

    @Column(name = "date_resolution")
    private LocalDateTime dateResolution;

    @Column(name = "commentaire_resolution", length = 1000)
    private String commentaireResolution;

    // verrouillage optimiste : empêche d'écraser une modif faite entre-temps
    @Version
    @Column(name = "version", nullable = false)
    private int version;

    // obligatoire pour jpa, protected pour obliger à passer par les méthodes declarer
    protected Incident() {
    }

    private Incident(Equipement equipement, Utilisateur declarant, SourceIncident source,
                     String description, LocalDateTime dateOuverture) {
        this.equipement = Objects.requireNonNull(equipement, "Un incident doit concerner un équipement");
        this.dateOuverture = Objects.requireNonNull(dateOuverture, "Un incident doit avoir une date d'ouverture");
        this.declarant = declarant;
        this.source = source;
        this.description = description;
        this.statut = StatutIncident.NON_AFFECTE;
    }

    public static Incident declarerManuellement(Equipement equipement, Utilisateur declarant,
                                                String description, LocalDateTime dateOuverture) {
        Objects.requireNonNull(declarant, "Un incident saisi manuellement doit avoir un déclarant");
        return new Incident(equipement, declarant, SourceIncident.MANUELLE, description, dateOuverture);
    }

    public static Incident declarerParCapteur(Equipement equipement, String description,
                                              LocalDateTime dateOuverture) {
        return new Incident(equipement, null, SourceIncident.CAPTEUR, description, dateOuverture);
    }

    // sert aussi pour la réaffectation
    public void affecter(Prestataire nouveauPrestataire, LocalDateTime maintenant) {
        Objects.requireNonNull(nouveauPrestataire, "Le prestataire à affecter est obligatoire");
        if (estResolu()) {
            throw new TransitionInterditeException(
                    "L'incident n° " + id + " est déjà résolu : il ne peut plus être affecté.");
        }
        this.prestataire = nouveauPrestataire;
        this.dateAffectation = maintenant;
        this.statut = StatutIncident.AFFECTE;
    }

    public void resoudre(LocalDateTime maintenant, String commentaire) {
        if (estResolu()) {
            throw new TransitionInterditeException("L'incident n° " + id + " est déjà résolu.");
        }
        if (maintenant.isBefore(dateOuverture)) {
            throw new TransitionInterditeException(
                    "L'incident n° " + id + " ne peut pas être résolu avant sa date d'ouverture.");
        }
        this.dateResolution = maintenant;
        this.commentaireResolution = commentaire;
        this.statut = StatutIncident.RESOLU;
    }

    public void modifierDescription(String nouvelleDescription) {
        if (estResolu()) {
            throw new TransitionInterditeException(
                    "L'incident n° " + id + " est résolu : sa description ne peut plus être modifiée.");
        }
        this.description = nouvelleDescription;
    }

    // calculée, pas stockée : s'arrête à la résolution si l'incident est résolu
    public Duration dureeOuverture(LocalDateTime maintenant) {
        LocalDateTime fin = estResolu() ? dateResolution : maintenant;
        return Duration.between(dateOuverture, fin);
    }

    public boolean estResolu() {
        return statut == StatutIncident.RESOLU;
    }

    public boolean estAffecte() {
        return statut == StatutIncident.AFFECTE;
    }

    // il faut un incident résolu ET un prestataire pour pouvoir évaluer
    public boolean estEvaluable() {
        return estResolu() && prestataire != null;
    }

    public Long getId() {
        return id;
    }

    public Equipement getEquipement() {
        return equipement;
    }

    public Utilisateur getDeclarant() {
        return declarant;
    }

    public SourceIncident getSource() {
        return source;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDateOuverture() {
        return dateOuverture;
    }

    public StatutIncident getStatut() {
        return statut;
    }

    public Prestataire getPrestataire() {
        return prestataire;
    }

    public LocalDateTime getDateAffectation() {
        return dateAffectation;
    }

    public LocalDateTime getDateResolution() {
        return dateResolution;
    }

    public String getCommentaireResolution() {
        return commentaireResolution;
    }
}
