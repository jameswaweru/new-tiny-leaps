/*
 * Tiny Leaps picture library (shared by the puzzle and painting games; each game ships its own copy).
 * Every picture is a list of shapes on a 400x400 canvas: [tag, attributes, fill].
 *   fill "#hex"   -> paintable region (white in painting mode)
 *   fill "!#hex"  -> fixed colour (eyes, shines...) never painted
 *   fill null     -> outline only
 */
(function () {
  'use strict';
  var INK = '#3D3357';

  var E = function (tag, a, fill, text) { return text == null ? [tag, a, fill] : [tag, a, fill, text]; };
  var sky = function (c) { return E('rect', { x: 0, y: 0, width: 400, height: 400 }, c || '#B3E5FC'); };
  var ground = function (c, y) { return E('rect', { x: 0, y: y || 320, width: 400, height: 400 - (y || 320) }, c || '#9CCC65'); };
  var sun = function () { return E('circle', { cx: 335, cy: 65, r: 34 }, '#FFEE58'); };
  var cloud = function (x, y) {
    return E('path', { d: 'M' + x + ' ' + y + 'a22 22 0 0 1 40-12a26 26 0 0 1 48 6a18 18 0 0 1 2 36h-84a18 18 0 0 1-6-30z' }, '#FFFFFF');
  };
  var eye = function (x, y, r) {
    return [E('circle', { cx: x, cy: y, r: r }, '!' + INK), E('circle', { cx: x + r * 0.35, cy: y - r * 0.35, r: r * 0.35, stroke: 'none' }, '!#FFFFFF')];
  };
  var cheek = function (x, y) { return E('ellipse', { cx: x, cy: y, rx: 13, ry: 9, stroke: 'none' }, '!#F8BBD0'); };
  var line = function (d, w) { return E('path', { d: d, 'stroke-width': w || 5 }, null); };
  var flat = function (arr) { return [].concat.apply([], arr.map(function (x) { return Array.isArray(x[0]) ? x : [x]; })); };

  var animals = [
    { id: 'cat', name: 'Kitty', shapes: flat([
      sky('#E1F5FE'), ground('#AED581', 340), cloud(40, 70),
      E('path', { d: 'M282 330 Q368 312 345 228 Q336 200 318 222 Q330 292 268 300 Z' }, '#FFA726'),
      E('ellipse', { cx: 200, cy: 292, rx: 95, ry: 80 }, '#FFB74D'),
      E('ellipse', { cx: 200, cy: 305, rx: 52, ry: 56 }, '#FFE0B2'),
      E('polygon', { points: '132,130 146,52 198,100' }, '#FFA726'),
      E('polygon', { points: '268,130 254,52 202,100' }, '#FFA726'),
      E('polygon', { points: '146,112 152,76 180,100' }, '#F8BBD0'),
      E('polygon', { points: '254,112 248,76 220,100' }, '#F8BBD0'),
      E('circle', { cx: 200, cy: 170, r: 82 }, '#FFB74D'),
      eye(170, 162, 13), eye(230, 162, 13), cheek(146, 196), cheek(254, 196),
      E('polygon', { points: '190,188 210,188 200,200' }, '!#F06292'),
      line('M200 200 Q192 214 180 207 M200 200 Q208 214 220 207', 4),
      line('M120 180 L80 172 M120 192 L80 196 M280 180 L320 172 M280 192 L320 196', 3),
      E('ellipse', { cx: 162, cy: 366, rx: 30, ry: 17 }, '#FFB74D'),
      E('ellipse', { cx: 238, cy: 366, rx: 30, ry: 17 }, '#FFB74D')
    ]) },
    { id: 'frog', name: 'Froggy', shapes: flat([
      sky('#B3E5FC'), E('ellipse', { cx: 200, cy: 370, rx: 230, ry: 70 }, '#4FC3F7'), sun(),
      E('ellipse', { cx: 200, cy: 345, rx: 165, ry: 36 }, '#81C784'),
      E('ellipse', { cx: 200, cy: 272, rx: 112, ry: 78 }, '#66BB6A'),
      E('ellipse', { cx: 200, cy: 288, rx: 66, ry: 50 }, '#DCEDC8'),
      E('circle', { cx: 142, cy: 138, r: 40 }, '#66BB6A'),
      E('circle', { cx: 258, cy: 138, r: 40 }, '#66BB6A'),
      E('ellipse', { cx: 200, cy: 192, rx: 104, ry: 70 }, '#66BB6A'),
      E('circle', { cx: 142, cy: 134, r: 27 }, '!#FFFFFF'), E('circle', { cx: 258, cy: 134, r: 27 }, '!#FFFFFF'),
      eye(146, 136, 13), eye(254, 136, 13), cheek(130, 212), cheek(270, 212),
      line('M150 206 Q200 248 250 206', 5),
      E('ellipse', { cx: 118, cy: 340, rx: 42, ry: 17 }, '#66BB6A'),
      E('ellipse', { cx: 282, cy: 340, rx: 42, ry: 17 }, '#66BB6A')
    ]) },
    { id: 'fish', name: 'Bubbles the Fish', shapes: flat([
      sky('#4FC3F7'), ground('#FFE082', 345),
      E('path', { d: 'M40 345 Q20 290 45 250 Q60 290 62 345 Z' }, '#66BB6A'),
      E('path', { d: 'M350 345 Q330 280 360 230 Q378 290 372 345 Z' }, '#66BB6A'),
      E('circle', { cx: 330, cy: 110, r: 12 }, '#E1F5FE'), E('circle', { cx: 352, cy: 76, r: 8 }, '#E1F5FE'),
      E('circle', { cx: 340, cy: 46, r: 5 }, '#E1F5FE'),
      E('polygon', { points: '112,200 38,138 38,262' }, '#FF8A65'),
      E('path', { d: 'M175 132 Q220 66 268 136 Z' }, '#FF8A65'),
      E('path', { d: 'M196 266 Q224 322 252 266 Z' }, '#FF8A65'),
      E('ellipse', { cx: 210, cy: 200, rx: 122, ry: 82 }, '#FFB74D'),
      E('ellipse', { cx: 184, cy: 200, rx: 16, ry: 74 }, '#FFF3E0'),
      E('ellipse', { cx: 140, cy: 200, rx: 13, ry: 58 }, '#FFF3E0'),
      E('circle', { cx: 282, cy: 180, r: 20 }, '!#FFFFFF'), eye(285, 182, 10), cheek(292, 222),
      line('M322 214 Q312 228 300 220', 4)
    ]) },
    { id: 'elephant', name: 'Ellie the Elephant', shapes: flat([
      sky('#E1F5FE'), ground('#AED581', 345), sun(), cloud(200, 50),
      line('M330 258 Q362 270 356 300', 6),
      E('rect', { x: 118, y: 290, width: 46, height: 78, rx: 14 }, '#90A4AE'),
      E('rect', { x: 290, y: 290, width: 46, height: 78, rx: 14 }, '#90A4AE'),
      E('ellipse', { cx: 225, cy: 268, rx: 115, ry: 78 }, '#90A4AE'),
      E('rect', { x: 175, y: 305, width: 46, height: 70, rx: 14 }, '#90A4AE'),
      E('rect', { x: 240, y: 305, width: 46, height: 70, rx: 14 }, '#90A4AE'),
      E('ellipse', { cx: 196, cy: 178, rx: 56, ry: 70 }, '#B0BEC5'),
      E('ellipse', { cx: 198, cy: 182, rx: 36, ry: 50 }, '#F8BBD0'),
      E('circle', { cx: 140, cy: 186, r: 72 }, '#90A4AE'),
      E('path', { d: 'M86 214 Q52 262 66 312 Q76 334 96 320 Q84 282 114 240 Z' }, '#90A4AE'),
      eye(122, 172, 11), cheek(110, 212)
    ]) }
  ];

  var road = function () {
    return [E('rect', { x: 0, y: 318, width: 400, height: 82 }, '#78909C'),
      E('rect', { x: 20, y: 370, width: 60, height: 10, rx: 5, stroke: 'none' }, '!#FFFFFF'),
      E('rect', { x: 170, y: 370, width: 60, height: 10, rx: 5, stroke: 'none' }, '!#FFFFFF'),
      E('rect', { x: 320, y: 370, width: 60, height: 10, rx: 5, stroke: 'none' }, '!#FFFFFF')];
  };
  var wheel = function (x, y, r) {
    return [E('circle', { cx: x, cy: y, r: r }, '#455A64'), E('circle', { cx: x, cy: y, r: r * 0.42 }, '#CFD8DC')];
  };

  var cars = [
    { id: 'car', name: 'Red Car', shapes: flat([
      sky(), sun(), cloud(40, 60), road(),
      E('path', { d: 'M108 214 L150 140 L272 140 L314 214 Z' }, '#EF5350'),
      E('rect', { x: 40, y: 208, width: 322, height: 86, rx: 32 }, '#EF5350'),
      E('polygon', { points: '128,206 160,152 206,152 206,206' }, '#E1F5FE'),
      E('polygon', { points: '216,206 216,152 262,152 294,206' }, '#E1F5FE'),
      E('circle', { cx: 344, cy: 240, r: 12 }, '#FFF59D'),
      E('rect', { x: 40, y: 232, width: 16, height: 24, rx: 4 }, '#FFCA28'),
      wheel(118, 296, 36), wheel(286, 296, 36)
    ]) },
    { id: 'bus', name: 'School Bus', shapes: flat([
      sky(), sun(), road(),
      E('rect', { x: 28, y: 128, width: 346, height: 176, rx: 28 }, '#FFCA28'),
      E('rect', { x: 50, y: 154, width: 58, height: 56, rx: 10 }, '#E1F5FE'),
      E('rect', { x: 120, y: 154, width: 58, height: 56, rx: 10 }, '#E1F5FE'),
      E('rect', { x: 190, y: 154, width: 58, height: 56, rx: 10 }, '#E1F5FE'),
      E('rect', { x: 262, y: 154, width: 44, height: 124, rx: 8 }, '#FFE082'),
      E('rect', { x: 318, y: 154, width: 42, height: 70, rx: 10 }, '#E1F5FE'),
      E('rect', { x: 28, y: 232, width: 234, height: 20 }, '#FF7043'),
      E('circle', { cx: 358, cy: 264, r: 10 }, '#FFF59D'),
      wheel(100, 304, 34), wheel(304, 304, 34)
    ]) },
    { id: 'truck', name: 'Big Truck', shapes: flat([
      sky(), cloud(150, 40), road(),
      E('rect', { x: 26, y: 138, width: 226, height: 146, rx: 14 }, '#42A5F5'),
      E('rect', { x: 50, y: 160, width: 178, height: 22, rx: 8 }, '#90CAF9'),
      E('path', { d: 'M256 188 L256 284 L372 284 L372 232 L336 188 Z' }, '#66BB6A'),
      E('polygon', { points: '272,200 328,200 354,232 272,232' }, '#E1F5FE'),
      E('rect', { x: 360, y: 256, width: 22, height: 26, rx: 6 }, '#FFCA28'),
      wheel(86, 296, 32), wheel(196, 296, 32), wheel(318, 296, 32)
    ]) }
  ];

  var houses = [
    { id: 'house', name: 'Cosy House', shapes: flat([
      sky(), sun(), cloud(30, 60), ground(),
      E('rect', { x: 248, y: 96, width: 32, height: 64 }, '#8D6E63'),
      E('rect', { x: 92, y: 190, width: 216, height: 150 }, '#FFCC80'),
      E('polygon', { points: '70,196 200,84 330,196' }, '#E57373'),
      E('rect', { x: 174, y: 258, width: 52, height: 82, rx: 8 }, '#8D6E63'),
      E('circle', { cx: 214, cy: 302, r: 5 }, '!#FFEE58'),
      E('rect', { x: 110, y: 214, width: 50, height: 46, rx: 6 }, '#81D4FA'),
      E('rect', { x: 240, y: 214, width: 50, height: 46, rx: 6 }, '#81D4FA'),
      line('M135 214 V260 M110 237 H160 M265 214 V260 M240 237 H290', 4),
      E('circle', { cx: 50, cy: 340, r: 14 }, '#F06292'), E('circle', { cx: 350, cy: 344, r: 14 }, '#BA68C8'),
      E('circle', { cx: 80, cy: 356, r: 12 }, '#FFD54F')
    ]) },
    { id: 'castle', name: 'Princess Castle', shapes: flat([
      sky('#E1F5FE'), cloud(160, 30), ground('#AED581', 336),
      line('M95 70 V34 M305 70 V34', 4),
      E('polygon', { points: '95,34 124,44 95,56' }, '#FF7043'), E('polygon', { points: '305,34 334,44 305,56' }, '#FF7043'),
      E('rect', { x: 60, y: 150, width: 70, height: 190 }, '#CE93D8'),
      E('rect', { x: 270, y: 150, width: 70, height: 190 }, '#CE93D8'),
      E('polygon', { points: '50,156 95,70 140,156' }, '#7E57C2'),
      E('polygon', { points: '260,156 305,70 350,156' }, '#7E57C2'),
      E('rect', { x: 126, y: 178, width: 30, height: 26 }, '#E1BEE7'),
      E('rect', { x: 185, y: 178, width: 30, height: 26 }, '#E1BEE7'),
      E('rect', { x: 244, y: 178, width: 30, height: 26 }, '#E1BEE7'),
      E('rect', { x: 120, y: 200, width: 160, height: 140 }, '#E1BEE7'),
      E('path', { d: 'M168 340 L168 282 Q200 244 232 282 L232 340 Z' }, '#8D6E63'),
      E('circle', { cx: 95, cy: 212, r: 13 }, '#FFF59D'), E('circle', { cx: 305, cy: 212, r: 13 }, '#FFF59D')
    ]) },
    { id: 'igloo', name: 'Snowy Igloo', shapes: flat([
      sky('#90CAF9'), ground('#F5F5F5', 316),
      E('circle', { cx: 60, cy: 60, r: 8 }, '#FFFFFF'), E('circle', { cx: 300, cy: 50, r: 6 }, '#FFFFFF'),
      E('circle', { cx: 360, cy: 120, r: 7 }, '#FFFFFF'), E('circle', { cx: 120, cy: 120, r: 5 }, '#FFFFFF'),
      E('path', { d: 'M70 330 A130 130 0 0 1 330 330 Z' }, '#E3F2FD'),
      line('M82 290 H318 M104 250 H296 M146 215 H254 M150 290 V250 M250 290 V250 M200 250 V215 M115 330 V290 M285 330 V290', 4),
      E('path', { d: 'M162 330 L162 290 A38 38 0 0 1 238 290 L238 330 Z' }, '#546E7A')
    ]) }
  ];

  var WORDS = ['Apple', 'Ball', 'Cat', 'Duck', 'Egg', 'Fish', 'Goat', 'Hat', 'Igloo', 'Jam', 'Kite', 'Lion', 'Moon',
    'Nest', 'Owl', 'Pig', 'Queen', 'Rain', 'Sun', 'Tree', 'Umbrella', 'Van', 'Whale', 'Xylophone', 'Yo-yo', 'Zebra'];
  var LETTER_BG = ['#FF8A80', '#FFB74D', '#FFD54F', '#AED581', '#4DB6AC', '#4FC3F7', '#9575CD', '#F06292'];
  var LETTER_FG = ['#FFFFFF', '#FFFFFF', '#5B4B8A', '#FFFFFF', '#FFFFFF', '#FFFFFF', '#FFFFFF', '#FFFFFF'];
  var alphabets = WORDS.map(function (w, i) {
    var L = String.fromCharCode(65 + i), bg = LETTER_BG[i % LETTER_BG.length], fg = LETTER_FG[i % LETTER_FG.length];
    var font = { 'font-family': "'Arial Rounded MT Bold','Trebuchet MS',Verdana,sans-serif", 'font-weight': 900, 'text-anchor': 'middle' };
    return { id: 'letter-' + L.toLowerCase(), name: L + ' is for ' + w, letter: true, shapes: [
      E('rect', { x: 0, y: 0, width: 400, height: 400 }, bg),
      E('circle', { cx: 46, cy: 50, r: 18, stroke: 'none', opacity: 0.35 }, '!#FFFFFF'),
      E('circle', { cx: 356, cy: 72, r: 26, stroke: 'none', opacity: 0.3 }, '!#FFFFFF'),
      E('circle', { cx: 60, cy: 330, r: 22, stroke: 'none', opacity: 0.3 }, '!#FFFFFF'),
      E('circle', { cx: 350, cy: 300, r: 14, stroke: 'none', opacity: 0.35 }, '!#FFFFFF'),
      E('rect', { x: 40, y: 300, width: 320, height: 76, rx: 38, stroke: 'none', opacity: 0.92 }, '!#FFFFFF'),
      E('text', Object.assign({ x: 200, y: 250, 'font-size': 230, 'stroke-width': 8, 'paint-order': 'stroke' }, font), '!' + fg, L + L.toLowerCase()),
      E('text', Object.assign({ x: 200, y: 352, 'font-size': w.length > 7 ? 40 : 50, stroke: 'none' }, font), '!#5B4B8A', w)
    ] };
  });

  function esc(s) { return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;'); }

  /** Builds an SVG string. mode: 'color' (finished picture) or 'paint' (white regions). */
  function toSvg(pic, mode, fills) {
    var paintIndex = 0;
    var body = pic.shapes.map(function (s) {
      var tag = s[0], a = s[1], fill = s[2], text = s[3];
      var attrs = { stroke: INK, 'stroke-width': 5, 'stroke-linejoin': 'round', 'stroke-linecap': 'round' };
      var paintable = typeof fill === 'string' && fill.charAt(0) !== '!';
      if (fill == null) attrs.fill = 'none';
      else if (!paintable) attrs.fill = fill.slice(1);
      else {
        var idx = paintIndex++;
        attrs.fill = mode === 'paint' ? ((fills && fills[idx]) || '#FFFFFF') : fill;
        attrs['data-r'] = idx;
        if (mode === 'paint') attrs['class'] = 'r';
      }
      for (var k in a) attrs[k] = a[k];
      var str = '<' + tag;
      for (var key in attrs) str += ' ' + key + '="' + esc(attrs[key]) + '"';
      return text != null ? str + '>' + esc(text) + '</' + tag + '>' : str + '/>';
    }).join('');
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400" width="400" height="400">' + body + '</svg>';
  }

  window.TLArt = { animals: animals, cars: cars, houses: houses, alphabets: alphabets, toSvg: toSvg, INK: INK };
})();
