package fr.uvsq;

/**
 * PredicateParser pour prédicats à trois parties :
 * "colonne opérateur valeur", ex : "age > 30" ou "nom = 'yeh'"
 * 
 * N'utilise pas java.util.function.Predicate
 */
public class PredicateParser {

    // Interface fonctionnelle personnalisée
    public interface RowPredicate {
        boolean test(Row row, Table table);
    }

    /**
     * Parse un prédicat de type "colonne opérateur valeur"
     */
    public static RowPredicate parse(String expr) {
        String[] parts = expr.trim().split(" ", 3);
        if (parts.length != 3)
            throw new IllegalArgumentException("Prédicat invalide, doit avoir 3 parties : " + expr);

        String col = parts[0];
        String op  = parts[1];
        String valStr = parts[2];

        // enlever les quotes pour les chaînes
        if (valStr.startsWith("'") && valStr.endsWith("'")) {
            valStr = valStr.substring(1, valStr.length() - 1);
        }

        final String finalCol = col;
        
        final String finalOp  = op;
        final String finalVal = valStr;

        // Retourne une instance de RowPredicate
        return new RowPredicate() {
            @Override
            public boolean test(Row row, Table table) {
                final int finalColIndex = table.getColumnIndex(finalCol);
                Object cell = row.get(finalColIndex);
                if (cell == null) return false;

                // Comparaison numérique si possible
                Double leftNum = null;
                Double rightNum = null;
                try {
                    if (cell instanceof Number n) leftNum = n.doubleValue();
                    else leftNum = Double.parseDouble(cell.toString());

                    rightNum = Double.parseDouble(finalVal);
                } catch (NumberFormatException e) {
                    // ce n'est pas un nombre → on comparera en chaîne
                }

                if (leftNum != null && rightNum != null) {
                    // Comparaison numérique
                    switch (finalOp) {
                        case ">"  : return leftNum > rightNum;
                        case "<"  : return leftNum < rightNum;
                        case ">=" : return leftNum >= rightNum;
                        case "<=" : return leftNum <= rightNum;
                        case "=":
                        case "==": return leftNum.equals(rightNum);
                        case "!=":
                        case "<>": return !leftNum.equals(rightNum);
                        default: return false;
                    }
                } else {
                    // Comparaison chaîne
                    String leftStr = cell.toString();
                    String rightStr = finalVal;
                    switch (finalOp) {
                        case "=":
                        case "==": return leftStr.equals(rightStr);
                        case "!=":
                        case "<>": return !leftStr.equals(rightStr);
                        default: return false; // >, < non valides pour chaînes
                    }
                }
            }
        };
    }
}
