package io.nexus.inventory.infrastructure.config;

import io.nexus.inventory.application.InventoryService;
import io.nexus.inventory.application.port.out.InventoryStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class InventoryConfiguration {

  @Produces
  @ApplicationScoped
  InventoryService inventoryService(InventoryStore store) {
    return new InventoryService(store);
  }
}
