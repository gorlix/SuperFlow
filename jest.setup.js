jest.mock('sn-plugin-lib', () => ({
  PluginCommAPI: {},
  PluginFileAPI: {},
}));

jest.mock('react-native-fs', () => ({
  writeFile: jest.fn(),
  readFile: jest.fn(),
}));
