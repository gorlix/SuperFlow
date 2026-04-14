import {NativePluginManager} from 'sn-plugin-lib';

const Constant = {
  maxInputNum: 20,

  // Plugin path cache
  _pluginPathCache: null,

  /**
   * Get the plugin directory path (with caching).
   * @returns {Promise<string>} Plugin directory path
   */
  async getPluginPath() {
    // If cached, return the cached value directly
    if (this._pluginPathCache !== null) {
      return this._pluginPathCache;
    }

    try {
      // Fetch plugin path and cache it
      this._pluginPathCache = await NativePluginManager.getPluginDirPath();
      return this._pluginPathCache;
    } catch (error) {
      console.error('Failed to get plugin path:', error);
      throw error;
    }
  },

  /**
   * Clear the plugin path cache (optional, for special cases).
   */
  clearPluginPathCache() {
    this._pluginPathCache = null;
  },
};

export default Constant;
