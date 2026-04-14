/**
 * @file Main entry point for the SuperFlow React Native Plugin.
 * Registers the root UI component and configured sidebar buttons with the Chauvet OS.
 */

import {AppRegistry, Image} from 'react-native';
import {name as appName} from './app.json';
import App from './App';
import './src/i18n';

import {PluginManager} from 'sn-plugin-lib';

// Register the root component UI for Supernote mapping layer.
AppRegistry.registerComponent(appName, () => App);

// Initialize SDK interactions.
PluginManager.init();

// Register the manual Trigger Button in the sidebar.
// i18n JSON strings are passed directly formatted as expected by the Chauvet OS.
PluginManager.registerButton(
  1, // Main Button ID
  ['NOTE', 'DOC'], // Array context mapping
  {
    id: 100,
    name:
      '{\n' +
      '    "en":"SuperFlow",\n' +
      '    "zh_CN":"SuperFlow",\n' +
      '    "zh_TW":"SuperFlow",\n' +
      '    "ja":"SuperFlow"\n' +
      '  }',
    color: 0xffffff,
    icon: Image.resolveAssetSource(require('./assets/icon/icon.png')).uri,
    bgColor: 0x000000,
    expandMenuItem: 0,
  },
);
