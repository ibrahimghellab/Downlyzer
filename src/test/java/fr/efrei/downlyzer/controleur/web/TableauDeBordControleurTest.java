package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.modele.NiveauAttention;
import fr.efrei.downlyzer.modele.Profil;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.station.Station;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import fr.efrei.downlyzer.securite.SecuriteConfiguration;
import fr.efrei.downlyzer.securite.UtilisateurConnecte;
import fr.efrei.downlyzer.service.IndicateursTableauDeBord;
import fr.efrei.downlyzer.service.LigneTableauDeBord;
import fr.efrei.downlyzer.service.OrdreTableauDeBord;
import fr.efrei.downlyzer.service.PartNiveau;
import fr.efrei.downlyzer.service.StationService;
import fr.efrei.downlyzer.service.TableauDeBordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {TableauDeBordControleur.class, ConnexionControleur.class})
@Import(SecuriteConfiguration.class)
class TableauDeBordControleurTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TableauDeBordService tableauDeBordService;

    @MockitoBean
    private StationService stationService;

    private static RequestPostProcessor connecte(Profil profil) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setLogin("claire.martin");
        utilisateur.setPrenom("Claire");
        utilisateur.setProfil(profil);
        utilisateur.setActif(true);
        return user(new UtilisateurConnecte(utilisateur));
    }

    @BeforeEach
    void preparer() {
        Station station = new Station();
        station.setNom("Gare Centrale");
        Equipement equipement = new Equipement();
        equipement.setReference("ASC-001");
        equipement.setStation(station);
        Incident incident = Incident.declarerManuellement(equipement, new Utilisateur(),
                "Ascenseur bloqué entre deux étages",
                LocalDateTime.of(2026, 10, 10, 8, 0));
        LigneTableauDeBord ligne = new LigneTableauDeBord(incident, NiveauAttention.ROUGE, Duration.ofHours(80), false);
        when(tableauDeBordService.listerIncidentsEnCours(any(), any(), any(), any())).thenReturn(List.of(ligne));
        when(tableauDeBordService.regrouperParNiveau(any())).thenReturn(colonnes(List.of(ligne)));
        when(tableauDeBordService.calculerIndicateurs()).thenReturn(
                new IndicateursTableauDeBord(LocalDateTime.of(2026, 10, 14, 12, 0), 11,
                        List.of(new PartNiveau(NiveauAttention.ROUGE, 3, 27.3),
                        new PartNiveau(NiveauAttention.ORANGE, 0, 0), new PartNiveau(NiveauAttention.VERT, 5, 45.5),
                        new PartNiveau(NiveauAttention.PRIS_EN_CHARGE, 3, 27.3)), 9, Duration.ofHours(53)));
    }

    private static Map<NiveauAttention, List<LigneTableauDeBord>> colonnes(List<LigneTableauDeBord> rouges) {
        Map<NiveauAttention, List<LigneTableauDeBord>> colonnes = new EnumMap<>(NiveauAttention.class);
        for (NiveauAttention niveau : NiveauAttention.values()) {
            colonnes.put(niveau, niveau == NiveauAttention.ROUGE ? rouges : List.of());
        }
        return colonnes;
    }

    @Test
    void parDefautLesIncidentsSontRangesEnColonnesParNiveauSousUnePhraseDeSituation() throws Exception {
        mockMvc.perform(get("/tableau-de-bord").with(connecte(Profil.RESPONSABLE)))
                .andExpect(status().isOk())
                .andExpect(view().name("tableau-de-bord"))
                .andExpect(content().string(containsString("3 incidents attendent un prestataire depuis 72 h ou plus")))
                .andExpect(content().string(containsString("72 h et plus sans prestataire")))
                .andExpect(content().string(containsString("Ascenseur bloqué entre deux étages")))
                .andExpect(content().string(containsString("En attente depuis")))
                .andExpect(content().string(containsString("3 j 8 h")))
                .andExpect(content().string(containsString("2 j 5 h")))
                .andExpect(content().string(not(containsString("<table"))));
    }

    private void rendreLEquipementFragile() {
        LigneTableauDeBord ligne = tableauDeBordService.listerIncidentsEnCours(null, null, null, null).getFirst();
        LigneTableauDeBord fragile = new LigneTableauDeBord(ligne.incident(), ligne.niveau(), ligne.dureeOuverture(), true);
        when(tableauDeBordService.listerIncidentsEnCours(any(), any(), any(), any())).thenReturn(List.of(fragile));
        when(tableauDeBordService.regrouperParNiveau(any())).thenReturn(colonnes(List.of(fragile)));
    }

    @Test
    void leBadgeFragileEstSurLaFicheDUnIncidentDontLEquipementEstFragile() throws Exception {
        rendreLEquipementFragile();

        mockMvc.perform(get("/tableau-de-bord").with(connecte(Profil.RESPONSABLE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Équipement fragile")));
    }

    @Test
    void leBadgeFragileEstAussiDansLaVueEnListe() throws Exception {
        rendreLEquipementFragile();

        mockMvc.perform(get("/tableau-de-bord").param("vue", "LISTE").with(connecte(Profil.RESPONSABLE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Équipement fragile")));
    }

    @Test
    void pasDeBadgeFragileSiLEquipementNeLEstPas() throws Exception {
        mockMvc.perform(get("/tableau-de-bord").with(connecte(Profil.RESPONSABLE)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Équipement fragile"))));
    }

    @Test
    void uneColonneTropChargeeRenvoieVersLaListeFiltreeSurSonNiveau() throws Exception {
        LigneTableauDeBord ligne = tableauDeBordService.listerIncidentsEnCours(null, null, null, null).getFirst();
        when(tableauDeBordService.regrouperParNiveau(any())).thenReturn(colonnes(Collections.nCopies(
                TableauDeBordControleur.FICHES_PAR_COLONNE + 1, ligne)));

        mockMvc.perform(get("/tableau-de-bord").with(connecte(Profil.RESPONSABLE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Voir les 7 incidents")))
                .andExpect(content().string(containsString("niveau=ROUGE")));
    }

    @Test
    void laVueEnListeAfficheLeTableauDesIncidentsAvecLeurNiveau() throws Exception {
        mockMvc.perform(get("/tableau-de-bord").param("vue", "LISTE").with(connecte(Profil.RESPONSABLE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<table")))
                .andExpect(content().string(containsString("Rouge (72 h et plus)")))
                .andExpect(content().string(containsString("En attente")));
    }

    @Test
    void laBarreDeRepartitionNAfficheQueLesNiveauxPresentsEtLaLegendeLesDonneTous() throws Exception {
        mockMvc.perform(get("/tableau-de-bord").param("vue", "LISTE").with(connecte(Profil.RESPONSABLE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rouge (72 h et plus) : 3 incident(s), 27 %")))
                .andExpect(content().string(containsString("flex-grow: 5")))
                .andExpect(content().string(not(containsString("Orange (48 à 72 h) : 0 incident(s)"))))
                .andExpect(content().string(containsString("Orange (48 à 72 h)")));
    }

    @Test
    void unAgentNAPasAccesAuTableauDeBord() throws Exception {
        mockMvc.perform(get("/tableau-de-bord").with(connecte(Profil.AGENT)))
                .andExpect(status().isForbidden());

        verify(tableauDeBordService, never()).listerIncidentsEnCours(any(), any(), any(), any());
    }

    @Test
    void lesFiltresChoisisSontTransmisAuServiceEnVueListe() throws Exception {
        mockMvc.perform(get("/tableau-de-bord").with(connecte(Profil.RESPONSABLE))
                        .param("stationId", "1").param("typeEquipementId", "").param("niveau", "ROUGE")
                        .param("ordre", "STATION").param("vue", "LISTE"))
                .andExpect(status().isOk());

        verify(tableauDeBordService).listerIncidentsEnCours(1L, null, NiveauAttention.ROUGE, OrdreTableauDeBord.STATION);
    }

    @Test
    void enVueParNiveauLeFiltreParNiveauNEstPasApplique() throws Exception {
        mockMvc.perform(get("/tableau-de-bord").with(connecte(Profil.RESPONSABLE))
                        .param("niveau", "ROUGE").param("ordre", "STATION"))
                .andExpect(status().isOk());

        verify(tableauDeBordService).listerIncidentsEnCours(null, null, null, OrdreTableauDeBord.STATION);
    }

    @Test
    void unFiltreInvalideDansLUrlEstIgnore() throws Exception {
        mockMvc.perform(get("/tableau-de-bord").with(connecte(Profil.RESPONSABLE))
                        .param("stationId", "abc").param("niveau", "VIOLET"))
                .andExpect(status().isOk())
                .andExpect(view().name("tableau-de-bord"));

        verify(tableauDeBordService).listerIncidentsEnCours(null, null, null, OrdreTableauDeBord.URGENCE);
    }

    @Test
    void apresLaConnexionLeResponsableArriveSurLeTableauDeBordEtLAgentSurLesStations() throws Exception {
        mockMvc.perform(get("/").with(connecte(Profil.RESPONSABLE)))
                .andExpect(redirectedUrl("/tableau-de-bord"));
        mockMvc.perform(get("/").with(connecte(Profil.AGENT)))
                .andExpect(redirectedUrl("/stations"));
    }
}
