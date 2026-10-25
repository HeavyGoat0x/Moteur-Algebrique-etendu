package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlanBuilderTest {

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
}
