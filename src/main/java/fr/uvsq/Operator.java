package fr.uvsq;

/**
 * Nœud d'un plan d'exécution : produit une table à partir de ses entrées.
 */
interface Operator {

  Table execute();
}
