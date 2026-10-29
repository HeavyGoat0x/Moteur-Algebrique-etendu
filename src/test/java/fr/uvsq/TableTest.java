package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TableTest {

  private Table vide;
  private Table users;
  private Row alice;
  private Row bob;

  @BeforeEach
  void setUp() {
    vide = new Table(List.of("id", "name", "age"));

    alice = new Row(List.of(1, "Alice", 22));
    bob = new Row(List.of(2, "Bob", 30));
    users = new Table(List.of("id", "name", "age"));
    users.addRow(alice);
    users.addRow(bob);
  }

  @Test
  void getColumns_tableCreee_renvoieLeSchema() {
    assertEquals(List.of("id", "name", "age"), users.getColumns());
  }

  @Test
  void getColumns_modification_leveUnsupportedOperation() {
    assertThrows(UnsupportedOperationException.class, () -> users.getColumns().add("email"));
  }

  @Test
  void constructeur_listeModifieeApres_schemaInchange() {
    List<String> colonnes = new ArrayList<>(List.of("id", "name"));
    Table table = new Table(colonnes);
    colonnes.add("age");
    assertEquals(List.of("id", "name"), table.getColumns());
  }

  @Test
  void constructeur_avecLignes_conserveLesLignesDansOrdre() {
    Table table = new Table(List.of("id", "name", "age"), List.of(alice, bob));
    assertEquals(List.of(alice, bob), table.getRows());
  }

  @Test
  void constructeur_lignesImmuables_addRowResteDisponible() {
    Table table = new Table(List.of("id", "name", "age"), List.of(alice));
    table.addRow(bob);
    assertEquals(List.of(alice, bob), table.getRows());
  }

  @Test
  void constructeur_listeDeLignesModifieeApres_tableInchangee() {
    List<Row> lignes = new ArrayList<>(List.of(alice));
    Table table = new Table(List.of("id", "name", "age"), lignes);
    lignes.add(bob);
    assertEquals(List.of(alice), table.getRows());
  }

  @Test
  void getColumnIndex_colonneExistante_renvoieSaPosition() {
    assertEquals(1, users.getColumnIndex("name"));
  }

  @Test
  void getColumnIndex_colonneInconnue_leveIllegalArgument() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> users.getColumnIndex("email"));
    assertTrue(e.getMessage().contains("email"));
  }

  @Test
  void getRows_tableVide_renvoieListeVide() {
    assertTrue(vide.getRows().isEmpty());
  }

  @Test
  void addRow_deuxLignes_conserveOrdre() {
    assertEquals(List.of(alice, bob), users.getRows());
  }

  @Test
  void getRows_modification_leveUnsupportedOperation() {
    assertThrows(UnsupportedOperationException.class, () -> users.getRows().add(alice));
  }
}
