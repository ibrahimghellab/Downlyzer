package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.service.EquipementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/equipements")
public class EquipementControleur {
    private final EquipementService equipementService;

    public EquipementControleur(EquipementService equipementService) {
        this.equipementService = equipementService;
    }

    @GetMapping("/{id}")
    public String afficherFicheEquipement(@PathVariable Long id, Model model) {
        model.addAttribute("equipement", equipementService.trouverEquipement(id));
        model.addAttribute("incidents", equipementService.listerIncidentsDeLEquipement(id));
        return "equipements/fiche";
    }
}
