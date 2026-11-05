package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.equipement.EquipementRepository;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.incident.IncidentRepository;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// vraie politique de fragilité, repositories simulés et horloge figée
class EquipementServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 5, 14, 0);
    private static final LocalDateTime DEBUT_FENETRE = MAINTENANT.minusDays(30);

    private IncidentRepository incidentRepository;
    private EquipementService equipementService;

    @BeforeEach
    void preparer() {
        incidentRepository = mock(IncidentRepository.class);
        Clock horloge = Clock.fixed(MAINTENANT.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        equipementService = new EquipementService(mock(EquipementRepository.class), incidentRepository,
                new PolitiqueFragilite(), horloge);
    }

    private static List<Incident> incidents(Long equipementId, int nombre) {
        Equipement equipement = new Equipement();
        ReflectionTestUtils.setField(equipement, "id", equipementId);
        return IntStream.range(0, nombre)
                .mapToObj(i -> Incident.declarerManuellement(equipement, new Utilisateur(), "Panne",
                        MAINTENANT.minusDays(i)))
                .toList();
    }

    @Test
    void lesIncidentsSontCherchesDepuisLeDebutDeLaFenetre() {
        when(incidentRepository.findByDateOuvertureGreaterThanEqual(DEBUT_FENETRE)).thenReturn(incidents(1L, 3));

        assertThat(equipementService.listerEquipementsFragiles()).containsExactly(1L);
    }

    @Test
    void unEquipementAvecTroisIncidentsRecentsEstFragile() {
        when(incidentRepository.findByDateOuvertureGreaterThanEqual(DEBUT_FENETRE)).thenReturn(incidents(1L, 3));

        assertThat(equipementService.estFragile(1L)).isTrue();
        assertThat(equipementService.estFragile(2L)).isFalse();
    }

    @Test
    void unEquipementSansAssezDIncidentsNEstPasFragile() {
        when(incidentRepository.findByDateOuvertureGreaterThanEqual(DEBUT_FENETRE)).thenReturn(incidents(1L, 2));

        assertThat(equipementService.estFragile(1L)).isFalse();
    }
}
