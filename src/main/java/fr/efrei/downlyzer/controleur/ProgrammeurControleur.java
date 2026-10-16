/*package fr.efrei.downlyzer.controleur;

import fr.efrei.downlyzer.service.ProgrammeurService;
// 5.4 -- Imports utiles à la couche "V" : Controller, Model (données transmises à la vue), HttpStatus et ResponseStatusException (erreur 404 si l'id n'existe pas).
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
// 5.4 -- Imports de la version REST (4.x) : ResponseEntity et ServletUriComponentsBuilder ne servent plus, car les méthodes ne renvoient plus du JSON mais le nom d'une vue.
//import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;

//import java.net.URI;
import java.util.List;

// 5.4 -- @RestController = @Controller + @ResponseBody : toute valeur retournée est écrite telle quelle dans la réponse HTTP (JSON).
// Avec Thymeleaf, la valeur retournée doit être le NOM d'un template HTML : on remplace donc @RestController par @Controller.
//@RestController
@Controller
//@RequestMapping("/api")
@RequestMapping("/programmeurs")
public class ProgrammeurControleur {

//    @Autowired
//    ProgrammeurService programmeurService;

    // Injection de dépendance via le constructeur -- DÉBUT //
    private final ProgrammeurService programmeurService;
    public ProgrammeurControleur(ProgrammeurService programmeurService) { this.programmeurService = programmeurService;}
    // Injection de dépendance via le constructeur -- FIN //

    // Ancienne route : @GetMapping("/programmeurs"). Le nom de la ressource est désormais dans le @RequestMapping de la classe.
    // GET /programmeurs : on lit la collection.
    // ResponseEntity<List<Programmeur>> : on contrôle explicitement le code HTTP. 200 OK, même si la liste est vide
    // (une liste vide est une réponse valide, pas une erreur : ce n'est PAS un 404).
    // 5.9 -- Avec Thymeleaf : la méthode reçoit un objet Model, y dépose la liste (addAttribute) et renvoie le nom du template à afficher.
    // Le 1er paramètre d'addAttribute ("programmeurs") est le nom sous lequel la vue retrouvera la liste : ${programmeurs} dans listeProgrammeurs.html (cf. 5.8).
    @GetMapping
//    public ResponseEntity<List<Programmeur>> afficherInfosProgrammeurs(){    // Sans Thymeleaf (réponse JSON, cf. 4.9)
    public String afficherInfosProgrammeurs(Model model){    // Avec Thymeleaf
        List<Programmeur> liste = programmeurService.getProgrammeurs();
//        return ResponseEntity.ok(liste);
        // 5.8 -- Première manipulation (faite en séance) : transmettre à la vue le prénom du DERNIER programmeur de la liste.
        // Dans le template : <span th:text="${prenomDernierProgrammeur}"></span>. Remplacée ensuite par la liste complète (5.9).
//        model.addAttribute("prenomDernierProgrammeur", liste.get(liste.size() - 1).getPrenom());
        model.addAttribute("programmeurs", liste);
        return "listeProgrammeurs";    // Nom du fichier HTML à charger, sans l'extension .html : src/main/resources/templates/listeProgrammeurs.html (cf. 5.3)
    }

    // Ancienne route : /unProgrammeur/{idProgrammeur}. "unProgrammeur" n'apporte rien : le verbe GET dit déjà "lire",
    // et /programmeurs/{id} se lit comme "le programmeur n° id".
    // Optional<Programmeur> en retour : si l'id n'existait pas, le client recevait une erreur 500.
    // Maintenant : 200 + le programmeur s'il existe, 404 sinon.
    // 5.12 -- Cette route sert maintenant à afficher la page "detailsProgrammeur" : un formulaire PRÉREMPLI avec les données du programmeur (bouton "Modifiez").
    // Le programmeur est déposé dans le Model sous le nom "programmeurToUpdate" : c'est le nom utilisé par th:object dans le template.
    // Le 404 est conservé : ResponseStatusException produit la même réponse qu'avant, mais sous forme de page d'erreur (cf. server.error.include-message dans application.properties).
    @GetMapping("/{idProgrammeur}")
//    public ResponseEntity<Programmeur> afficherUnProgrammeur(@PathVariable("idProgrammeur") Integer idProg) {
//        return programmeurService.getUnProgrammeur(idProg)
//                .map(ResponseEntity::ok)                          // présent : 200 OK + programmeur dans le corps
//                .orElse(ResponseEntity.notFound().build());       // absent : 404 Not Found, sans corps
    public String afficherUnProgrammeur(@PathVariable("idProgrammeur") Integer idProg, Model model) {
        Programmeur programmeur = programmeurService.getUnProgrammeur(idProg)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Le programmeur dont l'id est " + idProg + " n'existe pas"));
        model.addAttribute("programmeurToUpdate", programmeur);
        return "detailsProgrammeur";
    }

    // 5.11 -- Pour que Thymeleaf affiche le formulaire VIERGE d'ajout (bouton "Ajoutez") : on dépose un Programmeur vide dans le Model,
    // le formulaire de nouveauProgrammeur.html s'y rattache avec th:object.
    // Route : "/nouveau" (un nom, pas un verbe, cf. 4.8). Elle ressemble à "/{idProgrammeur}" mais Spring choisit toujours la route littérale
    // plutôt que la route à variable, quel que soit l'ordre de déclaration : "/programmeurs/nouveau" n'est donc pas pris pour un id.
    @GetMapping("/nouveau")
    public String preparerAjoutProgrammeur(Model model) {
        model.addAttribute("nouveauProgrammeur", new Programmeur());
        return "nouveauProgrammeur";
    }

    // Ancienne route : /supprimerProgrammeur/{idProgrammeur}. Le verbe est déjà dans la méthode HTTP : DELETE.
    // 204 No Content : la suppression a réussi et il n'y a plus rien à renvoyer. 404 si l'id n'existe pas.
    // 5.10.2 -- Un formulaire HTML ne sait envoyer que GET ou POST : cliquer sur "Supprimez" provoque une erreur 405 (Method Not Allowed),
    // car cette route n'accepte que DELETE. Solution (5.10.3) : le formulaire envoie un POST avec un champ caché _method=DELETE,
    // et le HiddenHttpMethodFilter transforme la requête en DELETE avant qu'elle n'arrive ici (à activer dans application.properties).
    // 5.10.3 -- Après la suppression, on REDIRIGE vers la liste au lieu de retourner directement le template :
    // le navigateur reçoit un 302 + l'en-tête Location et refait de lui-même un GET /programmeurs. La liste affichée est ainsi relue en base (donc à jour),
    // et un rafraîchissement de la page (F5) ne rejoue pas la suppression.
    @DeleteMapping("/{idProgrammeur}")
//    public ResponseEntity<Void> deleteProgrammeur(@PathVariable("idProgrammeur") Integer idProg) {
//        if (programmeurService.supprimerProgrammeur(idProg)) return ResponseEntity.noContent().build();
//        return ResponseEntity.notFound().build();
    public String deleteProgrammeur(@PathVariable("idProgrammeur") Integer idProg) {
        if (!programmeurService.supprimerProgrammeur(idProg)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Le programmeur dont l'id est " + idProg + " n'existe pas");
        }
        return "redirect:/programmeurs";    // "redirect:" + une ROUTE (pas un nom de template) : ici celle de la liste
    }

    // Ancienne route : /ajouterProgrammeur. Même principe : POST sur la collection = "ajouter un élément à /programmeurs".
    // 201 Created (et non 200) + en-tête Location (l'URL de la nouvelle ressource) + la ressource créée dans le corps.
    // Le client récupère ainsi l'id généré par la base sans avoir à refaire un GET.
    // 5.11.2 -- ATTENTION ! Notez bien l'usage de @ModelAttribute au lieu de @RequestBody : un formulaire HTML n'envoie pas du JSON
    // mais des paires nom=valeur (application/x-www-form-urlencoded). Les champs du formulaire (th:field) sont associés aux attributs de Programmeur d'après leur nom.
    // Si l'ajout a réussi, on redirige vers la liste (comme pour la suppression) : le nouveau programmeur y apparaît.
    @PostMapping
//    public ResponseEntity<Programmeur> creerProgrammeur(@RequestBody Programmeur programmeur){
//        Programmeur programmeurCree = programmeurService.ajouterProgrammeur(programmeur);
//        URI location = ServletUriComponentsBuilder.fromCurrentRequest()     // URL de la requête : .../programmeurs
//                .path("/{id}")                                              // on y ajoute /{id}
//                .buildAndExpand(programmeurCree.getId())
//                .toUri();
//        return ResponseEntity.created(location).body(programmeurCree);
    public String creerProgrammeur(@ModelAttribute Programmeur programmeur){
        programmeurService.ajouterProgrammeur(programmeur);
        return "redirect:/programmeurs";
    }

    // Ancienne route : "modifier/{idProgrammeur}" (verbe dans l'URL, et "/" initial oublié).
    // PUT /programmeurs/{id} : on REMPLACE l'ensemble de la ressource (cf. 4.6.3). PUT est idempotent.
    // 200 OK + le programmeur à jour. (204 No Content, sans corps, serait aussi acceptable.) 404 si l'id n'existe pas.
    // On ne crée PAS le programmeur s'il n'existe pas : les ids sont générés par la base, pas choisis par le client.
    // 5.12.2 -- Même mécanique que pour la suppression : le formulaire "detailsProgrammeur" envoie un POST avec _method=PUT (5.10.2),
    // les données arrivent sous forme de formulaire (@ModelAttribute au lieu de @RequestBody), puis on redirige vers la liste actualisée.
    @PutMapping("/{idProgrammeur}")
//    public ResponseEntity<Programmeur> modifierProgrammeur(@PathVariable Integer idProgrammeur,@RequestBody Programmeur programmeurModified ){
//        return programmeurService.modifierProgrammeur(idProgrammeur,programmeurModified)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
    public String modifierProgrammeur(@PathVariable Integer idProgrammeur,@ModelAttribute Programmeur programmeurModified ){
        if (programmeurService.modifierProgrammeur(idProgrammeur,programmeurModified).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Le programmeur dont l'id est " + idProgrammeur + " n'existe pas");
        }
        return "redirect:/programmeurs";
    }

}
*/