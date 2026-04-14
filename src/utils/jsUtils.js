import i18n from '../i18n';
import Constant from './Constant';

/**
 *
 * @param path
 * @param newGroupId
 */
export function splitBySecondLastSlash(path, newGroupId) {
  console.log(path, newGroupId, 'splitBySecondLastSlash');
  const regex = /sticker_group_\d+/g; // Match legacy identifier pattern
  // Replace the whole matched segment directly
  return path.replace(regex, newGroupId);
}

// Extract file name
/**
 *
 * @param path
 */
export const extractFileName = path => {
  // Match file name (excluding path and extension)
  const match = path.match(/([^/]*)\.[^.]+$/);
  // If matched, return the first capture group (file name); otherwise return empty string
  return match ? match[1] : '';
};

/**
 *
 * @param fileName
 */
export function incrementFileName(fileName) {
  console.log(fileName, 'splitBySecondLastSlash');

  // Match prefix, optional "_<number>" (capture group), and extension
  const regex = /(.*?)_(?:(\d+))?(\..*)?$/;
  const match = fileName.match(regex);

  if (match) {
    // Extract prefix, number, and extension
    const prefix = match[1];
    const numberStr = match[2] || '0'; // Default to '0' when absent
    const extension = match[3] || '';

    // Parse the number and increment
    const number = parseInt(numberStr, 10);
    const newNumber = number + 1;

    // Build the new file name
    return `${prefix}_${newNumber}${extension}`;
  } else {
    // If not matched (rare), append 1 to the original file name
    let str = fileName.split('.');
    return `${str[0]}1.${str[1]}`;
  }
}
/**
 *
 * @param str1
 * @param str2
 */
export function findStrNum(str1, str2) {
  // Find the first index where strings differ in length or character
  let diffIndex = -1;
  for (let i = 0; i < Math.min(str1.length, str2.length); i++) {
    if (str1[i] !== str2[i]) {
      diffIndex = i;
      break;
    }
  }
  // If no difference found but lengths differ, the diff is at the end of the longer string
  if (diffIndex === -1 && str1.length !== str2.length) {
    diffIndex = Math.max(str1.length, str2.length);
  }

  // Extract the differing part
  let diffPart = '';
  if (diffIndex !== -1 && diffIndex < str1.length) {
    diffPart = str1.slice(diffIndex);
  } else if (diffIndex !== -1 && diffIndex < str2.length) {
    diffPart = str2.slice(diffIndex);
  } else {
    // If reached here, strings are completely different; handle as needed
    return false;
  }

  // Check whether the differing part is numeric
  const numericRegex = /^\d+$/; // Matches a string of digits only
  return numericRegex.test(diffPart);
}

// Suffix for auto-generated sticker names
const stickerNameTail = '_sticker';

/// Get auto-generated sticker name when creating a sticker
/**
 *
 * @param arr
 * @param stickerGroupName
 * @param extractor
 */
export const findNextAutoCreateStickerName = (
  arr, // All stickers in the current group
  stickerGroupName, // Current sticker group name
  extractor = item => item.name,
) => {
  console.log(
    'findNextAutoCreateStickerName stickerGroupName',
    stickerGroupName,
  );
  let baseStr = stickerGroupName + stickerNameTail;
  // Truncate group name if the base name exceeds the max length
  if (baseStr.length > Constant.maxInputNum) {
    var cutLength = baseStr.length - Constant.maxInputNum;
    stickerGroupName = stickerGroupName.slice(
      0,
      stickerGroupName.length - cutLength,
    );
    baseStr = stickerGroupName + stickerNameTail;
  }

  console.log('findNextAutoCreateStickerName arr', arr);
  console.log('findNextAutoCreateStickerName baseStr', baseStr);
  if (
    !Array.isArray(arr) ||
    arr.length === 0 ||
    !arr.some(item => item.name === baseStr)
  ) {
    return baseStr; // If base name doesn't exist, return it directly
  }
  // Extract existing numeric suffixes
  const existingNumbers = arr
    .map(item => {
      const extractedStr = extractor(item).trim();
      if (extractedStr === baseStr) {
        return 0;
      }
      const match = extractedStr.match(/\(\d+\)$/);
      if (match) {
        // Extract the number
        const matchNum = match[0].match(/\d+/);
        return matchNum ? parseInt(matchNum[0], 10) : null;
      } else {
        return null;
      }
    })
    .filter(num => num !== null)
    .sort((a, b) => a - b); // Sort to find the first unused number
  console.log(
    'findNextAutoCreateStickerName existingNumbers:',
    existingNumbers,
  );
  if (!existingNumbers || existingNumbers.length <= 0) {
    return baseStr;
  }
  // Find the first unused number
  for (let i = 1; i <= 10000; i++) {
    // Assume we won't use very large suffix numbers
    if (!existingNumbers.includes(i)) {
      let tailStr = `(${i})`;
      let newName = baseStr + tailStr;
      if (newName.length > Constant.maxInputNum) {
        var cutLength = newName.length - Constant.maxInputNum;
        return findNextAutoCreateStickerName(
          arr,
          stickerGroupName.slice(0, stickerGroupName.length - 1),
          extractor,
        );
      }

      return `${baseStr}(${i})`;
    }
  }
  return baseStr + '(-1)';
};

/**
 *
 * @param arr
 * @param baseStr
 * @param extractor
 */
