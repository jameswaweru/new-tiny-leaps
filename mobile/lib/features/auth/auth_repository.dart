import 'dart:io';

import '../../core/network/api_client.dart';
import '../../core/network/api_response.dart';
import '../../core/storage/session.dart';
import '../../core/storage/session_store.dart';
import 'social_sign_in.dart';

/// Registered user's refresh token is no longer valid: they must sign in again.
class SessionExpiredException implements Exception {}

/// Implements the Tiny Leaps auth flow:
/// first visit -> guest; access expired -> refresh; refresh invalid -> recover guest by device
/// (or a new guest); registered users with an invalid refresh token go back to sign-in.
class AuthRepository implements SessionProvider {
  AuthRepository(this._api, this._store, this._social) {
    _api.sessionProvider = this;
  }

  final ApiClient _api;
  final SessionStore _store;
  final SocialSignIn _social;
  Session? _session;
  Future<Session>? _inflight;

  SocialSignIn get social => _social;
  Session? get current => _session;

  Future<Session?> loadSession() async => _session ??= await _store.readSession();

  @override
  Future<Session> validSession({bool forceRefresh = false}) {
    // Single-flight: concurrent callers share one refresh.
    return _inflight ??= _resolve(forceRefresh).whenComplete(() => _inflight = null);
  }

  Future<Session> _resolve(bool forceRefresh) async {
    final s = await loadSession();
    if (s == null) return continueAsGuest();
    if (!forceRefresh && !s.accessExpiringSoon) return s;
    try {
      final res = await _api.post('/api/v1/auth/refresh', body: {'refreshToken': s.refreshToken}, auth: false);
      return await _save(res);
    } on ApiException catch (e) {
      if (!e.isUnauthorized) rethrow;
      if (s.isGuest) return continueAsGuest(); // recover guest identity by device id (or new guest)
      await _clear();
      throw SessionExpiredException();
    }
  }

  /// Skip registration / first visit / guest recovery.
  Future<Session> continueAsGuest() async {
    final res = await _api.post('/api/v1/auth/guest', auth: false, body: {
      'deviceId': await _store.deviceId(),
      'platform': Platform.operatingSystem,
    });
    return _save(res);
  }

  Future<Session> signInWithGoogle() async {
    final cred = await _social.google();
    return _socialExchange('/api/v1/auth/google', cred);
  }

  Future<Session> signInWithApple() async {
    final cred = await _social.apple();
    return _socialExchange('/api/v1/auth/apple', cred);
  }

  Future<Session> _socialExchange(String path, SocialCredential cred) async {
    final res = await _api.post(path, auth: false, body: {
      'idToken': cred.idToken,
      'deviceId': await _store.deviceId(),
      'platform': Platform.operatingSystem,
      if (cred.displayName != null) 'displayName': cred.displayName,
    });
    return _save(res);
  }

  Future<void> signOut() async {
    final s = _session;
    if (s != null) {
      try {
        await _api.post('/api/v1/auth/logout', body: {'refreshToken': s.refreshToken});
      } catch (_) {
        // best effort; local sign-out still happens
      }
    }
    await _social.signOut();
    await _clear();
  }

  Future<Session> _save(ApiResponse res) async {
    final s = Session.fromJson(res.data as Map<String, dynamic>);
    _session = s;
    await _store.writeSession(s);
    return s;
  }

  Future<void> _clear() async {
    _session = null;
    await _store.clearSession();
  }
}
