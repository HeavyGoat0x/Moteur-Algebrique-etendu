package fr.uvsq;

import java.util.List;
import java.util.stream.Stream;

class Difference extends SetOperator {

  Difference(Operator left, Operator right) {
    super(left, right);
  }

  @Override
  protected Stream<Row> combine(List<Row> left, List<Row> right) {
    return left.stream().filter(row -> !right.contains(row));
  }

}
