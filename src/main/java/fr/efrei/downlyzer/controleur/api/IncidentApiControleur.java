package fr.efrei.downlyzer.controleur.api;

import fr.efrei.downlyzer.controleur.api.dto.DeclarationCapteurDto;
import fr.efrei.downlyzer.controleur.api.dto.IncidentDto;
import fr.efrei.downlyzer.controleur.api.dto.ResolutionDto;
import fr.efrei.downlyzer.modele.StatutIncident;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentApiControleur {

    private final IncidentService incidentService;

    public IncidentApiControleur(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public ResponseEntity<List<IncidentDto>> listerIncidents(@RequestParam(required = false) StatutIncident statut) {
        return ResponseEntity.ok(incidentService.listerIncidents(statut).stream().map(IncidentDto::depuis).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentDto> consulterIncident(@PathVariable Long id) {
        return ResponseEntity.ok(IncidentDto.depuis(incidentService.trouverIncident(id)));
    }

    // 404 si la référence existe pas, 409 si l'équipement a déjà un incident en cours
    @PostMapping
    public ResponseEntity<IncidentDto> signalerPanne(@Valid @RequestBody DeclarationCapteurDto declaration) {
        Incident incident = incidentService.declarerParCapteur(declaration.referenceEquipement(), declaration.description());
        URI adresse = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(incident.getId())
                .toUri();
        return ResponseEntity.created(adresse).body(IncidentDto.depuis(incident));
    }

    // patch car on change qu'une partie de l'incident
    @PatchMapping("/{id}/resolution")
    public ResponseEntity<Void> signalerRetourEnService(@PathVariable Long id,
                                                        @Valid @RequestBody(required = false) ResolutionDto resolution) {
        incidentService.resoudreIncident(id, resolution == null ? null : resolution.commentaire());
        return ResponseEntity.noContent().build();
    }
}
