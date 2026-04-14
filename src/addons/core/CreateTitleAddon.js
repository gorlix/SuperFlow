import BaseAddon from '../BaseAddon';

/**
 * @class CreateTitleAddon
 * @description Injects a text Title/Heading into the note's indices to test multi-schema arrays including booleans.
 */
export default class CreateTitleAddon extends BaseAddon {
  static id = 'core.create_title';
  static name = 'Inject Heading / Title';

  static settingsSchema = [
    {
      key: 'text',
      type: 'string',
      label: 'Title Text',
      placeholder: 'Enter title here...',
    },
    {
      key: 'isHeading',
      type: 'boolean',
      label: 'Set as Heading?',
    },
  ];

  /**
   * Executes title injection logic.
   * @async
   * @param {import('../BaseAddon').ExecutePayload} payload The protected execution payload
   */
  async execute(payload) {
    const {context, addonParams, toolkit} = payload;

    if (
      !addonParams ||
      typeof addonParams.text !== 'string' ||
      !addonParams.text.trim()
    ) {
      console.warn(
        'CreateTitleAddon aborted: Invalid or missing "text" parameter in JSON config.',
      );
      return;
    }

    if (addonParams.isHeading) {
      console.log(
        `[CreateTitleAddon] Executing as Heading: ${addonParams.text}`,
      );
    } else {
      console.log(
        `[CreateTitleAddon] Executing as Normal Title: ${addonParams.text}`,
      );
    }

    try {
      const success = await toolkit.injectTitle(
        context.activeFilePath,
        context.currentPageNum,
        addonParams.text,
      );

      if (!success) {
        console.error(
          'CreateTitleAddon aborted: toolkit.injectTitle returned false',
        );
      }
    } catch (e) {
      console.error(`CreateTitleAddon Execution Error: ${e.message}`);
    }
  }
}
