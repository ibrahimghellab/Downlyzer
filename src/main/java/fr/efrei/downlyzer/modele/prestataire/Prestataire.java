package fr.efrei.downlyzer.modele.prestataire;

import fr.efrei.downlyzer.modele.typeequipement.TypeEquipement;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "prestataire")
public class Prestataire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nom", nullable = false, length = 100, unique = true)
    private String nom;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "telephone", nullable = false, length = 20)
    private String telephone;

    @Column(name = "actif", nullable = false)
    private boolean actif;

    // Types d'équipement sur lesquels le prestataire sait intervenir
    @ManyToMany
    @JoinTable(
            name = "prestataire_specialite",
            joinColumns = @JoinColumn(name = "prestataire_id"),
            inverseJoinColumns = @JoinColumn(name = "type_equipement_id"))
    private Set<TypeEquipement> specialites = new HashSet<>();

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public Set<TypeEquipement> getSpecialites() {
        return specialites;
    }

    public void setSpecialites(Set<TypeEquipement> specialites) {
        this.specialites = specialites;
    }
}