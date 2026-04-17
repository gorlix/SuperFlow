import React, {useCallback, useState} from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import {useTranslation} from 'react-i18next';
import RNFS from 'react-native-fs';
import {PluginManager} from 'sn-plugin-lib';

// Internal Core Modules (Decoupled from UI)
import SpatialMappingEngine from '../core/SpatialMappingEngine';
import PluginAPI from '../core/PluginAPI';
import TemplateParser from '../core/TemplateParser';
import ConfigManager from '../core/ConfigManager';
import {SuperFlowSettings} from '../components/SuperFlowSettings';

/**
 * @file The primary React Native interface for SuperFlow.
 * Architecture: Four-view state machine.
 *   - 'dashboard'       : Entry point with three actions.
 *   - 'template_select' : Template picker before running Process.
 *   - 'plugin_settings' : Plugin settings — lists saved flows with Edit/Delete.
 *   - 'settings'        : Zone-mapping UI shown after Learn Template or Edit flow.
 * E-Ink Optimized: Strict #000000/#FFFFFF mapping, 2px borders, zero active animations.
 */
const SuperFlowPage = () => {
  const {t} = useTranslation();

  // Root Navigation states: 'dashboard' | 'template_select' | 'plugin_settings' | 'settings'
  const [currentView, setCurrentView] = useState('dashboard');
  const [isProcessing, setIsProcessing] = useState(false);
  const [availableTemplates, setAvailableTemplates] = useState([]);
  const [flowList, setFlowList] = useState([]);
  // Where to return after the Settings zone-mapping UI closes
  const [settingsReturnView, setSettingsReturnView] = useState('dashboard');

  // ─────────────────────────────────────────────────────────────────────────
  // PROCESS flow
  // Step 1: load available template configs and open the selector view.
  // ─────────────────────────────────────────────────────────────────────────
  const handleOpenTemplateSelect = useCallback(async () => {
    setIsProcessing(true);
    try {
      const templates = await PluginAPI.listTemplateConfigs();
      setAvailableTemplates(templates);
      setCurrentView('template_select');
    } catch (e) {
      console.error('[SuperFlowPage] Failed to list templates:', e);
    } finally {
      setIsProcessing(false);
    }
  }, []);

  // Step 2: user selected a template — run the process against the current note.
  const handleProcessWithTemplate = useCallback(async templateName => {
    setCurrentView('dashboard');
    setIsProcessing(true);
    let logString = `\n--- PROCESS TRIGGERED [${templateName}] ---\n`;
    try {
      console.log(
        `[SuperFlowPage] Processing page against template: ${templateName}`,
      );

      const ctx = await PluginAPI.getActiveContext();
      logString += `Context OK. Path: ${ctx.path} | pageNum: ${ctx.pageNum}\n`;

      const diag = await SpatialMappingEngine.processActivePage(templateName);
      logString += `Mapping result: exit=${diag.exit} template=${
        diag.templateName
      } configFound=${diag.configFound} hotzones=${diag.hotzoneCount} strokes=${
        diag.strokesFound
      } zonesMatched=${diag.zonesMatched} actionsDispatched=${
        diag.actionsDispatched
      }${diag.error ? ' error=' + diag.error : ''}\n`;
    } catch (e) {
      logString += `Mapping Error: ${e.message}\nStack: ${e.stack}\n`;
      console.error('[SuperFlowPage] Process Error:', e);
    } finally {
      setIsProcessing(false);
      try {
        const logDir = `${RNFS.ExternalStorageDirectoryPath}/MyStyle/Plugins`;
        const logFile = `${logDir}/SuperFlow_Log.txt`;
        await RNFS.mkdir(logDir);
        await RNFS.appendFile(logFile, logString, 'utf8');
      } catch (fsErr) {
        console.warn('Failed to append log:', fsErr);
      }
    }
  }, []);

  // ─────────────────────────────────────────────────────────────────────────
  // LEARN TEMPLATE flow
  // Must be run while on the template file (the one containing the drawn boxes).
  // Extracts geometric shapes, opens the zone-mapping UI, saves JSON config.
  // ─────────────────────────────────────────────────────────────────────────
  /**
   *
   */
  const handleLearnTemplate = async () => {
    let logString = '\n--- LEARN TEMPLATE TRIGGERED ---\n';
    try {
      console.log('[SuperFlowPage] Switching to Learn Protocol.');

      const ctx = await PluginAPI.getActiveContext();
      logString += `Context OK. Path: ${ctx.path} | pageNum: ${ctx.pageNum}\n`;

      const allElements = await PluginAPI.getRawAllElements(
        ctx.path,
        ctx.pageNum,
      );
      const typeMap = allElements.reduce((acc, el) => {
        acc[el.type] = (acc[el.type] || 0) + 1;
        return acc;
      }, {});
      logString += `All elements (no filter): ${
        allElements.length
      } | types: ${JSON.stringify(typeMap)}\n`;

      const rawStrokes = await PluginAPI.getRawStrokes(ctx.path, ctx.pageNum);
      logString += `Raw strokes (type=700): ${rawStrokes.length}\n`;

      if (rawStrokes.length > 0) {
        let minX = Infinity,
          minY = Infinity,
          maxX = -Infinity,
          maxY = -Infinity;
        rawStrokes.forEach((s, i) => {
          const ns = TemplateParser.normalizeStroke(s);
          logString += `  Stroke[${i}]: x=${ns.x} y=${ns.y} w=${ns.width} h=${ns.height}\n`;
          if (ns.x < minX) {
            minX = ns.x;
          }
          if (ns.y < minY) {
            minY = ns.y;
          }
          if (ns.x + ns.width > maxX) {
            maxX = ns.x + ns.width;
          }
          if (ns.y + ns.height > maxY) {
            maxY = ns.y + ns.height;
          }
        });
        logString += `  Envelope: minX=${minX} minY=${minY} maxX=${maxX} maxY=${maxY}\n`;
      }

      const parsedZones = TemplateParser.extractHotzones(rawStrokes);
      logString += `Zones detected: ${parsedZones.length}\n`;
      parsedZones.forEach((z, i) => {
        logString += `  Zone[${i}]: id=${z.id} x=${z.x} y=${z.y} w=${z.width} h=${z.height}\n`;
      });

      const rawFileName = ctx.path.split('/').pop() || 'Untitled';
      const templateName = rawFileName.replace(/\.[^/.]+$/, '');

      ConfigManager.initializeDraft(templateName, parsedZones);

      setSettingsReturnView('dashboard');
      setCurrentView('settings');
    } catch (e) {
      logString += `Learn Error: ${e.message}\nStack: ${e.stack}\n`;
      console.error('[SuperFlowPage] Learn initialization failed: ', e.message);
    } finally {
      try {
        const logDir = `${RNFS.ExternalStorageDirectoryPath}/MyStyle/Plugins`;
        const logFile = `${logDir}/SuperFlow_Log.txt`;
        await RNFS.mkdir(logDir);
        await RNFS.appendFile(logFile, logString, 'utf8');
      } catch (fsErr) {
        console.warn('Failed to append learn log:', fsErr);
      }
    }
  };

  // ─────────────────────────────────────────────────────────────────────────
  // PLUGIN SETTINGS flow
  // Lists all saved flows and allows editing or deleting each.
  // ─────────────────────────────────────────────────────────────────────────
  const handleOpenPluginSettings = useCallback(async () => {
    const templates = await PluginAPI.listTemplateConfigs();
    setFlowList(templates);
    setCurrentView('plugin_settings');
  }, []);

  const handleEditFlow = useCallback(async templateName => {
    try {
      const config = await PluginAPI.loadJSONConfig(templateName);
      if (!config) {
        console.warn(`[SuperFlowPage] Config not found for: ${templateName}`);
        return;
      }
      ConfigManager.loadDraftFromConfig(templateName, config);
      setSettingsReturnView('plugin_settings');
      setCurrentView('settings');
    } catch (e) {
      console.error('[SuperFlowPage] Failed to open flow for editing:', e);
    }
  }, []);

  const handleDeleteFlow = useCallback(async templateName => {
    await PluginAPI.deleteConfig(templateName);
    const updated = await PluginAPI.listTemplateConfigs();
    setFlowList(updated);
  }, []);

  // ─────────────────────────────────────────────────────────────────────────
  // SETTINGS close — returns to the originating view and refreshes if needed.
  // ─────────────────────────────────────────────────────────────────────────
  const closeSettings = useCallback(async () => {
    const returnTo = settingsReturnView;
    setSettingsReturnView('dashboard');
    if (returnTo === 'plugin_settings') {
      const updated = await PluginAPI.listTemplateConfigs();
      setFlowList(updated);
    }
    setCurrentView(returnTo);
  }, [settingsReturnView]);

  /**
   *
   */
  const handleExit = () => {
    PluginManager.closePluginView();
  };

  // ─── Settings view (zone-mapping UI) ─────────────────────────────────────
  if (currentView === 'settings') {
    return <SuperFlowSettings onExit={closeSettings} />;
  }

  // ─── Template selector view ───────────────────────────────────────────────
  if (currentView === 'template_select') {
    return (
      <View style={styles.container}>
        <TouchableOpacity
          style={styles.exitButton}
          onPress={() => setCurrentView('dashboard')}>
          <Text style={styles.exitButtonText}>{t('ui_back', '<')}</Text>
        </TouchableOpacity>

        <Text style={styles.title}>
          {t('ui_select_template', 'Select Template')}
        </Text>
        <Text style={styles.description}>
          {t(
            'ui_select_template_desc',
            'Choose the template to apply to the current page.',
          )}
        </Text>

        <ScrollView style={styles.listFull}>
          {availableTemplates.length === 0 ? (
            <Text style={styles.emptyText}>
              {t(
                'ui_no_templates',
                'No templates found. Use "Learn Template" on a template file first.',
              )}
            </Text>
          ) : (
            availableTemplates.map(name => (
              <TouchableOpacity
                key={name}
                style={styles.listItemFull}
                onPress={() => handleProcessWithTemplate(name)}>
                <Text style={styles.listItemFullText}>{name}</Text>
              </TouchableOpacity>
            ))
          )}
        </ScrollView>
      </View>
    );
  }

  // ─── Plugin Settings view ─────────────────────────────────────────────────
  if (currentView === 'plugin_settings') {
    return (
      <View style={styles.container}>
        <TouchableOpacity
          style={styles.exitButton}
          onPress={() => setCurrentView('dashboard')}>
          <Text style={styles.exitButtonText}>{t('ui_back', '<')}</Text>
        </TouchableOpacity>

        <Text style={styles.title}>{t('ui_settings', 'Settings')}</Text>

        <Text style={styles.sectionHeader}>
          {t('ui_saved_flows', 'Saved Flows')}
        </Text>

        <ScrollView style={styles.listFull}>
          {flowList.length === 0 ? (
            <Text style={styles.emptyText}>
              {t(
                'ui_no_flows',
                'No flows saved yet. Use "Learn Template" to create one.',
              )}
            </Text>
          ) : (
            flowList.map(name => (
              <View key={name} style={styles.flowRow}>
                <Text style={styles.flowName} numberOfLines={1}>
                  {name}
                </Text>
                <TouchableOpacity
                  style={styles.flowBtnEdit}
                  onPress={() => handleEditFlow(name)}>
                  <Text style={styles.flowBtnEditText}>
                    {t('ui_edit', 'Edit')}
                  </Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.flowBtnDelete}
                  onPress={() => handleDeleteFlow(name)}>
                  <Text style={styles.flowBtnDeleteText}>
                    {t('ui_delete', 'X')}
                  </Text>
                </TouchableOpacity>
              </View>
            ))
          )}
        </ScrollView>
      </View>
    );
  }

  // ─── Dashboard (default) ──────────────────────────────────────────────────
  return (
    <View style={styles.container}>
      <TouchableOpacity style={styles.exitButton} onPress={handleExit}>
        <Text style={styles.exitButtonText}>{t('ui_close', 'X')}</Text>
      </TouchableOpacity>

      <Text style={styles.title}>{t('ui_title', 'SuperFlow Engine')}</Text>
      <Text style={styles.description}>
        {t(
          'ui_description',
          'Automate mapped spatial logic workflows dynamically.',
        )}
      </Text>

      <TouchableOpacity
        style={styles.buttonMain}
        onPress={handleOpenTemplateSelect}
        disabled={isProcessing}>
        <Text style={styles.buttonTextMain}>
          {isProcessing
            ? t('button_processing', 'Processing...')
            : t('button_process', 'Process Page')}
        </Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={styles.buttonConfig}
        onPress={handleLearnTemplate}
        disabled={isProcessing}>
        <Text style={styles.buttonTextConfig}>
          {t('button_learn', 'Learn Template')}
        </Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={styles.buttonSettings}
        onPress={handleOpenPluginSettings}
        disabled={isProcessing}>
        <Text style={styles.buttonSettingsText}>
          {t('button_settings', 'Settings')}
        </Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#ffffff',
  },
  exitButton: {
    position: 'absolute',
    top: 30,
    right: 30,
    backgroundColor: '#000000',
    width: 50,
    height: 50,
    alignItems: 'center',
    justifyContent: 'center',
  },
  exitButtonText: {
    color: '#ffffff',
    fontSize: 24,
    fontWeight: 'bold',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    marginBottom: 10,
    color: '#000000',
  },
  description: {
    fontSize: 18,
    textAlign: 'center',
    marginBottom: 40,
    color: '#000000',
  },
  // ─── Dashboard buttons ────────────────────────────────────────────────────
  buttonMain: {
    paddingVertical: 18,
    paddingHorizontal: 40,
    backgroundColor: '#000000',
    borderWidth: 2,
    borderColor: '#000000',
    marginBottom: 20,
    width: '80%',
    alignItems: 'center',
  },
  buttonTextMain: {
    color: '#ffffff',
    fontSize: 20,
    fontWeight: 'bold',
  },
  buttonConfig: {
    paddingVertical: 15,
    paddingHorizontal: 40,
    backgroundColor: '#ffffff',
    borderWidth: 2,
    borderColor: '#000000',
    marginBottom: 14,
    width: '80%',
    alignItems: 'center',
  },
  buttonTextConfig: {
    color: '#000000',
    fontSize: 16,
    fontWeight: 'bold',
  },
  buttonSettings: {
    paddingVertical: 10,
    paddingHorizontal: 40,
    backgroundColor: '#ffffff',
    width: '80%',
    alignItems: 'center',
  },
  buttonSettingsText: {
    color: '#000000',
    fontSize: 14,
    textDecorationLine: 'underline',
  },
  // ─── Shared list styles (template selector + plugin settings) ─────────────
  listFull: {
    width: '100%',
    marginTop: 10,
  },
  listItemFull: {
    paddingVertical: 18,
    paddingHorizontal: 20,
    borderWidth: 2,
    borderColor: '#000000',
    marginBottom: 12,
    backgroundColor: '#ffffff',
  },
  listItemFullText: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#000000',
  },
  emptyText: {
    fontSize: 16,
    color: '#000000',
    textAlign: 'center',
    marginTop: 20,
    paddingHorizontal: 10,
  },
  // ─── Plugin Settings — section header ────────────────────────────────────
  sectionHeader: {
    alignSelf: 'flex-start',
    fontSize: 16,
    fontWeight: 'bold',
    color: '#000000',
    marginBottom: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#000000',
    paddingBottom: 4,
    width: '100%',
  },
  // ─── Plugin Settings — flow row ───────────────────────────────────────────
  flowRow: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#000000',
    marginBottom: 12,
    backgroundColor: '#ffffff',
    paddingVertical: 10,
    paddingHorizontal: 14,
  },
  flowName: {
    flex: 1,
    fontSize: 18,
    fontWeight: 'bold',
    color: '#000000',
    marginRight: 10,
  },
  flowBtnEdit: {
    borderWidth: 2,
    borderColor: '#000000',
    paddingVertical: 8,
    paddingHorizontal: 16,
    marginRight: 8,
    backgroundColor: '#ffffff',
  },
  flowBtnEditText: {
    color: '#000000',
    fontWeight: 'bold',
    fontSize: 14,
  },
  flowBtnDelete: {
    backgroundColor: '#000000',
    width: 36,
    height: 36,
    alignItems: 'center',
    justifyContent: 'center',
  },
  flowBtnDeleteText: {
    color: '#ffffff',
    fontWeight: 'bold',
    fontSize: 16,
  },
});

export default SuperFlowPage;
