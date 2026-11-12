import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

import 'session.dart';

/// Tokens live in the platform keystore (Keychain / Android Keystore); simple flags in SharedPreferences.
class SessionStore {
  SessionStore(this._prefs, {FlutterSecureStorage? secure}) : _secure = secure ?? const FlutterSecureStorage();

  final SharedPreferences _prefs;
  final FlutterSecureStorage _secure;

  static const _kSession = 'tl.session';
  static const _kDeviceId = 'tl.deviceId';
  static const _kOnboardingSeen = 'tl.onboardingSeen';

  static Future<SessionStore> create() async => SessionStore(await SharedPreferences.getInstance());

  bool get isFirstLaunch => !(_prefs.getBool(_kOnboardingSeen) ?? false);
  Future<void> markOnboardingSeen() => _prefs.setBool(_kOnboardingSeen, true);

  Future<Session?> readSession() async {
    try {
      final raw = await _secure.read(key: _kSession);
      return raw == null ? null : Session.fromJson(jsonDecode(raw) as Map<String, dynamic>);
    } catch (_) {
      return null; // corrupted / keystore reset -> treat as no session
    }
  }

  Future<void> writeSession(Session s) => _secure.write(key: _kSession, value: jsonEncode(s.toJson()));

  Future<void> clearSession() => _secure.delete(key: _kSession);

  /// Stable per-install device id used for guest recovery. Kept in the keystore
  /// (survives app updates; on iOS also reinstalls) with a prefs fallback.
  Future<String> deviceId() async {
    String? id;
    try {
      id = await _secure.read(key: _kDeviceId);
    } catch (_) {}
    id ??= _prefs.getString(_kDeviceId);
    if (id == null) {
      id = 'tl-${const Uuid().v4()}';
      try {
        await _secure.write(key: _kDeviceId, value: id);
      } catch (_) {}
    }
    await _prefs.setString(_kDeviceId, id);
    return id;
  }
}
