package fr.uvsq;

import com.google.gson.JsonObject;
import java.util.ArrayList;
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
  }

  // Associe un type d'opérateur à sa fabrique (remplace la fabrique déjà enregistrée)
  void register(String type, OperatorFactory factory) {
    factories.put(type, factory);
  }

  /**
   * Construit récursivement l'opérateur décrit par un nœud du plan.
   *
   * @param node nœud JSON portant un champ "type" (SCAN, SELECT, PROJECT, JOIN)
   * @return l'opérateur racine du sous-plan
   * @throws IllegalArgumentException si le type ou la table est inconnu
   */
  public Operator build(JsonObject node) {
    String type = node.get("type").getAsString();
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
    return new Scan(builder.table(node.get("table").getAsString()));
  }

  private static Operator select(JsonObject node, PlanBuilder builder) {
    Operator input = builder.build(node.getAsJsonObject("input"));
    PredicateParser.RowPredicate pred = PredicateParser.parse(node.get("predicate").getAsString());
    return new Select(input, pred);
  }

  private static Operator project(JsonObject node, PlanBuilder builder) {
    Operator input = builder.build(node.getAsJsonObject("input"));
    List<String> cols = new ArrayList<>();
    node.getAsJsonArray("columns").forEach(e -> cols.add(e.getAsString()));
    return new Project(input, cols);
  }

  private static Operator join(JsonObject node, PlanBuilder builder) {
    Operator left = builder.build(node.getAsJsonObject("left"));
    Operator right = builder.build(node.getAsJsonObject("right"));
    String leftCol = node.get("onLeft").getAsString();
    String rightCol = node.get("onRight").getAsString();
    return new Join(left, right, leftCol, rightCol);
  }
}
