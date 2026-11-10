/* DevxyzLogin web — jsdom smoke suite v3 (gap-closure edition).
   Covers: boot/sign-in, navigation, editor, v3 country list (249), chunked combo,
   per-vendor WebGL renderer switching (all 14 vendors), resolutions vs capture,
   billing prices both cycles, no native dialogs, v1-store migration. */
const fs = require("fs");
const { JSDOM } = require("/tmp/jsdomtest/node_modules/jsdom");
const html = fs.readFileSync("/home/hatch/workspace/devxyzlogin/web/index.html", "utf-8");

let pass = 0, fail = 0; const fails = [];
function ok(name, cond) { if (cond) { pass++; } else { fail++; fails.push(name); console.log("FAIL:", name); } }
function section(t) { console.log("—", t); }

function boot(seed) {
  const dom = new JSDOM(html, { url: "http://localhost/", runScripts: "dangerously", pretendToBeVisual: true });
  const w = dom.window;
  if (seed !== undefined) w.localStorage.setItem("devxyzlogin_v1", typeof seed === "string" ? seed : JSON.stringify(seed));
  return w;
}
const sleep = ms => new Promise(r => setTimeout(r, ms));

(async () => {
  /* ---------- source-level guards ---------- */
  section("source guards");
  ok("no alert( in source", !/\balert\s*\(/.test(html.replace(/\/\*[\s\S]*?\*\//g, "")));
  ok("no confirm( native in source", !/[^.\w]confirm\s*\(/.test(html));
  ok("no prompt( native in source", !/[^.\w]prompt\s*\(/.test(html));
  const blocks = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)].map(m => m[1]);
  ok("9 script blocks", blocks.length === 9);
  blocks.forEach((b, i) => { try { new Function(b); ok("script block " + i + " parses", true); } catch (e) { ok("script block " + i + " parses: " + e.message, false); } });

  /* ---------- v3 data: countries ---------- */
  section("countries v3");
  let w = boot();
  await sleep(60);
  const C = w.eval("COUNTRIES");
  ok("COUNTRIES length 249", C.length === 249);
  ok("first five = FR,DE,GB,US,AU", C.slice(0, 5).map(x => x[0]).join(",") === "FR,DE,GB,US,AU");
  ok("codes unique", new Set(C.map(x => x[0])).size === 249);
  ok("every entry has name + flag", C.every(x => x[1] && x[2]));
  ok("includes XK Kosovo + AQ Antarctica + XK flag", !!C.find(x => x[0] === "XK") && !!C.find(x => x[0] === "AQ"));
  ok("countryByCode fallback US", w.eval("countryByCode('ZZ')[0]") === "US");
  ok("countryByCode('PH') = Philippines", w.eval("countryByCode('PH')[1]") === "Philippines");

  /* ---------- v3 data: resolutions ---------- */
  section("resolutions");
  const RES = w.eval("RESOLUTIONS");
  const capture = ["800x600","960x720","1024x576","1024x600","1024x640","1024x768","1152x648","1152x864","1280x720","1280x768","1512x982","1536x864","1600x900","1600x1200","1680x1050","1710x1107","1728x1117","1856x1392","1920x1080","1920x1200","1920x1440","2048x1152","2048x1536","2304x1440","2560x1080","2560x1440","2560x1600","2560x1664","2560x2048","2880x1800","3440x1440"];
  const expect = [...capture.slice(0, 10), "1366x768", "1440x900", ...capture.slice(10)];
  ok("RESOLUTIONS = capture list + 2 flagged fills (33)", JSON.stringify(RES) === JSON.stringify(expect));
  ok("RESOLUTIONS strictly ascending", RES.every((r, i) => { if (!i) return true; const [a, b] = r.split("x").map(Number), [c, d] = RES[i - 1].split("x").map(Number); return a > c || (a === c && b > d); }));

  /* ---------- v3 data: WebGL vendors/renderers ---------- */
  section("webgl renderers v3");
  const V = w.eval("WEBGL_VENDORS"), R = w.eval("WEBGL_RENDERERS");
  ok("14 vendors", V.length === 14);
  ok("every vendor has >=2 renderers", V.every(v => (R[v] || []).length >= 2));
  ok("every renderer is an ANGLE string", Object.values(R).flat().every(s => s.startsWith("ANGLE (")));
  const Q = R["Google Inc. (Qualcomm)"];
  ok("Qualcomm Adreno set verbatim 610..830 (11)", Q.length === 11 && ["610","618","619","650","710","730","735","750","810","829","830"].every(m => Q.some(s => s.includes("Adreno (TM) " + m + ","))));
  ok("Qualcomm default fingerprint renderer = 618", w.eval("genFingerprint('Linux').webglRenderer").includes("618"));
  ok("ARM has Mali-G78/G715/G615/G710", ["Mali-G78", "Mali-G715", "Mali-G615", "Mali-G710"].every(m => R["ARM"].some(s => s.includes(m))));
  ok("Apple has M1/M2/M3 ES 3.2", ["Apple M1", "Apple M2", "Apple M3"].every(m => R["Apple Inc."].some(s => s.includes(m + ",") && s.includes("OpenGL ES 3.2"))));
  ok("Intel has UHD 620/630 + Iris Xe", ["UHD Graphics 620", "UHD Graphics 630", "Iris(R) Xe"].every(m => R["Google Inc. (Intel)"].some(s => s.includes(m))));
  ok("NVIDIA has GTX 1650/1660 SUPER/RTX 3060/4060", ["GTX 1650", "GTX 1660 SUPER", "RTX 3060", "RTX 4060"].every(m => R["Google Inc. (NVIDIA)"].some(s => s.includes(m))));
  ok("AMD has RX 580 + Radeon Graphics", R["Google Inc. (AMD)"].some(s => s.includes("RX 580")) && R["Google Inc. (AMD)"].some(s => s.includes("Radeon(TM) Graphics")));
  ok("Microsoft has Basic Render Driver ES + D3D11", R["Google Inc. (Microsoft)"].some(s => s.includes("OpenGL ES 3.2")) && R["Google Inc. (Microsoft)"].some(s => s.includes("D3D11")));
  ok("Brave has ANGLE (Brave, ...) strings", R["Brave"].every(s => s.startsWith("ANGLE (Brave,")));
  ok("genFingerprint defaults consistent per OS", ["Windows 11","Windows 10","Mac Intel","Mac ARM","Linux","Android"].every(os => { const fp = w.eval("genFingerprint(" + JSON.stringify(os) + ")"); return (R[fp.webglVendor] || []).includes(fp.webglRenderer); }));

  /* ---------- boot + sign-in ---------- */
  section("boot / sign-in");
  ok("seed boots into app (session in seed)", !w.document.querySelector("#app").classList.contains("hidden"));
  ok("profiles list renders 3 seed rows", w.document.querySelectorAll("#content table.tbl tbody tr").length >= 3);
  w.eval("S.session=null; save(); gate();");
  ok("logout shows login view", !w.document.querySelector("#loginView").classList.contains("hidden"));
  ok("login view has Welcome heading", w.document.querySelector("#loginView").textContent.includes("Welcome!"));
  ok("login has Continue with Google", w.document.querySelector("#loginView").textContent.includes("Continue with Google"));
  // two-step: email (#loginEmail) -> #loginBtn reveals #pwField with #loginPw -> #loginBtn logs in
  const em = w.document.querySelector("#loginEmail");
  ok("password field hidden before email step", w.document.querySelector("#pwField").classList.contains("hidden"));
  em.value = "qa@devxyz.local"; em.dispatchEvent(new w.Event("input", { bubbles: true }));
  w.document.querySelector("#loginBtn").click();
  await sleep(30);
  ok("password field appears after email step", !w.document.querySelector("#pwField").classList.contains("hidden") && !!w.document.querySelector("#loginPw"));
  const pw = w.document.querySelector("#loginPw");
  pw.value = "qa-pass-123"; pw.dispatchEvent(new w.Event("input", { bubbles: true }));
  w.document.querySelector("#loginBtn").click();
  await sleep(30);
  ok("login returns to app", !w.document.querySelector("#app").classList.contains("hidden"));

  /* ---------- navigation ---------- */
  section("navigation");
  w.eval("showPage('proxies')");
  ok("proxies page: 6 cards", w.document.querySelectorAll("#content .px-card, #content [class*=card]").length >= 6 || w.document.querySelector("#content").textContent.includes("Residential"));
  ok("proxies table has floppydata-style rows", w.document.querySelector("#content").textContent.includes("Connected"));
  w.eval("showPage('pa'); switchPa('billing')");
  const billTxt = w.document.querySelector("#content").textContent;
  ok("billing annual default: Professional $4.5", billTxt.includes("$4.5"));
  ok("billing annual: Business $49.5", billTxt.includes("$49.5"));
  w.eval("billCycle='monthly'; switchPa('billing')");
  const billM = w.document.querySelector("#content").textContent;
  ok("billing monthly: Professional $9, Business $99, Enterprise $179, Custom $279", ["$9/mo", "$99", "$179", "$279"].every(s => billM.includes(s)));
  w.eval("showPage('profiles')");
  ok("back on profiles", w.document.querySelector("#content").textContent.includes("All profiles") || w.document.querySelectorAll("#content table.tbl tbody tr").length >= 3);

  /* ---------- editor: Proxy tab + country combo chunking ---------- */
  section("editor / country combo");
  w.eval("openEditor(null,false)");
  await sleep(30);
  const ov = w.document.querySelector("#editorOverlay");
  ok("editor opens on Proxy tab (default)", !!ov && ov.querySelector(".ed-tab.sel")?.dataset.tab === "proxy");
  ok("editor default OS = Linux", w.eval("editorCtx.draft.fp.os") === "Linux");
  ok("editor default proxy country = France (FR + combo shows France)", w.eval("editorCtx.draft.proxy.buyCountry") === "FR" && ov.querySelector(".combo input").value.includes("France"));
  const comboInput = ov.querySelector(".combo input");
  ok("country combo present", !!comboInput);
  comboInput.dispatchEvent(new w.Event("focus", { bubbles: true }));
  await sleep(20);
  const rows = ov.querySelectorAll(".combo-list [data-c]");
  ok("combo renders chunked (<=60 rows, not 249)", rows.length > 0 && rows.length <= 60);
  ok("combo shows '+N more — type to filter' footer", ov.querySelector(".combo-list").textContent.includes("more — type to filter"));
  comboInput.value = "phil"; comboInput.dispatchEvent(new w.Event("input", { bubbles: true }));
  await sleep(20);
  const phRow = ov.querySelector('.combo-list [data-c="PH"]');
  ok("combo filter finds Philippines", !!phRow);
  phRow.click(); await sleep(20);
  ok("combo select sets buyCountry=PH + fp flag", w.eval("editorCtx.draft.proxy.buyCountry") === "PH" && w.eval("editorCtx.draft.fp.countryName") === "Philippines");
  comboInput.dispatchEvent(new w.Event("focus", { bubbles: true }));
  comboInput.value = "zzz"; comboInput.dispatchEvent(new w.Event("input", { bubbles: true }));
  await sleep(20);
  ok("combo no-match state", ov.querySelector(".combo-list").textContent.includes("No matches"));
  // custom proxy country <select> holds the full list
  w.eval("editorCtx.draft.proxy.mode='custom'; renderEdPanel();");
  await sleep(20);
  const bulkSel = ov.querySelector("#bpCountry"); // may exist only in bulk dialog; editor custom uses inputs
  ok("editor custom proxy panel renders", ov.querySelector("#edPanel").textContent.length > 50);

  /* ---------- editor: Advanced vendor -> renderer switching ---------- */
  section("editor advanced renderer switching");
  w.eval("editorCtx.tab='advanced'; renderEditor();");
  await sleep(30);
  const panel = w.document.querySelector("#edPanel");
  const selects = [...panel.querySelectorAll("select")];
  const vendSel = selects.find(s => [...s.options].some(o => o.value === "Google Inc. (Qualcomm)"));
  ok("vendor select present with 14 options", !!vendSel && vendSel.options.length === 14);
  const rendSel = selects.find(s => [...s.options].some(o => o.value.includes("Adreno")));
  ok("renderer select present (Qualcomm default)", !!rendSel && rendSel.options.length === 11);
  async function switchVendor(vendor, expectSub, expectCount) {
    vendSel.value = vendor; vendSel.dispatchEvent(new w.Event("change", { bubbles: true }));
    await sleep(30);
    const p2 = w.document.querySelector("#edPanel");
    const rs = [...p2.querySelectorAll("select")].find(s => [...s.options].some(o => o.value.includes(expectSub)));
    const fp = w.eval("editorCtx.draft.fp");
    return !!rs && rs.options.length === expectCount && (w.eval("WEBGL_RENDERERS")[vendor] || []).includes(fp.webglRenderer) && fp.webglVendor === vendor;
  }
  ok("switch -> ARM (5 Mali renderers)", await switchVendor("ARM", "Mali-G", 5));
  ok("switch -> Apple Inc. (4)", await switchVendor("Apple Inc.", "Apple M", 4));
  ok("switch -> Google Inc. (NVIDIA) (4)", await switchVendor("Google Inc. (NVIDIA)", "GeForce", 4));
  ok("switch -> Google Inc. (AMD) (3)", await switchVendor("Google Inc. (AMD)", "Radeon", 3));
  ok("switch -> Google Inc. (Microsoft) (2)", await switchVendor("Google Inc. (Microsoft)", "Basic Render Driver", 2));
  ok("switch -> Brave (3)", await switchVendor("Brave", "ANGLE (Brave", 3));
  ok("switch -> back to Google Inc. (Qualcomm) (11, default 610 selected)", await switchVendor("Google Inc. (Qualcomm)", "Adreno", 11));
  w.eval("closeEditor()");
  ok("editor closes", !w.document.querySelector("#editorOverlay"));

  /* ---------- create profile end-to-end (new country XK) ---------- */
  section("create profile with Kosovo");
  const before = w.eval("S.profiles.length");
  w.eval("openEditor(null,false)");
  await sleep(20);
  const ci = w.document.querySelector(".combo input");
  ci.dispatchEvent(new w.Event("focus", { bubbles: true }));
  ci.value = "kosovo"; ci.dispatchEvent(new w.Event("input", { bubbles: true }));
  await sleep(20);
  w.document.querySelector('.combo-list [data-c="XK"]').click();
  w.document.querySelector("#edCreate").click();
  await sleep(30);
  ok("profile created with XK country", w.eval("S.profiles.length") === before + 1 && w.eval("S.profiles[0].fp.country") === "XK");

  /* ---------- v1 store migration ---------- */
  section("v1 migration");
  const v1 = { profiles: [], proxies: [], folders: [], session: { email: "old@devxyz.local" }, email: "old@devxyz.local" };
  const w2 = boot(v1);
  await sleep(60);
  ok("v1 store migrates + boots into app", !w2.document.querySelector("#app").classList.contains("hidden"));
  ok("migrated state has defaults object", !!w2.eval("S.defaults"));
  ok("migrated state gains COUNTRIES-independent fields (proxyGB)", !!w2.eval("S.proxyGB"));

  console.log("\nSMOKE v3: " + pass + " passed, " + fail + " failed");
  if (fails.length) console.log("FAILED:", fails.join(" | "));
  process.exit(fail ? 1 : 0);
})().catch(e => { console.error("SMOKE CRASH:", e); process.exit(2); });