export const findNextEm = (arr, baseStr, extractor = item => item.name) => {
  if (
    !Array.isArray(arr) ||
    arr.length === 0 ||
    !arr.some(item => item.name === baseStr)
  ) {
    return baseStr; // If base name doesn't exist, return it directly
  }
  // Extract existing numeric suffixes
  const existingNumbers = arr
    .map(item => {
      const extractedStr = extractor(item).trim();
      const match = extractedStr.match(/\(\d+\)$/);
      if (match) {
        // Extract the number
        const matchNum = match[0].match(/\d+/);
        return matchNum ? parseInt(matchNum[0], 10) : null;
      } else {
        return null;
      }
    })
    .filter(num => num !== null)
    .sort((a, b) => a - b); // Sort to find the first unused number
  console.log('findNextEm existingNumbers:', existingNumbers);
  if (!existingNumbers || existingNumbers.length <= 0) {
    return baseStr;
  }

  // Find the first unused number
  for (let i = 1; i <= 10000; i++) {
    // Assume we won't use very large suffix numbers
    if (!existingNumbers.includes(i)) {
      return `${baseStr}(${i})`;
    }
  }
  return baseStr + ' ' + baseStr;
};
/**
 *
 * @param arr
 * @param baseStr
 */
export const findNext2Em = (arr, baseStr) => {
  if (
    !Array.isArray(arr) ||
    arr.length === 0 ||
    !arr.some(item => item.name === baseStr)
  ) {
    return baseStr; // If base name doesn't exist, return it directly
  }

  // Iterate numbers to generate a unique new name
  for (let i = 1; i <= 100; i++) {
    let newName = `${baseStr}(${i})`;
    // Use some() to check if the name already exists
    if (!arr.some(item => item.name === newName)) {
      return newName; // Unique name found
    }
  }
  return `${baseStr}(${101})`;
};

// Sort helper
/**
 *
 * @param arr
 * @param ascending
 */
export function sortByName(arr, ascending = true) {
  return arr.sort((a, b) => {
    if (ascending) {
      return a.name.localeCompare(b.name);
    } else {
      return b.name.localeCompare(a.name);
    }
  });
}

/**
 *
 * @param filePath
 */
export function extractFileInfo(filePath) {
  // Extract file name from path (excluding extension)
  const nameMatch = filePath.match(/([^\/]+)(?=\.[^\/]+$)/);
  const name = nameMatch ? nameMatch[0] : 'Unknown'; // Fallback name when not matched

  return {
    name: name,
    path: filePath,
  };
}
/**
 *
 * @param num
 */
export function incrementIfDecimal(num) {
  // Check whether the input is a decimal
  if (Number.isInteger(num)) {
    // If it's an integer, return as-is
    return num;
  } else {
    // If it has a fractional part, round to nearest integer and add 1
    return Math.round(num) + 1;
  }
}
/**
 *
 * @param input
 */
export const splitWhole = input => {
  // Find index of the last space
  const lastIndex = input.lastIndexOf(' ');

  // If no space found, return the whole string
  if (lastIndex === -1) {
    return input;
  }

  // Extract the second part and trim
  const secondPartTrimmed = input.slice(lastIndex + 1).trim();

  // Check whether the second part is numeric
  const isSecondPartNumber = /^\d+$/.test(secondPartTrimmed);

  // If numeric, return the first part (trimmed)
  if (isSecondPartNumber) {
    return input.slice(0, lastIndex).trimEnd();
  }

  // Otherwise return the whole string
  return input;
};
/**
 *
 * @param array
 * @param title
 * @param size
 */
const groupArray = (array, title, size) => {
  const result = [];
  for (let i = 0; i < array.length; i += size) {
    const chunk = array.slice(i, i + size);
    result.push({title, data: chunk});
  }
  return result;
};
/**
 *
 * @param arr
 * @param chunkSize
 */
export const chunkArray = (arr, chunkSize) => {
  return arr.reduce((acc, curr, idx) => {
    const chunkIndex = Math.floor(idx / chunkSize);
    if (!acc[chunkIndex]) {
      acc[chunkIndex] = [];
    }
    acc[chunkIndex].push(curr);
    return acc;
  }, []);
};

// Parse built-in sticker name JSON
/**
 *
 * @param jsonStr
 */
export const parseNameJson = jsonStr => {
  try {
    let lang = i18n.language;
    let obj = JSON.parse(jsonStr);
    if (lang === 'zh-CN') {
      return obj.zh_CN;
    } else if (lang === 'zh-TW') {
      return obj.zh_TW;
    } else if (lang.includes('ja')) {
      return obj.ja;
    } else if (lang.includes('en')) {
      return obj.en;
    }
  } catch (e) {
    return jsonStr;
  }
};

export const RegExpArr = {
  special: /[/<>*?|\\:`\"].*/,
  specialEmoji:
    /(?:[\u2700-\u27bf]|(?:\ud83c[\udde6-\uddff]){2}|[\ud800-\udbff][\udc00-\udfff])[\ufe0e\ufe0f]?(?:[\u0300-\u036f\ufe20-\ufe23\u20d0-\u20f0]|\ud83c[\udffb-\udfff])?(?:\u200d(?:[^\ud800-\udfff]|(?:\ud83c[\udde6-\uddff]){2}|[\ud800-\udbff][\udc00-\udfff])[\ufe0e\ufe0f]?(?:[\u0300-\u036f\ufe20-\ufe23\u20d0-\u20f0]|\ud83c[\udffb-\udfff])?)*/,
};

/**
 *
 * @param array
 * @param chunkSize
 */
export const checkArray = (array, chunkSize) => {
  const result = [];
  for (let i = 0; i < array.length; i += chunkSize) {
    const chunk = array.slice(i, i + chunkSize);
    result.push(chunk);
  }
  return result;
};
