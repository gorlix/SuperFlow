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
   * @returns {Promise<Array<object>>} List of valid stroke payload objects.
   * @throws {Error} If fetching file elements fails.
   */
  static async getRawStrokes(notePath, pageIndex) {
    let diag = '=== PATH NORMALIZATION & BYPASS DIAGNOSTIC ===\n';

    if (PluginNoteAPI && typeof PluginNoteAPI.saveCurrentNote === 'function') {
      try {
        await PluginNoteAPI.saveCurrentNote();
        diag += '[Sync] saveCurrentNote() succeeded.\n';
      } catch (e) {
        diag += `[Sync] saveCurrentNote() failed: ${e.message}\n`;
      }
    }

    diag += `\n[FS] Target Absolute: ${notePath}\n`;
    try {
      const exists = await RNFS.exists(notePath);
      diag += `[FS] Permissions/Exists Check: ${exists}\n`;
    } catch (e) {
      diag += `[FS] Exists Check Error: ${e.message}\n`;
    }

    try {
      const docSizeRes = await PluginFileAPI.getNoteTotalPageNum(notePath);
      diag += `[SDK] Bounds Check (Total Pages): ${JSON.stringify(
        docSizeRes,
      )}\n`;
    } catch (e) {
      diag += `[SDK] Bounds Check Error: ${e.message}\n`;
    }

    // Path variations
    const absolutePath = notePath;
    const userSpacePath = notePath.replace('/storage/emulated/0/', '');
    const relativePath = '../' + notePath.split('/').slice(-2).join('/'); // basic fallback

    diag += '\n--- SDK CALLS ---\n';
    const resAbsolute = await PluginFileAPI.getElements(
      pageIndex,
      absolutePath,
    );
    diag += `1. Absolute Path [${absolutePath}]: ${
      resAbsolute?.result?.length !== undefined
        ? resAbsolute.result.length + ' elements'
        : JSON.stringify(resAbsolute).substring(0, 50)
    }\n`;

    const resUserSpace = await PluginFileAPI.getElements(
      pageIndex,
      userSpacePath,
    );
    diag += `2. User-Space Path [${userSpacePath}]: ${
      resUserSpace?.result?.length !== undefined
        ? resUserSpace.result.length + ' elements'
        : JSON.stringify(resUserSpace).substring(0, 50)
    }\n`;

    const resRel = await PluginFileAPI.getElements(pageIndex, relativePath);
    diag += `3. Relative Path [${relativePath}]: ${
      resRel?.result?.length !== undefined
        ? resRel.result.length + ' elements'
        : JSON.stringify(resRel).substring(0, 50)
    }\n`;

    diag += '\n--- NATIVE BRIDGE BYPASS ---\n';
    const {NativeModules} = require('react-native');
    try {
      if (NativeModules.NativePluginAPI) {
        const bypassRes = await NativeModules.NativePluginAPI.getElements(
          pageIndex,
          notePath,
        );
        const bypassStr = JSON.stringify(bypassRes).substring(0, 150);
        const bypassLen = bypassRes?.result?.length;
        diag += `-> NativeModule output (len: ${bypassLen}): ${bypassStr}\n`;
      } else {
        diag += '-> NativePluginAPI missing from NativeModules\n';
      }
    } catch (e) {
      diag += `-> Native Bypass Crash: ${e.message}\n`;
    }

    throw new Error('DIAGNOSTIC HALT\n' + diag);
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
   * Injects a Title or Heading securely into the given note.
   * @async
   * @param {string} notePath Absolute target file path.
   * @param {number} page 0-based page index.
   * @param {string} titleText The text to inject as title.
   * @returns {Promise<boolean>} True if successfully injected.
   */
  static async injectTitle(notePath, page, titleText) {
    const res = await PluginNoteAPI.setTitle(notePath, page, titleText);
    return res.success === true;
  }
}
