package fr.uvsq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;


// PlanBuilder construit un plan à partir du JSON
public class PlanBuilder {

    private final Map<String, Table> tables; // tables déjà chargées

    public PlanBuilder(Map<String, Table> tables) {
        this.tables = tables;
    }

    public Operator build(JsonObject node) throws IOException {
        String type = node.get("type").getAsString();

        switch (type) {
            case "SCAN" -> {
                String tableName = node.get("table").getAsString();
                Table table = tables.get(tableName);
                if (table == null) throw new IllegalArgumentException("Table inconnue : " + tableName);
                return new Scan(table);
            }

            case "SELECT" -> {
                JsonObject inputNode = node.getAsJsonObject("input");
                Operator inputOp = build(inputNode);

                // On utilise PredicateParser pour interpréter le prédicat
                String predStr = node.get("predicate").getAsString();
                PredicateParser.RowPredicate pred = PredicateParser.parse(predStr);

                return new Select(inputOp, pred);
            }

            case "PROJECT" -> {
                Operator input = build(node.getAsJsonObject("input"));
                List<String> cols = new ArrayList<>();
                node.getAsJsonArray("columns").forEach(e -> cols.add(e.getAsString()));
                return new Project(input, cols);
            }

            case "JOIN" -> {
                Operator left = build(node.getAsJsonObject("left"));
                Operator right = build(node.getAsJsonObject("right"));
                String leftCol = node.get("onLeft").getAsString();
                String rightCol = node.get("onRight").getAsString();
                return new Join(left, right, leftCol, rightCol);
            }

            default -> throw new IllegalArgumentException("Type inconnu : " + type);
        }
    }
}