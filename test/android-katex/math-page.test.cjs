const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const { test } = require("node:test");
const { runInNewContext } = require("node:vm");
const katex = require("../../apps/android/app/src/katex/assets/katex/katex.min.js");

const html = readFileSync(resolve(__dirname,
    "../../apps/android/app/src/katex/assets/katex/shiroha-math.html"), "utf8");
const script = html.match(/<script>([\s\S]+?)<\/script>/)[1];

test("the bundled KaTeX renders representative formulas from the app", () => {
    const examples = [
        [String.raw`\dfrac{|x|}{x}\dfrac{\sin(x-1)+\sin 1}{x}`, "<mfrac>"],
        [String.raw`\int_0^1 x^2\,dx`, "<mo>∫</mo>"],
        [String.raw`\begin{bmatrix}1&2\\3&4\end{bmatrix}`, "<mtable"],
        [String.raw`\begin{cases}x^2,&x\ge0\\-x,&x<0\end{cases}`, "<mtable"],
        [String.raw`\begin{aligned}x&=1\\ y&=2\end{aligned}`, "<mtable"]
    ];
    for (const [formula, expected] of examples) {
        const rendered = katex.renderToString(formula, { displayMode: true, trust: false });
        assert.ok(rendered.includes(expected), formula);
    }
});

function page() {
    const reports = [];
    const timers = [];
    const fontReady = [];
    const runTimers = () => timers.splice(0).forEach(fn => fn());
    class Element {
        style = {};
        children = [];
        set textContent(value) {
            this.text = value;
            this.children = [];
        }
        appendChild(child) { this.children.push(child); }
        setAttribute(name, value) { this[name] = value; }
        get scrollWidth() { return Math.max(300, parseFloat(this.style.width) || 0); }
        getBoundingClientRect() {
            return { height: parseFloat(this.style.width) < 240 ? 60 : 30 };
        }
    }
    const root = new Element();
    const window = { innerWidth: 400, setTimeout: fn => timers.push(fn) };
    const document = {
        body: new Element(),
        fonts: { ready: { then: fn => fontReady.push(fn) } },
        documentElement: { clientWidth: 400, scrollWidth: 400 },
        getElementById: () => root,
        createElement: () => new Element(),
    };
    const context = {
        window, document,
        katex: { render() {}, ParseError: class ParseError extends Error {} },
        ShirohaMath: { onRenderFinished: (...args) => reports.push(args) },
    };
    runInNewContext(script, context);
    function render(gen, widthDp) {
        return window.ShirohaRender({ gen, widthDp, segments: [{ k: "m", v: "x" }] });
    }
    const runFontReady = () => fontReady.splice(0).forEach(fn => fn());
    return { context, render, reports, runTimers, runFontReady, root, window };
}

test("request width controls wrap and overflow even with a wider scroll viewport", () => {
    const { render, reports } = page();
    assert.equal(render(1, 180), true);
    assert.deepEqual(reports[0], [1, 60, 1, 300]);
    assert.equal(render(2, 360), true);
    assert.deepEqual(reports[1], [2, 30, 0, 360]);
});

test("old font and timer measurements are discarded after a new render", () => {
    const { render, reports, runTimers, runFontReady } = page();
    render(1, 180);
    render(2, 360);
    runTimers();
    assert.deepEqual(reports.map(report => report[0]), [1, 2, 2, 2]);
    runFontReady();
    assert.deepEqual(reports.map(report => report[0]), [1, 2, 2, 2, 2]);
});

test("missing KaTeX or an unexpected DOM exception fails the whole request", () => {
    const { context, render, reports } = page();
    delete context.katex;
    assert.equal(render(1, 180), false);
    assert.equal(reports.length, 0);
    context.katex = { render() {} };
    context.document.createElement = () => { throw new Error("DOM unavailable"); };
    assert.equal(render(2, 180), false);
    assert.equal(reports.length, 0);
});

test("an invalid formula keeps its literal LaTeX while the rest of the block renders", () => {
    const { context, root, window } = page();
    context.katex.render = () => { throw new context.katex.ParseError("invalid formula"); };
    assert.equal(window.ShirohaRender({
        gen: 3,
        widthDp: 200,
        style: { maxLines: 2, fontSizePx: 20, color: "#123456" },
        segments: [
            { k: "t", v: "<img onerror=alert(1)>" },
            { k: "m", v: "\\broken{", r: "$\\broken{$" },
        ],
    }), true);
    assert.equal(root.children[0].text, "<img onerror=alert(1)>");
    assert.equal(root.children[1].text, "$\\broken{$");
    assert.equal(root.children[1]["data-fallback"], "1");
    assert.equal(root.style.webkitLineClamp, "2");
    assert.equal(context.document.body.style.color, "#123456");
});

test("an unexpected exception from the KaTeX renderer fails the whole request", () => {
    const { context, render, reports } = page();
    context.katex.render = () => { throw new TypeError("renderer failed"); };
    assert.equal(render(1, 180), false);
    assert.equal(reports.length, 0);
});

test("a clipped preview does not expose an unreachable horizontal scroll region", () => {
    const { window, reports } = page();
    assert.equal(window.ShirohaRender({
        gen: 4, widthDp: 180, style: { maxLines: 2 }, segments: [{ k: "m", v: "x" }],
    }), true);
    assert.deepEqual(reports[0], [4, 60, 0, 300]);
});
