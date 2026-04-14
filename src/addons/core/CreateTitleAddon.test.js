import CreateTitleAddon from './CreateTitleAddon';
import {createTestPayload} from '../../utils/test-utils';

describe('CreateTitleAddon Scalability Test', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const addon = new CreateTitleAddon();

  it('aborts execution if text parameter is missing or empty', async () => {
    const payload = createTestPayload({});
    await addon.execute(payload);
    expect(payload.toolkit.injectTitle).not.toHaveBeenCalled();

    // Testing empty string
    const emptyPayload = createTestPayload({text: '   '});
    await addon.execute(emptyPayload);
    expect(emptyPayload.toolkit.injectTitle).not.toHaveBeenCalled();
  });

  it('successfully triggers toolkit.injectTitle with boolean flags (Normal Title)', async () => {
    const payload = createTestPayload({
      text: 'My Daily Note',
      isHeading: false,
    });
    await addon.execute(payload);

    expect(payload.toolkit.injectTitle).toHaveBeenCalledWith(
      '/MyStyle/template/test.note',
      0,
      'My Daily Note',
    );
  });

  it('successfully triggers toolkit.injectTitle with boolean flags (Heading)', async () => {
    const payload = createTestPayload({text: 'My Heading', isHeading: true});
    await addon.execute(payload);

    expect(payload.toolkit.injectTitle).toHaveBeenCalledWith(
      '/MyStyle/template/test.note',
      0,
      'My Heading',
    );
  });
});
