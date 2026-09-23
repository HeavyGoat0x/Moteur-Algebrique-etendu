package fr.uvsq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Table {
    private final List<String> columns;   // colonnes fixes
    private final List<Row> rows;         // lignes

    public Table(List<String> columns) {
        this.columns = List.copyOf(columns);
        this.rows = new ArrayList<>();
    }

      public int getColumnIndex(String colName) {
        int idx = columns.indexOf(colName);
        if (idx == -1) throw new IllegalArgumentException("Colonne inconnue: " + colName);
        return idx;
    }
    
    public List<String> getColumns() { return columns; }

    public List<Row> getRows() { return Collections.unmodifiableList(rows); }

        // accepte une Row
    public void addRow(Row row) {
        rows.add(row);
    }
}
