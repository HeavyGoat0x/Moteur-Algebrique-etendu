package fr.uvsq;

import com.google.gson.JsonObject;

/**
 * Fabrique d'un type d'opérateur : construit l'opérateur décrit par un nœud du plan JSON.
 */
@FunctionalInterface
interface OperatorFactory {

  /**
   * Construit l'opérateur d'un nœud.
   *
   * @param node nœud JSON de l'opérateur, avec ses paramètres
   * @param builder constructeur du plan, pour construire les sous-plans et trouver les tables
   * @return l'opérateur construit
   */
  Operator create(JsonObject node, PlanBuilder builder);
}
