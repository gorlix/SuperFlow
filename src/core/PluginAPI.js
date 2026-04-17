import {PluginCommAPI, PluginFileAPI, PluginNoteAPI} from 'sn-plugin-lib';
import RNFS from 'react-native-fs';

/**
 * @class PluginAPI
 * @description Safe boundary wrapper for Supernote SDK and local storage.
 * All core spatial logic and Addon execution MUST use this facade instead of
 * directly importing `sn-plugin-lib` to ensure safe operation.
 */
export default class PluginAPI {
  /**
   * Generates the configuration path in the public external storage.
   * @param {string} configName The name of the template config (e.g., 'Meeting').
   * @returns {string} The full valid public absolute path to the JSON file.
   */
  static _getConfigPath(configName) {
    return `${RNFS.ExternalStorageDirectoryPath}/MyStyle/template/configs/${configName}.json`;
  }

  /**
   * Gets the active Chauvet file context.
   * @async
   * @returns {Promise<{ path: string, pageNum: number }>} The current note context.
   * @throws {Error} If fetching the active file context fails.
   */
  static async getActiveContext() {
    const [pathRes, pageRes] = await Promise.all([
      PluginCommAPI.getCurrentFilePath(),
      PluginCommAPI.getCurrentPageNum(),
    ]);

    if (!pathRes.success || !pageRes.success) {
      throw new Error('Failed to retrieve active Chauvet context');
    }

    return {
      path: pathRes.result,
      pageNum: pageRes.result,
    };
  }

  /**
   * Retrieves raw strokes (trails) from a specified Supernote file.
   * @async
   * @param {string} notePath Absolute path to the .note file.
   * @param {number} pageIndex 0-indexed page number.
   * @param {number} [elementType] Element type to filter: 700 = geometric shapes (zone outlines),
   *   0 = freehand ink strokes (user handwriting inside zones).
   * @returns {Promise<Array<object>>} List of valid stroke payload objects.
   * @throws {Error} If fetching file elements fails.
   */
  static async getRawStrokes(notePath, pageIndex, elementType = 700) {
    let saveStatus = 'skipped';
    if (PluginNoteAPI && typeof PluginNoteAPI.saveCurrentNote === 'function') {
      try {
        await PluginNoteAPI.saveCurrentNote();
        saveStatus = 'ok';
      } catch (e) {
        saveStatus = `failed: ${e.message}`;
        console.warn(`[PluginAPI] saveCurrentNote failed: ${e.message}`);
      }
    }

    const response = await PluginFileAPI.getElements(pageIndex, notePath);
    const rawResult =
      response && Array.isArray(response.result) ? response.result : [];

    // ElementDataAccessor objects from the native bridge do not support plain
    // property access. Materializing via JSON round-trip creates plain JS objects
    // where nested paths (angles.contoursSrc.recognizeResult etc.) are accessible.
    const allElements = rawResult.map(el => {
      try {
        return JSON.parse(JSON.stringify(el));
      } catch (_) {
        return el;
      }
    });

    // Filter to the requested element type.
    // type=700: geometric shapes (zone outlines, used by Learn Template)
    // type=0:   freehand ink strokes (user handwriting, used by Process)
    const strokes = allElements.filter(el => el.type === elementType);
    const typeMap = allElements.reduce((acc, el) => {
      acc[el.type] = (acc[el.type] || 0) + 1;
      return acc;
    }, {});
    console.log(
      `[PluginAPI] saveCurrentNote=${saveStatus} | page=${pageIndex} | allElements=${
        allElements.length
      } types=${JSON.stringify(typeMap)} | strokes(type=${elementType})=${
        strokes.length
      }`,
    );

    return strokes;
  }

  /**
   * Diagnostic-only: returns ALL elements from a page without type filtering.
   * Used to surface raw SDK response data (type distribution, total count) to
   * the device log for debugging zero-stroke scenarios.
   * @async
   * @param {string} notePath Absolute path to the .note file.
   * @param {number} pageIndex 0-indexed page number.
   * @returns {Promise<Array<object>>} All raw element objects before any filtering.
   */
  static async getRawAllElements(notePath, pageIndex) {
    try {
      const response = await PluginFileAPI.getElements(pageIndex, notePath);
      return response && Array.isArray(response.result) ? response.result : [];
    } catch (e) {
      console.error(`[PluginAPI] getRawAllElements failed: ${e.message}`);
      return [];
    }
  }

