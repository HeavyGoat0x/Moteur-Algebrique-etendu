package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvUtilsTest {

  @TempDir
  Path dossier;

  private String ecrireFichier(String contenu) throws IOException {
    Path fichier = dossier.resolve("table.csv");
    Files.writeString(fichier, contenu);
    return fichier.toString();
  }

  private Table tableUsers() {
    Table users = new Table(List.of("id", "name", "score"));
    users.addRow(new Row(List.of(1, "Alice", 22.5)));
    users.addRow(new Row(List.of(2, "Bob", 30)));
    return users;
  }

  @Test
  void readCsv_fichierSimple_litLeSchema() throws IOException {
    Table table = CsvUtils.readCsv(ecrireFichier("id,name,age\n1,Alice,22\n"));
    assertEquals(List.of("id", "name", "age"), table.getColumns());
  }

  @Test
  void readCsv_fichierSimple_litLesLignesDansOrdre() throws IOException {
    Table table = CsvUtils.readCsv(ecrireFichier("id,name\n1,Alice\n2,Bob\n"));
    assertEquals(2, table.getRows().size());
    assertEquals(List.of(1, "Alice"), table.getRows().get(0).getValues());
    assertEquals(List.of(2, "Bob"), table.getRows().get(1).getValues());
  }

  @Test
  void readCsv_valeurEntiere_devientInteger() throws IOException {
    Table table = CsvUtils.readCsv(ecrireFichier("age\n22\n"));
    assertInstanceOf(Integer.class, table.getRows().get(0).get(0));
  }

  @Test
  void readCsv_valeurDecimale_devientDouble() throws IOException {
    Table table = CsvUtils.readCsv(ecrireFichier("score\n22.5\n"));
    assertEquals(22.5, table.getRows().get(0).get(0));
  }

  @Test
  void readCsv_valeurTexte_resteString() throws IOException {
    Table table = CsvUtils.readCsv(ecrireFichier("name\nAlice\n"));
    assertEquals("Alice", table.getRows().get(0).get(0));
  }

  @Test
  void readCsv_finDeLigneWindows_estIgnoree() throws IOException {
    Table table = CsvUtils.readCsv(ecrireFichier("id,name\r\n1,Alice\r\n"));
    assertEquals(List.of("id", "name"), table.getColumns());
    assertEquals(List.of(1, "Alice"), table.getRows().get(0).getValues());
  }

  @Test
  void readCsv_fichierVide_renvoieTableSansColonne() throws IOException {
    Table table = CsvUtils.readCsv(ecrireFichier(""));
    assertTrue(table.getColumns().isEmpty());
    assertTrue(table.getRows().isEmpty());
  }

  @Test
  void readCsv_enTeteSeule_renvoieTableSansLigne() throws IOException {
    Table table = CsvUtils.readCsv(ecrireFichier("id,name\n"));
    assertEquals(List.of("id", "name"), table.getColumns());
    assertTrue(table.getRows().isEmpty());
  }

  @Test
  void readCsv_fichierAbsent_leveUneErreurDeLecture() {
    String absent = dossier.resolve("absent.csv").toString();
    assertThrows(IOException.class, () -> CsvUtils.readCsv(absent));
  }

  @Test
  void readCsv_usersDuDepot_litLesQuatreUtilisateurs() throws IOException {
    Table users = CsvUtils.readCsv("data/users.csv");
    assertEquals(List.of("id", "name", "age"), users.getColumns());
    assertEquals(4, users.getRows().size());
    assertEquals(List.of(4, "David", 19), users.getRows().get(3).getValues());
  }

  @Test
  void readCsv_champVideEnFinDeLigne_estConserve() throws IOException {
    Table table = CsvUtils.readCsv(ecrireFichier("a,b,c\n1,2,\n"));
    assertEquals(List.of(1, 2, ""), table.getRows().get(0).getValues());
  }

  @Test
  void readCsv_colonneSansNomEnFin_estConservee() throws IOException {
    Table table = CsvUtils.readCsv(ecrireFichier("a,b,\n1,2,3\n"));
    assertEquals(List.of("a", "b", ""), table.getColumns());
  }

  @Test
  void writeCsv_tableRelue_redonneLeMemeFichier() throws IOException {
    Table table = CsvUtils.readCsv(ecrireFichier("a,b,c\n1,2,\n"));
    Path sortie = dossier.resolve("sortie.csv");
    CsvUtils.writeCsv(sortie.toString(), table);
    assertEquals("a,b,c\n1,2,\n", Files.readString(sortie));
  }

  // readString garde les fins de ligne, contrairement à readAllLines
  @Test
  void writeCsv_finsDeLigne_sontToujoursLf() throws IOException {
    Path sortie = dossier.resolve("sortie.csv");
    CsvUtils.writeCsv(sortie.toString(), tableUsers());
    assertEquals("id,name,score\n1,Alice,22.5\n2,Bob,30\n", Files.readString(sortie));
  }

  @Test
  void writeCsv_valeurAccentuee_ecritEnUtf8() throws IOException {
    Table table = new Table(List.of("name"), List.of(new Row(List.of("Éloïse"))));
    Path sortie = dossier.resolve("sortie.csv");
    CsvUtils.writeCsv(sortie.toString(), table);
    assertEquals("name\nÉloïse\n", Files.readString(sortie));
  }

  @Test
  void writeCsv_table_ecritEnTetePuisLignes() throws IOException {
    Path sortie = dossier.resolve("sortie.csv");
    CsvUtils.writeCsv(sortie.toString(), tableUsers());
    assertEquals(List.of("id,name,score", "1,Alice,22.5", "2,Bob,30"), Files.readAllLines(sortie));
  }

  @Test
  void writeCsv_tableSansLigne_ecritSeulementEnTete() throws IOException {
    Path sortie = dossier.resolve("sortie.csv");
    CsvUtils.writeCsv(sortie.toString(), new Table(List.of("id", "name")));
    assertEquals(List.of("id,name"), Files.readAllLines(sortie));
  }

  @Test
  void writeCsv_puisReadCsv_retrouveLesMemesDonnees() throws IOException {
    Path sortie = dossier.resolve("sortie.csv");
    Table origine = tableUsers();
    CsvUtils.writeCsv(sortie.toString(), origine);

    Table relue = CsvUtils.readCsv(sortie.toString());

    assertEquals(origine.getColumns(), relue.getColumns());
    assertEquals(origine.getRows().size(), relue.getRows().size());
    for (int i = 0; i < origine.getRows().size(); i++) {
      assertEquals(origine.getRows().get(i).getValues(), relue.getRows().get(i).getValues());
    }
  }
}
