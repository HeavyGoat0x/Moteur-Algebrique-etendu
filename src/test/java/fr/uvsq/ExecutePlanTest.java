package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests de bout en bout : plans JSON du dépôt (data/) comparés aux sorties attendues (expected/).
 */
class ExecutePlanTest {

  private static final Path DATA = Path.of("data");
  private static final Path EXPECTED = Path.of("expected");

  @TempDir
  Path dossier;

  // Le plan lit ses tables dans data/ (lecture seule) et écrit sa sortie dans le dossier temporaire
  private Path preparerRequete(String nom, Path sortie) throws Exception {
    JsonObject requete =
        JsonParser.parseString(Files.readString(DATA.resolve(nom + ".json"))).getAsJsonObject();
    JsonObject tables = requete.getAsJsonObject("tables");
    for (String table : List.copyOf(tables.keySet())) {
      Path csv = DATA.resolve(tables.get(table).getAsString()).toAbsolutePath();
      tables.addProperty(table, csv.toString());
    }
    requete.addProperty("output", sortie.toString());

    Path fichier = dossier.resolve(nom + ".json");
    Files.writeString(fichier, requete.toString());
    return fichier;
  }

  // Exécute main en capturant la sortie d'erreur
  private static String erreurDe(String... args) {
    PrintStream sortieErreur = System.err;
    ByteArrayOutputStream capture = new ByteArrayOutputStream();
    System.setErr(new PrintStream(capture, true, StandardCharsets.UTF_8));
    try {
      ExecutePlan.main(args);
    } finally {
      System.setErr(sortieErreur);
    }
    return capture.toString(StandardCharsets.UTF_8);
  }

  @ParameterizedTest
  @ValueSource(strings = {"plan1", "reqProjSel", "optJoin", "unionAges", "interAges", "diffAges"})
  void main_planDuDepot_produitLeResultatAttendu(String nom) throws Exception {
    Path sortie = Files.createDirectories(dossier.resolve("results")).resolve("output.csv");

    ExecutePlan.main(new String[] {preparerRequete(nom, sortie).toString()});

    assertEquals(Files.readAllLines(EXPECTED.resolve(nom + "_expected.csv")),
        Files.readAllLines(sortie));
  }

  @Test
  void main_sansArgument_afficheUsage() throws Exception {
    PrintStream sortieStandard = System.out;
    ByteArrayOutputStream capture = new ByteArrayOutputStream();
    System.setOut(new PrintStream(capture, true, StandardCharsets.UTF_8));
    try {
      ExecutePlan.main(new String[] {});
    } finally {
      System.setOut(sortieStandard);
    }
    assertTrue(capture.toString(StandardCharsets.UTF_8).contains("Usage"));
  }

  @Test
  void outputFile_sansChampOutput_renvoieResultsOutputCsv() {
    assertEquals("results/output.csv", ExecutePlan.outputFile(new JsonObject()));
  }

  @Test
  void outputFile_avecChampOutput_renvoieCeChemin() {
    JsonObject config = new JsonObject();
    config.addProperty("output", "sorties/resultat.csv");
    assertEquals("sorties/resultat.csv", ExecutePlan.outputFile(config));
  }

  @Test
  void run_dossierDeSortieAbsent_leCree() throws Exception {
    Path sortie = dossier.resolve("nouveau").resolve("output.csv");

    Path ecrit = ExecutePlan.run(preparerRequete("reqProjSel", sortie));

    assertEquals(sortie, ecrit);
    assertEquals(Files.readAllLines(EXPECTED.resolve("reqProjSel_expected.csv")),
        Files.readAllLines(sortie));
  }

  @Test
  void main_fichierAbsent_afficheFichierIntrouvable() {
    String erreur = erreurDe(dossier.resolve("absent.json").toString());
    assertTrue(erreur.contains("Fichier introuvable"));
    assertTrue(erreur.contains("absent.json"));
  }

  @Test
  void main_tableInconnue_afficheUnMessageClair() throws Exception {
    Path requete = Files.writeString(dossier.resolve("requete.json"), """
        {"tables": {}, "plan": {"type": "SCAN", "table": "clients"}}
        """);
    assertTrue(erreurDe(requete.toString()).contains("Table inconnue : clients"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"tables", "plan"})
  void main_champDeRequeteManquant_afficheLeChamp(String champ) throws Exception {
    JsonObject requete = JsonParser.parseString("""
        {"tables": {}, "plan": {"type": "SCAN", "table": "users"}}
        """).getAsJsonObject();
    requete.remove(champ);
    Path fichier = Files.writeString(dossier.resolve("requete.json"), requete.toString());
    assertTrue(erreurDe(fichier.toString()).contains("Champ manquant : " + champ));
  }

  @Test
  void main_jsonMalForme_afficheUneErreur() throws Exception {
    Path requete = Files.writeString(dossier.resolve("requete.json"), "{\"tables\": {");
    assertTrue(erreurDe(requete.toString()).startsWith("Erreur : "));
  }

  @Test
  void main_ligneDeMauvaiseLargeur_afficheUneErreur() throws Exception {
    Path csv = Files.writeString(dossier.resolve("users.csv"), "id,name,age\n2,Bob\n");
    JsonObject requete = JsonParser.parseString("""
        {"tables": {}, "plan": {"type": "SCAN", "table": "users"}}
        """).getAsJsonObject();
    requete.getAsJsonObject("tables").addProperty("users", csv.toString());
    requete.addProperty("output", dossier.resolve("output.csv").toString());
    Path fichier = Files.writeString(dossier.resolve("requete.json"), requete.toString());

    assertTrue(erreurDe(fichier.toString()).startsWith("Erreur : Ligne de 2 valeurs"));
  }

  @Test
  void main_schemasIncompatibles_afficheLesDeuxSchemasSansEcrire() throws Exception {
    JsonObject requete = JsonParser.parseString("""
        {"tables": {},
         "plan": {"type": "UNION",
                  "left": {"type": "SCAN", "table": "users"},
                  "right": {"type": "SCAN", "table": "orders"}}}
        """).getAsJsonObject();
    JsonObject tables = requete.getAsJsonObject("tables");
    tables.addProperty("users", DATA.resolve("users.csv").toAbsolutePath().toString());
    tables.addProperty("orders", DATA.resolve("orders.csv").toAbsolutePath().toString());
    Path sortie = dossier.resolve("output.csv");
    requete.addProperty("output", sortie.toString());
    Path fichier = Files.writeString(dossier.resolve("requete.json"), requete.toString());

    String erreur = erreurDe(fichier.toString());

    assertTrue(erreur.startsWith(
        "Erreur : Schémas incompatibles : [id, name, age] et [id, user_id, amount]"));
    assertFalse(Files.exists(sortie));
  }
}
