package fr.uvsq;

import java.util.List;

/**
 * Données partagées par les tests : mêmes valeurs que data/users.csv, construites en mémoire.
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

  // Row n'a pas d'equals : on compare les listes de valeurs, ligne par ligne
  static List<List<Object>> valeurs(Table table) {
    return table.getRows().stream().map(Row::getValues).toList();
  }
}
