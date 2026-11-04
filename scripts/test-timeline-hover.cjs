const fs = require('fs');
const vm = require('vm');
const assert = require('assert');
const source = fs.readFileSync('composeApp/src/desktopMain/resources/player-ui/controls.js', 'utf8');
const prefix = source.slice(0, source.indexOf('const positionLabel'));
const start = source.indexOf('seek.addEventListener("pointermove"');
const end = source.indexOf('volumeSlider.addEventListener', start);
assert(start > 0 && end > start, 'timeline pointer handlers must exist');
let now = 1, nextId = 1;
const timers = new Map(), events = [], handlers = {};
const element = () => ({ hidden: false, style: {}, removeAttribute() {}, replaceChildren() {} });
const nodes = new Map();
const seek = element();
seek.getBoundingClientRect = () => ({ left: 0, width: 100 });
seek.addEventListener = (name, callback) => { handlers[name] = callback; };
nodes.set('seek', seek);
const sandbox = {
  document: { getElementById: id => { if (!nodes.has(id)) nodes.set(id, element()); return nodes.get(id); } },
  state: { timelinePreviewEnabled: true, durationMs: 100000, isInPip: false, isLoading: false },
  activeModal: null, isScrubbing: false, performance: { now: () => now },
  formatTime: time => String(time), send: (name, value) => events.push([name, value]),
};
sandbox.window = {
  setTimeout: (callback, delay) => { const id = nextId++; timers.set(id, { callback, at: now + delay }); return id; },
  clearTimeout: id => timers.delete(id),
};
vm.createContext(sandbox);
vm.runInContext(prefix + '\n' + source.slice(start, end), sandbox);
function advance(ms) {
  const until = now + ms;
  while (true) {
    const next = [...timers.entries()].filter(([, v]) => v.at <= until).sort((a,b) => a[1].at-b[1].at)[0];
    if (!next) break;
    timers.delete(next[0]); now = next[1].at; next[1].callback();
  }
  now = until;
}
handlers.pointermove({ clientX: 50 });
advance(130);
assert.deepStrictEqual(events[0], ['timelinePreview', 50000]);
advance(620);
assert(events.some(e => e[0] === 'timelineFilmstrip' && e[1] === 50000), 'stationary hover should request adjacent frames');
handlers.pointermove({ clientX: 70 });
handlers.pointerleave();
const count = events.length;
advance(1000);
assert.strictEqual(events.length, count, 'leaving the timeline should cancel pending work');
assert.strictEqual(events.at(-1)[0], 'timelinePreviewClear');
handlers.pointermove({ clientX: 20 });
advance(400);
handlers.pointermove({ clientX: 80 });
advance(350);
assert(!events.some(e => e[0] === 'timelineFilmstrip' && e[1] === 20000), 'moving should cancel the old filmstrip');
advance(500);
assert(events.some(e => e[0] === 'timelineFilmstrip' && e[1] === 80000), 'the new stationary target should load');
console.log('Timeline hover: preview, delayed filmstrip and cancellation passed');
