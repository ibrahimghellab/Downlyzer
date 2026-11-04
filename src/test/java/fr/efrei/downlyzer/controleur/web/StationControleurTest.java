package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.modele.NiveauAttention;
import fr.efrei.downlyzer.modele.Profil;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import fr.efrei.downlyzer.securite.SecuriteConfiguration;
import fr.efrei.downlyzer.securite.UtilisateurConnecte;
import fr.efrei.downlyzer.service.SituationStation;
import fr.efrei.downlyzer.service.StationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = StationControleur.class)
@Import(SecuriteConfiguration.class)
class StationControleurTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StationService stationService;

    private static RequestPostProcessor connecte(Profil profil) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setLogin("karim.dupont");
        utilisateur.setPrenom("Karim");
        utilisateur.setProfil(profil);
        utilisateur.setActif(true);
        return user(new UtilisateurConnecte(utilisateur));
    }

    @BeforeEach
    void preparer() {
        when(stationService.listerSituationsDesStations()).thenReturn(List.of(
                new SituationStation(1L, "Gare Centrale", "GCE", 48.8443, 2.3740, NiveauAttention.ROUGE, 3),
                new SituationStation(6L, "Port Fluvial", "PFL", 48.8310, 2.3780, null, 0)));
    }

    @Test
    void leResponsableVoitLaSituationDeChaqueStationEtLOngletCarte() throws Exception {
        mockMvc.perform(get("/stations").with(connecte(Profil.RESPONSABLE)))
                .andExpect(status().isOk())
                .andExpect(view().name("stations/liste"))
                .andExpect(content().string(containsString("Rouge — 72 h et plus")))
                .andExpect(content().string(containsString("Aucun incident")))
                .andExpect(content().string(containsString("vue=carte")))
                .andExpect(content().string(not(containsString("leaflet.js"))));
    }

    @Test
    void lAgentVoitLeNombreDIncidentsSansNiveauDAttentionNiCarte() throws Exception {
        mockMvc.perform(get("/stations").with(connecte(Profil.AGENT)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Gare Centrale")))
                .andExpect(content().string(containsString("Incidents en cours")))
                .andExpect(content().string(not(containsString("Rouge — 72 h et plus"))))
                .andExpect(content().string(not(containsString("vue=carte"))));
    }

    @Test
    void longletCarteAfficheLaCarteAvecLaPositionEtLeNiveauDeChaqueStation() throws Exception {
        mockMvc.perform(get("/stations").param("vue", "carte").with(connecte(Profil.RESPONSABLE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("leaflet.js")))
                .andExpect(content().string(containsString("data-latitude=\"48.8443\"")))
                .andExpect(content().string(containsString("data-niveau=\"ROUGE\"")))
                .andExpect(content().string(containsString("data-niveau=\"AUCUN\"")));
    }

    @Test
    void unAgentQuiDemandeLaCarteObtientUne403() throws Exception {
        mockMvc.perform(get("/stations").param("vue", "carte").with(connecte(Profil.AGENT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void uneVueInconnueAfficheLaListe() throws Exception {
        mockMvc.perform(get("/stations").param("vue", "nimporte").with(connecte(Profil.RESPONSABLE)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("leaflet.js"))));
    }
}
