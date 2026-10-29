package fr.uvsq;

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

    // positions des colonnes demandées, calculées une seule fois pour toutes les lignes
    List<Integer> indexes = columns.stream()
        .map(inTable.getColumns()::indexOf)
        .toList();

    List<Row> rows = inTable.getRows().stream()
        .map(row -> new Row(indexes.stream().map(row::get).toList()))
        .toList();
    return new Table(columns, rows);
  }
}
