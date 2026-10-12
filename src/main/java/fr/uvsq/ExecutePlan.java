package fr.uvsq;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Point d'entrée : exécute le plan algébrique décrit dans un fichier JSON.
 */
public class ExecutePlan {

  /**
   * Charge les tables et le plan du fichier JSON, exécute le plan et écrit le résultat en CSV.
   *
   * @param args un seul argument, le chemin du fichier JSON de la requête
   * @throws Exception si la lecture, l'exécution ou l'écriture échoue
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("Usage: java ExecutePlan config.json");
      return;
    }

    String configFile = args[0];

    // Lire le fichier JSON
    JsonObject config = JsonParser.parseString(Files.readString(Path.of(configFile)))
        .getAsJsonObject();

    // Charger les tables
    JsonObject tablesJson = config.getAsJsonObject("tables");
    Map<String, Table> tables = new HashMap<>();
    for (String tableName : tablesJson.keySet()) {
      String csvPath = tablesJson.get(tableName).getAsString();
      Table t = CsvUtils.readCsv(csvPath);  // lecture CSV → Table
      tables.put(tableName, t);
    }

    // Construire le plan
    JsonObject planJson = config.getAsJsonObject("plan");
    PlanBuilder builder = new PlanBuilder(tables);
    Operator plan = builder.build(planJson);

    // Exécuter le plan → Table finale
    Table resultTable = plan.execute();

    // Fichier de sortie
    String outputFile =
        config.has("output") ? config.get("output").getAsString() : "results/output.csv";

    // Écrire le CSV avec schéma
    CsvUtils.writeCsv(outputFile, resultTable);

    System.out.println("Résultat écrit dans " + outputFile);
  }
}
