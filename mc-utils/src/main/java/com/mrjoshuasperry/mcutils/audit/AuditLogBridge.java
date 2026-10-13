package com.mrjoshuasperry.mcutils.audit;

import java.util.Map;

import org.bukkit.plugin.Plugin;

import com.mrjoshuasperry.auditlog.AuditLog;

/**
 * The only class that touches AuditLog's own, kept apart from {@link Audit} so the
 * JVM only goes looking for AuditLog once it's known to be installed. Every plugin
 * relocates its copy of mc-utils, so the record has to cross over through a class
 * that lives in AuditLog's jar alone.
 */
final class AuditLogBridge {
  private AuditLogBridge() {
  }

  static void record(Plugin source, String event, Map<String, Object> fields) {
    AuditLog.record(source, event, fields);
  }
}
