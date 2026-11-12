import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:sign_in_with_apple/sign_in_with_apple.dart';

import '../../core/network/api_response.dart';
import '../../core/services.dart';
import '../../core/storage/session.dart';
import '../../core/theme/app_theme.dart';
import '../../widgets/common.dart';
import '../home/home_shell.dart';
import 'social_sign_in.dart';

enum _Busy { none, apple, google, guest }

/// Registration options: Apple (iOS only, on top), Google, or skip.
class SignInScreen extends StatefulWidget {
  const SignInScreen({super.key, this.sessionExpired = false, this.fromHome = false});

  final bool sessionExpired;

  /// Opened from the account sheet (guest upgrading) instead of the onboarding flow.
  final bool fromHome;

  @override
  State<SignInScreen> createState() => _SignInScreenState();
}

class _SignInScreenState extends State<SignInScreen> {
  _Busy _busy = _Busy.none;

  @override
  void initState() {
    super.initState();
    if (widget.sessionExpired) {
      WidgetsBinding.instance.addPostFrameCallback(
          (_) => showFriendlySnack(context, 'Please sign in again to keep your progress safe.'));
    }
  }

  Future<void> _run(_Busy which, Future<Session> Function() action) async {
    if (_busy != _Busy.none) return;
    setState(() => _busy = which);
    try {
      await action();
      if (!mounted) return;
      if (widget.fromHome) {
        Navigator.of(context).pop(true);
      } else {
        Navigator.of(context).pushAndRemoveUntil(fadeRoute(const HomeShell()), (_) => false);
      }
    } on SignInCancelled {
      // user closed the sheet - nothing to say
    } on ApiException catch (e) {
      if (mounted) showFriendlySnack(context, e.customerMessage);
    } catch (_) {
      if (mounted) showFriendlySnack(context, "Sign-in didn't work this time. Please try again.");
    } finally {
      if (mounted) setState(() => _busy = _Busy.none);
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = services.auth;
    final text = Theme.of(context).textTheme;
    return Scaffold(
      appBar: widget.fromHome
          ? AppBar(backgroundColor: Colors.transparent, elevation: 0, foregroundColor: TLColors.ink)
          : null,
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, c) => SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 28),
            child: ConstrainedBox(
              constraints: BoxConstraints(minHeight: c.maxHeight),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const SizedBox(height: 24),
                  const TinyLeapsLogo(size: 150),
                  const SizedBox(height: 18),
                  const Wordmark(fontSize: 36),
                  const SizedBox(height: 26),
                  Text("Save your little one's progress", textAlign: TextAlign.center, style: text.headlineMedium),
                  const SizedBox(height: 10),
                  Text(
                    'Create a free parent account to keep games and favourites safe across devices, and to order from our marketplace.',
                    textAlign: TextAlign.center,
                    style: text.bodyLarge,
                  ),
                  const SizedBox(height: 32),
                  if (auth.social.appleAvailable) ...[
                    SizedBox(
                      height: 60,
                      width: double.infinity,
                      child: SignInWithAppleButton(
                        onPressed: () => _run(_Busy.apple, auth.signInWithApple),
                        borderRadius: const BorderRadius.all(Radius.circular(30)),
                        text: 'Continue with Apple',
                      ),
                    ),
                    const SizedBox(height: 14),
                  ],
                  PillButton(
                    label: 'Continue with Google',
                    color: Colors.white,
                    textColor: TLColors.ink,
                    borderColor: const Color(0xFFE6E0F0),
                    loading: _busy == _Busy.google,
                    leading: SvgPicture.asset('assets/branding/google_g.svg', width: 24, height: 24),
                    onPressed: () => _run(_Busy.google, auth.signInWithGoogle),
                  ),
                  if (!widget.fromHome) ...[
                    const SizedBox(height: 18),
                    TextButton(
                      onPressed: _busy == _Busy.none ? () => _run(_Busy.guest, auth.continueAsGuest) : null,
                      child: _busy == _Busy.guest
                          ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 3))
                          : const Text('Skip for now',
                              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600, color: TLColors.inkSoft)),
                    ),
                  ],
                  const SizedBox(height: 24),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
