package fr.uvsq;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Point d'entrée : exécute une requête JSON ou traduit une requête .alg en JSON.
 */
public class ExecutePlan {

  static final String DEFAULT_OUTPUT = "results/output.csv"; // emplacement attendu par l'automate

  /**
   * Exécute la requête passée en argument et indique le fichier produit.
   *
   * <p>Un fichier .alg est seulement traduit en un fichier .json placé à côté, sans être exécuté.
   *
   * <p>Une erreur de l'utilisateur (fichier absent, JSON mal formé ou incomplet, ligne .alg
   * illisible, table, colonne ou type inconnu) est affichée sur la sortie d'erreur, sans pile
   * d'appels.
   *
   * @param args un seul argument, le chemin du fichier JSON ou .alg de la requête
   */
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: java ExecutePlan requete.json|requete.alg");
      return;
    }

    try {
      String pathName = args[0];
      Path path = Path.of(pathName);
      if (pathName.endsWith(".alg")) {
        Path jsonPath = AlgTranslator.translateFile(path);
        System.out.println("Traduction en " + jsonPath.getFileName());
      } else {
        System.out.println("Résultat écrit dans " + run(path));
      }
    } catch (NoSuchFileException e) {
      System.err.println("Fichier introuvable : " + e.getFile());
    } catch (IOException | IllegalArgumentException | JsonParseException e) {
      System.err.println("Erreur : " + e.getMessage());
    }
  }

  // Charge les tables, exécute le plan et écrit le CSV ; renvoie le fichier écrit
  static Path run(Path configFile) throws IOException {
    // Lire le fichier JSON
    JsonObject config = JsonParser.parseString(Files.readString(configFile)).getAsJsonObject();

    // Charger les tables
    JsonObject tablesJson = PlanBuilder.required(config, "tables").getAsJsonObject();
    Map<String, Table> tables = new HashMap<>();
    for (String tableName : tablesJson.keySet()) {
      String csvPath = tablesJson.get(tableName).getAsString();
      tables.put(tableName, CsvUtils.readCsv(csvPath));  // lecture CSV → Table
    }

    // Construire et exécuter le plan → Table finale
    JsonObject planJson = PlanBuilder.required(config, "plan").getAsJsonObject();
    Operator plan = new PlanBuilder(tables).build(planJson);
    Table resultTable = plan.execute();

    // Écrire le CSV avec schéma, en créant son dossier s'il n'existe pas
    Path output = Path.of(outputFile(config));
    Path parent = output.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    CsvUtils.writeCsv(output.toString(), resultTable);
    return output;
  }

  // Fichier de sortie : champ "output" de la requête, sinon results/output.csv
  static String outputFile(JsonObject config) {
    return config.has("output") ? config.get("output").getAsString() : DEFAULT_OUTPUT;
  }
}
