package fr.efrei.downlyzer.modele;

public enum Profil {
    RESPONSABLE,
    AGENT;

    // Niveaux d'attention (RG3-RG4), carte et tableau de bord : des outils de pilotage, réservés au responsable.
    // Les routes correspondantes sont en plus bloquées par SecuriteConfiguration (RG11)
    public boolean peutSuperviser() {
        return this == RESPONSABLE;
    }
}
