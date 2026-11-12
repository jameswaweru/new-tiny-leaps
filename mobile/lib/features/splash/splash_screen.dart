import 'dart:async';

import 'package:flutter/material.dart';

import '../../core/network/api_response.dart';
import '../../core/services.dart';
import '../../core/theme/app_theme.dart';
import '../../widgets/common.dart';
import '../auth/auth_repository.dart';
import '../auth/sign_in_screen.dart';
import '../home/home_shell.dart';
import '../onboarding/onboarding_screen.dart';

/// Shows the logo while deciding where to go:
/// first launch -> onboarding; returning user -> auth checks -> home or sign-in.
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> with SingleTickerProviderStateMixin {
  late final AnimationController _hop =
      AnimationController(vsync: this, duration: const Duration(milliseconds: 900))..repeat(reverse: true);

  @override
  void initState() {
    super.initState();
    _decide();
  }

  @override
  void dispose() {
    _hop.dispose();
    super.dispose();
  }

  Future<void> _decide() async {
    final minimum = Future<void>.delayed(const Duration(milliseconds: 1600));
    final Widget next = await _destination();
    await minimum;
    if (!mounted) return;
    Navigator.of(context).pushReplacement(fadeRoute(next));
  }

  Future<Widget> _destination() async {
    if (services.store.isFirstLaunch) return const OnboardingScreen();

    final existing = await services.auth.loadSession();
    if (existing == null) return const SignInScreen(); // saw onboarding but never chose an option
    try {
      await services.auth.validSession(); // refresh / recover guest when needed
      return const HomeShell();
    } on SessionExpiredException {
      return const SignInScreen(sessionExpired: true);
    } on ApiException catch (e) {
      // Offline or backend down: still open the app; screens show friendly retry states.
      if (e.isNetwork || e.statusCode >= 500) return const HomeShell();
      return const SignInScreen();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFFF3EC),
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            AnimatedBuilder(
              animation: _hop,
              builder: (_, child) => Transform.translate(
                offset: Offset(0, -14 * Curves.easeInOut.transform(_hop.value)),
                child: child,
              ),
              child: const TinyLeapsLogo(size: 190),
            ),
            const SizedBox(height: 28),
            const Wordmark(fontSize: 44),
            const SizedBox(height: 10),
            Text('Play. Grow. Leap!', style: Theme.of(context).textTheme.bodyLarge?.copyWith(color: TLColors.inkSoft)),
          ],
        ),
      ),
    );
  }
}
