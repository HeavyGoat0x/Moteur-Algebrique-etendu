package fr.uvsq;

import java.util.List;

public class Row {
    private final List<Object> values;

    public Row(List<Object> values) {
        this.values = List.copyOf(values); // immuable, ordre conservé
    }

    public Object get(int index) { return values.get(index); }

    public int getInt(int index) { return (int) values.get(index); }

    public float getFloat(int index) {
        Object v = values.get(index);
        if (v instanceof Float) return (float) v;
        if (v instanceof Double) return ((Double) v).floatValue();
        throw new ClassCastException("Valeur non convertible en float: " + v);
    }

    public String getString(int index) { return (String) values.get(index); }

    public List<Object> getValues() { return values; }

    @Override
    public String toString() {
        return values.toString();
    }
}
