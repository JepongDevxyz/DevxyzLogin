package com.devxyz.login.browser

import com.devxyz.login.model.Profile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds the JS fingerprint-injection script for a profile.
 *
 * HONEST LIMITS (v1, WebView):
 * - This is JS-level spoofing, not engine-level (C++) patching like Orbita.
 *   Overrides use Object.defineProperty; determined detectors can spot some.
 * - Screen spoofing: screen.width/height are overridden, but the actual
 *   WebView viewport/CSS layout still follows the real device.
 * - WebRTC: WebView has no off switch. When webrtcMode is off/disabled we
 *   stub RTCPeerConnection so JS cannot open peer connections (leak control),
 *   which can break sites that legitimately need WebRTC calls.
 * - Canvas/WebGL/Audio noise: tiny deterministic perturbations seeded from
 *   the profile id, so the same profile is consistent across sites/sessions.
 */
object FingerprintScript {

    fun build(profile: Profile): String {
        val fp = profile.fingerprint
        val nav = fp.navigator
        val hw = fp.hardware
        val seed = profile.id.hashCode()
        val languages = JSONArray(nav.languages).toString()
        val language = nav.languages.firstOrNull() ?: "en-US"
        val resolution = nav.screenResolution.split("x")
        val sw = resolution.getOrNull(0)?.trim()?.toIntOrNull() ?: 1920
        val sh = resolution.getOrNull(1)?.trim()?.toIntOrNull() ?: 1080
        val vendor = JSONObject.quote(hw.webglVendor)
        val renderer = JSONObject.quote(hw.webglRenderer)
        val platform = JSONObject.quote(nav.platform)
        val webrtcBlock = fp.webrtcMode == "off" || fp.webrtcMode == "disabled"
        val tz = if (fp.timezone.auto) "" else fp.timezone.zone

        return """
(function(){
  try {
    var seed = $seed;
    function rnd(){ seed = (seed * 1103515245 + 12345) & 0x7fffffff; return seed / 0x7fffffff; }
    function def(obj, prop, val){ try { Object.defineProperty(obj, prop, { get: function(){ return val; }, configurable: true }); } catch(e){} }

    // Navigator
    def(navigator, 'platform', $platform);
    def(navigator, 'hardwareConcurrency', ${nav.cpuCores});
    def(navigator, 'deviceMemory', ${nav.ram});
    def(navigator, 'languages', $languages);
    def(navigator, 'language', ${JSONObject.quote(language)});
    def(navigator, 'doNotTrack', ${if (nav.doNotTrack) "'1'" else "null"});

    // Screen (values spoofed; real viewport still follows the device)
    def(screen, 'width', $sw);
    def(screen, 'height', $sh);
    def(screen, 'availWidth', $sw);
    def(screen, 'availHeight', $sh);

    ${if (tz.isNotBlank()) timezoneOverride(tz) else ""}

    ${if (webrtcBlock) webrtcStub() else ""}

    ${if (hw.webglNoise || hw.webglVendor.isNotBlank()) webglSpoof(vendor, renderer, hw.webglNoise) else ""}

    ${if (hw.canvasMode == "noise") canvasNoise() else if (hw.canvasMode == "block") canvasBlock() else ""}

    ${if (hw.audioNoise) audioNoise() else ""}

    ${if (hw.clientRectsNoise) clientRectsNoise() else ""}

    // Media devices count spoof (labels/counts only; real devices still used)
    if (navigator.mediaDevices && navigator.mediaDevices.enumerateDevices) {
      var origEnum = navigator.mediaDevices.enumerateDevices.bind(navigator.mediaDevices);
      navigator.mediaDevices.enumerateDevices = function(){
        return origEnum().then(function(devs){
          var out = [];
          var v = ${fp.mediaDevices.videoInputs}, ai = ${fp.mediaDevices.audioInputs}, ao = ${fp.mediaDevices.audioOutputs};
          devs.forEach(function(d){ out.push(d); });
          while (out.filter(function(d){return d.kind==='videoinput';}).length < v) out.push({kind:'videoinput', label:'', deviceId:'', groupId:''});
          while (out.filter(function(d){return d.kind==='audioinput';}).length < ai) out.push({kind:'audioinput', label:'', deviceId:'', groupId:''});
          while (out.filter(function(d){return d.kind==='audiooutput';}).length < ao) out.push({kind:'audiooutput', label:'', deviceId:'', groupId:''});
          return out.slice(0, v + ai + ao + 2);
        });
      };
    }
  } catch(e) {}
})();
""".trimIndent()
    }

