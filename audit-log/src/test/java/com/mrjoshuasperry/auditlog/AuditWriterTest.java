package com.mrjoshuasperry.auditlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditWriterTest {
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

  @TempDir
  Path root;

  @Test
  void linesAreAppendedToEachPlayersFileForTheDay() throws IOException {
    AuditWriter writer = new AuditWriter(root, Logger.getAnonymousLogger());
    writer.write(TODAY, List.of("players/Alice.jsonl", "players/Bob.jsonl"), "{\"n\":1}");
    writer.write(TODAY, List.of("players/Alice.jsonl"), "{\"n\":2}");
    writer.close();

    assertEquals(List.of("{\"n\":1}", "{\"n\":2}"), Files.readAllLines(root.resolve("2026-10-12/players/Alice.jsonl")));
    assertEquals(List.of("{\"n\":1}"), Files.readAllLines(root.resolve("2026-10-12/players/Bob.jsonl")));
  }

  @Test
  void aWeekOfLogsIsKeptAndAnythingOlderIsDeleted() throws IOException {
    for (String day : List.of("2026-10-12", "2026-10-05", "2026-10-04", "2026-09-01")) {
      Files.createDirectories(root.resolve(day).resolve("players"));
      Files.writeString(root.resolve(day).resolve("players/Alice.jsonl"), "{}\n");
    }
    Files.createDirectories(root.resolve("notes"));

    AuditWriter writer = new AuditWriter(root, Logger.getAnonymousLogger());
    writer.deleteExpired(TODAY, 7);
    writer.close();

    assertTrue(Files.exists(root.resolve("2026-10-12")));
    assertTrue(Files.exists(root.resolve("2026-10-05")), "exactly a week old is kept");
    assertFalse(Files.exists(root.resolve("2026-10-04")));
    assertFalse(Files.exists(root.resolve("2026-09-01")));
    assertTrue(Files.exists(root.resolve("notes")), "folders that aren't days are left alone");
  }

  @Test
  void onlyDateFoldersAreRecognised() {
    assertEquals(TODAY, AuditWriter.parseDay("2026-10-12"));
    assertNull(AuditWriter.parseDay("notes"));
  }
}
