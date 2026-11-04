package fr.efrei.downlyzer.controleur.web.formulaire;

import fr.efrei.downlyzer.modele.NiveauAttention;
import fr.efrei.downlyzer.service.OrdreTableauDeBord;

// Filtres du tableau de bord (F11), transmis dans l'URL : un champ vide signifie « tous »
public class FiltreTableauDeBordFormulaire {

    private Long stationId;
    private Long typeEquipementId;
    private NiveauAttention niveau;
    private OrdreTableauDeBord ordre = OrdreTableauDeBord.URGENCE;
    private VueTableauDeBord vue = VueTableauDeBord.COLONNES;

    public Long getStationId() {
        return stationId;
    }

    public void setStationId(Long stationId) {
        this.stationId = stationId;
    }

    public Long getTypeEquipementId() {
        return typeEquipementId;
    }

    public void setTypeEquipementId(Long typeEquipementId) {
        this.typeEquipementId = typeEquipementId;
    }

    public NiveauAttention getNiveau() {
        return niveau;
    }

    public void setNiveau(NiveauAttention niveau) {
        this.niveau = niveau;
    }

    public OrdreTableauDeBord getOrdre() {
        return ordre;
    }

    public void setOrdre(OrdreTableauDeBord ordre) {
        this.ordre = ordre;
    }

    public VueTableauDeBord getVue() {
        return vue;
    }

    public void setVue(VueTableauDeBord vue) {
        this.vue = vue;
    }
}
