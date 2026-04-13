/**
 * @fileoverview The AddonManager orchestrates the execution of third-party Add-ons.
 * When the SpatialMappingEngine determines that a user wrote inside an Action Zone,
 * this manager routes that event to the respective Add-on's `execute()` method.
 */

class AddonManager {
  /**
   * Initializes the AddonManager registry.
   */
  constructor() {
    /**
     * @type {Map<string, import('./BaseAddon').default>}
     * A map storing registered Add-ons, keyed by their unique IDs.
     */
    this.registry = new Map();
  }

  /**
   * Registers a new Add-on into the SuperFlow ecosystem.
   *
   * @param {import('./BaseAddon').default} addonInstance - An instantiated subclass of BaseAddon.
   * @returns {void}
   */
  registerAddon(addonInstance) {
    if (!addonInstance || !addonInstance.id) {
      console.warn('[AddonManager] Refused to register invalid Add-on.');
      return;
    }
    this.registry.set(addonInstance.id, addonInstance);
    console.log(`[AddonManager] Successfully registered Add-on: ${addonInstance.name} (${addonInstance.id})`);
  }

  /**
   * Dispatches an execution payload to a targeted Add-on.
   *
   * @param {string} addonId - The ID of the Add-on to trigger.
   * @param {Object} payload - The stroke and context data provided by the engine.
   * @returns {Promise<boolean>} Resolves to true if dispatched successfully, false otherwise.
   */
  async dispatchPayload(addonId, payload) {
    const addon = this.registry.get(addonId);
    if (!addon) {
      console.error(`[AddonManager] Add-on with ID '${addonId}' not found in registry.`);
      return false;
    }

    try {
      console.log(`[AddonManager] Dispatching payload to ${addon.name}...`);
      await addon.execute(payload);
      return true;
    } catch (error) {
      console.error(`[AddonManager] Error executing Add-on ${addon.name}:`, error);
      return false;
    }
  }
}

export default new AddonManager();
