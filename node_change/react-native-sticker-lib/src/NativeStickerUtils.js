// @flow
import type { TurboModule } from 'react-native/Libraries/TurboModule/RCTExport';
import { TurboModuleRegistry } from 'react-native';
// Encrypt/decrypt sticker files
export interface Spec extends TurboModule {
    encodeStickerZip(path:string):Promise<boolean>;

    decodeStickerZip(path:string):Promise<boolean>;

    // Check whether a file is a sticker zip
    checkStickerZip(path:string):Promise<boolean>;

  
}
export default (TurboModuleRegistry.get<Spec>(
  'RTNStickerUtils'
): ?Spec);
