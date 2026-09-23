package fr.uvsq;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Interface générale

interface Operator {
    Table execute(); 
    // List<String> getColumns();  // nouvelle méthode
}

// ---------------- Scan ----------------
class Scan implements Operator {
    private final Table table;

    public Scan(Table table) { this.table = table; }

    @Override
    public Table execute() {
        return table;   // retourne la table complète
    }
}

// ---------------- Select ----------------


class Select implements Operator {
    private final Operator input;
    private final PredicateParser.RowPredicate predicate;

    public Select(Operator input, PredicateParser.RowPredicate predicate) {
        this.input = input;
        this.predicate = predicate;
    }

    @Override
    public Table execute() {
        Table inputTable = input.execute();
        Table result = new Table(inputTable.getColumns());

        for (Row row : inputTable.getRows()) {
            if (predicate.test(row, inputTable)) {
                result.addRow(row);
            }
        }
        return result;
    }
}



class Project implements Operator {
    private final Operator input;
    private final List<String> columns;

    public Project(Operator input, List<String> columns) {
        this.input = input;
        this.columns = columns;
    }

    @Override
    public Table execute() {
        Table inTable = input.execute();
        Table outTable = new Table(columns);

        List<String> inCols = inTable.getColumns();
        for (Row row : inTable.getRows()) {
            List<Object> vals = new ArrayList<>();
            for (String c : columns) {
                int idx = inCols.indexOf(c);
                vals.add(row.get(idx));
            }
            // outTable.addRow(vals);
            outTable.addRow(new Row(vals));  
        }

        return outTable;
    }
}

class Join implements Operator {
    private final Operator left;
    private final Operator right;
    private final String leftCol;
    private final String rightCol;

    public Join(Operator left, Operator right, String leftCol, String rightCol) {
        this.left = left;
        this.right = right;
        this.leftCol = leftCol;
        this.rightCol = rightCol;
    }

    @Override
    public Table execute() {
        Table leftTable = left.execute();
        Table rightTable = right.execute();

        // colonnes fusionnées
        List<String> mergedCols = new ArrayList<>(leftTable.getColumns());
        for (String c : rightTable.getColumns()) {
            if (!mergedCols.contains(c)) mergedCols.add(c);
        }

        Table outTable = new Table(mergedCols);

        int leftIdx = leftTable.getColumns().indexOf(leftCol);
        int rightIdx = rightTable.getColumns().indexOf(rightCol);

        for (Row lRow : leftTable.getRows()) {
            for (Row rRow : rightTable.getRows()) {
                if (Objects.equals(lRow.get(leftIdx), rRow.get(rightIdx))) {
                    List<Object> mergedVals = new ArrayList<>(lRow.getValues());
                    for (int i = 0; i < rightTable.getColumns().size(); i++) {
                        String c = rightTable.getColumns().get(i);
                        if (!leftTable.getColumns().contains(c)) {
                            mergedVals.add(rRow.get(i));
                        }
                    }
                    outTable.addRow(new Row(mergedVals)); 
                    
                    
                }
            }
        }

        return outTable;
    }
}


