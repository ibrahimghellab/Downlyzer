package fr.efrei.downlyzer.controleur.api;

import fr.efrei.downlyzer.exception.EquipementIntrouvableException;
import fr.efrei.downlyzer.exception.IncidentDejaOuvertException;
import fr.efrei.downlyzer.exception.IncidentIntrouvableException;
import fr.efrei.downlyzer.exception.TransitionInterditeException;
import fr.efrei.downlyzer.modele.Profil;
import fr.efrei.downlyzer.modele.StatutIncident;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.station.Station;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import fr.efrei.downlyzer.securite.SecuriteConfiguration;
import fr.efrei.downlyzer.securite.UtilisateurConnecte;
import fr.efrei.downlyzer.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// le service est simulé, on teste juste les codes http, le json et la clé
@WebMvcTest(controllers = IncidentApiControleur.class)
@Import(SecuriteConfiguration.class)
@TestPropertySource(properties = "downlyzer.api.cle=cle-de-test")
class IncidentApiControleurTest {

    private static final String CLE = "cle-de-test";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentService incidentService;

    private static MockHttpServletRequestBuilder avecCle(MockHttpServletRequestBuilder requete) {
        return requete.header("X-API-KEY", CLE);
    }

    private static Incident incidentDeCapteur(long id) {
        Station station = new Station();
        station.setNom("Port Fluvial");
        Equipement equipement = new Equipement();
        equipement.setReference("ASC-004");
        equipement.setStation(station);
        Incident incident = Incident.declarerParCapteur(equipement, "Porte bloquée en position ouverte",
                LocalDateTime.of(2026, 11, 4, 9, 30));
        ReflectionTestUtils.setField(incident, "id", id);
        return incident;
    }

    @Test
    void sansCleLApiRepondUne401EnJson() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("X-API-KEY")));
    }

    @Test
    void uneCleFausseEstRefusee() throws Exception {
        mockMvc.perform(get("/api/incidents").header("X-API-KEY", "mauvaise-cle"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unUtilisateurDesPagesNePeutPasUtiliserLApiSansCle() throws Exception {
        Utilisateur claire = new Utilisateur();
        claire.setLogin("claire.martin");
        claire.setProfil(Profil.RESPONSABLE);
        claire.setActif(true);

        mockMvc.perform(get("/api/incidents").with(user(new UtilisateurConnecte(claire))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void lesIncidentsSontRenvoyesEnJsonSansExposerLEntite() throws Exception {
        when(incidentService.listerIncidents(null)).thenReturn(List.of(incidentDeCapteur(21)));

        mockMvc.perform(avecCle(get("/api/incidents")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(21))
                .andExpect(jsonPath("$[0].referenceEquipement").value("ASC-004"))
                .andExpect(jsonPath("$[0].station").value("Port Fluvial"))
                .andExpect(jsonPath("$[0].source").value("CAPTEUR"))
                .andExpect(jsonPath("$[0].statut").value("NON_AFFECTE"))
                .andExpect(jsonPath("$[0].version").doesNotExist());
    }

    @Test
    void leFiltreParStatutEstTransmisAuService() throws Exception {
        mockMvc.perform(avecCle(get("/api/incidents").param("statut", "RESOLU")))
                .andExpect(status().isOk());

        verify(incidentService).listerIncidents(StatutIncident.RESOLU);
    }

    @Test
    void unStatutInconnuEstUneRequeteInvalide() throws Exception {
        mockMvc.perform(avecCle(get("/api/incidents").param("statut", "PERDU")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("PERDU")));
    }

    @Test
    void unIncidentInexistantRepondUne404EnJson() throws Exception {
        when(incidentService.trouverIncident(999L)).thenThrow(new IncidentIntrouvableException(999L));

        mockMvc.perform(avecCle(get("/api/incidents/999")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("L'incident n° 999 n'existe pas."));
    }

    @Test
    void unIdentifiantNonNumeriqueEstUneRequeteInvalide() throws Exception {
        mockMvc.perform(avecCle(get("/api/incidents/abc")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unCapteurSignaleUnePanneEtRecoitUne201AvecLAdresseDeLIncident() throws Exception {
        when(incidentService.declarerParCapteur("ASC-004", "Porte bloquée en position ouverte"))
                .thenReturn(incidentDeCapteur(21));

        mockMvc.perform(avecCle(post("/api/incidents")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"referenceEquipement": "ASC-004", "description": "Porte bloquée en position ouverte"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/incidents/21")))
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.source").value("CAPTEUR"));
    }

    @Test
    void uneDeclarationInvalideDonneUne400AvecChaqueChampEnCause() throws Exception {
        mockMvc.perform(avecCle(post("/api/incidents")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"referenceEquipement": "", "description": "court"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("referenceEquipement")))
                .andExpect(jsonPath("$.detail").value(containsString("entre 10 et 1000 caractères")));

        verify(incidentService, never()).declarerParCapteur(anyString(), anyString());
    }

    @Test
    void unCorpsQuiNEstPasDuJsonDonneUne400() throws Exception {
        mockMvc.perform(avecCle(post("/api/incidents")).contentType(MediaType.APPLICATION_JSON).content("pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("JSON")));
    }

    @Test
    void uneReferenceInconnueDonneUne404() throws Exception {
        when(incidentService.declarerParCapteur("XXX-999", "Porte bloquée en position ouverte"))
                .thenThrow(new EquipementIntrouvableException("XXX-999"));

        mockMvc.perform(avecCle(post("/api/incidents")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"referenceEquipement": "XXX-999", "description": "Porte bloquée en position ouverte"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("XXX-999")));
    }

    @Test
    void unePanneDejaSignaleeDonneUne409AvecLIncidentExistant() throws Exception {
        when(incidentService.declarerParCapteur("ASC-001", "Porte bloquée en position ouverte"))
                .thenThrow(new IncidentDejaOuvertException("ASC-001", 9L));

        mockMvc.perform(avecCle(post("/api/incidents")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"referenceEquipement": "ASC-001", "description": "Porte bloquée en position ouverte"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.incidentExistant").value(9));
    }

    @Test
    void leRetourEnServiceResoutLIncidentEtRepondUne204() throws Exception {
        mockMvc.perform(avecCle(patch("/api/incidents/21/resolution")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentaire": "Le capteur ne détecte plus de défaut"}"""))
                .andExpect(status().isNoContent());

        verify(incidentService).resoudreIncident(21L, "Le capteur ne détecte plus de défaut");
    }

    @Test
    void leRetourEnServicePeutSeFaireSansCorps() throws Exception {
        mockMvc.perform(avecCle(patch("/api/incidents/21/resolution")))
                .andExpect(status().isNoContent());

        verify(incidentService).resoudreIncident(21L, null);
    }

    @Test
    void resoudreUnIncidentDejaResoluDonneUne409() throws Exception {
        doThrow(new TransitionInterditeException("L'incident n° 12 est déjà résolu."))
                .when(incidentService).resoudreIncident(any(), any());

        mockMvc.perform(avecCle(patch("/api/incidents/12/resolution")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("L'incident n° 12 est déjà résolu."));
    }
}
