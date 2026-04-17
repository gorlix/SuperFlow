import PluginAPI from './PluginAPI';
import AddonManager from '../addons/AddonManager';

/**
 * @typedef {object} ActionConfig
 * @property {string} id The registered Addon ID (e.g., 'core.inject_keyword').
 * @property {object} params The user-configured parameters for the execution payload.
 */

/**
 * @typedef {object} ZoneConfig
 * @property {string} id Unique Hotzone identifier.
 * @property {string} type Shape identifier ('rect').
 * @property {number} x X coordinate.
 * @property {number} y Y coordinate.
 * @property {number} width Width constraint.
 * @property {number} height Height constraint.
 * @property {Array<ActionConfig>} actions Modular actions explicitly mapped by the user to this zone.
 */

/**
 * @typedef {object} DraftState
 * @property {string} templateName The stripped note baseline filename.
 * @property {Array<ZoneConfig>} hotzones Array of zones being currently drafted across the UI.
 */

/**
 * @class ConfigManager
 * @description Singleton managing the "Template in Progress" state during the UI "Learn Phase".
 * Holds extracted math in memory, allows binding Addon actions dynamically, and safely
 * compiles the optimized JSON config into external storage.
 */
class ConfigManager {
  /**
   * Memory state singleton.
   * @class
   */
  constructor() {
    /**
     * @private
     * @type {DraftState | null}
     */
    this._draftState = null;
  }

  /**
   * Initializes a fresh draft state, preparing raw Hotzones to receive mapped actions.
   * @param {string} templateName Root filename representing this config map (e.g., "Meeting").
   * @param {Array<import('./TemplateParser').Hotzone>} parsedZones Mathematical bounding zones extracted previously.
   */
  initializeDraft(templateName, parsedZones) {
    if (!templateName || typeof templateName !== 'string') {
      throw new Error('initializeDraft requires a valid string templateName');
    }

    this._draftState = {
      templateName,
      // Map basic geometry into Config schema with an empty actions array wrapper
      hotzones: Array.isArray(parsedZones)
        ? parsedZones.map(zone => ({
            ...zone,
            actions: [],
          }))
        : [],
    };

    console.log(
      `[ConfigManager] Draft initialized for ${templateName} with ${this._draftState.hotzones.length} active spatial nodes.`,
    );
  }

  /**
   * Loads an existing saved config back into the draft for editing.
   * Unlike initializeDraft (which wipes actions), this preserves the full
   * zone geometry AND all previously mapped actions so the user can modify them.
   * @param {string} templateName Root filename of the config being edited.
   * @param {object} config The parsed JSON config object from storage.
   * @param {Array<ZoneConfig>} config.hotzones The zones with their existing action mappings.
   * @throws {Error} If templateName or config are invalid.
   */
  loadDraftFromConfig(templateName, config) {
    if (!templateName || typeof templateName !== 'string') {
      throw new Error(
        'loadDraftFromConfig requires a valid string templateName',
      );
    }
    if (!config || !Array.isArray(config.hotzones)) {
      throw new Error(
        'loadDraftFromConfig requires a valid config with a hotzones array',
      );
    }

    this._draftState = {
      templateName,
      hotzones: config.hotzones.map(zone => ({
        ...zone,
        actions: Array.isArray(zone.actions) ? zone.actions : [],
      })),
    };

    console.log(
      `[ConfigManager] Draft loaded from saved config [${templateName}] — ${this._draftState.hotzones.length} zones with existing mappings.`,
    );
  }

  /**
   * Resets the entire internal draft memory to prevent rogue state spillage
   * onto subsequent executions or templates.
   */
  clearDraft() {
    this._draftState = null;
    console.log('[ConfigManager] Draft state memory cleared safely.');
  }

  /**
   * Maps an execution instruction into a specific geometric zone.
   * Securely validates that the addon actually exists in the Manager registry.
   * @param {string} zoneId The UUID of the zone receiving the action.
   * @param {string} addonId The UUID of the registered logic module.
   * @param {object} [addonParams] User payload containing operation mechanics.
   * @throws {Error} Throws if draft is empty, zone is missing, or addon is completely unregistered.
   */
  mapActionToZone(zoneId, addonId, addonParams = {}) {
    if (!this._draftState) {
      throw new Error('No draft initialized. Cannot map action.');
    }

    // 1. Validate Geometry Matrix
    const targetZone = this._draftState.hotzones.find(z => z.id === zoneId);
    if (!targetZone) {
      throw new Error(
        `Mapping failed: Zone ID [${zoneId}] does not exist in the drafted layout.`,
      );
    }

    // 2. Addon Registry Proxy
    const validAddons = AddonManager.getAvailableAddons();
    const doesAddonExist = validAddons.some(addon => addon.id === addonId);
    if (!doesAddonExist) {
      throw new Error(
        `Security Halt: Attempted to map unregistered or corrupted plugin [${addonId}].`,
      );
    }

    // 3. Draft Injection
    targetZone.actions.push({
      id: addonId,
      params: addonParams,
    });
    console.log(
      `[ConfigManager] Hooked Addon [${addonId}] into Zone [${zoneId}]`,
    );
  }

  /**
   * Allows UI detachment of an accidentally mapped action.
   * @param {string} zoneId The target Zone ID.
   * @param {string} addonId The target Addon ID to strip out.
   */
  removeActionFromZone(zoneId, addonId) {
    if (!this._draftState) {
      return;
    }

    const targetZone = this._draftState.hotzones.find(z => z.id === zoneId);
    if (targetZone && Array.isArray(targetZone.actions)) {
      targetZone.actions = targetZone.actions.filter(
        action => action.id !== addonId,
      );
      console.log(
        `[ConfigManager] Extracted Addon [${addonId}] from Zone [${zoneId}]`,
      );
    }
  }

  /**
   * Compiles the in-memory draft down into final JSON state.
   * Vigorously prunes out zones lacking active mapping actions to ensure absolute
   * 0ms engine processing speeds later on. Finally, ships the optimized payload
   * directly to persistent RNFS storage.
   * @async
   * @returns {Promise<boolean>} True if output payload was successfully saved.
   */
  async compileAndSave() {
    if (!this._draftState) {
      console.warn(
        '[ConfigManager] Aborting compile order: Draft state is null.',
      );
      return false;
    }

    // Prune logic: Filter out zones that have 0 mapped actions. (Clean footprint optimization)
    const activeHotzones = this._draftState.hotzones.filter(
      zone => zone.actions && zone.actions.length > 0,
    );

    const optimizedJSONTree = {
      hotzones: activeHotzones,
    };

    console.log(
      `[ConfigManager] Compiling optimized AST... [Pruned ${
        this._draftState.hotzones.length - activeHotzones.length
      } empty zones]`,
    );

    // Asynchronous Storage Execution
    const isSuccess = await PluginAPI.saveJSONConfig(
      this._draftState.templateName,
      optimizedJSONTree,
    );

    if (isSuccess) {
      console.log(
        '[ConfigManager] ✅ Template mapped and saved permanently as JSON.',
      );
      this.clearDraft(); // Hard wipe memory for security & leak prevention
      return true;
    } else {
      console.error(
        '[ConfigManager] ❌ RNFS save instruction failed fundamentally.',
      );
      return false;
    }
  }
}

export default new ConfigManager();
