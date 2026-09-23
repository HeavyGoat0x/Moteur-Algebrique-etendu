package fr.uvsq;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

public class CSVUtils {

    // Écriture CSV à partir d'une Table
    public static void writeCSV(String filename, Table table) throws IOException {
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

    // Lecture CSV → Table
    public static Table readCSV(String filename) throws IOException {
        List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Path.of(filename));
        if (lines.isEmpty()) return new Table(List.of());

        // Première ligne → colonnes
        List<String> columns = List.of(lines.get(0).split(","));
        Table table = new Table(columns);

        // Lignes suivantes → données
        for (int i = 1; i < lines.size(); i++) {
            String[] tokens = lines.get(i).split(",");
            List<Object> values = new java.util.ArrayList<>();
            for (String t : tokens) values.add(parseValue(t));
            table.addRow(new Row(values));

        }

        return table;
    }

    // Conversion automatique int / float / string
    private static Object parseValue(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) {}
        try { return Double.parseDouble(s); } catch (Exception e) {}
        return s;
    }
}
