package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.prestataire.Prestataire;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// les 7 cas limites du sujet, heure fixe et vrais incidents
class PolitiqueFragiliteTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 5, 14, 0);
    private static final Long ID_EQUIPEMENT = 1L;
    private static final Long ID_VOISIN = 2L;

    private final PolitiqueFragilite politiqueFragilite = new PolitiqueFragilite();

    private static Equipement equipement(Long id) {
        Equipement equipement = new Equipement();
        ReflectionTestUtils.setField(equipement, "id", id);
        return equipement;
    }

    private static Incident ouvertDepuis(Long equipementId, Duration duree) {
        return Incident.declarerManuellement(equipement(equipementId), new Utilisateur(), "Panne",
                MAINTENANT.minus(duree));
    }

    private static List<Incident> incidentsRecents(int nombre) {
        List<Incident> incidents = new ArrayList<>();
        for (int i = 1; i <= nombre; i++) {
            incidents.add(ouvertDepuis(ID_EQUIPEMENT, Duration.ofDays(i)));
        }
        return incidents;
    }

    private boolean estFragile(List<Incident> incidents) {
        return politiqueFragilite.trouverEquipementsFragiles(incidents, MAINTENANT).contains(ID_EQUIPEMENT);
    }

    @Test
    void deuxIncidentsDansLaFenetreNeSuffisentPas() {
        assertThat(estFragile(incidentsRecents(2))).isFalse();
    }

    @Test
    void troisIncidentsDansLaFenetreRendentFragile() {
        assertThat(estFragile(incidentsRecents(3))).isTrue();
    }

    @Test
    void unIncidentResoluCompteAussi() {
        List<Incident> incidents = incidentsRecents(3);
        incidents.getFirst().resoudre(MAINTENANT.minusHours(1), null);

        assertThat(estFragile(incidents)).isTrue();
    }

    @Test
    void unIncidentAffecteCompteAussi() {
        List<Incident> incidents = incidentsRecents(3);
        incidents.getFirst().affecter(new Prestataire(), MAINTENANT.minusHours(1));

        assertThat(estFragile(incidents)).isTrue();
    }

    @Test
    void unIncidentOuvertIlYA30JoursEt1MinuteNeComptePlus() {
        List<Incident> incidents = incidentsRecents(2);
        incidents.add(ouvertDepuis(ID_EQUIPEMENT, Duration.ofDays(30).plusMinutes(1)));

        assertThat(estFragile(incidents)).isFalse();
    }

    @Test
    void unIncidentOuvertIlYA29JoursEt23h59Compte() {
        List<Incident> incidents = incidentsRecents(2);
        incidents.add(ouvertDepuis(ID_EQUIPEMENT, Duration.ofDays(29).plusHours(23).plusMinutes(59)));

        assertThat(estFragile(incidents)).isTrue();
    }

    @Test
    void unIncidentOuvertIlYA30JoursPileCompte() {
        List<Incident> incidents = incidentsRecents(2);
        incidents.add(ouvertDepuis(ID_EQUIPEMENT, Duration.ofDays(30)));

        assertThat(estFragile(incidents)).isTrue();
    }

    @Test
    void desIncidentsVieuxDe45JoursNeComptentPas() {
        List<Incident> incidents = incidentsRecents(2);
        for (int i = 0; i < 5; i++) {
            incidents.add(ouvertDepuis(ID_EQUIPEMENT, Duration.ofDays(45)));
        }

        assertThat(estFragile(incidents)).isFalse();
    }

    @Test
    void lEquipementVoisinNestPasContamine() {
        List<Incident> incidents = incidentsRecents(3);
        incidents.add(ouvertDepuis(ID_VOISIN, Duration.ofDays(1)));

        assertThat(politiqueFragilite.trouverEquipementsFragiles(incidents, MAINTENANT)).containsExactly(ID_EQUIPEMENT);
    }

    @Test
    void laFenetreCommence30JoursAvantMaintenant() {
        assertThat(politiqueFragilite.debutFenetre(MAINTENANT)).isEqualTo(MAINTENANT.minusDays(30));
    }
}
