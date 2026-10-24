package fr.uvsq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectTest {

  private Table users;

  @BeforeEach
  void setUp() {
    users = Fixtures.users();
  }

  @Test
  void execute_uneColonne_gardeSeulementCetteColonne() {
    Table resultat = new Project(new Scan(users), List.of("name")).execute();
    assertEquals(List.of("name"), resultat.getColumns());
    assertEquals(List.of(List.of("Alice"), List.of("Bob"), List.of("Charlie"), List.of("David")),
        Fixtures.valeurs(resultat));
  }

  @Test
  void execute_ordreDemande_reordonneLesColonnes() {
    Table resultat = new Project(new Scan(users), List.of("age", "id")).execute();
    assertEquals(List.of("age", "id"), resultat.getColumns());
    assertEquals(List.of(22, 1), resultat.getRows().get(0).getValues());
  }

  @Test
  void execute_projection_conserveLeNombreDeLignes() {
    Table resultat = new Project(new Scan(users), List.of("id", "age")).execute();
    assertEquals(users.getRows().size(), resultat.getRows().size());
  }

  @Test
  void execute_tableVide_renvoieLeSchemaProjete() {
    Table vide = new Table(List.of("id", "name", "age"));
    Table resultat = new Project(new Scan(vide), List.of("name")).execute();
    assertEquals(List.of("name"), resultat.getColumns());
    assertTrue(resultat.getRows().isEmpty());
  }

  @Test
  void execute_surSelection_reproduitReqProjSel() {
    Operator selection = new Select(new Scan(users), PredicateParser.parse("age < 25"));
    Table resultat = new Project(selection, List.of("name")).execute();
    assertEquals(List.of(List.of("Alice"), List.of("David")), Fixtures.valeurs(resultat));
  }

  @Test
  void execute_tableSource_resteInchangee() {
    new Project(new Scan(users), List.of("name")).execute();
    assertEquals(List.of("id", "name", "age"), users.getColumns());
    assertEquals(Fixtures.valeurs(Fixtures.users()), Fixtures.valeurs(users));
  }
}
