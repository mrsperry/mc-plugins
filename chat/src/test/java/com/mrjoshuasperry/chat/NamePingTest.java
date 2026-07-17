package com.mrjoshuasperry.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class NamePingTest {
  @Test
  void findsASingleMention() {
    assertEquals(List.of("alice"), NamePing.parseMentions("hey @alice"));
  }

  @Test
  void findsMultipleMentionsInOrder() {
    assertEquals(List.of("alice", "bob"), NamePing.parseMentions("@alice and @bob how are you"));
  }

  @Test
  void returnsEmptyWhenThereAreNoMentions() {
    assertTrue(NamePing.parseMentions("no mentions here").isEmpty());
  }

  @Test
  void stopsAtNonWordCharacters() {
    assertEquals(List.of("alice"), NamePing.parseMentions("@alice! hello"));
  }
}
