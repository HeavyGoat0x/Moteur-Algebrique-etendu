package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PlanBuilderTest {

  // Un nœud complet de chaque type : les tests de champ manquant en retirent un champ
  private static final Map<String, String> NOEUDS_COMPLETS = Map.of(
      "SCAN", """
          {"type": "SCAN", "table": "users"}
          """,
      "SELECT", """
          {"type": "SELECT", "predicate": "age < 25",
           "input": {"type": "SCAN", "table": "users"}}
          """,
      "PROJECT", """
          {"type": "PROJECT", "columns": ["name"],
           "input": {"type": "SCAN", "table": "users"}}
          """,
      "JOIN", """
          {"type": "JOIN", "onLeft": "id", "onRight": "user_id",
           "left": {"type": "SCAN", "table": "users"},
           "right": {"type": "SCAN", "table": "orders"}}
          """,
      "UNION", """
          {"type": "UNION",
           "left": {"type": "SCAN", "table": "users"},
           "right": {"type": "SCAN", "table": "users"}}
          """,
      "INTERSECTION", """
          {"type": "INTERSECTION",
           "left": {"type": "SCAN", "table": "users"},
           "right": {"type": "SCAN", "table": "users"}}
          """,
      "DIFFERENCE", """
          {"type": "DIFFERENCE",
           "left": {"type": "SCAN", "table": "users"},
           "right": {"type": "SCAN", "table": "users"}}
          """,
      "AGGREGATE", """
          {"type": "AGGREGATE", "groupBy": ["user_id"], "function": "SUM",
           "column": "amount", "as": "total",
           "input": {"type": "SCAN", "table": "orders"}}
          """);

  private PlanBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new PlanBuilder(Map.of("users", Fixtures.users(), "orders", Fixtures.orders()));
  }

  private static JsonObject json(String texte) {
    return JsonParser.parseString(texte).getAsJsonObject();
  }

  private Table executer(String plan) throws IOException {
    return builder.build(json(plan)).execute();
  }

  @Test
  void build_scan_renvoieLaTable() throws IOException {
    Table resultat = executer("""
        {"type": "SCAN", "table": "users"}
        """);
    assertEquals(Fixtures.valeurs(Fixtures.users()), Fixtures.valeurs(resultat));
  }

  @Test
  void build_select_appliqueLePredicat() throws IOException {
    Table resultat = executer("""
        {"type": "SELECT", "predicate": "name = 'Bob'",
         "input": {"type": "SCAN", "table": "users"}}
        """);
    assertEquals(List.of(List.of(2, "Bob", 30)), Fixtures.valeurs(resultat));
  }

  @Test
  void build_project_gardeLesColonnesDemandees() throws IOException {
    Table resultat = executer("""
        {"type": "PROJECT", "columns": ["name", "id"],
         "input": {"type": "SCAN", "table": "users"}}
        """);
    assertEquals(List.of("name", "id"), resultat.getColumns());
    assertEquals(List.of("Alice", 1), resultat.getRows().get(0).getValues());
  }

  @Test
  void build_join_utiliseLesClesOnLeftEtOnRight() throws IOException {
    Table resultat = executer("""
        {"type": "JOIN", "onLeft": "id", "onRight": "user_id",
         "left": {"type": "SCAN", "table": "users"},
         "right": {"type": "SCAN", "table": "orders"}}
        """);
    assertEquals(4, resultat.getRows().size());
    assertEquals(List.of(1, "Alice", 22, 1, 150), resultat.getRows().get(0).getValues());
  }

  @Test
  void build_planReqProjSel_renvoieLesNomsDesMoinsDeVingtCinqAns() throws IOException {
    Table resultat = executer("""
        {"type": "PROJECT", "columns": ["name"],
         "input": {"type": "SELECT", "predicate": "age < 25",
                   "input": {"type": "SCAN", "table": "users"}}}
        """);
    assertEquals(List.of(List.of("Alice"), List.of("David")), Fixtures.valeurs(resultat));
  }

  @Test
  void build_planOptJoin_renvoieLesMontantsParNom() throws IOException {
    Table resultat = executer("""
        {"type": "PROJECT", "columns": ["name", "amount"],
         "input": {"type": "JOIN", "onLeft": "id", "onRight": "user_id",
                   "left": {"type": "PROJECT", "columns": ["id", "name"],
                            "input": {"type": "SCAN", "table": "users"}},
                   "right": {"type": "SCAN", "table": "orders"}}}
        """);
    assertEquals(List.of("name", "amount"), resultat.getColumns());
    assertEquals(List.of(
        List.of("Alice", 150), List.of("Bob", 100), List.of("Bob", 200), List.of("Charlie", 50)),
        Fixtures.valeurs(resultat));
  }

  @Test
  void build_union_combineLesDeuxSousPlans() throws IOException {
    Table resultat = executer("""
        {"type": "UNION",
         "left": {"type": "SELECT", "predicate": "age < 25",
                  "input": {"type": "SCAN", "table": "users"}},
         "right": {"type": "SELECT", "predicate": "age > 25",
                   "input": {"type": "SCAN", "table": "users"}}}
        """);
    assertEquals(
        List.of(
            List.of(1, "Alice", 22),
            List.of(4, "David", 19),
            List.of(2, "Bob", 30),
            List.of(3, "Charlie", 27)),
        Fixtures.valeurs(resultat));
  }

  @Test
  void build_intersection_gardeLesLignesCommunes() throws IOException {
    Table resultat = executer("""
        {"type": "INTERSECTION",
         "left": {"type": "SELECT", "predicate": "age < 30",
                  "input": {"type": "SCAN", "table": "users"}},
         "right": {"type": "SELECT", "predicate": "age > 20",
                   "input": {"type": "SCAN", "table": "users"}}}
        """);
    assertEquals(
        List.of(List.of(1, "Alice", 22), List.of(3, "Charlie", 27)),
        Fixtures.valeurs(resultat));
  }

  @Test
  void build_difference_retireLesLignesDeDroite() throws IOException {
    Table resultat = executer("""
        {"type": "DIFFERENCE",
         "left": {"type": "SCAN", "table": "users"},
         "right": {"type": "SELECT", "predicate": "age < 25",
                   "input": {"type": "SCAN", "table": "users"}}}
        """);
    assertEquals(
        List.of(List.of(2, "Bob", 30), List.of(3, "Charlie", 27)),
        Fixtures.valeurs(resultat));
  }

  @Test
  void build_projectSurUnion_utiliseSonResultat() throws IOException {
    Table resultat = executer("""
        {"type": "PROJECT", "columns": ["name"],
         "input": {"type": "UNION",
                   "left": {"type": "SELECT", "predicate": "age < 25",
                            "input": {"type": "SCAN", "table": "users"}},
                   "right": {"type": "SCAN", "table": "users"}}}
        """);
    assertEquals(
        List.of(List.of("Alice"), List.of("David"), List.of("Bob"), List.of("Charlie")),
        Fixtures.valeurs(resultat));
  }

  @Test
  void build_aggregate_calculeLeTotalParUtilisateur() throws IOException {
    Table resultat = executer(NOEUDS_COMPLETS.get("AGGREGATE"));
    assertEquals(List.of("user_id", "total"), resultat.getColumns());
    assertEquals(
        List.of(List.of(2, 300L), List.of(1, 150L), List.of(3, 50L)), Fixtures.valeurs(resultat));
  }

  @Test
  void build_aggregateFonctionEnMinuscules_estReconnue() throws IOException {
    Table resultat = executer("""
        {"type": "AGGREGATE", "groupBy": ["user_id"], "function": "count",
         "column": "amount", "as": "nb",
         "input": {"type": "SCAN", "table": "orders"}}
        """);
    assertEquals(List.of(List.of(2, 2), List.of(1, 1), List.of(3, 1)), Fixtures.valeurs(resultat));
  }

  @Test
  void build_aggregateGroupByVide_renvoieUneSeuleLigne() throws IOException {
    Table resultat = executer("""
        {"type": "AGGREGATE", "groupBy": [], "function": "SUM",
         "column": "amount", "as": "total",
         "input": {"type": "SCAN", "table": "orders"}}
        """);
    assertEquals(List.of("total"), resultat.getColumns());
    assertEquals(List.of(List.of(500L)), Fixtures.valeurs(resultat));
  }

  // L'ordre de groupBy dans le JSON donne l'ordre des colonnes de sortie
  @Test
  void build_aggregateDeuxColonnesGroupBy_gardeLeurOrdre() throws IOException {
    Table resultat = executer("""
        {"type": "AGGREGATE", "groupBy": ["user_id", "id"], "function": "MAX",
         "column": "amount", "as": "max",
         "input": {"type": "SCAN", "table": "orders"}}
        """);
    assertEquals(List.of("user_id", "id", "max"), resultat.getColumns());
    assertEquals(List.of(2, 10, 100), resultat.getRows().get(0).getValues());
  }

  @Test
  void build_aggregateSurJoin_calculeLeTotalParNom() throws IOException {
    Table resultat = executer("""
        {"type": "AGGREGATE", "groupBy": ["name"], "function": "SUM",
         "column": "amount", "as": "total",
         "input": {"type": "JOIN", "onLeft": "id", "onRight": "user_id",
                   "left": {"type": "SCAN", "table": "users"},
                   "right": {"type": "SCAN", "table": "orders"}}}
        """);
    assertEquals(
        List.of(List.of("Alice", 150L), List.of("Bob", 300L), List.of("Charlie", 50L)),
        Fixtures.valeurs(resultat));
  }

  @Test
  void build_aggregateFonctionInconnue_leveIllegalArgumentQuiLaNomme() {
    JsonObject noeud = json(NOEUDS_COMPLETS.get("AGGREGATE"));
    noeud.addProperty("function", "SUMM");
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> builder.build(noeud));
    assertEquals("Fonction d'agrégation inconnue : SUMM", e.getMessage());
  }

  // L'alias en double ne dépend pas des données : refusé dès la construction du plan
  @Test
  void build_aggregateAliasDansGroupBy_leveIllegalArgument() {
    JsonObject noeud = json(NOEUDS_COMPLETS.get("AGGREGATE"));
    noeud.addProperty("as", "user_id");
    assertThrows(IllegalArgumentException.class, () -> builder.build(noeud));
  }

  // Le plan se construit : les schémas ne sont connus qu'à l'exécution
  @Test
  void execute_unionDeSchemasDifferents_leveSchemaMismatch() {
    Operator plan = builder.build(json("""
        {"type": "UNION",
         "left": {"type": "SCAN", "table": "users"},
         "right": {"type": "SCAN", "table": "orders"}}
        """));
    assertThrows(SchemaMismatchException.class, plan::execute);
  }

  @Test
  void build_tableInconnue_leveIllegalArgument() {
    JsonObject plan = json("""
        {"type": "SCAN", "table": "clients"}
        """);
    assertThrows(IllegalArgumentException.class, () -> builder.build(plan));
  }

  @Test
  void build_typeInconnu_leveIllegalArgument() {
    JsonObject plan = json("""
        {"type": "INCONNU", "input": {"type": "SCAN", "table": "users"}}
        """);
    assertThrows(IllegalArgumentException.class, () -> builder.build(plan));
  }

  // Témoin : sans champ retiré, chaque nœud complet se construit
  @ParameterizedTest
  @ValueSource(strings = {
      "SCAN", "SELECT", "PROJECT", "JOIN", "UNION", "INTERSECTION", "DIFFERENCE", "AGGREGATE"
  })
  void build_noeudComplet_seConstruit(String type) {
    assertDoesNotThrow(() -> builder.build(json(NOEUDS_COMPLETS.get(type))));
  }

  @ParameterizedTest
  @CsvSource({
      "SCAN, type", "SCAN, table",
      "SELECT, input", "SELECT, predicate",
      "PROJECT, input", "PROJECT, columns",
      "JOIN, left", "JOIN, right", "JOIN, onLeft", "JOIN, onRight",
      "UNION, left", "UNION, right",
      "INTERSECTION, left", "INTERSECTION, right",
      "DIFFERENCE, left", "DIFFERENCE, right",
      "AGGREGATE, input", "AGGREGATE, groupBy", "AGGREGATE, function", "AGGREGATE, column",
      "AGGREGATE, as"
  })
  void build_champManquant_leveIllegalArgumentQuiLeNomme(String type, String champ) {
    JsonObject noeud = json(NOEUDS_COMPLETS.get(type));
    noeud.remove(champ);
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> builder.build(noeud));
    assertEquals("Champ manquant : " + champ, e.getMessage());
  }

  @Test
  void register_nouveauType_estConstruitParBuild() throws IOException {
    builder.register("IDENTITE", (node, b) -> b.build(node.getAsJsonObject("input")));
    Table resultat = executer("""
        {"type": "IDENTITE", "input": {"type": "SCAN", "table": "users"}}
        """);
    assertEquals(Fixtures.valeurs(Fixtures.users()), Fixtures.valeurs(resultat));
  }
}
