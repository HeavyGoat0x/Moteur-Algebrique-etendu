package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PredicateParserTest {

  private Table users;
  private Row alice;
  private Row bob;

  @BeforeEach
  void setup() {
    alice = new Row(List.of(1, "Alice", 22));
    bob = new Row(List.of(2, "Bob", 30));
    users = new Table(List.of("id", "name", "age"));
    users.addRow(alice);
    users.addRow(bob);
  }

  @Test
  void parse_inferieurSatisfait_renvoieVrai() {
    assertTrue(PredicateParser.parse("age < 25").test(alice, users));
  }

  @Test
  void parse_inferieurNonSatisfait_renvoieFaux() {
    assertFalse(PredicateParser.parse("age < 25").test(bob, users));
  }

  @Test
  void parse_superieurSatisfait_renvoieVrai() {
    assertTrue(PredicateParser.parse("age > 25").test(bob, users));
  }

  @Test
  void parse_egalNumerique_renvoieVrai() {
    assertTrue(PredicateParser.parse("age = 30").test(bob, users));
  }

  @Test
  void parse_valeurDecimale_compareNumeriquement() {
    assertTrue(PredicateParser.parse("age > 21.5").test(alice, users));
  }

  @Test
  void parse_egalChaineQuotee_renvoieVrai() {
    assertTrue(PredicateParser.parse("name = 'Bob'").test(bob, users));
  }

  @Test
  void parse_egalChaineDifferente_renvoieFaux() {
    assertFalse(PredicateParser.parse("name = 'Bob'").test(alice, users));
  }

  @Test
  void parse_superieurSurChaine_renvoieFaux() {
    assertFalse(PredicateParser.parse("name > 'Alice'").test(bob, users));
  }

  @Test
  void parse_chaineAvecEspace_conserveLaValeur() {
    Row bobSmith = new Row(List.of(3, "Bob Smith", 40));
    assertTrue(PredicateParser.parse("name = 'Bob Smith'").test(bobSmith, users));
  }

  @Test
  void parse_espacesAutour_sontIgnores() {
    assertTrue(PredicateParser.parse("  age < 25  ").test(alice, users));
  }

  @Test
  void parse_deuxParties_leveIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> PredicateParser.parse("age <"));
  }

  @Test
  void parse_colonneInconnue_leveIllegalArgumentAuTest() {
    PredicateParser.RowPredicate p = PredicateParser.parse("email = 'x'");
    assertThrows(IllegalArgumentException.class, () -> p.test(alice, users));
  }

  @ParameterizedTest
  @CsvSource({
      "age >= 30, true",
      "age >= 31, false",
      "age <= 30, true",
      "age <= 29, false",
      "age != 30, false",
      "age != 22, true"
  })
  void parse_operateursEtendus_comparentNumeriquement(String predicat, boolean attendu) {
    assertEquals(attendu, PredicateParser.parse(predicat).test(bob, users));
  }

  @ParameterizedTest
  @CsvSource(quoteCharacter = '"', value = {
      "name != 'Bob', false",
      "name != 'Alice', true"
  })
  void parse_differentSurChaine_compareLesChaines(String predicat, boolean attendu) {
    assertEquals(attendu, PredicateParser.parse(predicat).test(bob, users));
  }
}
