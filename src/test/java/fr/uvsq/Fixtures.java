package fr.uvsq;

import java.util.List;

/**
 * Données partagées par les tests : mêmes valeurs que data/users.csv et data/orders.csv.
 */
final class Fixtures {

  private Fixtures() {
  }

  static Table users() {
    Table users = new Table(List.of("id", "name", "age"));
    users.addRow(new Row(List.of(1, "Alice", 22)));
    users.addRow(new Row(List.of(2, "Bob", 30)));
    users.addRow(new Row(List.of(3, "Charlie", 27)));
    users.addRow(new Row(List.of(4, "David", 19)));
    return users;
  }

  static Table orders() {
    Table orders = new Table(List.of("id", "user_id", "amount"));
    orders.addRow(new Row(List.of(10, 2, 100)));
    orders.addRow(new Row(List.of(11, 1, 150)));
    orders.addRow(new Row(List.of(12, 2, 200)));
    orders.addRow(new Row(List.of(13, 3, 50)));
    return orders;
  }

  // Valeurs brutes, ligne par ligne : plus strict que Row.equals (1 et 1.0 restent différents),
  // ce qui vérifie aussi le type, donc le format de sortie
  static List<List<Object>> valeurs(Table table) {
    return table.getRows().stream().map(Row::getValues).toList();
  }
}
