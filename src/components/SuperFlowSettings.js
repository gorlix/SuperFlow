import React, {useState} from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
} from 'react-native';
import {useTranslation} from 'react-i18next';
import ConfigManager from '../core/ConfigManager';
import AddonManager from '../addons/AddonManager';

/**
 *
 * @param root0
 * @param root0.addonMeta
 * @param root0.onSave
 * @param root0.onCancel
 */
const DynamicAddonForm = ({addonMeta, onSave, onCancel}) => {
  const {t} = useTranslation();
  const [formData, setFormData] = useState({});
  const addon = AddonManager._addons.get(addonMeta.id); // Direct link for metadata usage

  /**
   *
   */
  const handleSave = () => {
    onSave(formData);
  };

  return (
    <View style={styles.formContainer}>
      <Text style={styles.title}>
        {t('ui_configuration', 'Configuration: {{name}}', {
          name: addon.constructor.name,
        })}
      </Text>

      {addon.constructor.settingsSchema &&
        addon.constructor.settingsSchema.map(field => {
          if (field.type === 'string') {
            return (
              <View key={field.key} style={styles.fieldContainer}>
                <Text style={styles.label}>{field.label}</Text>
                <TextInput
                  style={styles.input}
                  placeholder={field.placeholder || ''}
                  placeholderTextColor="#666666"
                  value={formData[field.key] || ''}
                  onChangeText={text =>
                    setFormData({...formData, [field.key]: text})
                  }
                />
              </View>
            );
          }
          if (field.type === 'boolean') {
            const isToggled = !!formData[field.key];
            return (
              <View key={field.key} style={styles.fieldContainer}>
                <Text style={styles.label}>{field.label}</Text>
                <TouchableOpacity
                  style={[
                    styles.toggleBox,
                    isToggled && styles.toggleBoxActive,
                  ]}
                  onPress={() =>
                    setFormData({...formData, [field.key]: !isToggled})
                  }>
                  <Text
                    style={[
                      styles.toggleText,
                      isToggled && styles.toggleTextActive,
                    ]}>
                    {isToggled ? t('ui_yes', 'YES') : t('ui_no', 'NO')}
                  </Text>
                </TouchableOpacity>
              </View>
            );
          }
          return null;
        })}

      <View style={styles.actions}>
        <TouchableOpacity style={styles.btnCancel} onPress={onCancel}>
          <Text style={styles.btnCancelText}>{t('ui_cancel', 'Cancel')}</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.btnSave} onPress={handleSave}>
          <Text style={styles.btnSaveText}>
            {t('ui_confirm', 'Confirm & Apply')}
          </Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

/**
 *
 * @param root0
 * @param root0.onSelect
 * @param root0.onCancel
 */
const AddonPicker = ({onSelect, onCancel}) => {
  const {t} = useTranslation();
  const addons = AddonManager.getAvailableAddons();

  return (
    <ScrollView style={styles.listContainer}>
      <Text style={styles.title}>
        {t('ui_select_action', 'Select Action Logic')}
      </Text>
      {addons.map(addon => (
        <TouchableOpacity
          key={addon.id}
          style={styles.listItem}
          onPress={() => onSelect(addon)}>
          <Text style={styles.listItemText}>{addon.name}</Text>
        </TouchableOpacity>
      ))}
      <TouchableOpacity
        style={[styles.btnCancel, {marginTop: 20}]}
        onPress={onCancel}>
        <Text style={styles.btnCancelText}>{t('ui_go_back', 'Go Back')}</Text>
      </TouchableOpacity>
    </ScrollView>
  );
};

/**
 *
 * @param root0
 * @param root0.onExit
 */
export const SuperFlowSettings = ({onExit}) => {
  const {t} = useTranslation();
  // Navigation states: 'zone_list', 'addon_picker', 'dynamic_form'
  const [viewState, setViewState] = useState('zone_list');
  const [selectedZoneUI, setSelectedZoneUI] = useState(null);
  const [selectedAddonMeta, setSelectedAddonMeta] = useState(null);

  // Local trigger forces UI refresh
  const [, forceUpdate] = useState(0);

  const draft = ConfigManager._draftState;

  if (!draft) {
    return (
      <View style={styles.container}>
        <Text style={styles.errorText}>
          {t('ui_fatal_draft', 'Fatal: No draft state materialized in memory.')}
        </Text>
      </View>
    );
  }

  /**
   *
   * @param zoneId
   */
  const handlePickAddonForZone = zoneId => {
    setSelectedZoneUI(zoneId);
    setViewState('addon_picker');
  };

  /**
   *
   * @param addonMeta
   */
  const handleSelectAddon = addonMeta => {
    setSelectedAddonMeta(addonMeta);
    setViewState('dynamic_form');
  };

  /**
   *
   * @param params
   */
  const handleSaveActionForm = params => {
    ConfigManager.mapActionToZone(selectedZoneUI, selectedAddonMeta.id, params);
    // Return to the zone mapping list
    setViewState('zone_list');
    forceUpdate(n => n + 1); // Fast rerender trigger mapping preview
  };

  /**
   *
   */
  const handleCommitJSON = async () => {
    const success = await ConfigManager.compileAndSave();
    if (success) {
      onExit();
    }
  };

  if (viewState === 'addon_picker') {
    return (
      <AddonPicker
        onSelect={handleSelectAddon}
        onCancel={() => setViewState('zone_list')}
      />
    );
  }

  if (viewState === 'dynamic_form') {
    return (
      <DynamicAddonForm
        addonMeta={selectedAddonMeta}
        onSave={handleSaveActionForm}
        onCancel={() => setViewState('addon_picker')}
      />
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>
        {t('ui_map_draft', 'Map Draft [{{name}}]', {name: draft.templateName})}
      </Text>
      <ScrollView style={{width: '100%', marginBottom: 20}}>
        {draft.hotzones.map(zone => (
          <View key={zone.id} style={styles.zoneCard}>
            <Text style={styles.cardHeader}>
              {t('ui_zone', 'Zone: {{id}}', {id: zone.id})}
            </Text>
            <Text style={styles.cardMetrics}>
              {t(
                'ui_geometry',
                'Geometry: x({{x}}) y({{y}}) w({{w}}) h({{h}})',
                {x: zone.x, y: zone.y, w: zone.width, h: zone.height},
              )}
            </Text>

            <View style={styles.actionPills}>
              {zone.actions.map((act, index) => (
                <View key={index} style={styles.pillDecoration}>
                  <Text style={styles.pillText}>{act.id}</Text>
                </View>
              ))}
            </View>

            <TouchableOpacity
              style={styles.btnSecondary}
              onPress={() => handlePickAddonForZone(zone.id)}>
              <Text style={styles.btnSecondaryText}>
                {t('ui_assign_action', '+ Assign Action')}
              </Text>
            </TouchableOpacity>
          </View>
        ))}
      </ScrollView>

      <View style={styles.actions}>
        <TouchableOpacity
          style={styles.btnCancel}
          onPress={() => {
            ConfigManager.clearDraft();
            onExit();
          }}>
          <Text style={styles.btnCancelText}>
            {t('ui_discard_draft', 'Discard Draft')}
          </Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.btnSave} onPress={handleCommitJSON}>
          <Text style={styles.btnSaveText}>
            {t('ui_save_compile', 'Save & Compile JSON')}
          </Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#ffffff',
    padding: 20,
    width: '100%',
  },
  listContainer: {
    flex: 1,
    backgroundColor: '#ffffff',
    padding: 20,
  },
  formContainer: {
    flex: 1,
    backgroundColor: '#ffffff',
    padding: 20,
  },
  title: {
    fontSize: 22,
    fontWeight: '700',
    color: '#000000',
    marginBottom: 20,
    borderBottomWidth: 2,
    borderBottomColor: '#000000',
    paddingBottom: 10,
  },
  errorText: {
    fontSize: 18,
    color: '#000000',
    fontWeight: 'bold',
  },
  zoneCard: {
    borderWidth: 2,
    borderColor: '#000000',
    padding: 15,
    marginBottom: 15,
    backgroundColor: '#ffffff',
  },
  cardHeader: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#000000',
  },
  cardMetrics: {
    fontSize: 12,
    color: '#333333',
    marginBottom: 10,
  },
  actionPills: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 10,
  },
  pillDecoration: {
    borderWidth: 1,
    borderColor: '#000000',
    paddingHorizontal: 8,
    paddingVertical: 4,
    marginRight: 5,
    marginBottom: 5,
  },
  pillText: {
    fontSize: 12,
    color: '#000000',
  },
  fieldContainer: {
    marginBottom: 20,
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    color: '#000000',
    marginBottom: 5,
  },
  input: {
    borderWidth: 2,
    borderColor: '#000000',
    padding: 12,
    fontSize: 16,
    color: '#000000',
    backgroundColor: '#ffffff',
  },
  listItem: {
    borderWidth: 2,
    borderColor: '#000000',
    padding: 15,
    marginBottom: 10,
  },
  listItemText: {
    fontSize: 18,
    color: '#000000',
  },
  actions: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    width: '100%',
    paddingTop: 10,
  },
  btnSecondary: {
    alignSelf: 'flex-start',
    borderWidth: 2,
    borderColor: '#000000',
    paddingVertical: 8,
    paddingHorizontal: 15,
  },
  btnSecondaryText: {
    color: '#000000',
    fontWeight: 'bold',
  },
  btnSave: {
    flex: 1,
    marginLeft: 10,
    backgroundColor: '#000000',
    borderWidth: 2,
    borderColor: '#000000',
    padding: 15,
    alignItems: 'center',
  },
  btnSaveText: {
    color: '#ffffff',
    fontWeight: 'bold',
    fontSize: 16,
  },
  btnCancel: {
    flex: 1,
    marginRight: 10,
    backgroundColor: '#ffffff',
    borderWidth: 2,
    borderColor: '#000000',
    padding: 15,
    alignItems: 'center',
  },
  btnCancelText: {
    color: '#000000',
    fontWeight: 'bold',
    fontSize: 16,
  },
  toggleBox: {
    borderWidth: 2,
    borderColor: '#000000',
    padding: 12,
    backgroundColor: '#ffffff',
    alignItems: 'center',
  },
  toggleBoxActive: {
    backgroundColor: '#000000',
  },
  toggleText: {
    color: '#000000',
    fontSize: 16,
    fontWeight: 'bold',
  },
  toggleTextActive: {
    color: '#ffffff',
  },
});
