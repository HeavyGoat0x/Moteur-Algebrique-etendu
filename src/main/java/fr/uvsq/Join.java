package fr.uvsq;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Équi-jointure de deux tables sur une colonne de chaque côté.
 */
class Join implements Operator {

  private final Operator left;
  private final Operator right;
  private final String leftCol;
  private final String rightCol;

  public Join(Operator left, Operator right, String leftCol, String rightCol) {
    this.left = left;
    this.right = right;
    this.leftCol = leftCol;
    this.rightCol = rightCol;
  }

  @Override
  public Table execute() {
    Table leftTable = left.execute();
    Table rightTable = right.execute();
    List<String> leftCols = leftTable.getColumns();
    List<String> rightCols = rightTable.getColumns();

    // colonnes de droite gardées : celles dont le nom n'existe pas déjà à gauche
    List<Integer> keptRight = IntStream.range(0, rightCols.size())
        .filter(i -> !leftCols.contains(rightCols.get(i)))
        .boxed()
        .toList();

    // colonnes fusionnées
    List<String> mergedCols =
        Stream.concat(leftCols.stream(), keptRight.stream().map(rightCols::get)).toList();

    int leftIdx = leftTable.getColumnIndex(leftCol);
    int rightIdx = rightTable.getColumnIndex(rightCol);

    // chaque ligne de gauche est associée à toutes les lignes de droite de même clé
    List<Row> rows = leftTable.getRows().stream()
        .flatMap(leftRow -> rightTable.getRows().stream()
            .filter(rightRow -> Row.normalize(leftRow.get(leftIdx))
                .equals(Row.normalize(rightRow.get(rightIdx))))
            .map(rightRow -> merge(leftRow, rightRow, keptRight)))
        .toList();
    return new Table(mergedCols, rows);
  }

  // valeurs de la ligne de gauche suivies des colonnes gardées de la ligne de droite
  private static Row merge(Row leftRow, Row rightRow, List<Integer> keptRight) {
    Stream<Object> rightValues = keptRight.stream().map(rightRow::get);
    return new Row(Stream.concat(leftRow.getValues().stream(), rightValues).toList());
  }
}
