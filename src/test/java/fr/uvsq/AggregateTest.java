package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

// Fixtures.valeurs distingue 1 et 1.0, 300 et 300L : les résultats vérifient aussi les types
class AggregateTest {

  private static final List<String> VENTES = List.of("ville", "rayon", "montant");

  private static Row ligne(Object... valeurs) {
    return new Row(List.of(valeurs));
  }

  private static Operator ventes() {
    return new Scan(new Table(VENTES, List.of(
        ligne("Paris", "A", 10),
        ligne("Paris", "B", 20),
        ligne("Lyon", "A", 5),
        ligne("Paris", "A", 30))));
  }

  private static Table parUserId(AggregateFunction fonction, String alias) {
    return new Aggregate(new Scan(Fixtures.orders()), List.of("user_id"), fonction, "amount", alias)
        .execute();
  }

  @Test
  void execute_sumParUserId_renvoieUnTotalParGroupe() {
    Table resultat = parUserId(AggregateFunction.SUM, "total");
    assertEquals(
        List.of(List.of(2, 300L), List.of(1, 150L), List.of(3, 50L)), Fixtures.valeurs(resultat));
  }

  @Test
  void execute_sumParUserId_schemaGroupByPuisAlias() {
    Table resultat = parUserId(AggregateFunction.SUM, "total");
    assertEquals(List.of("user_id", "total"), resultat.getColumns());
  }

  @Test
  void execute_countParUserId_compteLesLignesDeChaqueGroupe() {
    Table resultat = parUserId(AggregateFunction.COUNT, "nb");
    assertEquals(List.of(List.of(2, 2), List.of(1, 1), List.of(3, 1)), Fixtures.valeurs(resultat));
  }

  @Test
  void execute_maxParUserId_gardeLaPlusGrandeValeur() {
    Table resultat = parUserId(AggregateFunction.MAX, "max");
    assertEquals(
        List.of(List.of(2, 200), List.of(1, 150), List.of(3, 50)), Fixtures.valeurs(resultat));
  }

  // Les clés 3, 1, 3, 2 donnent les groupes 3, 1, 2 : ordre de première apparition
  @Test
  void execute_clesDansLeDesordre_gardeOrdreDePremiereApparition() {
    Table table = new Table(List.of("k", "v"), List.of(
        ligne(3, 1), ligne(1, 1), ligne(3, 1), ligne(2, 1)));
    Table resultat =
        new Aggregate(new Scan(table), List.of("k"), AggregateFunction.COUNT, "v", "nb").execute();
    assertEquals(List.of(List.of(3, 2), List.of(1, 1), List.of(2, 1)), Fixtures.valeurs(resultat));
  }

  @Test
  void execute_deuxColonnesGroupBy_formeUnGroupeParCombinaison() {
    Table resultat = new Aggregate(
        ventes(), List.of("ville", "rayon"), AggregateFunction.SUM, "montant", "total").execute();
    assertEquals(
        List.of(List.of("Paris", "A", 40L), List.of("Paris", "B", 20L), List.of("Lyon", "A", 5L)),
        Fixtures.valeurs(resultat));
  }

  // La clé suit l'ordre de groupBy, pas celui des colonnes de la table
  @Test
  void execute_groupByDansUnAutreOrdre_suitOrdreDeGroupBy() {
    Table resultat = new Aggregate(
        ventes(), List.of("rayon", "ville"), AggregateFunction.SUM, "montant", "total").execute();
    assertEquals(List.of("rayon", "ville", "total"), resultat.getColumns());
    assertEquals(List.of("A", "Paris", 40L), Fixtures.valeurs(resultat).get(0));
  }

  // La clé est une Row : 1 et 1.0 sont égaux, comme en SQL, et la première valeur est gardée
  @Test
  void execute_entierEtDecimalEgaux_formentUnSeulGroupe() {
    Table table = new Table(List.of("k", "v"), List.of(ligne(1, 10), ligne(1.0, 20)));
    Table resultat =
        new Aggregate(new Scan(table), List.of("k"), AggregateFunction.SUM, "v", "total").execute();
    assertEquals(List.of(List.of(1, 30L)), Fixtures.valeurs(resultat));
  }

