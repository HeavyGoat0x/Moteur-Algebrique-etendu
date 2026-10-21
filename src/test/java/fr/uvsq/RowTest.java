package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RowTest {

  private List<Object> valeurs;
  private Row row;

  @BeforeEach
  void setUp() {
    valeurs = new ArrayList<>(List.of(1, "Alice", 22.5));
    row = new Row(valeurs);
  }

  @Test
  void get_indexValide_renvoieLaValeur() {
    assertEquals(1, row.get(0));
  }

  @Test
  void get_indexHorsLimites_leveIndexOutOfBounds() {
    assertThrows(IndexOutOfBoundsException.class, () -> row.get(3));
  }

  @Test
  void getInt_colonneEntiere_renvoieEntier() {
    assertEquals(1, row.getInt(0));
  }

  @Test
  void getInt_colonneTexte_leveClassCastException() {
    assertThrows(ClassCastException.class, () -> row.getInt(1));
  }

  @Test
  void getString_colonneTexte_renvoieLaChaine() {
    assertEquals("Alice", row.getString(1));
  }

  @Test
  void getFloat_colonneDouble_convertitEnFloat() {
    assertEquals(22.5f, row.getFloat(2), 0.0001f);
  }

  @Test
  void getFloat_colonneFloat_renvoieLeFloat() {
    Row ligne = new Row(List.of(1.5f));
    assertEquals(1.5f, ligne.getFloat(0), 0.0001f);
  }

  @Test
  void getFloat_colonneEntiere_leveClassCastException() {
    assertThrows(ClassCastException.class, () -> row.getFloat(0));
  }

  @Test
  void getValues_ligneCreee_renvoieValeursDansOrdre() {
    assertEquals(List.of(1, "Alice", 22.5), row.getValues());
  }

  @Test
  void getValues_modification_leveUnsupportedOperation() {
    assertThrows(UnsupportedOperationException.class, () -> row.getValues().add(4));
  }

  @Test
  void constructeur_listeModifieeApres_ligneInchangee() {
    valeurs.set(1, "Bob");
    assertEquals("Alice", row.get(1));
  }

  @Test
  void constructeur_valeurNulle_leveNullPointer() {
    List<Object> avecNull = Arrays.asList(1, null);
    assertThrows(NullPointerException.class, () -> new Row(avecNull));
  }

  @Test
  void toString_ligneCreee_afficheLesValeurs() {
    assertEquals("[1, Alice, 22.5]", row.toString());
  }
}