  /**
   * Saves the UI dynamic zone mapping config.
   * @async
   * @param {string} templateName Base name of the config (e.g., "Daily Planner").
   * @param {object} data The compiled JSON Action Mapping.
   * @returns {Promise<boolean>} True if the save was successful.
   */
  static async saveJSONConfig(templateName, data) {
    try {
      const configPath = this._getConfigPath(templateName);
      const uriParts = configPath.split('/');
      uriParts.pop(); // Remove trailing file name

      const dirPath = uriParts.join('/');

      const dirExists = await RNFS.exists(dirPath);
      if (!dirExists) {
        await RNFS.mkdir(dirPath);
      }

      await RNFS.writeFile(configPath, JSON.stringify(data, null, 2), 'utf8');
      return true;
    } catch (e) {
      console.error(`saveJSONConfig failed: ${e.message}`);
      return false;
    }
  }

  /**
   * Loads the configured JSON map.
   * @async
   * @param {string} templateName Base config name.
   * @returns {Promise<object | null>} The parsed JSON config, or null if missing.
   */
  static async loadJSONConfig(templateName) {
    try {
      const configPath = this._getConfigPath(templateName);
      const fileExists = await RNFS.exists(configPath);
      if (!fileExists) {
        return null; // Return null safely if the template has no config
      }
      const rawText = await RNFS.readFile(configPath, 'utf8');
      return JSON.parse(rawText);
    } catch (e) {
      console.error(`loadJSONConfig failed: ${e.message}`);
      return null;
    }
  }

  /**
   * Lists all available template config names from the configs directory.
   * @async
   * @returns {Promise<Array<string>>} Array of template base names (without .json extension).
   */
  static async listTemplateConfigs() {
    try {
      const configDir = `${RNFS.ExternalStorageDirectoryPath}/MyStyle/template/configs`;
      const exists = await RNFS.exists(configDir);
      if (!exists) {
        return [];
      }
      const items = await RNFS.readDir(configDir);
      return items
        .filter(item => item.isFile() && item.name.endsWith('.json'))
        .map(item => item.name.replace(/\.json$/, ''));
    } catch (e) {
      console.error(`[PluginAPI] listTemplateConfigs failed: ${e.message}`);
      return [];
    }
  }

  /**
   * Permanently deletes a saved template config from storage.
   * @async
   * @param {string} templateName Base config name to remove (e.g., "Meeting").
   * @returns {Promise<boolean>} True if the file was deleted or did not exist.
   */
  static async deleteConfig(templateName) {
    try {
      const configPath = this._getConfigPath(templateName);
      const exists = await RNFS.exists(configPath);
      if (!exists) {
        return true;
      }
      await RNFS.unlink(configPath);
      return true;
    } catch (e) {
      console.error(`[PluginAPI] deleteConfig failed: ${e.message}`);
      return false;
    }
  }

  /**
   * Injects a keyword tag securely into the given note.
   * @async
   * @param {string} notePath Absolute target file path.
   * @param {number} page 0-based page index.
   * @param {string} keyword The tag/keyword text to inject.
   * @returns {Promise<boolean>} True if successfully injected.
   */
  static async injectKeyword(notePath, page, keyword) {
    const res = await PluginFileAPI.insertKeyWord(notePath, page, keyword);
    return res.success === true;
  }

  /**
   * Injects a Title text box into the currently active note page.
   * @async
   * @param {string} notePath Accepted for API symmetry with injectKeyword — not forwarded.
   *   The SDK's insertText operates on the current view; there is no file-addressed text API.
   * @param {number} page Accepted for API symmetry — not forwarded (see above).
   * @param _page
   * @param _notePath
   * @param {string} titleText The text content to insert.
   * @returns {Promise<boolean>} True if successfully injected.
   */
  static async injectTitle(_notePath, _page, titleText) {
    const res = await PluginNoteAPI.insertText({
      textContentFull: titleText,
      textRect: {left: 50, top: 50, right: 1354, bottom: 200},
      fontSize: 36,
      textBold: 1,
    });
    return !!(res && res.success);
  }
}
