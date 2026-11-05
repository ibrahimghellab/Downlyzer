package fr.efrei.downlyzer.controleur.web;

import fr.efrei.downlyzer.modele.EtatEquipement;
import fr.efrei.downlyzer.modele.Profil;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.station.Station;
import fr.efrei.downlyzer.modele.typeequipement.TypeEquipement;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import fr.efrei.downlyzer.securite.SecuriteConfiguration;
import fr.efrei.downlyzer.securite.UtilisateurConnecte;
import fr.efrei.downlyzer.service.EquipementService;
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

@WebMvcTest(controllers = EquipementControleur.class)
@Import(SecuriteConfiguration.class)
class EquipementControleurTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipementService equipementService;

    private static RequestPostProcessor agent() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setLogin("karim.dupont");
        utilisateur.setPrenom("Karim");
        utilisateur.setProfil(Profil.AGENT);
        utilisateur.setActif(true);
        return user(new UtilisateurConnecte(utilisateur));
    }

    @BeforeEach
    void preparer() {
        Station station = new Station();
        station.setNom("Gare Centrale");
        TypeEquipement type = new TypeEquipement();
        type.setLibelle("Ascenseur");
        Equipement equipement = new Equipement();
        equipement.setReference("ASC-001");
        equipement.setLibelle("Ascenseur quai A");
        equipement.setEtat(EtatEquipement.EN_SERVICE);
        equipement.setStation(station);
        equipement.setTypeEquipement(type);
        when(equipementService.trouverEquipement(1L)).thenReturn(equipement);
        when(equipementService.listerIncidentsDeLEquipement(1L)).thenReturn(List.of());
    }

    @Test
    void laFicheDUnEquipementFragileAfficheLeBadge() throws Exception {
        when(equipementService.estFragile(1L)).thenReturn(true);

        mockMvc.perform(get("/equipements/1").with(agent()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Équipement fragile")));
    }

    @Test
    void laFicheDUnEquipementNonFragileNAffichePasLeBadge() throws Exception {
        when(equipementService.estFragile(1L)).thenReturn(false);

        mockMvc.perform(get("/equipements/1").with(agent()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Équipement fragile"))));
    }
}
