import i18n from 'i18next';
import {initReactI18next} from 'react-i18next';
import * as RNLocalize from 'react-native-localize';
import en from './locales/en_US.json';
import zh from './locales/zh_CN.json';
import zhHant from './locales/zh_TW.json';
import ja from './locales/ja_JP.json';
import {PluginManager} from 'sn-plugin-lib';

const resources = {
  'en': {
    translation: en,
  },
  'zh-CN': {
    translation: zh,
  },
  'zh-TW': {
    translation: zhHant,
  },
  'ja': {
    translation: ja,
  },
};

const locales = RNLocalize.getLocales();
console.log(locales, 'localeslocaleslocales');
let systemLanguage = locales[0]?.languageTag || 'en'; // Default language is English
// Get system language
PluginManager.registerLangListener({
  onMsg: msg => {
    console.log('lange change:' + msg.lang);
    const nextLng = msg.lang.replace('_', '-');
    if(nextLng !== i18n.language) {
      i18n.changeLanguage(nextLng).catch(err => {
        console.warn('i18n.changeLanguage failed:', err);
      });
      systemLanguage = nextLng;
    }
  },
});

i18n.use(initReactI18next).init({
  resources,
  lng: systemLanguage, // Set default language
  fallbackLng: systemLanguage, // Set fallback language (used when requested language is not available)
  interpolation: {
    escapeValue: false, // Whether to escape values
  },
});

export default i18n;
