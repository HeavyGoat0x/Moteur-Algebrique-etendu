package fr.uvsq;

import java.util.List;

/**
 * Sélection : garde les lignes qui satisfont un prédicat.
 */
class Select implements Operator {

  private final Operator input;
  private final PredicateParser.RowPredicate predicate;

  public Select(Operator input, PredicateParser.RowPredicate predicate) {
    this.input = input;
    this.predicate = predicate;
  }

  @Override
  public Table execute() {
    Table inputTable = input.execute();
    List<Row> rows = inputTable.getRows().stream()
        .filter(row -> predicate.test(row, inputTable))
        .toList();
    return new Table(inputTable.getColumns(), rows);
  }
}
