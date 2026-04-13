import React, { useCallback } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useTranslation } from 'react-i18next';

// Internal Core Modules (Decoupled from UI)
import SpatialMappingEngine from '../core/SpatialMappingEngine';
import PluginAPI from '../core/PluginAPI';

/**
 * @fileoverview The primary React Native interface for SuperFlow.
 * This component acts as the user's manual trigger point, adhering to the
 * "0ms latency" constraint by only processing ink strokes when explicitly commanded.
 */
const SuperFlowPage = () => {
  const { t } = useTranslation();

  /**
   * Orchestrates the mapping process when the user taps "Process Now".
   * It bridges the PluginAPI (stroke fetching) with the SpatialMappingEngine (intersection logic).
   *
   * @returns {Promise<void>}
   */
  const handleProcessActions = useCallback(async () => {
    console.log('[SuperFlowPage] User triggered manual process.');
    try {
      // 1. Fetch current strokes from SDK via our PluginAPI facade.
      const strokes = await PluginAPI.getCurrentPageStrokes();

      // 2. Pass them to the pure mathematical engine for intersection evaluation.
      const intersections = SpatialMappingEngine.detectIntersections(strokes);

      console.log(`[SuperFlowPage] Found ${intersections.length} action intersections.`);

      // 3. (Future) Route results to AddonManager for execution.
    } catch (error) {
      console.error('[SuperFlowPage] Processing failed:', error);
    }
  }, []);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>{t('ui_title')}</Text>
      <Text style={styles.description}>{t('ui_description')}</Text>

      <TouchableOpacity style={styles.button} onPress={handleProcessActions}>
        <Text style={styles.buttonText}>{t('button_process')}</Text>
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
    backgroundColor: '#ffffff', // Clean e-ink white
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 10,
    color: '#000000',
  },
  description: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 30,
    color: '#333333',
  },
  button: {
    paddingVertical: 15,
    paddingHorizontal: 40,
    backgroundColor: '#000000',
    borderRadius: 8,
    borderWidth: 2,
    borderColor: '#000000',
  },
  buttonText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '600',
  },
});

export default SuperFlowPage;
