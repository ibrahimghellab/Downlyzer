package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.service.EvaluationService;
import fr.efrei.downlyzer.service.IncidentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// accessible aux deux profils, juste en lecture
@Controller
public class HistoriqueControleur {

    private final IncidentService incidentService;
    private final EvaluationService evaluationService;

    public HistoriqueControleur(IncidentService incidentService, EvaluationService evaluationService) {
        this.incidentService = incidentService;
        this.evaluationService = evaluationService;
    }

    @GetMapping("/historique")
    public String afficherHistorique(Model model) {
        model.addAttribute("incidents", incidentService.listerIncidentsResolus());
        model.addAttribute("evaluations", evaluationService.trouverEvaluationsParIncident());
        return "incidents/historique";
    }
}
