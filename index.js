import {AppRegistry, Image} from 'react-native';
import RNFS from 'react-native-fs';
import {PluginManager} from 'sn-plugin-lib';
import {name as appName} from './app.json';
import pluginConfig from './PluginConfig.json';
import App from './App';
import './src/i18n';

/**
 *
 */
const runBootSequence = async () => {
  let logString = '--- SUPERFLOW BOOT ---\n';
  const logFile = `${RNFS.ExternalStorageDirectoryPath}/SUPERFLOW_CRASH.txt`;

  try {
    const iconReq = require('./assets/icon/icon.png');
    let iconUri;
    try {
      const resolved = Image.resolveAssetSource(iconReq);
      iconUri = resolved ? resolved.uri : undefined;
    } catch (iconErr) {
      logString += `Failed to resolve icon URI: ${iconErr.message}\n`;
    }

    logString += 'Registering real App Component...\n';
    AppRegistry.registerComponent(appName, () => App);

    logString += 'Calling PluginManager.init()...\n';
    PluginManager.init();

    logString += 'Calling PluginManager.registerButton()...\n';
    PluginManager.registerButton(1, ['NOTE', 'DOC'], {
      id: 100,
      name: 'SuperFlow',
      showType: 1,
      ...(iconUri ? {icon: iconUri} : {}),
    });

    logString += '\n--- END OF BOOT COMPLETE (NO CRASHES CATCHED) ---';
  } catch (e) {
    logString += '\n!!! FATAL CRASH DETECTED !!!\n';
    logString += `Name: ${e.name}\n`;
    logString += `Message: ${e.message}\n`;
    logString += `Stack: ${e.stack}\n`;
  } finally {
    try {
      await RNFS.writeFile(logFile, logString, 'utf8');
      console.log(`Wrote boot log to ${logFile}`);
    } catch (fsErr) {
      console.log('Failed to write boot file:', fsErr);
    }
  }
};

runBootSequence();
