package fr.uvsq;

import java.util.List;

/**
 * Ligne immuable d'une table : valeurs ordonnées selon le schéma de la table.
 */
public class Row {

  private final List<Object> values;

  /**
   * Crée une ligne à partir d'une copie des valeurs.
   *
   * @param values valeurs de la ligne, dans l'ordre des colonnes
   */
  public Row(List<Object> values) {
    this.values = List.copyOf(values); // immuable, ordre conservé
  }

  /**
   * Renvoie la valeur d'une colonne.
   *
   * @param index position de la colonne
   * @return la valeur à cette position
   */
  public Object get(int index) {
    return values.get(index);
  }

  /**
   * Renvoie la valeur d'une colonne entière.
   *
   * @param index position de la colonne
   * @return la valeur convertie en int
   */
  public int getInt(int index) {
    return (int) values.get(index);
  }

  /**
   * Renvoie la valeur d'une colonne décimale.
   *
   * @param index position de la colonne
   * @return la valeur convertie en float
   * @throws ClassCastException si la valeur n'est ni un Float ni un Double
   */
  public float getFloat(int index) {
    Object v = values.get(index);
    if (v instanceof Float) {
      return (float) v;
    }
    if (v instanceof Double) {
      return ((Double) v).floatValue();
    }
    throw new ClassCastException("Valeur non convertible en float: " + v);
  }

  /**
   * Renvoie la valeur d'une colonne texte.
   *
   * @param index position de la colonne
   * @return la valeur convertie en String
   */
  public String getString(int index) {
    return (String) values.get(index);
  }

  /**
   * Renvoie toutes les valeurs de la ligne.
   *
   * @return la liste immuable des valeurs
   */
  public List<Object> getValues() {
    return values;
  }

  @Override
  public String toString() {
    return values.toString();
  }
}
