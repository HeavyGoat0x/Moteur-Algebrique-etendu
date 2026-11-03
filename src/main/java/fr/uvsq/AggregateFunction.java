package fr.uvsq;

import java.util.Comparator;
import java.util.List;

enum AggregateFunction {

  COUNT {
    @Override
    Object apply(List<Object> values) {
      return values.size();
    }
  },

  SUM {
    @Override
    Object apply(List<Object> values) {
      List<Number> numbers = numbers(values);
      boolean isInteger = numbers.stream()
          .allMatch(v -> (v instanceof Integer || v instanceof Long));

      if (isInteger) {
        return numbers.stream().mapToLong(Number::longValue).sum();
      } else {
        return numbers.stream().mapToDouble(Number::doubleValue).sum();
      }
    }
  },

  AVG {
    @Override
    Object apply(List<Object> values) {
      return numbers(values).stream().mapToDouble(Number::doubleValue).average().orElseThrow();
    }
  },

  MIN {
    @Override
    Object apply(List<Object> values) {
      return numbers(values).stream()
          .min(Comparator.comparingDouble(Number::doubleValue))
          .orElseThrow();
    }
  },

  MAX {
    @Override
    Object apply(List<Object> values) {
      return numbers(values).stream()
          .max(Comparator.comparingDouble(Number::doubleValue))
          .orElseThrow();
    }
  };

  abstract Object apply(List<Object> values);

  // Refuse toute valeur qui n'est pas un nombre, sinon renvoie les valeurs sous forme de Number
  private static List<Number> numbers(List<Object> values) {
    for (Object value : values) {
      if (!(value instanceof Number)) {
        throw new IllegalArgumentException("Valeur non numérique : " + value);
      }
    }
    return values.stream().map(Number.class::cast).toList();
  }
}
