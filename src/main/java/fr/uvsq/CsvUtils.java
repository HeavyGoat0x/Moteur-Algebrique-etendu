package fr.uvsq;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lecture et écriture de tables au format CSV (séparateur virgule, en-tête en première ligne).
 */
public class CsvUtils {

  /**
   * Écrit une table dans un fichier CSV, en-tête compris.
   *
   * @param filename chemin du fichier à écrire
   * @param table table à écrire
   * @throws IOException si le fichier ne peut pas être écrit
   */
  public static void writeCsv(String filename, Table table) throws IOException {
    List<String> cols = table.getColumns();  // récupérer le schéma
    try (PrintWriter pw = new PrintWriter(filename)) {
      // Ligne d'en-tête
      pw.println(String.join(",", cols));

      // Écrire les lignes
      for (Row row : table.getRows()) {
        List<String> values = row.getValues().stream()
            .map(Object::toString)   // convertir chaque valeur en String
            .collect(Collectors.toList());
        pw.println(String.join(",", values));
      }
    }
  }

  /**
   * Lit un fichier CSV dont la première ligne donne les noms de colonnes.
   *
   * @param filename chemin du fichier à lire
   * @return la table lue, vide si le fichier est vide
   * @throws IOException si le fichier ne peut pas être lu
   */
  public static Table readCsv(String filename) throws IOException {
    List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Path.of(filename));
    if (lines.isEmpty()) {
      return new Table(List.of());
    }

    // Première ligne → colonnes
    List<String> columns = List.of(lines.get(0).split(","));
    Table table = new Table(columns);

    // Lignes suivantes → données
    for (int i = 1; i < lines.size(); i++) {
      String[] tokens = lines.get(i).split(",");
      List<Object> values = new java.util.ArrayList<>();
      for (String t : tokens) {
        values.add(parseValue(t));
      }
      table.addRow(new Row(values));

    }

    return table;
  }

  // Conversion automatique int / float / string
  private static Object parseValue(String s) {
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException expected) {
      // pas un entier : on essaie un décimal
    }
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException expected) {
      // pas un nombre : la valeur reste une chaîne
    }
    return s;
  }
}
