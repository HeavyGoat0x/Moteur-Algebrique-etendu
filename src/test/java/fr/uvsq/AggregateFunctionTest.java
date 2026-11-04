package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// assertEquals compare ici des objets : 300L et l'entier 300 ne sont pas égaux, le type est vérifié
class AggregateFunctionTest {

  @Test
  void apply_countTroisValeurs_renvoieTrois() {
    assertEquals(3, AggregateFunction.COUNT.apply(List.of(1, 2, 3)));
  }

  @Test
  void apply_countValeursNonNumeriques_lesCompteAussi() {
    assertEquals(2, AggregateFunction.COUNT.apply(List.of("Alice", "Bob")));
  }

  @Test
  void apply_countListeVide_renvoieZero() {
    assertEquals(0, AggregateFunction.COUNT.apply(List.of()));
  }

  @Test
  void apply_sumEntiers_renvoieUnLong() {
    assertEquals(300L, AggregateFunction.SUM.apply(List.of(100, 200)));
  }

  @Test
  void apply_sumDecimaux_renvoieUnDouble() {
    assertEquals(4.0, AggregateFunction.SUM.apply(List.of(2.5, 1.5)));
  }

  @Test
  void apply_sumEntierPuisDecimal_renvoieUnDouble() {
    assertEquals(102.5, AggregateFunction.SUM.apply(List.of(100, 2.5)));
  }

  @Test
  void apply_sumDecimalPuisEntier_renvoieUnDouble() {
    assertEquals(102.5, AggregateFunction.SUM.apply(List.of(2.5, 100)));
  }

  // Un total calculé en int deviendrait négatif, sans aucune erreur
  @Test
  void apply_sumDepasseCapaciteInt_renvoieTotalExact() {
    List<Object> valeurs = List.of(2_000_000_000, 2_000_000_000);
    assertEquals(4_000_000_000L, AggregateFunction.SUM.apply(valeurs));
  }

  // 2^53 + 1 n'existe pas en double : un total calculé en double serait faux d'une unité
  @Test
  void apply_sumTresGrandsEntiers_resteExact() {
    List<Object> valeurs = List.of(9_007_199_254_740_992L, 1L);
    assertEquals(9_007_199_254_740_993L, AggregateFunction.SUM.apply(valeurs));
  }

  // SUM produit des long : ces valeurs arrivent quand on agrège le résultat d'une agrégation
  @Test
  void apply_sumDeLongs_renvoieUnLong() {
    assertEquals(450L, AggregateFunction.SUM.apply(List.of(300L, 150L)));
  }

  @Test
  void apply_sumLongEtEntier_renvoieUnLong() {
    assertEquals(450L, AggregateFunction.SUM.apply(List.of(300L, 150)));
  }

  @Test
  void apply_sumValeursNegatives_lesAdditionne() {
    assertEquals(-2L, AggregateFunction.SUM.apply(List.of(-5, 3)));
  }

  @Test
  void apply_sumListeVide_renvoieZero() {
    assertEquals(0L, AggregateFunction.SUM.apply(List.of()));
  }

  @Test
  void apply_sumNombreEcritEnTexte_leveIllegalArgument() {
    List<Object> valeurs = List.of(100, "200");
    assertThrows(IllegalArgumentException.class, () -> AggregateFunction.SUM.apply(valeurs));
  }

  @Test
  void apply_avgEntiers_renvoieUnDouble() {
    assertEquals(150.0, AggregateFunction.AVG.apply(List.of(100, 200)));
  }

  // Une division entière donnerait 1 au lieu de 1.5
  @Test
  void apply_avgResultatNonEntier_gardeLesDecimales() {
    assertEquals(1.5, AggregateFunction.AVG.apply(List.of(1, 2)));
  }

  @Test
  void apply_avgEntierEtDecimal_calculeLaMoyenne() {
    assertEquals(51.25, AggregateFunction.AVG.apply(List.of(100, 2.5)));
  }

  @Test
  void apply_minEntiers_renvoieUnEntier() {
    assertEquals(100, AggregateFunction.MIN.apply(List.of(200, 100)));
  }

  @Test
  void apply_minDecimalPlusPetit_renvoieLeDecimal() {
    assertEquals(2.5, AggregateFunction.MIN.apply(List.of(100, 2.5)));
  }

  // La valeur d'origine est rendue : le CSV affiche 1 et non 1.0
  @Test
  void apply_minEntierPlusPetit_resteUnEntier() {
    assertEquals(1, AggregateFunction.MIN.apply(List.of(1, 2.5)));
  }

  @Test
  void apply_minDecimauxProches_compareLesDecimales() {
    assertEquals(2.1, AggregateFunction.MIN.apply(List.of(2.5, 2.1)));
  }

  @Test
  void apply_minDeLongs_renvoieUnLong() {
    assertEquals(150L, AggregateFunction.MIN.apply(List.of(300L, 150L)));
  }

  @Test
  void apply_minValeursNegatives_renvoieLaPlusPetite() {
    assertEquals(-5, AggregateFunction.MIN.apply(List.of(3, -5)));
  }

  @Test
  void apply_maxEntiers_renvoieUnEntier() {
    assertEquals(200, AggregateFunction.MAX.apply(List.of(100, 200)));
  }

  // La valeur d'origine est rendue : le CSV affiche 100 et non 100.0
  @Test
  void apply_maxEntierPlusGrand_resteUnEntier() {
    assertEquals(100, AggregateFunction.MAX.apply(List.of(100, 2.5)));
  }

  @Test
  void apply_maxDecimalPlusGrand_renvoieLeDecimal() {
    assertEquals(2.5, AggregateFunction.MAX.apply(List.of(1, 2.5)));
  }

  @Test
  void apply_maxDecimauxProches_compareLesDecimales() {
    assertEquals(2.5, AggregateFunction.MAX.apply(List.of(2.1, 2.5)));
  }

  @Test
  void apply_maxDeLongs_renvoieUnLong() {
    assertEquals(300L, AggregateFunction.MAX.apply(List.of(300L, 150L)));
  }

  @Test
  void apply_maxValeursNegatives_renvoieLaPlusGrande() {
    assertEquals(-5, AggregateFunction.MAX.apply(List.of(-9, -5)));
  }

  @ParameterizedTest
  @EnumSource(names = {"SUM", "AVG", "MIN", "MAX"})
  void apply_valeurNonNumerique_leveIllegalArgument(AggregateFunction fonction) {
    List<Object> valeurs = List.of(100, "Alice");
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> fonction.apply(valeurs));
    assertEquals("Valeur non numérique : Alice", e.getMessage());
  }
}
