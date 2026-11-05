package fr.uvsq;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduit une requête écrite en .alg en requête JSON exécutable.
 *
 * <p>Les lignes "nom: fichier" déclarent les tables, les lignes "R = OP arguments" définissent
 * les relations. La dernière relation définie devient le plan de la requête.
 */
class AlgTranslator {

  // Requête JSON {"tables": ..., "plan": ...} correspondant aux lignes d'un fichier .alg
  static JsonObject translate(List<String> lines) {
    JsonObject tables = new JsonObject();
    Map<String, JsonObject> relations = new HashMap<>();
    JsonObject plan = null; // dernière relation définie

    for (String line : lines) {
      if (line.isBlank()) {
        continue;
      }
      // "=" d'abord : un prédicat comme name = 'a:b' contient aussi ":"
      if (line.contains("=")) {
        String[] firstSplit = line.split("=", 2);
        String relation = firstSplit[0].trim();
        String[] operationParts = firstSplit[1].trim().split("\\s+", 2);
        if (relation.isEmpty() || operationParts.length < 2) {
          throw new IllegalArgumentException("Ligne illisible : " + line);
        }
        String operator = operationParts[0];
        String rest = operationParts[1];

        JsonObject node = switch (operator) {
          case "SELECT" -> select(rest, tables, relations);
          case "PROJ" -> project(rest, tables, relations);
          case "JOIN" -> join(rest, tables, relations);
          default -> throw new IllegalArgumentException("Opérateur inconnu : " + operator);
        };
        relations.put(relation, node);
        plan = node;
      } else if (line.contains(":")) {
        String[] parts = line.split(":", 2); // limite 2 : le chemin peut contenir ":"
        if (parts[0].isBlank() || parts[1].isBlank()) {
          throw new IllegalArgumentException("Ligne illisible : " + line);
        }
        tables.addProperty(parts[0].trim(), parts[1].trim());
      } else {
        throw new IllegalArgumentException("Ligne illisible : " + line);
      }
    }

    if (plan == null) {
      throw new IllegalArgumentException("Aucune relation définie");
    }
    JsonObject request = new JsonObject();
    request.add("tables", tables);
    request.add("plan", plan);
    return request;
  }

  // SELECT relation prédicat : le prédicat est le reste de la ligne, gardé entier
  private static JsonObject select(String rest, JsonObject tables,
      Map<String, JsonObject> relations) {
    String[] parts = rest.split("\\s+", 2);
    if (parts.length != 2) {
      throw new IllegalArgumentException("SELECT attend une relation et un prédicat : " + rest);
    }
    JsonObject result = new JsonObject();
    result.addProperty("type", "SELECT");
    result.addProperty("predicate", parts[1]);
    result.add("input", relation(parts[0], tables, relations));
    return result;
  }

  // JOIN relation1 relation2 colonneGauche colonneDroite : exactement 4 arguments
  private static JsonObject join(String rest, JsonObject tables,
      Map<String, JsonObject> relations) {
    String[] parts = rest.split("\\s+");
    if (parts.length != 4) {
      throw new IllegalArgumentException("JOIN attend 4 arguments : " + rest);
    }
    JsonObject result = new JsonObject();
    result.addProperty("type", "JOIN");
    result.add("left", relation(parts[0], tables, relations));
    result.add("right", relation(parts[1], tables, relations));
    result.addProperty("onLeft", parts[2]);
    result.addProperty("onRight", parts[3]);
    return result;
  }

  // PROJ relation colonne1 colonne2… : tous les morceaux après la relation sont des colonnes
  private static JsonObject project(String rest, JsonObject tables,
      Map<String, JsonObject> relations) {
    String[] parts = rest.split("\\s+");
    if (parts.length < 2) {
      throw new IllegalArgumentException(
          "PROJ attend une relation et au moins une colonne : " + rest);
    }
    JsonArray columns = new JsonArray();
    for (int i = 1; i < parts.length; i++) {
      columns.add(parts[i]);
    }
    JsonObject result = new JsonObject();
    result.addProperty("type", "PROJECT");
    result.add("columns", columns);
    result.add("input", relation(parts[0], tables, relations));
    return result;
  }

  // Nœud d'une entrée : SCAN pour une table de l'en-tête, nœud déjà construit pour une relation
  private static JsonObject relation(String inputName, JsonObject tables,
      Map<String, JsonObject> relations) {
    if (tables.has(inputName)) {
      JsonObject scan = new JsonObject();
      scan.addProperty("type", "SCAN");
      scan.addProperty("table", inputName);
      return scan;
    }
    if (relations.containsKey(inputName)) {
      return relations.get(inputName);
    }
    throw new IllegalArgumentException("Relation inconnue : " + inputName);
  }
}
