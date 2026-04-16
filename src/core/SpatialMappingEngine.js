import PluginAPI from './PluginAPI';
import TemplateParser from './TemplateParser';
import AddonManager from '../addons/AddonManager';

/**
 * @class SpatialMappingEngine
 * @description The core operational brain of SuperFlow. Dispatched manually via the UI
 * sidebar. Cross references live ink with the stored JSON action maps and rapidly triggers
 * isolated modular Addons through AABB collision routing.
 */
class SpatialMappingEngine {
  /**
   * Parses the raw absolute Chauvet filepath to securely extract the trailing filename
   * minus its document extension (.note).
   * @param {string} filepath Absolute device path (e.g. /storage/emulated/0/Note/Meeting.note).
   * @returns {string} Stripped base name (e.g. "Meeting").
   */
  _extractTemplateName(filepath) {
    if (!filepath) {
      return '';
    }
    const nameWithExt = filepath.split('/').pop() || '';
    return nameWithExt.replace(/\.[^/.]+$/, '');
  }

  /**
   * Computes standard Axis-Aligned Bounding Box (AABB) intersection between two geometries.
   * Allows any partial overlap to trigger `true`, preventing strict inclusion misses
   * common in fast organic handwriting.
   * @param {import('./TemplateParser').NormalizedRect} stroke A sanitized ink box representation.
   * @param {import('./TemplateParser').Hotzone} zone The geometric constraints of an Action map block.
   * @returns {boolean} True if geometries collide.
   */
  _doAABBIntersect(stroke, zone) {
    return (
      stroke.x <= zone.x + zone.width &&
      stroke.x + stroke.width >= zone.x &&
      stroke.y <= zone.y + zone.height &&
      stroke.y + stroke.height >= zone.y
    );
  }

  /**
   * Brain function for the entire "Process Phase" of SuperFlow.
   * Intended to be invoked directly from the floating widget sidebar when user completes their page.
   * Resolves the environment context, fetches mapped logic dynamically, parses on-page ink elements
   * and routes intersections logically into isolated Addons via the manager Singleton.
   * @async
   * @returns {Promise<void>} Resolves when all mapped zone actions are sequentially processed.
   */
  async processActivePage() {
    const diag = {
      exit: 'unknown',
      templateName: null,
      configFound: false,
      hotzoneCount: 0,
      strokesFound: 0,
      zonesMatched: 0,
      actionsDispatched: 0,
      error: null,
    };

    try {
      // 1. Context Logging
      console.log(
        '[SpatialMappingEngine] 🚀 Commencing Process Phase execution...',
      );
      const ctx = await PluginAPI.getActiveContext();

      // 2. Map Resolving
      const templateName = this._extractTemplateName(ctx.path);
      diag.templateName = templateName;
      if (!templateName) {
        console.warn(
          '[SpatialMappingEngine] Active file string is malformed or invalid.',
        );
        diag.exit = 'bad_path';
        return diag;
      }

      console.log(
        `[SpatialMappingEngine] Resolving compiled actions for Template: [${templateName}]`,
      );
      const config = await PluginAPI.loadJSONConfig(templateName);

      if (!config || !config.hotzones || !Array.isArray(config.hotzones)) {
        console.info(
          `[SpatialMappingEngine] Clean exit. No mapped configuration exists for ${templateName}.json`,
        );
        diag.exit = 'no_config';
        return diag;
      }

      diag.configFound = true;
      diag.hotzoneCount = config.hotzones.length;

      // 3. Raw Data Pulling
      const rawStrokes = await PluginAPI.getRawStrokes(ctx.path, ctx.pageNum);
      diag.strokesFound = rawStrokes.length;

      if (rawStrokes.length === 0) {
        console.info(
          '[SpatialMappingEngine] Page ink context is completely blank. Halting execution.',
        );
        diag.exit = 'no_strokes';
        return diag;
      }

      const normalizedStrokes = rawStrokes.map(TemplateParser.normalizeStroke);

      // 4. AABB Collision Analysis & Action Dispatching Loop
      for (const zone of config.hotzones) {
        // Collect strokes that have overlapped with this specific Zone geometry.
        const intersectingStrokes = normalizedStrokes.filter(stroke =>
          this._doAABBIntersect(stroke, zone),
        );

        if (intersectingStrokes.length > 0) {
          diag.zonesMatched++;
          console.log(
            `[SpatialMappingEngine] ✅ Detected ${intersectingStrokes.length} stroke(s) spanning inside Zone [${zone.id}]`,
          );

          if (!Array.isArray(zone.actions)) {
            continue; // Edge protection against malicious/malformed manual JSON edits
          }

          const executionContext = {
            activeFilePath: ctx.path,
            currentPageNum: ctx.pageNum,
            matchedZoneId: zone.id,
            triggerStrokes: intersectingStrokes,
          };

          // Route all configured Addon commands linked to this Zone independently
          for (const actionConfig of zone.actions) {
            console.log(
              `[SpatialMappingEngine] ⚙️ Executing Addon Payload => ${actionConfig.id}`,
            );
            diag.actionsDispatched++;

            // Execute safely isolated in AddonManager Singleton proxy.
            await AddonManager.executeAction(
              actionConfig.id,
              actionConfig.params || {},
              executionContext,
            );
          }
        }
      }

      diag.exit = 'done';
      console.log(
        '[SpatialMappingEngine] 🏁 Processing complete. Zero-latency manual iteration finished.',
      );
    } catch (e) {
      diag.exit = 'error';
      diag.error = e.message;
      console.error(
        `[CRITICAL] SpatialMappingEngine encountered a fatal error during Process Phase: ${e.message}`,
      );
    }

    return diag;
  }
}

export default new SpatialMappingEngine();
