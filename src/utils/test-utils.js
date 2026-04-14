/**
 * Test Utility to mock Addon execution payloads.
 * @param {object} addonParams Config options passed into the addon
 * @param {object} configOptions Engine payload defaults
 * @returns {import('../addons/BaseAddon').ExecutePayload} Mocked payload
 */
export function createTestPayload(addonParams = {}, configOptions = {}) {
  return {
    context: {
      activeFilePath: configOptions.path || '/MyStyle/template/test.note',
      currentPageNum: configOptions.page || 0,
      matchedZoneId: configOptions.zoneId || 'zone_1',
      triggerStrokes: configOptions.strokes || [],
    },
    toolkit: {
      injectKeyword: jest.fn().mockResolvedValue(true),
    },
    addonParams,
  };
}
