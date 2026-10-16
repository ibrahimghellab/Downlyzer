package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.controleur.web.formulaire.DeclarationIncidentFormulaire;
import fr.efrei.downlyzer.securite.UtilisateurConnecte;
import fr.efrei.downlyzer.service.EquipementService;
import fr.efrei.downlyzer.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class IncidentControleur {
    private final IncidentService incidentService;
    private final EquipementService equipementService;

    public IncidentControleur(IncidentService incidentService, EquipementService equipementService) {
        this.incidentService = incidentService;
        this.equipementService = equipementService;
    }

    @GetMapping("/equipements/{equipementId}/incidents/nouveau")
    public String afficherFormulaireDeclaration(@PathVariable Long equipementId, Model model) {
        model.addAttribute("equipement", equipementService.trouverEquipement(equipementId));
        model.addAttribute("declaration", new DeclarationIncidentFormulaire());
        return "incidents/declaration";
    }
    @PostMapping("/equipements/{equipementId}/incidents")
    public String declarerIncident(@PathVariable Long equipementId,
                                   @Valid @ModelAttribute("declaration") DeclarationIncidentFormulaire declaration,
                                   BindingResult erreurs,
                                   @AuthenticationPrincipal UtilisateurConnecte utilisateur,
                                   Model model, RedirectAttributes redirection) {
        if (erreurs.hasErrors()) {
            model.addAttribute("equipement", equipementService.trouverEquipement(equipementId));
            return "incidents/declaration";
        }
        incidentService.declarerIncident(equipementId, declaration.getDescription(), utilisateur.getUsername());
        redirection.addFlashAttribute("succes", "L'incident a bien été déclaré.");
        return "redirect:/equipements/" + equipementId;
    }


}
