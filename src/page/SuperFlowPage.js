import React, {useCallback, useState} from 'react';
import {View, Text, StyleSheet, TouchableOpacity} from 'react-native';
import {useTranslation} from 'react-i18next';

// Internal Core Modules (Decoupled from UI)
import SpatialMappingEngine from '../core/SpatialMappingEngine';
import PluginAPI from '../core/PluginAPI';
import TemplateParser from '../core/TemplateParser';
import ConfigManager from '../core/ConfigManager';
import {SuperFlowSettings} from '../components/SuperFlowSettings';

/**
 * @file The primary React Native interface for SuperFlow.
 * Architecture: True "Obsidian-style" execution view with State-Switched Config hidden inside Settings context bounds.
 * E-Ink Optimized: Strict #000000/#FFFFFF mapping, 2px borders, zero active animations.
 */
const SuperFlowPage = () => {
  const {t} = useTranslation();

  // Root Navigation states: 'dashboard' | 'settings'
  const [currentView, setCurrentView] = useState('dashboard');

  /**
   * Orchestrates the mapping process when the user taps "Process Now".
   */
  const handleProcessActions = useCallback(async () => {
    console.log('[SuperFlowPage] Processing triggered on Dashboard.');
    await SpatialMappingEngine.processActivePage();
  }, []);

  /**
   * Bridges to the Settings Interface (Phase 3 & 4 binding config mode).
   */
  const handleLearnTemplate = async () => {
    try {
      console.log('[SuperFlowPage] Switching to Learn Protocol.');

      const ctx = await PluginAPI.getActiveContext();
      const rawStrokes = await PluginAPI.getRawStrokes(ctx.path, ctx.pageNum);

      const parsedZones = TemplateParser.extractHotzones(rawStrokes);

      const rawFileName = ctx.path.split('/').pop() || 'Untitled';
      const templateName = rawFileName.replace(/\.[^/.]+$/, '');

      ConfigManager.initializeDraft(templateName, parsedZones);

      setCurrentView('settings');
    } catch (e) {
      console.error('[SuperFlowPage] Learn initialization failed: ', e.message);
    }
  };

  /**
   * Safe view un-mounter when Config Manager concludes a save operation.
   */
  const closeSettings = () => {
    setCurrentView('dashboard');
  };

  if (currentView === 'settings') {
    return <SuperFlowSettings onExit={closeSettings} />;
  }

  // Pure E-ink Dashboard Route
  return (
    <View style={styles.container}>
      <Text style={styles.title}>{t('ui_title') || 'SuperFlow Engine'}</Text>
      <Text style={styles.description}>
        {t('ui_description') ||
          'Automate mapped spatial logic workflows dynamically.'}
      </Text>

      <TouchableOpacity
        style={styles.buttonMain}
        onPress={handleProcessActions}>
        <Text style={styles.buttonTextMain}>
          {t('button_process') || 'Process Active Page'}
        </Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={styles.buttonConfig}
        onPress={handleLearnTemplate}>
        <Text style={styles.buttonTextConfig}>
          {t('button_settings') || 'Learn Template Map'}
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
    backgroundColor: '#ffffff', // High Contrast
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
    width: '80%',
    alignItems: 'center',
  },
  buttonTextConfig: {
    color: '#000000',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

export default SuperFlowPage;
