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

  it('normalizes Chauvet OS type-700 ink element (recognizeResult path)', () => {
    const chauvetStroke = {
      type: 700,
      angles: {
        contoursSrc: {
          recognizeResult: {
            up_left_point_x: 46,
            up_left_point_y: 168,
            down_right_point_x: 1758,
            down_right_point_y: 170,
          },
        },
      },
    };
    expect(TemplateParser.normalizeStroke(chauvetStroke)).toEqual({
      x: 46,
      y: 168,
      width: 1712,
      height: 2,
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

  it('clusters Chauvet type-700 strokes forming a rectangle using recognizeResult coords', () => {
    // Mirrors the real flowtest.note large rectangle: full-width bar near top of page.
    // Top edge y≈168, bottom edge y≈328 (gap=158 < PROXIMITY_THRESHOLD=300).
    const chauvetRect = [
      {
        type: 700,
        angles: {
          contoursSrc: {
            recognizeResult: {
              up_left_point_x: 46,
              up_left_point_y: 168,
              down_right_point_x: 1758,
              down_right_point_y: 170,
            },
          },
        },
      }, // top
      {
        type: 700,
        angles: {
          contoursSrc: {
            recognizeResult: {
              up_left_point_x: 46,
              up_left_point_y: 326,
              down_right_point_x: 1758,
              down_right_point_y: 328,
            },
          },
        },
      }, // bottom
      {
        type: 700,
        angles: {
          contoursSrc: {
            recognizeResult: {
              up_left_point_x: 46,
              up_left_point_y: 168,
              down_right_point_x: 48,
              down_right_point_y: 328,
            },
          },
        },
      }, // left
      {
        type: 700,
        angles: {
          contoursSrc: {
            recognizeResult: {
              up_left_point_x: 1756,
              up_left_point_y: 168,
              down_right_point_x: 1758,
              down_right_point_y: 328,
            },
          },
        },
      }, // right
    ];
    const zones = TemplateParser.extractHotzones(chauvetRect);
    expect(zones.length).toBe(1);
    expect(zones[0].width).toBeGreaterThan(1700);
  });

  it('clusters sparse rectangle edges where opposite sides have large pen-lift gaps', () => {
    // Simulates a hand-drawn rectangle where opposite sides are ~177px apart —
    // larger than the old PROXIMITY_THRESHOLD (30px), within the new one (200px).
    const strokes = [
      {x: 60, y: 60, width: 180, height: 3}, // top edge
      {x: 60, y: 240, width: 180, height: 3}, // bottom edge (gap ≈ 177px from top)
      {x: 60, y: 60, width: 3, height: 180}, // left edge
      {x: 240, y: 60, width: 3, height: 180}, // right edge
    ];
    const zones = TemplateParser.extractHotzones(strokes);
    expect(zones.length).toBe(1);
    expect(zones[0].width).toBeGreaterThan(180);
    expect(zones[0].height).toBeGreaterThan(180);
  });
});
