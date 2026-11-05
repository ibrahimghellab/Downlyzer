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

@Controller
public class TableauDeBordControleur {

    // au-delà on renvoie vers la liste pour que la colonne reste lisible
    static final int FICHES_PAR_COLONNE = 6;

    private final TableauDeBordService tableauDeBordService;
    private final StationService stationService;

    public TableauDeBordControleur(TableauDeBordService tableauDeBordService, StationService stationService) {
        this.tableauDeBordService = tableauDeBordService;
        this.stationService = stationService;
    }

    // une valeur invalide dans l'url est juste ignorée, BindingResult évite l'erreur
    @GetMapping("/tableau-de-bord")
    public String afficherTableauDeBord(@ModelAttribute("filtre") FiltreTableauDeBordFormulaire filtre,
                                        BindingResult valeursIgnorees, Model model) {
        boolean vueColonnes = filtre.getVue() != VueTableauDeBord.LISTE;
        // en colonnes chaque niveau a déjà sa colonne, donc pas de filtre par niveau
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
