package fr.uvsq;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    // colonnes fusionnées
    List<String> mergedCols = new ArrayList<>(leftTable.getColumns());
    for (String c : rightTable.getColumns()) {
      if (!mergedCols.contains(c)) {
        mergedCols.add(c);
      }
    }

    Table outTable = new Table(mergedCols);

    int leftIdx = leftTable.getColumns().indexOf(leftCol);
    int rightIdx = rightTable.getColumns().indexOf(rightCol);

    for (Row leftRow : leftTable.getRows()) {
      for (Row rightRow : rightTable.getRows()) {
        if (Objects.equals(leftRow.get(leftIdx), rightRow.get(rightIdx))) {
          List<Object> mergedVals = new ArrayList<>(leftRow.getValues());
          for (int i = 0; i < rightTable.getColumns().size(); i++) {
            String c = rightTable.getColumns().get(i);
            if (!leftTable.getColumns().contains(c)) {
              mergedVals.add(rightRow.get(i));
            }
          }
          outTable.addRow(new Row(mergedVals));
        }
      }
    }

    return outTable;
  }
}
