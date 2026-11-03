package fr.uvsq;

import java.util.List;
import java.util.stream.Stream;

/**
 * Union de deux relations : les lignes de gauche, puis celles de droite, sans doublon.
 */
class Union extends SetOperator {

  Union(Operator left, Operator right) {
    super(left, right);
  }

  @Override
  protected Stream<Row> combine(List<Row> left, List<Row> right) {
    return Stream.concat(left.stream(), right.stream());
  }
}
