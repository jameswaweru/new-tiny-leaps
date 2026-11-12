/// Authenticated (guest or registered) session issued by auth-service.
class Session {
  Session({
    required this.userUuid,
    required this.accountType,
    required this.accessToken,
    required this.accessTokenExpiresAt,
    required this.refreshToken,
    required this.refreshTokenExpiresAt,
    this.email,
    this.displayName,
  });

  final String userUuid;
  final String accountType; // GUEST | REGISTERED
  final String accessToken;
  final DateTime accessTokenExpiresAt;
  final String refreshToken;
  final DateTime refreshTokenExpiresAt;
  final String? email;
  final String? displayName;

  bool get isGuest => accountType == 'GUEST';

  /// Refresh a little before expiry so a game launch never races the expiry.
  bool get accessExpiringSoon => DateTime.now().isAfter(accessTokenExpiresAt.subtract(const Duration(seconds: 60)));

  /// Credentials sent with every authenticated call and injected into the game WebView.
  Map<String, String> get authHeaders => {
        'Authorization': 'Bearer $accessToken',
        'X-User-UUID': userUuid,
      };

  factory Session.fromJson(Map<String, dynamic> j) => Session(
        userUuid: j['userUuid'] as String,
        accountType: j['accountType'] as String,
        accessToken: j['accessToken'] as String,
        accessTokenExpiresAt: DateTime.parse(j['accessTokenExpiresAt'] as String),
        refreshToken: j['refreshToken'] as String,
        refreshTokenExpiresAt: DateTime.parse(j['refreshTokenExpiresAt'] as String),
        email: j['email'] as String?,
        displayName: j['displayName'] as String?,
      );

  Map<String, dynamic> toJson() => {
        'userUuid': userUuid,
        'accountType': accountType,
        'accessToken': accessToken,
        'accessTokenExpiresAt': accessTokenExpiresAt.toIso8601String(),
        'refreshToken': refreshToken,
        'refreshTokenExpiresAt': refreshTokenExpiresAt.toIso8601String(),
        'email': email,
        'displayName': displayName,
      };
}
