/**
 * @fileoverview Base class for all SuperFlow Add-ons.
 * This class establishes a strict interface that third-party developers must implement to create an Add-on.
 * By extending this class, developers ensure their Add-on conforms to the expected execution lifecycle.
 */

export default class BaseAddon {
  /**
   * Constructs the Add-on with standard metadata.
   *
   * @param {Object} config - Configuration object for the Add-on.
   * @param {string} config.id - A unique identifier for the Add-on (e.g., 'obsidian_sync').
   * @param {string} config.name - The human-readable name of the Add-on.
   * @param {React.Component|null} [config.settingsComponent] - Optional UI component for configuring the Add-on.
   */
  constructor({ id, name, settingsComponent = null }) {
    if (!id || !name) {
      throw new Error('[BaseAddon] Add-on must have a valid \'id\' and \'name\'.');
    }

    /** @type {string} Unique identifier. */
    this.id = id;
    /** @type {string} Display name. */
    this.name = name;
    /** @type {React.Component|null} Panel for the user to configure Add-on settings. */
    this.settingsComponent = settingsComponent;
  }

  /**
   * The core execution lifecycle hook. This method is called by the AddonManager
   * when an Action Zone assigned to this Add-on detects a pen stroke.
   *
   * @abstract
   * @param {Object} payload - Data from the SpatialMappingEngine.
   * @param {Array<Object>} payload.strokes - The handwriting strokes intersecting the zone.
   * @param {Object} payload.context - Details about the current page and notebook.
   * @returns {Promise<void>} Resolves when execution completes.
   * @throws {Error} Method must be implemented by subclasses.
   */
  async execute(payload) {
    throw new Error(`[BaseAddon] execute() method not implemented for addon: ${this.id}`);
  }
}
