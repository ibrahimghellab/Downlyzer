package fr.efrei.downlyzer.modele.typeequipement;

import jakarta.persistence.*;

@Entity
@Table(name = "type_equipement")
public class TypeEquipement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "libelle", nullable = false, length = 50, unique = true)
    private String libelle;

    public Long getId() {
        return id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }
}