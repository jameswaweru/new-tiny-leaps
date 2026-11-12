import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../../core/services.dart';
import '../../core/theme/app_theme.dart';
import '../../widgets/common.dart';
import '../auth/sign_in_screen.dart';

class _Slide {
  const _Slide(this.asset, this.title, this.body, this.tint);
  final String asset;
  final String title;
  final String body;
  final Color tint;
}

const _slides = [
  _Slide('assets/illustrations/no_ads.svg', 'Play without\ninterruptions',
      'Strictly no ads. Ever. Little ones play happily with no pop-ups and no links that lead somewhere they shouldn\'t go.',
      Color(0xFFFFE1EC)),
  _Slide('assets/illustrations/brain_growth.svg', 'Made for\ngrowing minds',
      'Every game is curated for your child\'s age to help their brain grow through play: puzzles, colours, letters and more.',
      Color(0xFFE3F6EA)),
  _Slide('assets/illustrations/marketplace.svg', 'Toys & games\nmarketplace',
      'Parents can browse hand-picked toys and games in our marketplace and place an order in a few taps.',
      Color(0xFFFFF1D6)),
];

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final _pages = PageController();
  int _index = 0;

  bool get _isLast => _index == _slides.length - 1;

  Future<void> _finish() async {
    await services.store.markOnboardingSeen();
    if (!mounted) return;
    Navigator.of(context).pushReplacement(fadeRoute(const SignInScreen()));
  }

  void _next() {
    if (_isLast) {
      _finish();
    } else {
      _pages.nextPage(duration: const Duration(milliseconds: 420), curve: Curves.easeOutCubic);
    }
  }

  @override
  void dispose() {
    _pages.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      body: AnimatedContainer(
        duration: const Duration(milliseconds: 400),
        color: Color.lerp(_slides[_index].tint, TLColors.cream, 0.55),
        child: SafeArea(
          child: Column(
            children: [
              Align(
                alignment: Alignment.centerRight,
                child: AnimatedOpacity(
                  opacity: _isLast ? 0 : 1,
                  duration: const Duration(milliseconds: 200),
                  child: TextButton(
                    onPressed: _isLast ? null : _finish,
                    child: const Text('Skip', style: TextStyle(fontSize: 17, color: TLColors.inkSoft)),
                  ),
                ),
              ),
              Expanded(
                child: PageView.builder(
                  controller: _pages,
                  itemCount: _slides.length,
                  onPageChanged: (i) => setState(() => _index = i),
                  itemBuilder: (_, i) {
                    final s = _slides[i];
                    return Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 28),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Flexible(child: SvgPicture.asset(s.asset, height: 300)),
                          const SizedBox(height: 28),
                          Text(s.title, textAlign: TextAlign.center, style: text.headlineLarge),
                          const SizedBox(height: 14),
                          Text(s.body, textAlign: TextAlign.center, style: text.bodyLarge),
                        ],
                      ),
                    );
                  },
                ),
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(_slides.length, (i) {
                  final active = i == _index;
                  return AnimatedContainer(
                    duration: const Duration(milliseconds: 250),
                    margin: const EdgeInsets.symmetric(horizontal: 5),
                    width: active ? 28 : 10,
                    height: 10,
                    decoration: BoxDecoration(
                      color: active ? TLColors.peach : TLColors.ink.withValues(alpha: 0.18),
                      borderRadius: BorderRadius.circular(5),
                    ),
                  );
                }),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(28, 28, 28, 24),
                child: PillButton(label: _isLast ? "Let's go!" : 'Next', onPressed: _next),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
