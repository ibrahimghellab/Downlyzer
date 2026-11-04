package fr.efrei.downlyzer.modele;

public enum Profil {
    RESPONSABLE,
    AGENT;

    // les niveaux, la carte et le tableau de bord sont réservés au responsable
    public boolean peutSuperviser() {
        return this == RESPONSABLE;
    }
}
