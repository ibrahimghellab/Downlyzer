import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

// simule des capteurs qui signalent des pannes à l'api (clé dans DOWNLYZER_API_KEY)
// lancer avec : java simulateur/SimulateurCapteurs.java [nombre] [adresse]
public class SimulateurCapteurs {

    private static final Logger JOURNAL = Logger.getLogger(SimulateurCapteurs.class.getName());
    private static final Duration PAUSE_ENTRE_SIGNALEMENTS = Duration.ofSeconds(3);

    // références des équipements des données de démo
    private static final List<String> REFERENCES = List.of(
            "ASC-001", "ASC-002", "ASC-003", "ASC-004", "ESC-001", "ESC-002", "ESC-003",
            "POR-001", "POR-002", "POR-003", "DIS-001", "DIS-002", "DIS-003", "ECR-001", "ECR-002", "ECR-003");
    private static final List<String> PANNES = List.of(
            "Porte bloquée en position ouverte depuis plus de 2 minutes",
            "Moteur en surchauffe, arrêt de sécurité déclenché",
            "Capteur de présence en défaut, l'équipement ne démarre plus",
            "Perte de communication avec le contrôleur de l'équipement",
            "Arrêt d'urgence déclenché, redémarrage automatique impossible");

    public static void main(String[] arguments) throws Exception {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%n");
        int nombre = arguments.length > 0 ? Integer.parseInt(arguments[0]) : 5;
        String adresse = arguments.length > 1 ? arguments[1] : "http://localhost:8080";
        String cle = System.getenv("DOWNLYZER_API_KEY");
        if (cle == null || cle.isBlank()) {
            System.err.println("Définissez la variable DOWNLYZER_API_KEY avec la clé d'API de l'application.");
            System.exit(1);
        }

        HttpClient client = HttpClient.newHttpClient();
        Random hasard = new Random();
        for (int i = 1; i <= nombre; i++) {
            String reference = REFERENCES.get(hasard.nextInt(REFERENCES.size()));
            String panne = PANNES.get(hasard.nextInt(PANNES.size()));
            HttpRequest requete = HttpRequest.newBuilder(URI.create(adresse + "/api/incidents"))
                    .header("Content-Type", "application/json")
                    .header("X-API-KEY", cle)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"referenceEquipement\":\"" + reference + "\",\"description\":\"" + panne + "\"}"))
                    .build();
            HttpResponse<String> reponse = client.send(requete, HttpResponse.BodyHandlers.ofString());
            JOURNAL.info(String.format("%s  capteur %-8s  %d  %s",
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    reference, reponse.statusCode(), resumer(reponse)));
            if (i < nombre) {
                Thread.sleep(PAUSE_ENTRE_SIGNALEMENTS);
            }
        }
    }

    private static String resumer(HttpResponse<String> reponse) {
        return switch (reponse.statusCode()) {
            case 201 -> "incident créé : " + reponse.headers().firstValue("Location").orElse("");
            case 409 -> "déjà signalé, incident en cours conservé";
            default -> reponse.body();
        };
    }
}
