package fr.efrei.downlyzer.service;

import fr.efrei.downlyzer.modele.NiveauAttention;
import fr.efrei.downlyzer.modele.equipement.Equipement;
import fr.efrei.downlyzer.modele.incident.Incident;
import fr.efrei.downlyzer.modele.prestataire.Prestataire;
import fr.efrei.downlyzer.modele.utilisateur.Utilisateur;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// heure fixe pour que le résultat dépende pas du jour où on lance le test
class PolitiqueUrgenceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 10, 14, 12, 0);

    private final PolitiqueUrgence politiqueUrgence = new PolitiqueUrgence();

    private static Incident incidentNonAffecteOuvertDepuis(Duration duree) {
        return Incident.declarerManuellement(new Equipement(), new Utilisateur(), "Ascenseur bloqué",
                MAINTENANT.minus(duree));
    }

    private NiveauAttention niveauApres(Duration duree) {
        return politiqueUrgence.evaluer(incidentNonAffecteOuvertDepuis(duree), MAINTENANT);
    }

    @Test
    void unIncidentNonAffecteOuvertDepuisMoinsDe48HeuresEstVert() {
        assertThat(niveauApres(Duration.ofHours(2))).isEqualTo(NiveauAttention.VERT);
        assertThat(niveauApres(Duration.ofHours(47))).isEqualTo(NiveauAttention.VERT);
        assertThat(niveauApres(Duration.ofHours(48).minusSeconds(1))).isEqualTo(NiveauAttention.VERT);
    }

    @Test
    void aPartirDe48HeuresPileIlDevientOrange() {
        assertThat(niveauApres(Duration.ofHours(48))).isEqualTo(NiveauAttention.ORANGE);
        assertThat(niveauApres(Duration.ofHours(72).minusSeconds(1))).isEqualTo(NiveauAttention.ORANGE);
    }

    @Test
    void aPartirDe72HeuresPileIlDevientRouge() {
        assertThat(niveauApres(Duration.ofHours(72))).isEqualTo(NiveauAttention.ROUGE);
        assertThat(niveauApres(Duration.ofDays(5))).isEqualTo(NiveauAttention.ROUGE);
    }

    @Test
    void unIncidentAffecteEstPrisEnChargeMemeOuvertDepuisLongtemps() {
        for (Duration duree : new Duration[]{Duration.ofHours(1), Duration.ofHours(80)}) {
            Incident incident = incidentNonAffecteOuvertDepuis(duree);
            incident.affecter(new Prestataire(), MAINTENANT.minusMinutes(30));

            assertThat(politiqueUrgence.evaluer(incident, MAINTENANT)).isEqualTo(NiveauAttention.PRIS_EN_CHARGE);
        }
    }

    @Test
    void unIncidentResoluNAPasDeNiveauDAttention() {
        Incident incident = incidentNonAffecteOuvertDepuis(Duration.ofHours(10));
        incident.resoudre(MAINTENANT.minusHours(1), null);

        assertThatThrownBy(() -> politiqueUrgence.evaluer(incident, MAINTENANT))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
