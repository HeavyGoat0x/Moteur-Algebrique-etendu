package fr.uvsq;

import java.util.ArrayList;
import java.util.List;

/**
 * Projection : garde uniquement les colonnes demandées, dans l'ordre demandé.
 */
class Project implements Operator {

  private final Operator input;
  private final List<String> columns;

  public Project(Operator input, List<String> columns) {
    this.input = input;
    this.columns = columns;
  }

  @Override
  public Table execute() {
    Table inTable = input.execute();
    Table outTable = new Table(columns);

    List<String> inCols = inTable.getColumns();
    for (Row row : inTable.getRows()) {
      List<Object> vals = new ArrayList<>();
      for (String c : columns) {
        int idx = inCols.indexOf(c);
        vals.add(row.get(idx));
      }
      outTable.addRow(new Row(vals));
    }

    return outTable;
  }
}
