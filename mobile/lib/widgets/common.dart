import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../core/theme/app_theme.dart';

/// Squishy press feedback for kid-sized tap targets.
class Bouncy extends StatefulWidget {
  const Bouncy({super.key, required this.child, required this.onTap, this.enabled = true});
  final Widget child;
  final VoidCallback? onTap;
  final bool enabled;

  @override
  State<Bouncy> createState() => _BouncyState();
}

class _BouncyState extends State<Bouncy> {
  bool _down = false;

  @override
  Widget build(BuildContext context) {
    final active = widget.enabled && widget.onTap != null;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTapDown: active ? (_) => setState(() => _down = true) : null,
      onTapCancel: active ? () => setState(() => _down = false) : null,
      onTapUp: active ? (_) => setState(() => _down = false) : null,
      onTap: active
          ? () {
              HapticFeedback.lightImpact();
              widget.onTap!();
            }
          : null,
      child: AnimatedScale(
        scale: _down ? 0.94 : 1,
        duration: const Duration(milliseconds: 120),
        curve: Curves.easeOut,
        child: widget.child,
      ),
    );
  }
}

/// Big rounded button with a soft "toy" drop shadow.
class PillButton extends StatelessWidget {
  const PillButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.color = TLColors.peach,
    this.textColor = Colors.white,
    this.leading,
    this.loading = false,
    this.borderColor,
  });

  final String label;
  final VoidCallback? onPressed;
  final Color color;
  final Color textColor;
  final Widget? leading;
  final bool loading;
  final Color? borderColor;

  @override
  Widget build(BuildContext context) {
    return Bouncy(
      onTap: loading ? null : onPressed,
      child: Container(
        height: 60,
        decoration: BoxDecoration(
          color: color,
          borderRadius: BorderRadius.circular(30),
          border: borderColor == null ? null : Border.all(color: borderColor!, width: 2),
          boxShadow: [BoxShadow(color: TLColors.ink.withValues(alpha: 0.14), offset: const Offset(0, 6))],
        ),
        alignment: Alignment.center,
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: loading
            ? SizedBox(width: 26, height: 26, child: CircularProgressIndicator(strokeWidth: 3, color: textColor))
            : Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  if (leading != null) ...[leading!, const SizedBox(width: 12)],
                  Flexible(
                    child: Text(label,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(fontSize: 19, fontWeight: FontWeight.w600, color: textColor)),
                  ),
                ],
              ),
      ),
    );
  }
}

class TinyLeapsLogo extends StatelessWidget {
  const TinyLeapsLogo({super.key, this.size = 160});
  final double size;

  @override
  Widget build(BuildContext context) =>
      SvgPicture.asset('assets/branding/logo.svg', width: size, height: size, semanticsLabel: 'Tiny Leaps logo');
}

class Wordmark extends StatelessWidget {
  const Wordmark({super.key, this.fontSize = 40});
  final double fontSize;

  @override
  Widget build(BuildContext context) {
    final style = TextStyle(fontSize: fontSize, fontWeight: FontWeight.w700, letterSpacing: 0.5, height: 1);
    return Text.rich(
      TextSpan(children: [
        TextSpan(text: 'Tiny ', style: style.copyWith(color: TLColors.peach)),
        TextSpan(text: 'Leaps', style: style.copyWith(color: const Color(0xFF4FB37A))),
      ]),
      semanticsLabel: 'Tiny Leaps',
    );
  }
}

Route<T> fadeRoute<T>(Widget page) => PageRouteBuilder<T>(
      transitionDuration: const Duration(milliseconds: 380),
      pageBuilder: (_, _, _) => page,
      transitionsBuilder: (_, anim, _, child) => FadeTransition(
        opacity: CurvedAnimation(parent: anim, curve: Curves.easeOut),
        child: child,
      ),
    );

void showFriendlySnack(BuildContext context, String message) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(message)));
}