  @Test
  void execute_groupByVide_agregeToutesLesLignes() {
    Table resultat = new Aggregate(
        new Scan(Fixtures.orders()), List.of(), AggregateFunction.SUM, "amount", "total").execute();
    assertEquals(List.of("total"), resultat.getColumns());
    assertEquals(List.of(List.of(500L)), Fixtures.valeurs(resultat));
  }

  // Limite assumée : SQL renverrait une ligne (COUNT = 0) pour une agrégation sans GROUP BY
  @Test
  void execute_tableVide_renvoieTableVideAvecSchema() {
    Operator vide = new Scan(new Table(List.of("id", "user_id", "amount")));
    Table resultat =
        new Aggregate(vide, List.of(), AggregateFunction.COUNT, "amount", "nb").execute();
    assertEquals(List.of("nb"), resultat.getColumns());
    assertTrue(resultat.getRows().isEmpty());
  }

  @Test
  void execute_surUnSelect_agregeLesLignesFiltrees() {
    Operator grosses =
        new Select(new Scan(Fixtures.orders()), PredicateParser.parse("amount > 100"));
    Table resultat = new Aggregate(
        grosses, List.of("user_id"), AggregateFunction.SUM, "amount", "total").execute();
    assertEquals(List.of(List.of(1, 150L), List.of(2, 200L)), Fixtures.valeurs(resultat));
  }

  @Test
  void execute_aliasIdentiqueColonneAgregee_estAccepte() {
    Table resultat = parUserId(AggregateFunction.SUM, "amount");
    assertEquals(List.of("user_id", "amount"), resultat.getColumns());
  }

  // Comme SQL, une requête invalide est refusée avant de lire les données
  @Test
  void execute_colonneAgregeeInconnue_leveIllegalArgumentMemeSurTableVide() {
    Operator vide = new Scan(new Table(List.of("id", "user_id", "amount")));
    Operator agregat =
        new Aggregate(vide, List.of("user_id"), AggregateFunction.SUM, "prix", "total");
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, agregat::execute);
    assertTrue(e.getMessage().contains("prix"));
  }

  @Test
  void execute_colonneGroupByInconnue_leveIllegalArgumentMemeSurTableVide() {
    Operator vide = new Scan(new Table(List.of("id", "user_id", "amount")));
    Operator agregat =
        new Aggregate(vide, List.of("client"), AggregateFunction.SUM, "amount", "total");
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, agregat::execute);
    assertTrue(e.getMessage().contains("client"));
  }

  @Test
  void execute_valeurNonNumerique_leveIllegalArgument() {
    Operator agregat = new Aggregate(
        new Scan(Fixtures.users()), List.of(), AggregateFunction.SUM, "name", "total");
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, agregat::execute);
    assertEquals("Valeur non numérique : Alice", e.getMessage());
  }

  // Sinon la sortie aurait deux colonnes user_id
  @Test
  void constructeur_aliasDejaDansGroupBy_leveIllegalArgument() {
    Operator source = new Scan(Fixtures.orders());
    List<String> groupBy = List.of("id", "user_id");
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> new Aggregate(source, groupBy, AggregateFunction.SUM, "amount", "user_id"));
    assertEquals("Alias déjà présent dans groupBy : user_id", e.getMessage());
  }

  @Test
  void execute_tableSource_resteInchangee() {
    Table orders = Fixtures.orders();
    new Aggregate(new Scan(orders), List.of("user_id"), AggregateFunction.SUM, "amount", "total")
        .execute();
    assertEquals(Fixtures.valeurs(Fixtures.orders()), Fixtures.valeurs(orders));
    assertEquals(List.of("id", "user_id", "amount"), orders.getColumns());
  }
}
