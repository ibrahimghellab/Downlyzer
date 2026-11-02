package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.controleur.web.formulaire.AffectationFormulaire;
import fr.efrei.downlyzer.controleur.web.formulaire.DescriptionIncidentFormulaire;
import fr.efrei.downlyzer.controleur.web.formulaire.ResolutionFormulaire;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.prestataire.Prestataire;
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
        return afficherDetail(id, utilisateur, model);
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

    // F7 : route réservée au responsable par SecuriteConfiguration
    @PostMapping("/incidents/{id}/affectation")
    public String affecterPrestataire(@PathVariable Long id,
                                      @Valid @ModelAttribute("affectation") AffectationFormulaire affectation,
                                      BindingResult erreurs,
                                      @AuthenticationPrincipal UtilisateurConnecte utilisateur,
                                      Model model, RedirectAttributes redirection) {
        if (erreurs.hasErrors()) {
            return afficherDetail(id, utilisateur, model);
        }
        Prestataire prestataire = incidentService.affecterPrestataire(id, affectation.getPrestataireId());
        redirection.addFlashAttribute("succes", "L'incident est affecté à " + prestataire.getNom() + ".");
        return "redirect:/incidents/" + id;
    }

    // F8 : route réservée au responsable par SecuriteConfiguration
    @PostMapping("/incidents/{id}/resolution")
    public String resoudreIncident(@PathVariable Long id,
                                   @Valid @ModelAttribute("resolution") ResolutionFormulaire resolution,
                                   BindingResult erreurs,
                                   @AuthenticationPrincipal UtilisateurConnecte utilisateur,
                                   Model model, RedirectAttributes redirection) {
        if (erreurs.hasErrors()) {
            return afficherDetail(id, utilisateur, model);
        }
        incidentService.resoudreIncident(id, resolution.getCommentaire());
        redirection.addFlashAttribute("succes", "L'incident est déclaré résolu.");
        return "redirect:/incidents/" + id;
    }

    // Partagé par l'affichage normal et par le réaffichage d'un formulaire F7-F8 invalide :
    // un formulaire déjà présent dans le modèle (avec ses erreurs) n'est pas remplacé
    private String afficherDetail(Long id, UtilisateurConnecte utilisateur, Model model) {
        Incident incident = incidentService.trouverIncident(id);
        model.addAttribute("incident", incident);
        model.addAttribute("dureeOuverture", incidentService.calculerDureeOuverture(incident));
        model.addAttribute("evaluation", incidentService.trouverEvaluation(id).orElse(null));
        model.addAttribute("descriptionModifiable",
                incidentService.peutModifierDescription(incident, utilisateur.getProfil()));
        boolean affectationEtResolutionPossibles =
                incidentService.peutAffecterEtResoudre(incident, utilisateur.getProfil());
        model.addAttribute("affectationEtResolutionPossibles", affectationEtResolutionPossibles);
        if (affectationEtResolutionPossibles) {
            model.addAttribute("prestataires", incidentService.listerPrestatairesAffectables(incident));
            if (!model.containsAttribute("affectation")) {
                model.addAttribute("affectation", new AffectationFormulaire());
            }
            if (!model.containsAttribute("resolution")) {
                model.addAttribute("resolution", new ResolutionFormulaire());
            }
        }
        return "incidents/detail";
    }
}
