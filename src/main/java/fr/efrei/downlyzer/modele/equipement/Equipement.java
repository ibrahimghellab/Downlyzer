package fr.efrei.downlyzer.modele.equipement;

import fr.efrei.downlyzer.modele.EtatEquipement;
import fr.efrei.downlyzer.modele.station.Station;
import fr.efrei.downlyzer.modele.typeequipement.TypeEquipement;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "equipement")
public class Equipement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "reference", nullable = false, length = 20, unique = true)
    private String reference;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false, length = 20)
    private EtatEquipement etat;

    @ManyToOne(optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ManyToOne(optional = false)
    @JoinColumn(name = "type_equipement_id", nullable = false)
    private TypeEquipement typeEquipement;

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public EtatEquipement getEtat() {
        return etat;
    }

    public void setEtat(EtatEquipement etat) {
        this.etat = etat;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public TypeEquipement getTypeEquipement() {
        return typeEquipement;
    }

    public void setTypeEquipement(TypeEquipement typeEquipement) {
        this.typeEquipement = typeEquipement;
    }
}