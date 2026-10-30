package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.exception.ActionNonAutoriseeException;
import fr.efrei.downlyzer.exception.IncidentIntrouvableException;
import fr.efrei.downlyzer.exception.TransitionInterditeException;
import fr.efrei.downlyzer.modele.Profil;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.station.Station;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import fr.efrei.downlyzer.securite.SecuriteConfiguration;
import fr.efrei.downlyzer.securite.UtilisateurConnecte;
import fr.efrei.downlyzer.service.EquipementService;
import fr.efrei.downlyzer.service.IncidentService;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {IncidentControleur.class, ErreurControleur.class})
@Import(SecuriteConfiguration.class)
class IncidentControleurTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentService incidentService;

    @MockitoBean
    private EquipementService equipementService;

    private static RequestPostProcessor connecte(Profil profil) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setLogin("karim.dupont");
        utilisateur.setPrenom("Karim");
        utilisateur.setProfil(profil);
        utilisateur.setActif(true);
        return user(new UtilisateurConnecte(utilisateur));
    }

    private static Incident incidentNonAffecte() {
        Station station = new Station();
        station.setNom("Gare Centrale");
        Equipement equipement = new Equipement();
        equipement.setReference("ASC-001");
        equipement.setLibelle("Ascenseur quai A");
        equipement.setStation(station);
        Utilisateur declarant = new Utilisateur();
        declarant.setPrenom("Karim");
        declarant.setNom("Dupont");
        return Incident.declarerManuellement(equipement, declarant, "Ascenseur bloqué entre deux étages",
                LocalDateTime.of(2026, 10, 18, 13, 0));
    }

    @Test
    void sansConnexionLaPageDUnIncidentRedirigeVersLaConnexion() throws Exception {
        mockMvc.perform(get("/incidents/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/connexion"));
    }

    @Test
    void leDetailDUnIncidentEstAffiche() throws Exception {
        Incident incident = incidentNonAffecte();
        when(incidentService.trouverIncident(1L)).thenReturn(incident);
        when(incidentService.calculerDureeOuverture(incident)).thenReturn(Duration.ofHours(47));
        when(incidentService.peutModifierDescription(incident, Profil.AGENT)).thenReturn(true);

        mockMvc.perform(get("/incidents/1").with(connecte(Profil.AGENT)))
                .andExpect(status().isOk())
                .andExpect(view().name("incidents/detail"))
                .andExpect(content().string(containsString("Ascenseur bloqué entre deux étages")))
                .andExpect(content().string(containsString("1 j 23 h 0 min")))
                .andExpect(content().string(containsString("Modifier la description")));
    }

    @Test
    void leLienDeModificationEstMasqueQuandLaModificationEstInterdite() throws Exception {
        Incident incident = incidentNonAffecte();
        when(incidentService.trouverIncident(1L)).thenReturn(incident);
        when(incidentService.calculerDureeOuverture(incident)).thenReturn(Duration.ofHours(47));
        when(incidentService.peutModifierDescription(incident, Profil.AGENT)).thenReturn(false);

        mockMvc.perform(get("/incidents/1").with(connecte(Profil.AGENT)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Modifier la description"))));
    }

    @Test
    void unIncidentInexistantAfficheLaPageDErreur404() throws Exception {
        when(incidentService.trouverIncident(999L)).thenThrow(new IncidentIntrouvableException(999L));

        mockMvc.perform(get("/incidents/999").with(connecte(Profil.AGENT)))
                .andExpect(status().isNotFound())
                .andExpect(view().name("erreur"))
                .andExpect(content().string(containsString("L&#39;incident n° 999 n&#39;existe pas.")))
                .andExpect(content().string(containsString("Karim")));
    }

    @Test
    void unIdentifiantNonNumeriqueAfficheLaPageDErreur404() throws Exception {
        mockMvc.perform(get("/incidents/ab").with(connecte(Profil.AGENT)))
                .andExpect(status().isNotFound())
                .andExpect(view().name("erreur"))
                .andExpect(content().string(containsString("Cette page n&#39;existe pas.")));
    }

    @Test
    void uneDescriptionValideEstEnregistreeEtRameneAuDetail() throws Exception {
        mockMvc.perform(post("/incidents/1/description").with(connecte(Profil.AGENT)).with(csrf())
                        .param("description", "Ascenseur bloqué au 2e étage"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/1"))
                .andExpect(flash().attribute("succes", "La description a bien été modifiée."));

        verify(incidentService).modifierDescription(1L, "Ascenseur bloqué au 2e étage", Profil.AGENT);
    }

    @Test
    void uneDescriptionTropCourteReafficheLeFormulaireSansRienEnregistrer() throws Exception {
        when(incidentService.trouverIncident(1L)).thenReturn(incidentNonAffecte());

        mockMvc.perform(post("/incidents/1/description").with(connecte(Profil.AGENT)).with(csrf())
                        .param("description", "Bloqué"))
                .andExpect(status().isOk())
                .andExpect(view().name("incidents/modification"))
                .andExpect(model().attributeHasFieldErrors("modification", "description"));

        verify(incidentService, never()).modifierDescription(anyLong(), anyString(), any());
    }

    @Test
    void unAgentQuiModifieUnIncidentAffecteObtientUne403() throws Exception {
        doThrow(new ActionNonAutoriseeException("Seul un responsable d'exploitation peut encore modifier sa description."))
                .when(incidentService).modifierDescription(1L, "Ascenseur bloqué au 2e étage", Profil.AGENT);

        mockMvc.perform(post("/incidents/1/description").with(connecte(Profil.AGENT)).with(csrf())
                        .param("description", "Ascenseur bloqué au 2e étage"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("erreur"));
    }

    @Test
    void modifierUnIncidentResoluObtientUne409() throws Exception {
        when(incidentService.trouverIncidentAModifier(1L, Profil.RESPONSABLE))
                .thenThrow(new TransitionInterditeException("L'incident n° 1 est résolu."));

        mockMvc.perform(get("/incidents/1/description").with(connecte(Profil.RESPONSABLE)))
                .andExpect(status().isConflict())
                .andExpect(view().name("erreur"));
    }

    @Test
    void uneModificationSansJetonCsrfEstRefusee() throws Exception {
        mockMvc.perform(post("/incidents/1/description").with(connecte(Profil.AGENT))
                        .param("description", "Ascenseur bloqué au 2e étage"))
                .andExpect(status().isForbidden());

        verify(incidentService, never()).modifierDescription(anyLong(), anyString(), any());
    }

    @Test
    void uneUrlInconnueAfficheLaPageDErreur404() throws Exception {
        mockMvc.perform(get("/error").with(connecte(Profil.AGENT))
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
                .andExpect(status().isNotFound())
                .andExpect(view().name("erreur"))
                .andExpect(content().string(containsString("Cette page n&#39;existe pas.")))
                .andExpect(content().string(containsString("Karim")));
    }

    @Test
    void uneErreurImprevueAfficheUnMessageGeneriqueSansDetailTechnique() throws Exception {
        mockMvc.perform(get("/error").with(connecte(Profil.AGENT))
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("Connexion à la base perdue")))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("erreur"))
                .andExpect(content().string(containsString("Erreur inattendue")))
                .andExpect(content().string(not(containsString("Connexion à la base perdue"))));
    }
}