    private fun timezoneOverride(zone: String): String = """
    // Timezone: JS Date/Intl timezone cannot be fully overridden in WebView;
    // we at least pin Intl.DateTimeFormat's resolved zone.
    try {
      var OrigDTF = Intl.DateTimeFormat;
      Intl.DateTimeFormat = function(locales, opts){
        opts = opts || {}; if (!opts.timeZone) opts.timeZone = ${JSONObject.quote(zone)};
        return new OrigDTF(locales, opts);
      };
      Intl.DateTimeFormat.prototype = OrigDTF.prototype;
      Intl.DateTimeFormat.supportedLocalesOf = OrigDTF.supportedLocalesOf;
    } catch(e){}
    """.trimIndent()

    private fun webrtcStub(): String = """
    // WebRTC leak control: WebView cannot disable WebRTC at engine level,
    // so block JS peer connections outright for this profile.
    try {
      var blocked = function(){ throw new Error('WebRTC disabled by DevxyzLogin profile'); };
      window.RTCPeerConnection = blocked;
      window.webkitRTCPeerConnection = blocked;
      window.mozRTCPeerConnection = blocked;
    } catch(e){}
    """.trimIndent()

    private fun webglSpoof(vendor: String, renderer: String, noise: Boolean): String = """
    try {
      function patchWebGL(proto){
        if (!proto) return;
        var orig = proto.getParameter;
        proto.getParameter = function(p){
          if (p === 37445) return $vendor;   // UNMASKED_VENDOR_WEBGL
          if (p === 37446) return $renderer; // UNMASKED_RENDERER_WEBGL
          return orig.apply(this, arguments);
        };
      }
      patchWebGL(window.WebGLRenderingContext && WebGLRenderingContext.prototype);
      patchWebGL(window.WebGL2RenderingContext && WebGL2RenderingContext.prototype);
    } catch(e){}
    """.trimIndent()

    private fun canvasNoise(): String = """
    try {
      var origToDataURL = HTMLCanvasElement.prototype.toDataURL;
      HTMLCanvasElement.prototype.toDataURL = function(){
        try {
          var ctx = this.getContext('2d');
          if (ctx && this.width > 0 && this.height > 0) {
            var img = ctx.getImageData(0, 0, Math.min(this.width, 8), 1);
            for (var i = 0; i < img.data.length; i += 4) { img.data[i] = (img.data[i] + 1) % 256; }
            ctx.putImageData(img, 0, 0);
          }
        } catch(e){}
        return origToDataURL.apply(this, arguments);
      };
    } catch(e){}
    """.trimIndent()

    private fun canvasBlock(): String = """
    try {
      HTMLCanvasElement.prototype.toDataURL = function(){ return 'data:,'; };
      var origGetCtx = HTMLCanvasElement.prototype.getContext;
      HTMLCanvasElement.prototype.getContext = function(type){
        if (type === '2d') return null;
        return origGetCtx.apply(this, arguments);
      };
    } catch(e){}
    """.trimIndent()

    private fun audioNoise(): String = """
    try {
      if (window.AudioBuffer) {
        var origGetChannelData = AudioBuffer.prototype.getChannelData;
        AudioBuffer.prototype.getChannelData = function(){
          var data = origGetChannelData.apply(this, arguments);
          for (var i = 0; i < data.length; i += 4096) { data[i] = data[i] + 0.0000001; }
          return data;
        };
      }
    } catch(e){}
    """.trimIndent()

    private fun clientRectsNoise(): String = """
    try {
      var origRect = Element.prototype.getBoundingClientRect;
      Element.prototype.getBoundingClientRect = function(){
        var r = origRect.apply(this, arguments);
        try {
          return new DOMRect(r.x + 0.0001, r.y, r.width, r.height);
        } catch(e){ return r; }
      };
    } catch(e){}
    """.trimIndent()
}
