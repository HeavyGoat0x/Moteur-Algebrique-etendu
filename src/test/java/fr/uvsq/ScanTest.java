package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScanTest {

  @Test
  void execute_tableChargee_renvoieSonSchema() {
    Table resultat = new Scan(Fixtures.users()).execute();
    assertEquals(List.of("id", "name", "age"), resultat.getColumns());
  }

  @Test
  void execute_tableChargee_renvoieToutesLesLignesDansOrdre() {
    Table users = Fixtures.users();
    Table resultat = new Scan(users).execute();
    assertEquals(Fixtures.valeurs(users), Fixtures.valeurs(resultat));
  }

  @Test
  void execute_tableVide_renvoieTableVide() {
    Table resultat = new Scan(new Table(List.of("id"))).execute();
    assertEquals(List.of("id"), resultat.getColumns());
    assertTrue(resultat.getRows().isEmpty());
  }
}
