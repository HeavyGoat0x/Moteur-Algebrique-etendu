package fr.uvsq;

import java.util.List;
import java.util.stream.Stream;

class Intersection extends SetOperator {

  Intersection(Operator left, Operator right) {
    super(left, right);
  }

  @Override
  protected Stream<Row> combine(List<Row> left, List<Row> right) {
    return left.stream().filter(row -> right.contains(row));
  }

}
