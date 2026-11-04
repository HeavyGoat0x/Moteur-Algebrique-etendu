package fr.uvsq;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construit l'arbre d'opérateurs correspondant au plan JSON.
 *
 * <p>Chaque type d'opérateur ("SCAN", "SELECT"…) est associé à sa fabrique dans un registre :
 * ajouter un opérateur revient à enregistrer une fabrique, sans modifier {@link #build}.
 */
public class PlanBuilder {

  private final Map<String, Table> tables; // tables déjà chargées
  private final Map<String, OperatorFactory> factories = new HashMap<>(); // type → fabrique

  /**
   * Crée un constructeur de plan sur des tables déjà chargées.
   *
   * @param tables tables disponibles, indexées par leur nom dans la requête
   */
  public PlanBuilder(Map<String, Table> tables) {
    this.tables = tables;
    register("SCAN", PlanBuilder::scan);
    register("SELECT", PlanBuilder::select);
    register("PROJECT", PlanBuilder::project);
    register("JOIN", PlanBuilder::join);
    register("UNION", PlanBuilder::union);
    register("INTERSECTION", PlanBuilder::intersection);
    register("DIFFERENCE", PlanBuilder::difference);
    register("AGGREGATE", PlanBuilder::aggregate);
  }

  // Associe un type d'opérateur à sa fabrique (remplace la fabrique déjà enregistrée)
  void register(String type, OperatorFactory factory) {
    factories.put(type, factory);
  }

  /**
   * Construit récursivement l'opérateur décrit par un nœud du plan.
   *
   * @param node nœud JSON portant un champ "type" (SCAN, SELECT, PROJECT, JOIN, UNION,
   *     INTERSECTION, DIFFERENCE)
   * @return l'opérateur racine du sous-plan
   * @throws IllegalArgumentException si le type ou la table est inconnu, ou si un champ
   *     obligatoire manque
   */
  public Operator build(JsonObject node) {
    String type = required(node, "type").getAsString();
    OperatorFactory factory = factories.get(type);
    if (factory == null) {
      throw new IllegalArgumentException("Type inconnu : " + type);
    }
    return factory.create(node, this);
  }

  // Table chargée portant ce nom dans la requête
  Table table(String name) {
    Table table = tables.get(name);
    if (table == null) {
      throw new IllegalArgumentException("Table inconnue : " + name);
    }
    return table;
  }

  private static Operator scan(JsonObject node, PlanBuilder builder) {
    return new Scan(builder.table(required(node, "table").getAsString()));
  }

  private static Operator select(JsonObject node, PlanBuilder builder) {
    Operator input = builder.build(required(node, "input").getAsJsonObject());
    PredicateParser.RowPredicate pred =
        PredicateParser.parse(required(node, "predicate").getAsString());
    return new Select(input, pred);
  }

  private static Operator project(JsonObject node, PlanBuilder builder) {
    Operator input = builder.build(required(node, "input").getAsJsonObject());
    List<String> cols = new ArrayList<>();
    required(node, "columns").getAsJsonArray().forEach(e -> cols.add(e.getAsString()));
    return new Project(input, cols);
  }

  private static Operator join(JsonObject node, PlanBuilder builder) {
    Operator left = builder.build(required(node, "left").getAsJsonObject());
    Operator right = builder.build(required(node, "right").getAsJsonObject());
    String leftCol = required(node, "onLeft").getAsString();
    String rightCol = required(node, "onRight").getAsString();
    return new Join(left, right, leftCol, rightCol);
  }

  private static Operator union(JsonObject node, PlanBuilder builder) {
    Operator left = builder.build(required(node, "left").getAsJsonObject());
    Operator right = builder.build(required(node, "right").getAsJsonObject());
    return new Union(left, right);
  }

  private static Operator intersection(JsonObject node, PlanBuilder builder) {
    Operator left = builder.build(required(node, "left").getAsJsonObject());
    Operator right = builder.build(required(node, "right").getAsJsonObject());
    return new Intersection(left, right);
  }

  private static Operator difference(JsonObject node, PlanBuilder builder) {
    Operator left = builder.build(required(node, "left").getAsJsonObject());
    Operator right = builder.build(required(node, "right").getAsJsonObject());
    return new Difference(left, right);
  }

  private static Operator aggregate(JsonObject node, PlanBuilder builder) {
    Operator input = builder.build(required(node, "input").getAsJsonObject());
    String alias = required(node, "as").getAsString();
    String column = required(node, "column").getAsString();
    List<String> groupBy = required(node, "groupBy").getAsJsonArray().asList().stream()
        .map(JsonElement::getAsString).toList();
    String functionName = required(node, "function").getAsString();
    AggregateFunction function = Arrays.stream(AggregateFunction.values())
        .filter(f -> f.name().equalsIgnoreCase(functionName))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Fonction d'agrégation inconnue : " + functionName));
    return new Aggregate(input, groupBy, function, column, alias);
  }

  // Champ obligatoire d'un nœud JSON : message clair au lieu d'une NullPointerException de Gson
  static JsonElement required(JsonObject node, String field) {
    if (!node.has(field)) {
      throw new IllegalArgumentException("Champ manquant : " + field);
    }
    return node.get(field);
  }
}
