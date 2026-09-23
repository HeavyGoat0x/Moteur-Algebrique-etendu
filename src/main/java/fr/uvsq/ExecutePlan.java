package fr.uvsq;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
public class ExecutePlan {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java ExecutePlan config.json");
            return;
        }
        // String csvPath2 = "/disk1/tag/ly/Documents/ens/form/cplProg/td/mkTD/cp4_alg_noter/chkPrjEtu/results/td4-miniprojet-uvsq22507989/TSelect_output.csv";
        // Table t2 = CSVUtils.readCSV(csvPath2);  // lecture CSV → Table

        String configFile = args[0];

        // Lire le fichier JSON
        JsonObject config = JsonParser.parseString(Files.readString(Path.of(configFile))).getAsJsonObject();

        // Charger les tables
        JsonObject tablesJson = config.getAsJsonObject("tables");
        Map<String, Table> tables = new HashMap<>();
        for (String tableName : tablesJson.keySet()) {
            String csvPath = tablesJson.get(tableName).getAsString();
            Table t = CSVUtils.readCSV(csvPath);  // lecture CSV → Table
            tables.put(tableName, t);
        }

        // Construire le plan
        JsonObject planJson = config.getAsJsonObject("plan");
        PlanBuilder builder = new PlanBuilder(tables);
        Operator plan = builder.build(planJson);

        // Exécuter le plan → Table finale
        Table resultTable = plan.execute();

        // Fichier de sortie
        String outputFile = config.has("output") ? config.get("output").getAsString() : "results/output.csv";

        // Écrire le CSV avec schéma
        CSVUtils.writeCSV(outputFile, resultTable);

        System.out.println("Résultat écrit dans " + outputFile);
    }
}
