package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

  @Test
  void equals_memeInstance_renvoieVrai() {
    assertTrue(row.equals(row));
  }

  @Test
  void equals_memesValeurs_renvoieVrai() {
    Row copie = new Row(List.of(1, "Alice", 22.5));
    assertEquals(row, copie);
    assertEquals(row.hashCode(), copie.hashCode());
  }

  @Test
  void equals_valeurDifferente_renvoieFaux() {
    assertNotEquals(row, new Row(List.of(1, "Bob", 22.5)));
  }

  @Test
  void equals_ordreDifferent_renvoieFaux() {
    assertNotEquals(new Row(List.of(1, "Bob")), new Row(List.of("Bob", 1)));
  }

  @Test
  void equals_largeursDifferentes_renvoieFaux() {
    assertNotEquals(new Row(List.of(1)), new Row(List.of(1, "Bob")));
  }

  // Comme en SQL : 1 = 1.0
  @Test
  void equals_entierEtDecimalDeMemeValeur_renvoieVrai() {
    Row entier = new Row(List.of(1, "Alice"));
    Row decimal = new Row(List.of(1.0, "Alice"));
    assertEquals(entier, decimal);
    assertEquals(entier.hashCode(), decimal.hashCode());
  }

  // Comme en SQL : -0.0 = 0
  @Test
  void equals_zeroNegatifEtZero_renvoieVrai() {
    Row negatif = new Row(List.of(-0.0));
    Row zero = new Row(List.of(0));
    assertEquals(negatif, zero);
    assertEquals(negatif.hashCode(), zero.hashCode());
  }

  @Test
  void equals_nombreEtChaine_renvoieFaux() {
    assertNotEquals(new Row(List.of(1)), new Row(List.of("1")));
  }

  @Test
  void equals_nullOuAutreType_renvoieFaux() {
    assertFalse(row.equals(null));
    assertFalse(row.equals(List.of(1, "Alice", 22.5)));
  }

  // Ce que fera UNION : un LinkedHashSet élimine les doublons et garde l'ordre d'arrivée
  @Test
  void equals_doublonsDansUnLinkedHashSet_gardeLaPremiereOccurrence() {
    Set<Row> lignes = new LinkedHashSet<>();
    lignes.add(new Row(List.of(1, "Alice")));
    lignes.add(new Row(List.of(2, "Bob")));
    lignes.add(new Row(List.of(1.0, "Alice")));
    assertEquals(2, lignes.size());
    assertEquals(1, lignes.iterator().next().get(0)); // l'Integer 1, pas le Double 1.0
  }

  // La normalisation sert seulement à comparer : la sortie CSV doit garder "1", pas "1.0"
  @Test
  void equals_entierEtDecimal_laisseLesValeursIntactes() {
    Row entier = new Row(List.of(1));
    assertEquals(entier, new Row(List.of(1.0)));
    assertEquals(List.of(1), entier.getValues());
    assertEquals("[1]", entier.toString());
  }

  @Test
  void normalize_nombre_renvoieUnDouble() {
    assertEquals(1.0, Row.normalize(1));
  }

  @Test
  void normalize_chaine_renvoieLaMemeChaine() {
    assertEquals("Alice", Row.normalize("Alice"));
  }
}
