package fr.uvsq;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Construit l'arbre d'opérateurs correspondant au plan JSON.
 */
public class PlanBuilder {

  private final Map<String, Table> tables; // tables déjà chargées

  /**
   * Crée un constructeur de plan sur des tables déjà chargées.
   *
   * @param tables tables disponibles, indexées par leur nom dans la requête
   */
  public PlanBuilder(Map<String, Table> tables) {
    this.tables = tables;
  }

  /**
   * Construit récursivement l'opérateur décrit par un nœud du plan.
   *
   * @param node nœud JSON portant un champ "type" (SCAN, SELECT, PROJECT, JOIN)
   * @return l'opérateur racine du sous-plan
   * @throws IOException si la construction nécessite une lecture qui échoue
   * @throws IllegalArgumentException si le type ou la table est inconnu
   */
  public Operator build(JsonObject node) throws IOException {
    String type = node.get("type").getAsString();

    switch (type) {
      case "SCAN" -> {
        String tableName = node.get("table").getAsString();
        Table table = tables.get(tableName);
        if (table == null) {
          throw new IllegalArgumentException("Table inconnue : " + tableName);
        }
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
