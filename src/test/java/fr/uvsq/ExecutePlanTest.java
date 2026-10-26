package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests de bout en bout : plans JSON du dépôt (data/) comparés aux sorties attendues (expected/).
 */
class ExecutePlanTest {

  private static final Path DATA = Path.of("data");
  private static final Path EXPECTED = Path.of("expected");

  @TempDir
  Path dossier;

  // Le plan lit ses tables dans data/ (lecture seule) et écrit sa sortie dans le dossier temporaire
  private Path preparerRequete(String nom, Path sortie) throws Exception {
    JsonObject requete =
        JsonParser.parseString(Files.readString(DATA.resolve(nom + ".json"))).getAsJsonObject();
    JsonObject tables = requete.getAsJsonObject("tables");
    for (String table : List.copyOf(tables.keySet())) {
      Path csv = DATA.resolve(tables.get(table).getAsString()).toAbsolutePath();
      tables.addProperty(table, csv.toString());
    }
    requete.addProperty("output", sortie.toString());

    Path fichier = dossier.resolve(nom + ".json");
    Files.writeString(fichier, requete.toString());
    return fichier;
  }

  @ParameterizedTest
  @ValueSource(strings = {"plan1", "reqProjSel", "optJoin"})
  void main_planDuDepot_produitLeResultatAttendu(String nom) throws Exception {
    Path sortie = Files.createDirectories(dossier.resolve("results")).resolve("output.csv");

    ExecutePlan.main(new String[] {preparerRequete(nom, sortie).toString()});

    assertEquals(Files.readAllLines(EXPECTED.resolve(nom + "_expected.csv")),
        Files.readAllLines(sortie));
  }

  @Test
  void main_sansArgument_afficheUsage() throws Exception {
    PrintStream sortieStandard = System.out;
    ByteArrayOutputStream capture = new ByteArrayOutputStream();
    System.setOut(new PrintStream(capture, true, StandardCharsets.UTF_8));
    try {
      ExecutePlan.main(new String[] {});
    } finally {
      System.setOut(sortieStandard);
    }
    assertTrue(capture.toString(StandardCharsets.UTF_8).contains("Usage"));
  }
}
