package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JoinTest {

  private Table users;

  @BeforeEach
  void setUp() {
    users = Fixtures.users();
  }

  // Table de droite sans colonne homonyme de users
  @SafeVarargs
  private static Table achats(List<Object>... lignes) {
    Table achats = new Table(List.of("user_id", "amount"));
    for (List<Object> ligne : lignes) {
      achats.addRow(new Row(ligne));
    }
    return achats;
  }

  private Table joindre(Table gauche, Table droite, String cleGauche, String cleDroite) {
    return new Join(new Scan(gauche), new Scan(droite), cleGauche, cleDroite).execute();
  }

  @Test
  void execute_clesDeNomsDifferents_concateneLesSchemas() {
    Table resultat = joindre(users, achats(List.of(1, 150)), "id", "user_id");
    assertEquals(List.of("id", "name", "age", "user_id", "amount"), resultat.getColumns());
  }

  @Test
  void execute_correspondancesUniques_associeLesLignes() {
    Table resultat = joindre(users, achats(List.of(1, 150), List.of(3, 50)), "id", "user_id");
    assertEquals(List.of(List.of(1, "Alice", 22, 1, 150), List.of(3, "Charlie", 27, 3, 50)),
        Fixtures.valeurs(resultat));
  }

  @Test
  void execute_correspondancesMultiples_produitUneLigneParCouple() {
    Table resultat = joindre(users, achats(List.of(2, 100), List.of(2, 200)), "id", "user_id");
    assertEquals(List.of(List.of(2, "Bob", 30, 2, 100), List.of(2, "Bob", 30, 2, 200)),
        Fixtures.valeurs(resultat));
  }

  @Test
  void execute_aucuneCorrespondance_renvoieTableVideAvecSchema() {
    Table resultat = joindre(users, achats(List.of(9, 10)), "id", "user_id");
    assertEquals(List.of("id", "name", "age", "user_id", "amount"), resultat.getColumns());
    assertTrue(resultat.getRows().isEmpty());
  }

  @Test
  void execute_tableDroiteVide_renvoieTableVide() {
    Table resultat = joindre(users, achats(), "id", "user_id");
    assertTrue(resultat.getRows().isEmpty());
  }

  @Test
  void execute_memeNomDeCle_neGardeQuUneColonne() {
    Table notes = new Table(List.of("id", "note"));
    notes.addRow(new Row(List.of(4, 18)));
    Table resultat = joindre(users, notes, "id", "id");
    assertEquals(List.of("id", "name", "age", "note"), resultat.getColumns());
    assertEquals(List.of(List.of(4, "David", 19, 18)), Fixtures.valeurs(resultat));
  }

  // Comportement conservé (statut Q1) : la colonne de droite homonyme est retirée
  @Test
  void execute_colonneHomonyme_gardeCelleDeGauche() {
    Table resultat = joindre(users, Fixtures.orders(), "id", "user_id");
    assertEquals(List.of("id", "name", "age", "user_id", "amount"), resultat.getColumns());
    assertEquals(List.of(1, "Alice", 22, 1, 150), resultat.getRows().get(0).getValues());
  }

  @Test
  void execute_joinPuisProject_reproduitPlan1Attendu() {
    Operator jointure =
        new Join(new Scan(users), new Scan(Fixtures.orders()), "id", "user_id");
    Table resultat = new Project(jointure, List.of("amount", "name")).execute();
    assertEquals(List.of(
        List.of(150, "Alice"), List.of(100, "Bob"), List.of(200, "Bob"), List.of(50, "Charlie")),
        Fixtures.valeurs(resultat));
  }

  @Test
  void execute_tablesSources_restentInchangees() {
    Table orders = Fixtures.orders();
    joindre(users, orders, "id", "user_id");
    assertEquals(Fixtures.valeurs(Fixtures.users()), Fixtures.valeurs(users));
    assertEquals(Fixtures.valeurs(Fixtures.orders()), Fixtures.valeurs(orders));
  }
}
