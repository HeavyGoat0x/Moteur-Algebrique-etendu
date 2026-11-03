package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DifferenceTest {

  private static final List<String> SCHEMA = List.of("id", "name");
  private static final Row ALICE = ligne(1, "Alice");
  private static final Row BOB = ligne(2, "Bob");
  private static final Row CHARLIE = ligne(3, "Charlie");

  private static Row ligne(Object... valeurs) {
    return new Row(List.of(valeurs));
  }

  private static Operator source(Row... lignes) {
    return new Scan(new Table(SCHEMA, List.of(lignes)));
  }

  private static Operator sansLigne(String... colonnes) {
    return new Scan(new Table(List.of(colonnes)));
  }

  @Test
  void execute_ligneCommune_laRetire() {
    Table resultat = new Difference(source(ALICE, BOB), source(BOB, CHARLIE)).execute();
    assertEquals(List.of(ALICE), resultat.getRows());
  }

  @Test
  void execute_tablesInversees_changeLeResultat() {
    Table resultat = new Difference(source(BOB, CHARLIE), source(ALICE, BOB)).execute();
    assertEquals(List.of(CHARLIE), resultat.getRows());
  }

  @Test
  void execute_memeSchema_gardeLeSchema() {
    Table resultat = new Difference(source(ALICE), source(BOB)).execute();
    assertEquals(SCHEMA, resultat.getColumns());
  }

  @Test
  void execute_tablesDisjointes_renvoieLaTableGauche() {
    Table resultat = new Difference(source(ALICE, BOB), source(CHARLIE)).execute();
    assertEquals(List.of(ALICE, BOB), resultat.getRows());
  }

  @Test
  void execute_tablesIdentiques_renvoieTableVideAvecSchema() {
    Table resultat = new Difference(source(ALICE, BOB), source(BOB, ALICE)).execute();
    assertEquals(SCHEMA, resultat.getColumns());
    assertTrue(resultat.getRows().isEmpty());
  }

  @Test
  void execute_doublonGauche_estElimine() {
    Table resultat = new Difference(source(ALICE, ALICE, BOB), source(BOB)).execute();
    assertEquals(List.of(ALICE), resultat.getRows());
  }

  @Test
  void execute_ordreDifferent_suitLaTableGauche() {
    Table resultat = new Difference(source(CHARLIE, ALICE, BOB), source(ALICE)).execute();
    assertEquals(List.of(CHARLIE, BOB), resultat.getRows());
  }

  @Test
  void execute_entierEtDecimalEgaux_retireLaLigne() {
    Table resultat =
        new Difference(source(ligne(1, "Alice")), source(ligne(1.0, "Alice"))).execute();
    assertTrue(resultat.getRows().isEmpty());
  }

  @Test
  void execute_tableDroiteVide_renvoieLaTableGauche() {
    Table resultat = new Difference(source(ALICE, BOB), source()).execute();
    assertEquals(List.of(ALICE, BOB), resultat.getRows());
  }

  @Test
  void execute_tableGaucheVide_renvoieTableVide() {
    Table resultat = new Difference(source(), source(ALICE)).execute();
    assertTrue(resultat.getRows().isEmpty());
  }

  @Test
  void execute_colonnesDifferentes_leveSchemaMismatch() {
    Operator difference = new Difference(sansLigne("a", "b"), sansLigne("a", "c"));
    SchemaMismatchException e = assertThrows(SchemaMismatchException.class, difference::execute);
    assertEquals("Schémas incompatibles : [a, b] et [a, c]", e.getMessage());
  }

  @Test
  void execute_ordreDeColonnesDifferent_leveSchemaMismatch() {
    Operator difference = new Difference(sansLigne("a", "b"), sansLigne("b", "a"));
    assertThrows(SchemaMismatchException.class, difference::execute);
  }

  @Test
  void execute_tablesSources_restentInchangees() {
    Table gauche = new Table(SCHEMA, List.of(ALICE, ALICE));
    Table droite = new Table(SCHEMA, List.of(ALICE, BOB));

    new Difference(new Scan(gauche), new Scan(droite)).execute();

    assertEquals(List.of(ALICE, ALICE), gauche.getRows());
    assertEquals(List.of(ALICE, BOB), droite.getRows());
  }

  @Test
  void execute_sousPlanSelect_retireSesLignes() {
    Operator jeunes = new Select(new Scan(Fixtures.users()), PredicateParser.parse("age < 25"));

    Table resultat = new Difference(new Scan(Fixtures.users()), jeunes).execute();

    assertEquals(
        List.of(List.of(2, "Bob", 30), List.of(3, "Charlie", 27)),
        Fixtures.valeurs(resultat));
  }
}
