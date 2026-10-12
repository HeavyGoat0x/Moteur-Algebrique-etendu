package fr.uvsq;

/**
 * Feuille du plan : renvoie une table chargée.
 */
class Scan implements Operator {

  private final Table table;

  public Scan(Table table) {
    this.table = table;
  }

  @Override
  public Table execute() {
    return table;   // retourne la table complète
  }
}
