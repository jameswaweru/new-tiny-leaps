import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:webview_flutter_android/webview_flutter_android.dart';

import '../../core/network/api_response.dart';
import '../../core/services.dart';
import '../../core/storage/session.dart';
import '../../core/theme/app_theme.dart';
import '../../widgets/common.dart';
import '../auth/auth_repository.dart';
import '../auth/sign_in_screen.dart';
import 'game.dart';

/// Renders `{gamesBaseUrl}/{gameId}` in a locked-down WebView.
///
/// 1. Makes sure the session is valid (refreshing / recovering the guest if needed).
/// 2. Loads the game with the credentials (Bearer token + X-User-UUID) in the request headers;
///    the gateway validates them with auth-service before the game is served.
/// 3. Blocks every navigation outside this game, so no ad or external link can open.
class GamePlayerScreen extends StatefulWidget {
  const GamePlayerScreen({super.key, required this.game});
  final Game game;

  @override
  State<GamePlayerScreen> createState() => _GamePlayerScreenState();
}

class _GamePlayerScreenState extends State<GamePlayerScreen> {
  late final WebViewController _web;
  bool _loading = true;
  String? _error;
  bool _retriedAuth = false;

  @override
  void initState() {
    super.initState();
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
    _web = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(TLColors.cream)
      ..enableZoom(false)
      ..addJavaScriptChannel('TinyLeaps', onMessageReceived: _onGameMessage)
      ..setNavigationDelegate(NavigationDelegate(
        onNavigationRequest: _guardNavigation,
        onPageStarted: (_) => setState(() => _loading = true),
        onPageFinished: (_) {
          _injectSession();
          if (mounted) setState(() => _loading = false);
        },
        onHttpError: _onHttpError,
        onWebResourceError: (e) {
          if (e.isForMainFrame ?? true) _fail("We couldn't open the game. Check your internet and try again.");
        },
      ));
    final platform = _web.platform;
    if (platform is AndroidWebViewController) {
      platform.setMediaPlaybackRequiresUserGesture(false); // game sound effects
    }
    _launch();
  }

  @override
  void dispose() {
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    super.dispose();
  }

  Future<void> _launch({bool forceRefresh = false}) async {
    setState(() {
      _error = null;
      _loading = true;
    });
    try {
      final Session s = await services.auth.validSession(forceRefresh: forceRefresh);
      await _web.loadRequest(widget.game.launchUri, headers: s.authHeaders);
    } on SessionExpiredException {
      if (!mounted) return;
      Navigator.of(context).pushAndRemoveUntil(fadeRoute(const SignInScreen(sessionExpired: true)), (_) => false);
    } on ApiException catch (e) {
      _fail(e.customerMessage);
    }
  }

  /// Only the selected game may load. Everything else (ads, external links, other sites) is blocked.
  NavigationDecision _guardNavigation(NavigationRequest req) {
    final uri = Uri.tryParse(req.url);
    final base = widget.game.launchUri;
    final allowed = uri != null &&
        ((uri.scheme == base.scheme && uri.host == base.host && uri.port == base.port &&
                uri.path.startsWith(base.path)) ||
            uri.scheme == 'about' ||
            uri.scheme == 'data' ||
            uri.scheme == 'blob');
    return allowed ? NavigationDecision.navigate : NavigationDecision.prevent;
  }

  void _onHttpError(HttpResponseError err) {
    final url = err.request?.uri;
    final isMain = url != null && url.path.replaceAll(RegExp(r'/$'), '') == widget.game.launchUri.path;
    if (!isMain) return;
    final status = err.response?.statusCode ?? 0;
    if (status == 401 && !_retriedAuth) {
      _retriedAuth = true; // token rejected by the gateway: refresh once and retry
      _launch(forceRefresh: true);
    } else {
      _fail(status == 401
          ? 'Please go back and tap the game again.'
          : "This game is taking a little nap. Please try again soon.");
    }
  }

  /// Makes the (non-secret) session context available to the game page.
  void _injectSession() {
    final s = services.auth.current;
    if (s == null) return;
    final ctx = jsonEncode({'userUuid': s.userUuid, 'accountType': s.accountType, 'gameId': widget.game.gameId});
    _web.runJavaScript('window.TinyLeapsSession = $ctx; window.dispatchEvent(new Event("tinyleaps:session"));');
  }

  void _onGameMessage(JavaScriptMessage msg) {
    try {
      final data = jsonDecode(msg.message) as Map<String, dynamic>;
      if (data['type'] == 'exit' && mounted) Navigator.of(context).pop();
    } catch (_) {}
  }

  void _fail(String message) {
    if (!mounted) return;
    setState(() {
      _error = message;
      _loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: TLColors.cream,
      body: Stack(
        children: [
          if (_error == null) WebViewWidget(controller: _web),
          if (_loading && _error == null)
            Container(
              color: TLColors.cream,
              alignment: Alignment.center,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  SvgPicture.asset('assets/branding/mark.svg', width: 110),
                  const SizedBox(height: 18),
                  Text('Getting ${widget.game.title} ready...', style: Theme.of(context).textTheme.titleMedium),
                ],
              ),
            ),
          if (_error != null)
            SafeArea(
              child: Center(
                child: Padding(
                  padding: const EdgeInsets.all(32),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      SvgPicture.asset('assets/branding/mark.svg', width: 110),
                      const SizedBox(height: 18),
                      Text(_error!, textAlign: TextAlign.center, style: Theme.of(context).textTheme.titleMedium),
                      const SizedBox(height: 22),
                      SizedBox(
                          width: 220,
                          child: PillButton(
                              label: 'Try again',
                              onPressed: () {
                                _retriedAuth = false;
                                _launch(forceRefresh: true);
                              })),
                      TextButton(
                          onPressed: () => Navigator.of(context).pop(),
                          child: const Text('Back to games', style: TextStyle(fontSize: 17))),
                    ],
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
