package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.securite.UtilisateurConnecte;
import fr.efrei.downlyzer.service.StationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/stations")
public class StationControleur {
    private final StationService stationService;

    public StationControleur(StationService stationService) {
        this.stationService = stationService;
    }

    // deux onglets : la liste, et la carte avec ?vue=carte (réservée au responsable)
    @GetMapping
    public String afficherStations(@RequestParam(defaultValue = "liste") String vue,
                                   @AuthenticationPrincipal UtilisateurConnecte utilisateur, Model model) {
        model.addAttribute("situations", stationService.listerSituationsDesStations());
        model.addAttribute("supervision", utilisateur.getProfil().peutSuperviser());
        model.addAttribute("vueCarte", "carte".equals(vue));
        return "stations/liste";
    }

    @GetMapping("/{id}")
    public String afficherEquipementsDeLaStation(@PathVariable Long id, Model model) {
        model.addAttribute("station", stationService.trouverStation(id));
        model.addAttribute("equipements", stationService.listerEquipementsDeLaStation(id));
        return "stations/detail";
    }
}
