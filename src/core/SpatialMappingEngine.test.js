jest.mock('./PluginAPI');
import PluginAPI from './PluginAPI';
import SpatialMappingEngine from './SpatialMappingEngine';
import AddonManager from '../addons/AddonManager';

jest.mock('../addons/AddonManager', () => ({
  executeAction: jest.fn(),
  getAvailableAddons: jest.fn(),
}));

describe('SpatialMappingEngine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('safely extracts template name from absolute filepath', () => {
    const name = SpatialMappingEngine._extractTemplateName(
      '/storage/emulated/0/Note/Meeting.note',
    );
    expect(name).toBe('Meeting');
  });

  it('AABB math correctly detects stroke inclusions', () => {
    const strokeInside = {x: 50, y: 50, width: 10, height: 10};
    const strokeOutside = {x: 200, y: 50, width: 10, height: 10};
    const zone = {x: 0, y: 0, width: 100, height: 100};

    expect(SpatialMappingEngine._doAABBIntersect(strokeInside, zone)).toBe(
      true,
    );
    expect(SpatialMappingEngine._doAABBIntersect(strokeOutside, zone)).toBe(
      false,
    );
  });

  it('processes mapped hotzones correctly through AddonManager payload execution', async () => {
    PluginAPI.getActiveContext.mockResolvedValue({
      path: '/Test.note',
      pageNum: 0,
    });
    PluginAPI.loadJSONConfig.mockResolvedValue({
      hotzones: [
        {
          id: 'mock_zone',
          x: 0,
          y: 0,
          width: 100,
          height: 100,
          actions: [{id: 'mock_addon', params: {}}],
        },
      ],
    });
    PluginAPI.getRawStrokes.mockResolvedValue([
      {x: 10, y: 10, width: 5, height: 5},
      {X: 500, Y: 500, Width: 10, Height: 10},
    ]);

    await SpatialMappingEngine.processActivePage();

    expect(AddonManager.executeAction).toHaveBeenCalledTimes(1);
    expect(AddonManager.executeAction).toHaveBeenCalledWith(
      'mock_addon',
      expect.any(Object),
      expect.objectContaining({matchedZoneId: 'mock_zone'}),
    );
  });
});
