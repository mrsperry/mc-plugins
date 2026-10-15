package com.mrjoshuasperry.mobeggs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.ZombieHorse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.world.WorldMock;

class MobStackTest {
  private WorldMock world;

  @BeforeEach
  void setUp() {
    world = MockBukkit.mock().addSimpleWorld("world");
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  private <T extends Entity> T spawn(Class<T> type) {
    return world.spawn(new Location(world, 0, 64, 0), type);
  }

  /** Bottom first: each one rides the one before it. */
  private List<Zombie> stackOfZombies(int size) {
    List<Zombie> stack = new ArrayList<>();
    for (int index = 0; index < size; index++) {
      Zombie zombie = spawn(Zombie.class);
      if (!stack.isEmpty()) {
        stack.getLast().addPassenger(zombie);
      }
      stack.add(zombie);
    }
    return stack;
  }

  @Test
  void aRiderOrItsMountCanBothBeCaught() {
    ZombieHorse horse = spawn(ZombieHorse.class);
    Zombie zombie = spawn(Zombie.class);
    horse.addPassenger(zombie);

    assertNull(MobCapture.denialFor(horse, Set.of()));
    assertNull(MobCapture.denialFor(zombie, Set.of()));
  }

  @Test
  void catchingTheMountLeavesItsRiderBehind() {
    ZombieHorse horse = spawn(ZombieHorse.class);
    Zombie zombie = spawn(Zombie.class);
    horse.addPassenger(zombie);

    assertTrue(MobCapture.detach(horse));
    assertNull(zombie.getVehicle());
  }

  @Test
  void catchingTheBottomOfAStackLeavesTheRestStacked() {
    List<Zombie> stack = stackOfZombies(4);

    assertTrue(MobCapture.detach(stack.get(0)));

    assertNull(stack.get(1).getVehicle());
    assertEquals(List.of(stack.get(2)), stack.get(1).getPassengers());
    assertEquals(List.of(stack.get(3)), stack.get(2).getPassengers());
  }

  @Test
  void catchingTheMiddleSplitsTheStackAroundIt() {
    List<Zombie> stack = stackOfZombies(4);

    assertTrue(MobCapture.detach(stack.get(1)));

    assertTrue(stack.get(0).getPassengers().isEmpty(), "the one below carries on alone");
    assertNull(stack.get(2).getVehicle(), "the ones above get off, still stacked");
    assertEquals(List.of(stack.get(3)), stack.get(2).getPassengers());
    assertTrue(stack.get(1).getPassengers().isEmpty());
    assertNull(stack.get(1).getVehicle());
  }
}
