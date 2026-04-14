import {AppRegistry, Image, View} from 'react-native';
import RNFS from 'react-native-fs';
import {PluginManager} from 'sn-plugin-lib';
import {name as appName} from './app.json';
import pluginConfig from './PluginConfig.json';

/**
 *
 */
const runDiagnosticBoot = async () => {
  let logString = '--- SUPERFLOW DIAGNOSTIC BOOT ---\n';
  const logFile = `${RNFS.ExternalStorageDirectoryPath}/SUPERFLOW_CRASH.txt`;

  try {
    // 3. String Integrity Check
    logString += `App Name (app.json): "${appName}"\n`;
    logString += `Plugin Key (Config): "${pluginConfig.pluginKey}"\n`;
    logString += `Keys Identical?: ${appName === pluginConfig.pluginKey}\n\n`;

    // 4. Asset Resolution Audit
    const iconReq = require('./assets/icon/icon.png');
    logString += `Icon required value (ID): ${iconReq}\n`;
    let iconUri = null;
    try {
      const resolved = Image.resolveAssetSource(iconReq);
      iconUri = resolved ? resolved.uri : null;
      logString += `Icon URI resolved: ${iconUri}\n\n`;
    } catch (iconErr) {
      logString += `Failed to resolve icon URI: ${iconErr.message}\n\n`;
    }

    // 1. Naked Boot Registration
    logString += 'Registering dummy Component...\n';
    AppRegistry.registerComponent(appName, () => {
      return function DiagnosticApp() {
        return <View style={{flex: 1, backgroundColor: 'blue'}} />;
      };
    });

    logString += 'Calling PluginManager.init()...\n';
    PluginManager.init();

    logString += 'Calling PluginManager.registerButton()...\n';
    PluginManager.registerButton(1, ['NOTE', 'DOC'], {
      id: 100,
      name: 'SuperFlow',
      showType: 1,
      // If icon resolution failed, fallback to undefined so it doesn't crash the dictionary
      ...(iconUri ? {icon: iconUri} : {}),
    });

    logString += '\n--- END OF BOOT COMPLETE (NO CRASHES CATCHED) ---';
  } catch (e) {
    // 2. The try-catch Trap
    logString += '\n!!! FATAL CRASH DETECTED !!!\n';
    logString += `Name: ${e.name}\n`;
    logString += `Message: ${e.message}\n`;
    logString += `Stack: ${e.stack}\n`;
  } finally {
    try {
      // Create external directory if it somehow doesn't exist, though typically SDCard root does.
      await RNFS.writeFile(logFile, logString, 'utf8');
      console.log(`Wrote diagnostic log to ${logFile}`);
    } catch (fsErr) {
      console.log('Failed to write diagnostic file:', fsErr);
    }
  }
};

runDiagnosticBoot();
