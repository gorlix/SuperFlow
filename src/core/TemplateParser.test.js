import TemplateParser from './TemplateParser';

describe('TemplateParser', () => {
  it('normalizes geometry inputs', () => {
    const rawUpper = {X: 10, Y: 20, Width: 100, Height: 50};
    const rawLower = {rect: {x: 10, y: 20, width: 100, height: 50}};

    expect(TemplateParser.normalizeStroke(rawUpper)).toEqual({
      x: 10,
      y: 20,
      width: 100,
      height: 50,
    });
    expect(TemplateParser.normalizeStroke(rawLower)).toEqual({
      x: 10,
      y: 20,
      width: 100,
      height: 50,
    });
  });

  it('clusters intersecting/proximate strokes via Union-Find and applies proper padding', () => {
    const strokes = [
      {x: 10, y: 10, width: 100, height: 5}, // Top edge
      {x: 10, y: 15, width: 5, height: 95}, // Left edge touching top
      {x: 100, y: 10, width: 5, height: 90}, // Right edge close but not touching
      {x: 15, y: 100, width: 90, height: 5}, // Bottom edge touching left
    ];

    const zones = TemplateParser.extractHotzones(strokes);
    expect(zones.length).toBe(1);
    expect(zones[0].x).toBe(0); // 10 - 20 (padded), clamped to 0
    expect(zones[0].y).toBe(0); // 10 - 20 (padded), clamped to 0
    expect(zones[0].width).toBe(130);
    expect(zones[0].height).toBe(130);
  });

  it('drops artifacts smaller than minimum valid dimensions', () => {
    const smallDot = [{x: 50, y: 50, width: 5, height: 5}];
    const zones = TemplateParser.extractHotzones(smallDot);
    expect(zones.length).toBe(0);
  });
});
