jest.mock('./PluginAPI');
import PluginAPI from './PluginAPI';
import ConfigManager from './ConfigManager';
import AddonManager from '../addons/AddonManager';

jest.mock('../addons/AddonManager', () => ({
  getAvailableAddons: jest.fn(),
}));

describe('ConfigManager', () => {
  beforeEach(() => {
    ConfigManager.clearDraft();
    jest.clearAllMocks();
  });

  it('initializes draft cleanly', () => {
    ConfigManager.initializeDraft('TestTemplate', [{id: 'z1'}]);
    expect(ConfigManager._draftState.hotzones.length).toBe(1);
    expect(ConfigManager._draftState.hotzones[0].actions).toEqual([]);
  });

  it('maps valid addons safely', () => {
    ConfigManager.initializeDraft('TestTemplate', [{id: 'z1'}]);
    AddonManager.getAvailableAddons.mockReturnValue([{id: 'valid_addon'}]);

    ConfigManager.mapActionToZone('z1', 'valid_addon', {data: 123});
    expect(ConfigManager._draftState.hotzones[0].actions).toHaveLength(1);
    expect(ConfigManager._draftState.hotzones[0].actions[0].id).toBe(
      'valid_addon',
    );
  });

  it('throws error when mapping invalid addon', () => {
    ConfigManager.initializeDraft('TestTemplate', [{id: 'z1'}]);
    AddonManager.getAvailableAddons.mockReturnValue([{id: 'valid_addon'}]);

    expect(() => {
      ConfigManager.mapActionToZone('z1', 'invalid_addon', {});
    }).toThrow(/Security Halt/);
  });

  it('prunes empty zones inside compileAndSave', async () => {
    ConfigManager.initializeDraft('TestTemplate', [
      {id: 'empty_zone'},
      {id: 'active_zone'},
    ]);
    AddonManager.getAvailableAddons.mockReturnValue([{id: 'addon1'}]);
    ConfigManager.mapActionToZone('active_zone', 'addon1');

    PluginAPI.saveJSONConfig.mockResolvedValue(true);
    await ConfigManager.compileAndSave();

    expect(PluginAPI.saveJSONConfig).toHaveBeenCalledWith('TestTemplate', {
      hotzones: expect.arrayContaining([
        expect.objectContaining({id: 'active_zone'}),
      ]),
    });

    // Extract what was exactly passed
    const passedZones = PluginAPI.saveJSONConfig.mock.calls[0][1].hotzones;
    expect(passedZones).toHaveLength(1);
    expect(ConfigManager._draftState).toBeNull(); // memory cleared
  });
});
