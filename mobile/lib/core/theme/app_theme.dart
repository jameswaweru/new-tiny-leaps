import 'package:flutter/material.dart';

/// Soft, baby-friendly palette taken from the Tiny Leaps logo.
class TLColors {
  TLColors._();
  static const ink = Color(0xFF4A3F6B);
  static const inkSoft = Color(0xFF7A6F99);
  static const cream = Color(0xFFFFF7F0);
  static const blush = Color(0xFFFFE1EC);
  static const peach = Color(0xFFFF8A65);
  static const mint = Color(0xFF7FD6A0);
  static const sky = Color(0xFF9AD8FF);
  static const sun = Color(0xFFFFD166);
  static const lavender = Color(0xFFB39DDB);
  static const white = Colors.white;
}

class TLTheme {
  TLTheme._();
  static const fontFamily = 'Fredoka';

  static ThemeData light() {
    final base = ThemeData(
      useMaterial3: true,
      fontFamily: fontFamily,
      colorScheme: ColorScheme.fromSeed(
        seedColor: TLColors.peach,
        primary: TLColors.peach,
        secondary: TLColors.mint,
        surface: TLColors.cream,
        onSurface: TLColors.ink,
      ),
      scaffoldBackgroundColor: TLColors.cream,
    );
    return base.copyWith(
      textTheme: base.textTheme.apply(bodyColor: TLColors.ink, displayColor: TLColors.ink).copyWith(
            headlineLarge: const TextStyle(fontSize: 32, fontWeight: FontWeight.w700, color: TLColors.ink, height: 1.15),
            headlineMedium: const TextStyle(fontSize: 26, fontWeight: FontWeight.w700, color: TLColors.ink),
            titleLarge: const TextStyle(fontSize: 20, fontWeight: FontWeight.w600, color: TLColors.ink),
            titleMedium: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600, color: TLColors.ink),
            bodyLarge: const TextStyle(fontSize: 17, fontWeight: FontWeight.w400, color: TLColors.inkSoft, height: 1.4),
            bodyMedium: const TextStyle(fontSize: 15, fontWeight: FontWeight.w400, color: TLColors.inkSoft),
            labelLarge: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
          ).apply(fontFamily: fontFamily),
      snackBarTheme: const SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        backgroundColor: TLColors.ink,
        contentTextStyle: TextStyle(fontFamily: fontFamily, fontSize: 15, color: Colors.white),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.all(Radius.circular(16))),
      ),
    );
  }
}
