package fr.uvsq;

import java.util.List;

/**
 * Erreur levée quand un opérateur ensembliste reçoit deux relations de schémas différents.
 */
class SchemaMismatchException extends IllegalArgumentException {

  SchemaMismatchException(List<String> leftColumns, List<String> rightColumns) {
    super("Schémas incompatibles : " + leftColumns + " et " + rightColumns);
  }
}
