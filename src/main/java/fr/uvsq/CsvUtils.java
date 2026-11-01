package fr.uvsq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lecture et écriture de tables au format CSV (séparateur virgule, en-tête en première ligne).
 */
public class CsvUtils {

  /**
   * Écrit une table dans un fichier CSV, en-tête compris.
   *
   * <p>Le fichier est en UTF-8, avec des fins de ligne LF sur tous les systèmes.
   *
   * @param filename chemin du fichier à écrire
   * @param table table à écrire
   * @throws IOException si le fichier ne peut pas être écrit
   */
  public static void writeCsv(String filename, Table table) throws IOException {
    Stream<String> header = Stream.of(String.join(",", table.getColumns()));
    Stream<String> rows = table.getRows().stream().map(CsvUtils::toLine);

    String content = Stream.concat(header, rows)
        .map(line -> line + "\n")
        .collect(Collectors.joining());
    Files.writeString(Path.of(filename), content);
  }

  /**
   * Lit un fichier CSV dont la première ligne donne les noms de colonnes.
   *
   * @param filename chemin du fichier à lire
   * @return la table lue, vide si le fichier est vide
   * @throws IOException si le fichier ne peut pas être lu
   */
  public static Table readCsv(String filename) throws IOException {
    List<String> lines = Files.readAllLines(Path.of(filename));
    if (lines.isEmpty()) {
      return new Table(List.of());
    }

    // Première ligne → colonnes
    List<String> columns = List.of(lines.get(0).split(",", -1));

    // Lignes suivantes → données
    List<Row> rows = lines.stream().skip(1).map(CsvUtils::parseLine).toList();
    return new Table(columns, rows);
  }

  private static String toLine(Row row) {
    return row.getValues().stream().map(Object::toString).collect(Collectors.joining(","));
  }

  // La limite -1 demande à split de garder les champs vides en fin de ligne
  private static Row parseLine(String line) {
    return new Row(Arrays.stream(line.split(",", -1)).map(CsvUtils::parseValue).toList());
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
