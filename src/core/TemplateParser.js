/**
 * @typedef {object} NormalizedRect
 * @property {number} x Left X coordinate.
 * @property {number} y Top Y coordinate.
 * @property {number} width Width in pixels.
 * @property {number} height Height in pixels.
 */

/**
 * @typedef {object} Hotzone
 * @property {string} id Unique identifier for the extracted zone.
 * @property {string} type Geometric type, always 'rect'.
 * @property {number} x Padded Left X coordinate.
 * @property {number} y Padded Top Y coordinate.
 * @property {number} width Padded Width in pixels.
 * @property {number} height Padded Height in pixels.
 */

/**
 * @class TemplateParser
 * @description Pure mathematical engine responsible for processing raw SDK strokes
 * into logical, clustered rectangular Action Zones (Hotzones) using graph theory.
 */
export default class TemplateParser {
  /**
   * Distance in pixels that two strokes can be apart and still cluster together.
   * Helps bridge gaps from pen lifts at corners.
   */
  static PROXIMITY_THRESHOLD = 200;

  /**
   * Minimum valid width for a Zone to avoid small noise artifacts.
   */
  static MIN_ZONE_WIDTH = 40;

  /**
   * Minimum valid height for a Zone to avoid small noise artifacts.
   */
  static MIN_ZONE_HEIGHT = 40;

  /**
   * Padding to safely expand the final Bounding Box footprint.
   */
  static ZONE_PADDING = 20;

  /**
   * Safely unwraps the raw Chauvet OS stroke element into a standardized rectangle.
   * Handles uppercase and lowercase inconsistencies across generic firmware.
   * @param {object} rawStroke The raw element object from PluginFileAPI.
   * @returns {NormalizedRect} Standardized rect geometry.
   */
  static normalizeStroke(rawStroke) {
    // Chauvet typically nests in .rect or uses flat X, Y uppercase
    const rectSource = rawStroke.rect || rawStroke || {};
    return {
      x: typeof rectSource.x === 'number' ? rectSource.x : rectSource.X || 0,
      y: typeof rectSource.y === 'number' ? rectSource.y : rectSource.Y || 0,
      width:
        typeof rectSource.width === 'number'
          ? rectSource.width
          : rectSource.Width || 0,
      height:
        typeof rectSource.height === 'number'
          ? rectSource.height
          : rectSource.Height || 0,
    };
  }

  /**
   * Evaluates if two NormalizedRect bounding boxes intersect or are close enough
   * to be considered part of the same physical drawing.
   * @param {NormalizedRect} rect1 First rectangle.
   * @param {NormalizedRect} rect2 Second rectangle.
   * @param {number} threshold The permissible gap padding.
   * @returns {boolean} True if they logically overlap/touch within the threshold.
   */
  static doRectsIntersect(rect1, rect2, threshold) {
    const r1Left = rect1.x - threshold;
    const r1Right = rect1.x + rect1.width + threshold;
    const r1Top = rect1.y - threshold;
    const r1Bottom = rect1.y + rect1.height + threshold;

    const r2Left = rect2.x;
    const r2Right = rect2.x + rect2.width;
    const r2Top = rect2.y;
    const r2Bottom = rect2.y + rect2.height;

    // Standard AABB overlapping test
    return !(
      r2Left > r1Right ||
      r2Right < r1Left ||
      r2Top > r1Bottom ||
      r2Bottom < r1Top
    );
  }

  /**
   * Parses an array of raw strokes and extracts distinct Hotzones.
   * @param {Array<object>} rawStrokes Array of standard Chauvet Elements.
   * @returns {Array<Hotzone>} Array of filtered, normalized Hotzones.
   */
  static extractHotzones(rawStrokes) {
    if (!rawStrokes || !Array.isArray(rawStrokes) || rawStrokes.length === 0) {
      return [];
    }

    const rects = rawStrokes.map(this.normalizeStroke);
    const n = rects.length;

    // Disjoint-Set Union (Union-Find) initialization
    const parent = new Array(n).fill(0).map((_, i) => i);

    /**
     * Finds the root of a set.
     * @param {number} i Node index.
     * @returns {number} Root index.
     */
    const find = i => {
      if (parent[i] === i) {
        return i;
      }
      parent[i] = find(parent[i]); // Path compression
      return parent[i];
    };

    /**
     * Unifies two sets.
     * @param {number} i First node.
     * @param {number} j Second node.
     */
    const union = (i, j) => {
      const rootI = find(i);
      const rootJ = find(j);
      if (rootI !== rootJ) {
        parent[rootJ] = rootI;
      }
    };

    // Analyze O(N^2) intersections to cluster proximate lines
    for (let i = 0; i < n; i++) {
      for (let j = i + 1; j < n; j++) {
        if (
          this.doRectsIntersect(rects[i], rects[j], this.PROXIMITY_THRESHOLD)
        ) {
          union(i, j);
        }
      }
    }

    // Group clustered rects by their root index
    const clusters = {};
    for (let i = 0; i < n; i++) {
      const root = find(i);
      if (!clusters[root]) {
        clusters[root] = [];
      }
      clusters[root].push(rects[i]);
    }

    const hotzones = [];
    const timestamp = Date.now();
    let zoneCounter = 0;

    // Consolidate Bounding Boxes for each extracted cluster
    for (const key of Object.keys(clusters)) {
      const group = clusters[key];

      let minX = Infinity;
      let minY = Infinity;
      let maxX = -Infinity;
      let maxY = -Infinity;

      for (const r of group) {
        if (r.x < minX) {
          minX = r.x;
        }
        if (r.y < minY) {
          minY = r.y;
        }
        if (r.x + r.width > maxX) {
          maxX = r.x + r.width;
        }
        if (r.y + r.height > maxY) {
          maxY = r.y + r.height;
        }
      }

      const exactWidth = maxX - minX;
      const exactHeight = maxY - minY;

      // Filter artifacts that are too physically small to be deliberate zones
      if (
        exactWidth >= this.MIN_ZONE_WIDTH &&
        exactHeight >= this.MIN_ZONE_HEIGHT
      ) {
        // Enforce padding while clamping strictly bounds to >= 0
        const paddedX = Math.max(0, minX - this.ZONE_PADDING);
        const paddedY = Math.max(0, minY - this.ZONE_PADDING);
        // Expand the bounds based on the effective shift
        const shiftedRight = maxX + this.ZONE_PADDING;
        const shiftedBottom = maxY + this.ZONE_PADDING;

        hotzones.push({
          id: `zone_${timestamp}_${zoneCounter++}`,
          type: 'rect',
          x: paddedX,
          y: paddedY,
          width: shiftedRight - paddedX,
          height: shiftedBottom - paddedY,
        });
      }
    }

    return hotzones;
  }
}
