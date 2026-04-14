import PluginAPI from '../core/PluginAPI';
import InjectKeywordAddon from './core/InjectKeywordAddon';
import CreateTitleAddon from './core/CreateTitleAddon';

/**
 * @class AddonManager
 * @description In-memory registry system compiling and routing Addon execution via the JSON engine.
 * It enforces safe execution by building the protected toolkit Payload before shipping to Addons.
 */
class AddonManager {
  /**
   * Instantiates an empty Addon Manager.
   * @class
   */
  constructor() {
    /**
     * @private
     * @type {Map<string, object>}
     */
    this._addons = new Map();
  }

  /**
   * Registers a fully initialized Addon to the executing system pool.
   * @param {object} addonInstance Concrete child class instance of BaseAddon.
   * @throws {Error} Throws if the addon lacks a valid `id` or `execute` architecture.
   */
  registerAddon(addonInstance) {
    // Basic duck-typing verification
    if (
      !addonInstance.constructor.id ||
      typeof addonInstance.execute !== 'function'
    ) {
      throw new Error('Invalid Addon instance provided to AddonManager');
    }
    this._addons.set(addonInstance.constructor.id, addonInstance);
  }

  /**
   * Retrieve all registered Add-ons for UI Configuration (e.g., when the user builds a JSON template map).
   * @returns {Array<{ id: string, name: string }>} Serialized array of addon metadata.
   */
  getAvailableAddons() {
    return Array.from(this._addons.values()).map(addon => ({
      id: addon.constructor.id,
      name: addon.constructor.name,
    }));
  }

  /**
   * Securely routes a JSON configured action command to its responsible Addon payload.
   * Automatically isolates the Addon from dangerous global state by provisioning a DI `toolkit`.
   * @async
   * @param {string} addonId The unique registered ID of the module (e.g., 'core.inject_keyword').
   * @param {object} addonParams Config settings dictating what the Addon should do (fetched from JSON).
   * @param {object} executionContext A snapshot representing the triggering context event.
   * @param {string} executionContext.activeFilePath The user's current note.
   * @param {number} executionContext.currentPageNum The user's active page.
   * @param {string} executionContext.matchedZoneId The specific Hotzone matching the strokes.
   * @param {Array<object>} executionContext.triggerStrokes The ink geometry causing this execution.
   * @returns {Promise<boolean>} True if action resolved cleanly.
   */
  async executeAction(addonId, addonParams, executionContext) {
    const addon = this._addons.get(addonId);

    if (!addon) {
      console.error(`Attempted to execute unregistered Addon: ${addonId}`);
      return false;
    }

    // Construct the Dependency Injection safe toolkit.
    // Notice how we DO NOT pass the raw PluginAPI to prevent over-reach.
    const safeToolkit = {
      /**
       * Proxy to physically inject a keyword tag.
       * @param {string} path Target .note file path.
       * @param {number} page 0-indexed page number.
       * @param {string} keyword Real text to embed into file.
       * @returns {Promise<boolean>} Success of insertion.
       */
      injectKeyword: async (path, page, keyword) => {
        return PluginAPI.injectKeyword(path, page, keyword);
      },
      /**
       * Proxy to physically inject a Title.
       * @param {string} path Target .note file path.
       * @param {number} page 0-indexed page number.
       * @param {string} title Real text to inject as Title.
       * @returns {Promise<boolean>} Success of insertion.
       */
      injectTitle: async (path, page, title) => {
        return PluginAPI.injectTitle(path, page, title);
      },
    };

    /** @type {import('./BaseAddon').ExecutePayload} */
    const payload = {
      context: executionContext,
      toolkit: safeToolkit,
      addonParams: addonParams,
    };

    try {
      await addon.execute(payload);
      return true;
    } catch (e) {
      console.error(`Execution failed for Addon [${addonId}]: ${e.message}`);
      return false;
    }
  }
}

const manager = new AddonManager();
manager.registerAddon(new InjectKeywordAddon());
manager.registerAddon(new CreateTitleAddon());

export default manager;
