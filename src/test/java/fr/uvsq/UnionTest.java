package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class UnionTest {

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
  void execute_tablesDisjointes_concateneGauchePuisDroite() {
    Table resultat = new Union(source(ALICE), source(BOB)).execute();
    assertEquals(List.of(ALICE, BOB), resultat.getRows());
  }

  @Test
  void execute_memeSchema_gardeLeSchema() {
    Table resultat = new Union(source(ALICE), source(BOB)).execute();
    assertEquals(SCHEMA, resultat.getColumns());
  }

  @Test
  void execute_ligneCommune_neLaGardeQuUneFois() {
    Table resultat = new Union(source(ALICE, BOB), source(BOB, CHARLIE)).execute();
    assertEquals(List.of(ALICE, BOB, CHARLIE), resultat.getRows());
  }

  @Test
  void execute_doublonDansUneTable_estElimine() {
    Table resultat = new Union(source(ALICE, ALICE), source(BOB)).execute();
    assertEquals(List.of(ALICE, BOB), resultat.getRows());
  }

  // Fixtures.valeurs distingue 1 et 1.0, contrairement à Row.equals
  @Test
  void execute_entierEtDecimalEgaux_gardeLaPremiereOccurrence() {
    Table resultat = new Union(source(ligne(1, "Alice")), source(ligne(1.0, "Alice"))).execute();
    assertEquals(List.of(List.of(1, "Alice")), Fixtures.valeurs(resultat));
  }

  @Test
  void execute_tableDroiteVide_renvoieLaTableGauche() {
    Table resultat = new Union(source(ALICE, BOB), source()).execute();
    assertEquals(List.of(ALICE, BOB), resultat.getRows());
  }

  @Test
  void execute_tablesInversees_changeSeulementOrdre() {
    Table resultat = new Union(source(BOB, CHARLIE), source(ALICE, BOB)).execute();
    assertEquals(List.of(BOB, CHARLIE, ALICE), resultat.getRows());
  }

  @Test
  void execute_colonnesDifferentes_leveSchemaMismatch() {
    Operator union = new Union(sansLigne("a", "b"), sansLigne("a", "c"));
    SchemaMismatchException e = assertThrows(SchemaMismatchException.class, union::execute);
    assertEquals("Schémas incompatibles : [a, b] et [a, c]", e.getMessage());
  }

  // Sans ce refus, la même personne apparaîtrait deux fois : Row.equals compare par position
  @Test
  void execute_ordreDeColonnesDifferent_leveSchemaMismatch() {
    Operator droite = new Scan(new Table(List.of("name", "id"), List.of(ligne("Alice", 1))));
    Operator union = new Union(source(ALICE), droite);
    assertThrows(SchemaMismatchException.class, union::execute);
  }

  @Test
  void execute_nombreDeColonnesDifferent_leveSchemaMismatch() {
    Operator union = new Union(sansLigne("id", "name"), sansLigne("id"));
    assertThrows(SchemaMismatchException.class, union::execute);
  }

  @Test
  void execute_tablesSources_restentInchangees() {
    Table gauche = new Table(SCHEMA, List.of(ALICE, ALICE));
    Table droite = new Table(SCHEMA, List.of(ALICE, BOB));

    new Union(new Scan(gauche), new Scan(droite)).execute();

    assertEquals(List.of(ALICE, ALICE), gauche.getRows());
    assertEquals(List.of(ALICE, BOB), droite.getRows());
  }

  @Test
  void execute_sousPlansSelect_combineLeursResultats() {
    Operator jeunes = new Select(new Scan(Fixtures.users()), PredicateParser.parse("age < 25"));
    Operator aines = new Select(new Scan(Fixtures.users()), PredicateParser.parse("age > 25"));

    Table resultat = new Union(jeunes, aines).execute();

    assertEquals(
        List.of(
            List.of(1, "Alice", 22),
            List.of(4, "David", 19),
            List.of(2, "Bob", 30),
            List.of(3, "Charlie", 27)),
        Fixtures.valeurs(resultat));
  }

  @Test
  void execute_deuxTablesVides_renvoieTableVideAvecSchema() {
    Table resultat = new Union(source(), source()).execute();
    assertEquals(SCHEMA, resultat.getColumns());
    assertTrue(resultat.getRows().isEmpty());
  }
}
