package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class IntersectionTest {

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
  void execute_ligneCommune_gardeSeulementElle() {
    Table resultat = new Intersection(source(ALICE, BOB), source(BOB, CHARLIE)).execute();
    assertEquals(List.of(BOB), resultat.getRows());
  }

  @Test
  void execute_memeSchema_gardeLeSchema() {
    Table resultat = new Intersection(source(ALICE), source(ALICE)).execute();
    assertEquals(SCHEMA, resultat.getColumns());
  }

  @Test
  void execute_tablesDisjointes_renvoieTableVideAvecSchema() {
    Table resultat = new Intersection(source(ALICE), source(BOB)).execute();
    assertEquals(SCHEMA, resultat.getColumns());
    assertTrue(resultat.getRows().isEmpty());
  }

  @Test
  void execute_doublonGauche_estElimine() {
    Table resultat = new Intersection(source(BOB, BOB), source(BOB)).execute();
    assertEquals(List.of(BOB), resultat.getRows());
  }

  @Test
  void execute_doublonDroite_gardeUneSeuleLigne() {
    Table resultat = new Intersection(source(BOB), source(BOB, BOB)).execute();
    assertEquals(List.of(BOB), resultat.getRows());
  }

  @Test
  void execute_ordreDifferent_suitLaTableGauche() {
    Table resultat =
        new Intersection(source(CHARLIE, BOB, ALICE), source(ALICE, BOB, CHARLIE)).execute();
    assertEquals(List.of(CHARLIE, BOB, ALICE), resultat.getRows());
  }

  // Fixtures.valeurs distingue 1 et 1.0, contrairement à Row.equals
  @Test
  void execute_entierEtDecimalEgaux_gardeLaValeurDeGauche() {
    Table resultat =
        new Intersection(source(ligne(1, "Alice")), source(ligne(1.0, "Alice"))).execute();
    assertEquals(List.of(List.of(1, "Alice")), Fixtures.valeurs(resultat));
  }

  @Test
  void execute_tableDroiteVide_renvoieTableVide() {
    Table resultat = new Intersection(source(ALICE), source()).execute();
    assertTrue(resultat.getRows().isEmpty());
  }

  @Test
  void execute_tableGaucheVide_renvoieTableVide() {
    Table resultat = new Intersection(source(), source(ALICE)).execute();
    assertTrue(resultat.getRows().isEmpty());
  }

  @Test
  void execute_colonnesDifferentes_leveSchemaMismatch() {
    Operator intersection = new Intersection(sansLigne("a", "b"), sansLigne("a", "c"));
    SchemaMismatchException e =
        assertThrows(SchemaMismatchException.class, intersection::execute);
    assertEquals("Schémas incompatibles : [a, b] et [a, c]", e.getMessage());
  }

  @Test
  void execute_ordreDeColonnesDifferent_leveSchemaMismatch() {
    Operator intersection = new Intersection(sansLigne("a", "b"), sansLigne("b", "a"));
    assertThrows(SchemaMismatchException.class, intersection::execute);
  }

  @Test
  void execute_tablesSources_restentInchangees() {
    Table gauche = new Table(SCHEMA, List.of(ALICE, ALICE));
    Table droite = new Table(SCHEMA, List.of(ALICE, BOB));

    new Intersection(new Scan(gauche), new Scan(droite)).execute();

    assertEquals(List.of(ALICE, ALICE), gauche.getRows());
    assertEquals(List.of(ALICE, BOB), droite.getRows());
  }

  @Test
  void execute_sousPlansSelect_gardeLesLignesDesDeuxResultats() {
    Operator moinsDeTrente =
        new Select(new Scan(Fixtures.users()), PredicateParser.parse("age < 30"));
    Operator plusDeVingt =
        new Select(new Scan(Fixtures.users()), PredicateParser.parse("age > 20"));

    Table resultat = new Intersection(moinsDeTrente, plusDeVingt).execute();

    assertEquals(
        List.of(List.of(1, "Alice", 22), List.of(3, "Charlie", 27)),
        Fixtures.valeurs(resultat));
  }
}
