import 'dart:io';

import 'package:google_sign_in/google_sign_in.dart';
import 'package:sign_in_with_apple/sign_in_with_apple.dart';

import '../../core/config/app_config.dart';

class SignInCancelled implements Exception {}

class SocialCredential {
  SocialCredential(this.idToken, this.displayName);
  final String idToken;
  final String? displayName;
}

/// Obtains provider identity tokens; the backend verifies them.
class SocialSignIn {
  bool _googleReady = false;

  bool get appleAvailable => Platform.isIOS;

  Future<SocialCredential> google() async {
    final g = GoogleSignIn.instance;
    if (!_googleReady) {
      await g.initialize(
        clientId: Platform.isIOS && AppConfig.googleIosClientId.isNotEmpty ? AppConfig.googleIosClientId : null,
        serverClientId: AppConfig.googleServerClientId.isNotEmpty ? AppConfig.googleServerClientId : null,
      );
      _googleReady = true;
    }
    try {
      final account = await g.authenticate(scopeHint: const ['email']);
      final idToken = account.authentication.idToken;
      if (idToken == null) throw StateError('Google did not return an ID token (check GOOGLE_SERVER_CLIENT_ID)');
      return SocialCredential(idToken, account.displayName);
    } on GoogleSignInException catch (e) {
      if (e.code == GoogleSignInExceptionCode.canceled) throw SignInCancelled();
      rethrow;
    }
  }

  Future<SocialCredential> apple() async {
    try {
      final c = await SignInWithApple.getAppleIDCredential(
        scopes: const [AppleIDAuthorizationScopes.email, AppleIDAuthorizationScopes.fullName],
      );
      final token = c.identityToken;
      if (token == null) throw StateError('Apple did not return an identity token');
      final name = [c.givenName, c.familyName].whereType<String>().join(' ').trim();
      return SocialCredential(token, name.isEmpty ? null : name);
    } on SignInWithAppleAuthorizationException catch (e) {
      if (e.code == AuthorizationErrorCode.canceled) throw SignInCancelled();
      rethrow;
    }
  }

  Future<void> signOut() async {
    if (_googleReady) {
      try {
        await GoogleSignIn.instance.signOut();
      } catch (_) {}
    }
  }
}
