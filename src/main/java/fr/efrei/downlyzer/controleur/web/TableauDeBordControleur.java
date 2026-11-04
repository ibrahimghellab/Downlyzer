package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.controleur.web.formulaire.FiltreTableauDeBordFormulaire;
import fr.efrei.downlyzer.controleur.web.formulaire.VueTableauDeBord;
import fr.efrei.downlyzer.modele.NiveauAttention;
import fr.efrei.downlyzer.service.LigneTableauDeBord;
import fr.efrei.downlyzer.service.OrdreTableauDeBord;
import fr.efrei.downlyzer.service.StationService;
import fr.efrei.downlyzer.service.TableauDeBordService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

// F11 : réservé au responsable par SecuriteConfiguration
@Controller
public class TableauDeBordControleur {

    // Au-delà, une colonne renvoie vers la liste filtrée sur son niveau : elle reste lisible même très chargée
    static final int FICHES_PAR_COLONNE = 6;

    private final TableauDeBordService tableauDeBordService;
    private final StationService stationService;

    public TableauDeBordControleur(TableauDeBordService tableauDeBordService, StationService stationService) {
        this.tableauDeBordService = tableauDeBordService;
        this.stationService = stationService;
    }

    // Un filtre dont la valeur est invalide dans l'URL (niveau inconnu, identifiant non numérique) est ignoré :
    // BindingResult recueille l'erreur, le champ reste vide et le tableau s'affiche sans ce filtre
    @GetMapping("/tableau-de-bord")
    public String afficherTableauDeBord(@ModelAttribute("filtre") FiltreTableauDeBordFormulaire filtre,
                                        BindingResult valeursIgnorees, Model model) {
        boolean vueColonnes = filtre.getVue() != VueTableauDeBord.LISTE;
        // En colonnes, chaque niveau a déjà sa colonne : le filtre par niveau ne s'applique qu'à la liste
        List<LigneTableauDeBord> lignes = tableauDeBordService.listerIncidentsEnCours(filtre.getStationId(),
                filtre.getTypeEquipementId(), vueColonnes ? null : filtre.getNiveau(), filtre.getOrdre());
        model.addAttribute("lignes", lignes);
        model.addAttribute("vueColonnes", vueColonnes);
        model.addAttribute("colonnes", tableauDeBordService.regrouperParNiveau(lignes));
        model.addAttribute("fichesParColonne", FICHES_PAR_COLONNE);
        model.addAttribute("indicateurs", tableauDeBordService.calculerIndicateurs());
        model.addAttribute("stations", stationService.listerStations());
        model.addAttribute("types", tableauDeBordService.listerTypesEquipement());
        model.addAttribute("niveaux", NiveauAttention.values());
        model.addAttribute("ordres", OrdreTableauDeBord.values());
        return "tableau-de-bord";
    }
}
