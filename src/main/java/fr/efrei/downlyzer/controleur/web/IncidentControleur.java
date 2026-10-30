package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.controleur.web.formulaire.DescriptionIncidentFormulaire;
import fr.efrei.downlyzer.modele.incident.Incident;
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
        model.addAttribute("declaration", new DescriptionIncidentFormulaire());
        return "incidents/declaration";
    }
    @PostMapping("/equipements/{equipementId}/incidents")
    public String declarerIncident(@PathVariable Long equipementId,
                                   @Valid @ModelAttribute("declaration") DescriptionIncidentFormulaire declaration,
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

    @GetMapping("/incidents/{id}")
    public String afficherDetailIncident(@PathVariable Long id,
                                         @AuthenticationPrincipal UtilisateurConnecte utilisateur, Model model) {
        Incident incident = incidentService.trouverIncident(id);
        model.addAttribute("incident", incident);
        model.addAttribute("dureeOuverture", incidentService.calculerDureeOuverture(incident));
        model.addAttribute("evaluation", incidentService.trouverEvaluation(id).orElse(null));
        model.addAttribute("descriptionModifiable",
                incidentService.peutModifierDescription(incident, utilisateur.getProfil()));
        return "incidents/detail";
    }

    @GetMapping("/incidents/{id}/description")
    public String afficherFormulaireModification(@PathVariable Long id,
                                                 @AuthenticationPrincipal UtilisateurConnecte utilisateur,
                                                 Model model) {
        Incident incident = incidentService.trouverIncidentAModifier(id, utilisateur.getProfil());
        DescriptionIncidentFormulaire modification = new DescriptionIncidentFormulaire();
        modification.setDescription(incident.getDescription());
        model.addAttribute("incident", incident);
        model.addAttribute("modification", modification);
        return "incidents/modification";
    }

    @PostMapping("/incidents/{id}/description")
    public String modifierDescription(@PathVariable Long id,
                                      @Valid @ModelAttribute("modification") DescriptionIncidentFormulaire modification,
                                      BindingResult erreurs,
                                      @AuthenticationPrincipal UtilisateurConnecte utilisateur,
                                      Model model, RedirectAttributes redirection) {
        if (erreurs.hasErrors()) {
            model.addAttribute("incident", incidentService.trouverIncident(id));
            return "incidents/modification";
        }
        incidentService.modifierDescription(id, modification.getDescription(), utilisateur.getProfil());
        redirection.addFlashAttribute("succes", "La description a bien été modifiée.");
        return "redirect:/incidents/" + id;
    }
}
