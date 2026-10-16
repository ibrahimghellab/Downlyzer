package fr.efrei.downlyzer.exception;

public class UtilisateurIntrouvableException extends RessourceIntrouvableException{
    public UtilisateurIntrouvableException(String login) {
        super("L'utilisateur « " + login + " » n'existe pas.");
    }
}
