/**
 * @typedef {object} ExecutePayload
 * @property {object} context Context object representing the active execution environment.
 * @property {string} context.activeFilePath The active .note file being modified.
 * @property {number} context.currentPageNum The current 0-indexed page inside the file.
 * @property {string} context.matchedZoneId The unique ID of the matched dynamic Hotzone.
 * @property {Array<object>} context.triggerStrokes The intersection geometry bounding array that triggered this execution for future-proof OCR capability.
 * @property {object} toolkit The injected safe PluginAPI wrapper instance to execute functions securely without importing the SDK.
 * @property {function(string, number, string): Promise<boolean>} toolkit.injectKeyword Proxy to PluginAPI.injectKeyword.
 * @property {object} addonParams Configurable user-set parameters from the template JSON map.
 */

/**
 * @class BaseAddon
 * @description Standardized interface for all SuperFlow Add-ons. Addons MUST extend this class
 * and MUST rely exclusively on Dependency Injection via `payload.toolkit`. They are explicitly
 * prohibited from importing `sn-plugin-lib` or directly altering Chauvet SDK states.
 */
export default class BaseAddon {
  /**
   * The unique system identifier for this Addon (e.g., 'core.inject_keyword').
   * Must be overridden by subclasses.
   * @type {string}
   */
  static id = 'base_addon';

  /**
   * A human-readable display name for the plugin settings UI mapping dropdown.
   * Must be overridden by subclasses.
   * @type {string}
   */
  static name = 'Base Addon';

  /**
   * Main execution method. Called by AddonManager when a stroke crosses the defined Hotzone.
   * @async
   * @param {ExecutePayload} payload The injected DI container with context, api proxy, and settings.
   * @returns {Promise<void>}
   * @throws {Error} Derived classes should throw cleanly on execution failure.
   */
  async execute(payload) {
    throw new Error(
      'Method "execute" must be implemented by concrete subclass',
    );
  }
}
