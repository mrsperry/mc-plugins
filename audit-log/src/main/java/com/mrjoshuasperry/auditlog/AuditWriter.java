package com.mrjoshuasperry.auditlog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Appends lines to the day's files off the main thread. Each line is written and
 * the file closed as soon as it reaches the front of the queue, so a crash loses at
 * most the few lines still waiting.
 */
final class AuditWriter {
    private final Path root;
    private final Logger logger;
    // One thread, so lines land in the order they were recorded and two writes never
    // interleave in one file; the Bukkit scheduler's async pool promises neither
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "AuditLog writer");
        thread.setDaemon(true);
        return thread;
    });

    AuditWriter(Path root, Logger logger) {
        this.root = root;
        this.logger = logger;
    }

    /** Queues the line for each of the files, given relative to the day's folder. */
    void write(LocalDate day, List<String> files, String line) {
        this.submit(() -> {
            Path folder = this.root.resolve(day.toString());
            for (String file : files) {
                this.append(folder.resolve(file), line);
            }
        });
    }

    /** Queues deleting every day's folder older than the retention period. */
    void deleteExpired(LocalDate today, int retentionDays) {
        this.submit(() -> {
            if (!Files.isDirectory(this.root)) {
                return;
            }

            try (Stream<Path> days = Files.list(this.root)) {
                for (Path folder : days.toList()) {
                    LocalDate day = parseDay(folder.getFileName().toString());
                    if (day != null && isExpired(day, today, retentionDays)) {
                        this.delete(folder);
                    }
                }
            } catch (IOException exception) {
                this.logger.log(Level.WARNING, "Could not clean up old audit logs", exception);
            }
        });
    }

    /** Waits for every queued line to be written. */
    void close() {
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(30, TimeUnit.SECONDS)) {
                this.logger.severe("Gave up waiting for the audit log to finish writing; recent records may be missing");
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    /** A day is kept for retentionDays after it ends, so 7 keeps today and the week before. */
    static boolean isExpired(LocalDate day, LocalDate today, int retentionDays) {
        return day.isBefore(today.minusDays(retentionDays));
    }

    static LocalDate parseDay(String folderName) {
        try {
            return LocalDate.parse(folderName);
        } catch (DateTimeParseException _) {
            return null;
        }
    }

    private void submit(Runnable task) {
        try {
            this.executor.execute(task);
        } catch (RejectedExecutionException _) {
            this.logger.severe("An audit record arrived after the audit log closed and was not written");
        }
    }

    private void append(Path file, String line) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, line + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException exception) {
            this.logger.log(Level.SEVERE, "Could not write to the audit log " + file + ": " + line, exception);
        }
    }

    private void delete(Path folder) throws IOException {
        try (Stream<Path> paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
