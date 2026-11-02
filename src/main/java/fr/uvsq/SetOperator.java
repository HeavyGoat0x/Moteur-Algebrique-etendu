package fr.uvsq;

import java.util.List;
import java.util.stream.Stream;

/**
 * Opérateur ensembliste : vérifie les schémas et retire les doublons ; la combinaison des lignes
 * est laissée à chaque sous-classe.
 */
abstract class SetOperator implements Operator {

  private final Operator left;
  private final Operator right;

  SetOperator(Operator left, Operator right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public final Table execute() {
    Table leftTable = left.execute();
    Table rightTable = right.execute();
    List<String> leftColumns = leftTable.getColumns();
    List<String> rightColumns = rightTable.getColumns();

    if (!leftColumns.equals(rightColumns)) {
      throw new SchemaMismatchException(leftColumns, rightColumns);
    }

    List<Row> resultRows = combine(leftTable.getRows(), rightTable.getRows()).distinct().toList();
    return new Table(leftColumns, resultRows);
  }

  protected abstract Stream<Row> combine(List<Row> left, List<Row> right);
}
