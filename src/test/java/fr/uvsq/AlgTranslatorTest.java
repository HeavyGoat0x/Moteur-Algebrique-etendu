package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests de la traduction des requêtes .alg en requêtes JSON.
 */
class AlgTranslatorTest {

  private static final Path DATA = Path.of("data");

  private static JsonObject traduire(String... lignes) {
    return AlgTranslator.translate(List.of(lignes));
  }

  // Plan traduit, sous un en-tête qui déclare users et orders
  private static JsonObject plan(String... relations) {
    List<String> lignes = new ArrayList<>(List.of("users: users.csv", "orders: orders.csv"));
    lignes.addAll(List.of(relations));
    return AlgTranslator.translate(lignes).getAsJsonObject("plan");
  }

  // Message de l'erreur levée en traduisant ces relations sous le même en-tête
  private static String erreur(String... relations) {
    return assertThrows(IllegalArgumentException.class, () -> plan(relations)).getMessage();
  }

  private static JsonElement json(String texte) {
    return JsonParser.parseString(texte);
  }

  @ParameterizedTest
  @ValueSource(strings = {"reqProjSel", "optJoin"})
  void translate_exempleDuReadme_donneLaRequeteDuDepot(String nom) throws Exception {
    JsonElement attendu = json(Files.readString(DATA.resolve(nom + ".json")));

    JsonObject traduite = AlgTranslator.translate(Files.readAllLines(DATA.resolve(nom + ".alg")));

    assertEquals(attendu, traduite);
  }

  @Test
  void translate_planTraduit_estExecutable() throws Exception {
    JsonObject requete = AlgTranslator.translate(Files.readAllLines(DATA.resolve("optJoin.alg")));
    Map<String, Table> tables = Map.of("users", Fixtures.users(), "orders", Fixtures.orders());

    Table resultat = new PlanBuilder(tables).build(requete.getAsJsonObject("plan")).execute();

    assertEquals(List.of("name", "amount"), resultat.getColumns());
    assertEquals(List.of(List.of("Alice", 150), List.of("Bob", 100), List.of("Bob", 200),
        List.of("Charlie", 50)), Fixtures.valeurs(resultat));
  }

  @Test
  void translate_enTete_remplitTablesAvecLesNomsEtLesFichiers() {
    JsonObject requete = traduire("users: users.csv", "orders:orders.csv", "R = PROJ users name");

    assertEquals(json("""
        {"users": "users.csv", "orders": "orders.csv"}
        """), requete.get("tables"));
  }

  @Test
  void translate_cheminAvecDeuxPoints_gardeLeCheminEntier() {
    JsonObject requete = traduire("users: C:/data/users.csv", "R = PROJ users name");

    assertEquals("C:/data/users.csv",
        requete.getAsJsonObject("tables").get("users").getAsString());
  }

  @Test
  void translate_select_construitLeNoeudSurUnScan() {
    assertEquals(json("""
        {"type": "SELECT", "predicate": "age < 25",
         "input": {"type": "SCAN", "table": "users"}}
        """), plan("R = SELECT users age < 25"));
  }

  @Test
  void translate_selectChaineAvecEspaces_gardeLePredicatEntier() {
    assertEquals("name = 'Jean  Paul'",
        plan("R = SELECT users name = 'Jean  Paul'").get("predicate").getAsString());
  }

  @Test
  void translate_predicatAvecDeuxPoints_estLuCommeUneRelation() {
    JsonObject requete = traduire("users: users.csv", "R = SELECT users name = 'a:b'");

    assertEquals("name = 'a:b'", requete.getAsJsonObject("plan").get("predicate").getAsString());
    assertEquals(Set.of("users"), requete.getAsJsonObject("tables").keySet());
  }

  @Test
  void translate_proj_donneUnProjectSansLaRelationDansLesColonnes() {
    assertEquals(json("""
        {"type": "PROJECT", "columns": ["id", "name"],
         "input": {"type": "SCAN", "table": "users"}}
        """), plan("R = PROJ users id name"));
  }

  @Test
  void translate_join_construitLesDeuxEntrees() {
    assertEquals(json("""
        {"type": "JOIN", "onLeft": "id", "onRight": "user_id",
         "left": {"type": "SCAN", "table": "users"},
         "right": {"type": "SCAN", "table": "orders"}}
        """), plan("R = JOIN users orders id user_id"));
  }

  @Test
  void translate_derniereRelation_devientLePlan() {
    assertEquals(json("""
        {"type": "PROJECT", "columns": ["amount"],
         "input": {"type": "SCAN", "table": "orders"}}
        """), plan("R1 = PROJ users name", "R2 = PROJ orders amount"));
  }

  @Test
  void translate_relationRedefinieSurElleMeme_reprendSaDefinitionPrecedente() {
    assertEquals(json("""
        {"type": "PROJECT", "columns": ["name"],
         "input": {"type": "SELECT", "predicate": "age < 25",
                   "input": {"type": "SCAN", "table": "users"}}}
        """), plan("R = SELECT users age < 25", "R = PROJ R name"));
  }

  @Test
  void translate_lignesVidesEtEspacesMultiples_sontIgnores() {
    JsonObject requete =
        traduire("", "  users:   users.csv  ", "   ", "\t", "R  =  PROJ   users   name  ");

    assertEquals(json("""
        {"tables": {"users": "users.csv"},
         "plan": {"type": "PROJECT", "columns": ["name"],
                  "input": {"type": "SCAN", "table": "users"}}}
        """), requete);
  }

  @Test
  void translate_operateurInconnu_leveIllegalArgumentQuiLeNomme() {
    assertEquals("Opérateur inconnu : PROJECT", erreur("R = PROJECT users name"));
  }

  @Test
  void translate_motCleEnMinuscules_estRefuse() {
    assertEquals("Opérateur inconnu : proj", erreur("R = proj users name"));
  }

  @Test
  void translate_relationInconnue_leveIllegalArgumentQuiLaNomme() {
    assertEquals("Relation inconnue : R9", erreur("R = PROJ R9 name"));
  }

  @Test
  void translate_relationDefiniePlusBas_estInconnue() {
    assertEquals("Relation inconnue : R1",
        erreur("R = PROJ R1 name", "R1 = SELECT users age < 25"));
  }

  @Test
  void translate_nomDeFichierAuLieuDuNomDeTable_estInconnu() {
    assertEquals("Relation inconnue : users.csv", erreur("R = PROJ users.csv name"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"users orders id", "users orders id user_id amount"})
  void translate_joinSansQuatreArguments_leveIllegalArgument(String arguments) {
    assertEquals("JOIN attend 4 arguments : " + arguments, erreur("R = JOIN " + arguments));
  }

  @Test
  void translate_selectSansPredicat_leveIllegalArgument() {
    assertEquals("SELECT attend une relation et un prédicat : users", erreur("R = SELECT users"));
  }

  @Test
  void translate_projSansColonne_leveIllegalArgument() {
    assertEquals("PROJ attend une relation et au moins une colonne : users",
        erreur("R = PROJ users"));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "n'importe quoi", "R = PROJ", "R =", "= PROJ users name", "users:", ": users.csv"
  })
  void translate_ligneIllisible_leveIllegalArgumentQuiLaCite(String ligne) {
    assertEquals("Ligne illisible : " + ligne, erreur(ligne));
  }

  @Test
  void translate_enTeteSansRelation_leveIllegalArgument() {
    assertEquals("Aucune relation définie", erreur());
  }

  @Test
  void translate_aucuneLigne_leveIllegalArgument() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> AlgTranslator.translate(List.of()));
    assertEquals("Aucune relation définie", e.getMessage());
  }
}
