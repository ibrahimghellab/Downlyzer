import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

// charge db/create.sql dans la base déployée, avec le pilote de l'appli (le client de xampp sait pas parler à mysql 8)
// lancer depuis la racine du projet avec : java -cp <jar du pilote mariadb> db/ChargementBase.java
// variables : DB_ADMIN_URL, DB_ADMIN_USERNAME, DB_ADMIN_PASSWORD, et DB_PASSWORD pour créer downlyzer_app
public class ChargementBase {

    public static void main(String[] arguments) throws Exception {
        String url = variable("DB_ADMIN_URL");
        String motDePasseApplication = variable("DB_PASSWORD");
        // le mot de passe est collé dans le CREATE USER, qui accepte pas de paramètre
        if (!motDePasseApplication.matches("[A-Za-z0-9]{16,}")) {
            arreter("DB_PASSWORD doit faire au moins 16 caractères, lettres et chiffres seulement.");
        }

        // un ; en fin de ligne termine une instruction, même suivi d'un commentaire
        String[] instructions = Files.readString(Path.of("db/create.sql")).split(";[ \\t]*(--[^\\n]*)?\\r?\\n");

        try (Connection connexion = DriverManager.getConnection(url, variable("DB_ADMIN_USERNAME"), variable("DB_ADMIN_PASSWORD"));
             Statement requete = connexion.createStatement()) {
            // même fuseau que l'appli, sinon NOW() est en utc et les incidents sont décalés d'une heure
            requete.execute("SET time_zone = '+01:00'");
            int executees = 0;
            for (String instruction : instructions) {
                if (!estVide(instruction)) {
                    requete.execute(instruction);
                    executees++;
                }
            }
            System.out.println(executees + " instructions de db/create.sql exécutées.");

            requete.execute("CREATE USER IF NOT EXISTS 'downlyzer_app'@'%' IDENTIFIED BY '" + motDePasseApplication + "'");
            requete.execute("ALTER USER 'downlyzer_app'@'%' IDENTIFIED BY '" + motDePasseApplication + "'");
            requete.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON downlyzer.* TO 'downlyzer_app'@'%'");
            System.out.println("Compte downlyzer_app prêt.");
        }
    }

    // un morceau qui contient que des commentaires et des lignes vides
    private static boolean estVide(String instruction) {
        return instruction.lines().map(String::strip).allMatch(ligne -> ligne.isEmpty() || ligne.startsWith("--"));
    }

    private static String variable(String nom) {
        String valeur = System.getenv(nom);
        if (valeur == null || valeur.isBlank()) {
            arreter("Définissez la variable " + nom + ".");
        }
        return valeur;
    }

    private static void arreter(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
