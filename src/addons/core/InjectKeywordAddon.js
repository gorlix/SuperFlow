import BaseAddon from '../BaseAddon';

/**
 * @class InjectKeywordAddon
 * @augments BaseAddon
 * @description Native V1 reference Add-on. Acts as the blueprint for evaluating
 * strict Dependency Injection. When a hotzone mapped to this addon is triggered,
 * it parses the configured JSON text and injects it as a native Chauvet Keyword
 * into the user's current spatial writing path.
 */
export default class InjectKeywordAddon extends BaseAddon {
  static id = 'core.inject_keyword';
  static name = 'Add Keyword';

  /**
   * Dispatches the keyword injection logic ensuring total SDK separation via `toolkit`.
   * @async
   * @param {import('../BaseAddon').ExecutePayload} payload DI executing runtime block.
   * @returns {Promise<void>}
   * @throws {Error} Halts and reports cleanly if the injection encounters a proxy failure or bad parameter.
   */
  async execute(payload) {
    const {context, toolkit, addonParams} = payload;

    // 1. Data Validation: Ensure the User configured a keyword in the JSON Settings panel.
    const keywordText = addonParams?.text;
    if (
      !keywordText ||
      typeof keywordText !== 'string' ||
      keywordText.trim() === ''
    ) {
      throw new Error(
        'InjectKeywordAddon aborted: Invalid or missing "text" parameter in JSON config.',
      );
    }

    // 2. Safe Execution: Use the explicitly injected Toolkit Proxy to invoke the action
    // WITHOUT directly requiring 'sn-plugin-lib' anywhere in this file.
    const success = await toolkit.injectKeyword(
      context.activeFilePath,
      context.currentPageNum,
      keywordText.trim(),
    );

    if (!success) {
      throw new Error(
        `Execution failed: Proxy rejected keyword injection at path ${context.activeFilePath}`,
      );
    }
  }
}
