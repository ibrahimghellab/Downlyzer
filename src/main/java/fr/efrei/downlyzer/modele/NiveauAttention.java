package fr.efrei.downlyzer.modele;

// Déclarés du plus urgent au moins urgent : l'ordre naturel de l'énumération sert à trouver
// l'incident le plus urgent d'une station. Un incident non affecté passe avant un incident pris en charge
public enum NiveauAttention {
    ROUGE,
    ORANGE,
    VERT,
    PRIS_EN_CHARGE
}
