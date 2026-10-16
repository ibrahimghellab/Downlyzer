package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.service.StationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/stations")
public class StationControleur {
    private final StationService stationService;

    public StationControleur(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping("/{id}")
    public String afficherEquipementsDeLaStation(@PathVariable Long id, Model model) {
        model.addAttribute("station", stationService.trouverStation(id));
        model.addAttribute("equipements", stationService.listerEquipementsDeLaStation(id));
        return "stations/detail";
    }
}

