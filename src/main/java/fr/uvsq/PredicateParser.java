package fr.uvsq;

/**
 * Analyse les prédicats à trois parties "colonne opérateur valeur".
 *
 * <p>Exemples : "age > 30" ou "nom = 'yeh'". N'utilise pas java.util.function.Predicate.
 */
public class PredicateParser {

  /**
   * Interface fonctionnelle personnalisée : condition évaluée sur une ligne de table.
   */
  @FunctionalInterface
  public interface RowPredicate {

    /**
     * Évalue la condition sur une ligne.
     *
     * @param row ligne à tester
     * @param table table de la ligne, qui fournit le schéma
     * @return vrai si la ligne satisfait la condition
     */
    boolean test(Row row, Table table);
  }

  /**
   * Analyse un prédicat de la forme "colonne opérateur valeur".
   *
   * @param expr prédicat, les trois parties séparées par un espace
   * @return la condition correspondante
   * @throws IllegalArgumentException si le prédicat n'a pas trois parties
   */
  public static RowPredicate parse(String expr) {
    String[] parts = expr.trim().split(" ", 3);
    if (parts.length != 3) {
      throw new IllegalArgumentException("Prédicat invalide, doit avoir 3 parties : " + expr);
    }

    String col = parts[0];
    String op = parts[1];
    String valStr = parts[2];

    // enlever les quotes pour les chaînes
    if (valStr.startsWith("'") && valStr.endsWith("'")) {
      valStr = valStr.substring(1, valStr.length() - 1);
    }

    final String value = valStr;
    final Double number = toNumber(value); // analysée une seule fois, pas à chaque ligne

    return (row, table) -> {
      Object cell = row.get(table.getColumnIndex(col));
      if (cell == null) {
        return false;
      }

      // Comparaison numérique si possible, sinon comparaison de chaînes
      Double cellNumber = toNumber(cell);
      if (cellNumber != null && number != null) {
        return compareNumbers(op, cellNumber, number);
      }
      return compareStrings(op, cell.toString(), value);
    };
  }

  // Valeur numérique d'une cellule ou d'un texte, ou null si ce n'est pas un nombre
  private static Double toNumber(Object o) {
    if (o instanceof Number n) {
      return n.doubleValue();
    }
    try {
      return Double.parseDouble(o.toString());
    } catch (NumberFormatException expected) {
      return null;
    }
  }

  private static boolean compareNumbers(String op, Double left, Double right) {
    return switch (op) {
      case ">" -> left > right;
      case "<" -> left < right;
      case ">=" -> left >= right;
      case "<=" -> left <= right;
      case "=", "==" -> left.equals(right);
      case "!=", "<>" -> !left.equals(right);
      default -> false;
    };
  }

  private static boolean compareStrings(String op, String left, String right) {
    return switch (op) {
      case "=", "==" -> left.equals(right);
      case "!=", "<>" -> !left.equals(right);
      default -> false; // >, < non valides pour chaînes
    };
  }
}
