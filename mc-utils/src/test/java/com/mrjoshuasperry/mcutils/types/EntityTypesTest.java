package com.mrjoshuasperry.mcutils.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

class EntityTypesTest {
  @Test
  void categoriesAreClassifiedCorrectly() {
    assertTrue(EntityTypes.getHostileTypes().contains(EntityType.CREEPER));
    assertTrue(EntityTypes.getNeutralTypes().contains(EntityType.COW));
  }

  @Test
  void getAllTypesIsHostilePlusNeutral() {
    int expected = EntityTypes.getHostileTypes().size() + EntityTypes.getNeutralTypes().size();
    assertEquals(expected, EntityTypes.getAllTypes().size());
  }

  @Test
  void getAllTypesDoesNotMutateSharedLists() {
    // Regression: getAllTypes() used to addAll onto the `hostile` reference,
    // permanently growing the shared static list on every call.
    int hostileBefore = EntityTypes.getHostileTypes().size();
    int combinedBefore = EntityTypes.getAllTypes().size();

    EntityTypes.getAllTypes();
    EntityTypes.getAllTypes();

    assertEquals(hostileBefore, EntityTypes.getHostileTypes().size());
    assertEquals(combinedBefore, EntityTypes.getAllTypes().size());
  }
}
