package fr.uvsq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Table en mémoire : un schéma fixe (noms de colonnes) et une liste de lignes.
 */
public class Table {

  private final List<String> columns;   // colonnes fixes
  private final List<Row> rows;         // lignes

  /**
   * Crée une table vide.
   *
   * @param columns noms des colonnes, dans l'ordre
   */
  public Table(List<String> columns) {
    this(columns, List.of());
  }

  /**
   * Crée une table à partir d'une copie de ses lignes (résultat d'un flux, par exemple).
   *
   * @param columns noms des colonnes, dans l'ordre
   * @param rows lignes de la table, dans l'ordre
   * @throws IllegalArgumentException si une ligne n'a pas autant de valeurs que de colonnes
   */
  public Table(List<String> columns, List<Row> rows) {
    this.columns = List.copyOf(columns);
    rows.forEach(this::verifyRowLength);
    this.rows = new ArrayList<>(rows);
  }

  /**
   * Renvoie la position d'une colonne.
   *
   * @param colName nom de la colonne
   * @return son index dans le schéma
   * @throws IllegalArgumentException si la colonne n'existe pas
   */
  public int getColumnIndex(String colName) {
    int idx = columns.indexOf(colName);
    if (idx == -1) {
      throw new IllegalArgumentException("Colonne inconnue : " + colName);
    }
    return idx;
  }

  /**
   * Renvoie le schéma de la table.
   *
   * @return la liste immuable des noms de colonnes
   */
  public List<String> getColumns() {
    return columns;
  }

  /**
   * Renvoie les lignes de la table.
   *
   * @return une vue non modifiable des lignes
   */
  public List<Row> getRows() {
    return Collections.unmodifiableList(rows);
  }

  /**
   * Ajoute une ligne à la fin de la table.
   *
   * @param row ligne à ajouter
   * @throws IllegalArgumentException si la ligne n'a pas autant de valeurs que de colonnes
   */
  public void addRow(Row row) {
    verifyRowLength(row);
    rows.add(row);
  }

  private void verifyRowLength(Row row) {
    if (row.getValues().size() != this.columns.size()) {
      throw new IllegalArgumentException(
          "Ligne de " + row.getValues().size() + " valeurs pour un schéma de " + this.columns.size()
              + " colonnes : " + row);
    }
  }
}
