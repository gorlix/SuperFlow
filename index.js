import {AppRegistry, Image} from 'react-native';
import {name as appName} from './app.json';
import App from './App';
import './src/i18n';
import {PluginManager} from 'sn-plugin-lib';

// Register the root component UI matching the pluginKey
AppRegistry.registerComponent(appName, () => App);

// Initialize SDK interactions
PluginManager.init();

// Register the manual Trigger Button in the sidebar
PluginManager.registerButton(1, ['NOTE', 'DOC'], {
  id: 100,
  name: 'SuperFlow',
  icon: Image.resolveAssetSource(require('./assets/icon/icon.png')).uri,
  showType: 1,
});
