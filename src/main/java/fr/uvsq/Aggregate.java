package fr.uvsq;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agrégation : regroupe les lignes et calcule une valeur par groupe.
 */
class Aggregate implements Operator {

  private final Operator input;
  private final List<String> groupBy;
  private final AggregateFunction function;
  private final String column;
  private final String alias;

  Aggregate(Operator input, List<String> groupBy, AggregateFunction function, String column,
      String alias) {
    this.input = input;
    this.groupBy = groupBy;
    this.function = function;
    this.column = column;
    if (groupBy.contains(alias)) {
      throw new IllegalArgumentException("Alias déjà présent dans groupBy : " + alias);
    }
    this.alias = alias;
  }

  private Row aggregateGroup(Row key, List<Row> groupRows, int columnIndex) {
    List<Object> targetValues = groupRows.stream().map(row -> row.get(columnIndex)).toList();
    List<Object> newRow = new ArrayList<>(key.getValues());
    newRow.add(function.apply(targetValues));
    return new Row(newRow);
  }

  @Override
  public Table execute() {
    Table inTable = input.execute();
    int columnIndex = inTable.getColumnIndex(column);
    List<Integer> groupByColIndex = groupBy.stream().map(inTable::getColumnIndex).toList();

    Map<Row, List<Row>> groups = inTable.getRows().stream().collect(
        Collectors.groupingBy(row -> new Row(groupByColIndex.stream().map(row::get).toList()),
            LinkedHashMap::new,
            Collectors.toList()));

    List<Row> result = groups.entrySet().stream()
        .map(e -> aggregateGroup(e.getKey(), e.getValue(), columnIndex)).toList();

    List<String> columnNames = new ArrayList<>(groupBy);
    columnNames.add(alias);

    return new Table(columnNames, result);
  }
}
