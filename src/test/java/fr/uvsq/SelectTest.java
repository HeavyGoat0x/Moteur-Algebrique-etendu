package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SelectTest {

  private Table users;

  @BeforeEach
  void setUp() {
    users = Fixtures.users();
  }

  private Table selectionner(Operator source, String predicat) {
    return new Select(source, PredicateParser.parse(predicat)).execute();
  }

  @Test
  void execute_predicatNumerique_gardeLesLignesSatisfaitesDansOrdre() {
    Table resultat = selectionner(new Scan(users), "age < 25");
    assertEquals(List.of(List.of(1, "Alice", 22), List.of(4, "David", 19)),
        Fixtures.valeurs(resultat));
  }

  @Test
  void execute_predicatChaine_gardeLaLigneCorrespondante() {
    Table resultat = selectionner(new Scan(users), "name = 'Bob'");
    assertEquals(List.of(List.of(2, "Bob", 30)), Fixtures.valeurs(resultat));
  }

  @Test
  void execute_predicat_conserveLeSchema() {
    Table resultat = selectionner(new Scan(users), "age > 25");
    assertEquals(users.getColumns(), resultat.getColumns());
  }

  @Test
  void execute_aucuneCorrespondance_renvoieTableVideAvecSchema() {
    Table resultat = selectionner(new Scan(users), "age > 100");
    assertEquals(users.getColumns(), resultat.getColumns());
    assertTrue(resultat.getRows().isEmpty());
  }

  @Test
  void execute_toutCorrespond_renvoieToutesLesLignes() {
    Table resultat = selectionner(new Scan(users), "age > 0");
    assertEquals(Fixtures.valeurs(users), Fixtures.valeurs(resultat));
  }

  @Test
  void execute_selectionsEnChaine_combinentLesConditions() {
    Operator plusDeVingt = new Select(new Scan(users), PredicateParser.parse("age > 20"));
    Table resultat = selectionner(plusDeVingt, "age < 28");
    assertEquals(List.of(List.of(1, "Alice", 22), List.of(3, "Charlie", 27)),
        Fixtures.valeurs(resultat));
  }

  @Test
  void execute_tableSource_resteInchangee() {
    selectionner(new Scan(users), "age < 25");
    assertEquals(Fixtures.valeurs(Fixtures.users()), Fixtures.valeurs(users));
  }
}
