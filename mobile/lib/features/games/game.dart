import 'dart:ui';

import '../../core/config/app_config.dart';

/// Virtual game from the catalogue service.
class Game {
  Game({
    required this.gameId,
    required this.title,
    required this.description,
    required this.category,
    required this.ageLabel,
    required this.themes,
    required this.thumbnailPath,
    required this.themeColor,
  });

  final String gameId;
  final String title;
  final String description;
  final String category;
  final String ageLabel;
  final List<String> themes;
  final String thumbnailPath;
  final Color themeColor;

  String get thumbnailUrl => '${AppConfig.apiBaseUrl}$thumbnailPath';

  /// Rendered in the WebView at `{gamesBaseUrl}/{gameId}`.
  Uri get launchUri => Uri.parse('${AppConfig.gamesBaseUrl}/$gameId');

  factory Game.fromJson(Map<String, dynamic> j) => Game(
        gameId: j['gameId'] as String,
        title: j['title'] as String,
        description: j['description'] as String? ?? '',
        category: j['category'] as String? ?? '',
        ageLabel: j['ageLabel'] as String? ?? '',
        themes: (j['themes'] as List?)?.cast<String>() ?? const [],
        thumbnailPath: j['thumbnailPath'] as String? ?? '',
        themeColor: _parseColor(j['themeColor'] as String?),
      );

  static Color _parseColor(String? hex) {
    final h = (hex ?? '#FFB74D').replaceFirst('#', '');
    return Color(int.tryParse(h.length == 6 ? 'FF$h' : h, radix: 16) ?? 0xFFFFB74D);
  }
}
