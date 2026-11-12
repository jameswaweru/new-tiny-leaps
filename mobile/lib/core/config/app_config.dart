import 'dart:io';

/// Build-time configuration. Override with --dart-define, e.g.
/// `flutter run --dart-define=API_BASE_URL=http://192.168.1.20:8080`.
class AppConfig {
  AppConfig._();

  static const String _apiBaseUrl = String.fromEnvironment('API_BASE_URL');

  /// Google OAuth *Web* client ID (used as serverClientId so Android returns an ID token).
  static const String googleServerClientId = String.fromEnvironment('GOOGLE_SERVER_CLIENT_ID');

  /// Google OAuth iOS client ID (only needed on iOS).
  static const String googleIosClientId = String.fromEnvironment('GOOGLE_IOS_CLIENT_ID');

  /// Gateway base URL. Defaults: Android emulator -> host loopback, iOS simulator -> localhost.
  static String get apiBaseUrl {
    if (_apiBaseUrl.isNotEmpty) return _apiBaseUrl.replaceAll(RegExp(r'/+$'), '');
    //254 - 192.168.1.37
    //Mtaa
    return Platform.isAndroid ? 'http://10.0.2.2:8080' : 'http://192.168.1.37:8080';
    // return Platform.isAndroid ? 'http://10.0.2.2:8080' : 'http://localhost:8080';
  }

  /// Games are rendered from `{gamesBaseUrl}/{gameId}`.
  static String get gamesBaseUrl => '$apiBaseUrl/games';

  static const Duration requestTimeout = Duration(seconds: 15);
}
