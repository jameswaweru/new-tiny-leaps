import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../../core/network/api_response.dart';
import '../../core/services.dart';
import '../../core/theme/app_theme.dart';
import '../../widgets/common.dart';
import '../auth/auth_repository.dart';
import '../auth/sign_in_screen.dart';
import '../splash/splash_screen.dart';
import 'game.dart';
import 'game_player_screen.dart';

/// Games catalogue in a grid, loaded page by page from the catalogue service.
class GamesCatalogueScreen extends StatefulWidget {
  const GamesCatalogueScreen({super.key});

  @override
  State<GamesCatalogueScreen> createState() => _GamesCatalogueScreenState();
}

class _GamesCatalogueScreenState extends State<GamesCatalogueScreen> {
  final _games = <Game>[];
  final _scroll = ScrollController();
  bool _loading = true;
  bool _loadingMore = false;
  String? _error;
  Pagination? _page;

  @override
  void initState() {
    super.initState();
    _load(reset: true);
    _scroll.addListener(() {
      if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 300) _loadMore();
    });
  }

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  Future<void> _load({bool reset = false}) async {
    setState(() {
      _loading = reset && _games.isEmpty;
      _error = null;
    });
    try {
      final page = await services.catalogue.fetchGames(page: 0);
      if (!mounted) return;
      setState(() {
        _games
          ..clear()
          ..addAll(page.games);
        _page = page.pagination;
      });
    } on SessionExpiredException {
      _toSignIn();
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.customerMessage);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _loadMore() async {
    final p = _page;
    if (_loadingMore || p == null || !p.hasNext) return;
    setState(() => _loadingMore = true);
    try {
      final next = await services.catalogue.fetchGames(page: p.page + 1);
      if (!mounted) return;
      setState(() {
        _games.addAll(next.games);
        _page = next.pagination;
      });
    } catch (_) {
      // keep what we have; user can pull to refresh
    } finally {
      if (mounted) setState(() => _loadingMore = false);
    }
  }

  void _toSignIn() {
    if (!mounted) return;
    Navigator.of(context).pushAndRemoveUntil(fadeRoute(const SignInScreen(sessionExpired: true)), (_) => false);
  }

  void _open(Game g) => Navigator.of(context).push(MaterialPageRoute(builder: (_) => GamePlayerScreen(game: g)));

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return SafeArea(
      bottom: false,
      child: RefreshIndicator(
        color: TLColors.peach,
        onRefresh: () => _load(),
        child: CustomScrollView(
          controller: _scroll,
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
              sliver: SliverToBoxAdapter(
                child: Row(
                  children: [
                    SvgPicture.asset('assets/branding/mark.svg', width: 52, height: 52),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text("Let's play!", style: text.headlineMedium),
                          Text('No ads. Just fun.', style: text.bodyMedium),
                        ],
                      ),
                    ),
                    const _AccountButton(),
                  ],
                ),
              ),
            ),
            if (_loading)
              const SliverFillRemaining(hasScrollBody: false, child: Center(child: _HoppingLoader()))
            else if (_error != null && _games.isEmpty)
              SliverFillRemaining(hasScrollBody: false, child: _ErrorState(message: _error!, onRetry: () => _load(reset: true)))
            else if (_games.isEmpty)
              const SliverFillRemaining(
                  hasScrollBody: false, child: Center(child: Text('New games are on their way!', style: TextStyle(fontSize: 18))))
            else
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
                sliver: SliverGrid(
                  gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                    maxCrossAxisExtent: 240,
                    mainAxisSpacing: 18,
                    crossAxisSpacing: 18,
                    childAspectRatio: 0.74,
                  ),
                  delegate: SliverChildBuilderDelegate(
                    (_, i) => _GameCard(game: _games[i], onTap: () => _open(_games[i])),
                    childCount: _games.length,
                  ),
                ),
              ),
            if (_loadingMore)
              const SliverToBoxAdapter(
                  child: Padding(padding: EdgeInsets.all(16), child: Center(child: CircularProgressIndicator()))),
          ],
        ),
      ),
    );
  }
}

class _GameCard extends StatelessWidget {
  const _GameCard({required this.game, required this.onTap});
  final Game game;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Bouncy(
      onTap: onTap,
      child: Semantics(
        button: true,
        label: 'Play ${game.title}',
        child: Container(
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(28),
            boxShadow: [BoxShadow(color: TLColors.ink.withValues(alpha: 0.10), offset: const Offset(0, 7))],
          ),
          padding: const EdgeInsets.all(10),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(
                child: Container(
                  decoration: BoxDecoration(
                    color: game.themeColor.withValues(alpha: 0.22),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  padding: const EdgeInsets.all(8),
                  child: SvgPicture.network(
                    game.thumbnailUrl,
                    fit: BoxFit.contain,
                    placeholderBuilder: (_) => Icon(Icons.extension_rounded, size: 56, color: game.themeColor),
                    errorBuilder: (_, _, _) => Icon(Icons.extension_rounded, size: 56, color: game.themeColor),
                  ),
                ),
              ),
              const SizedBox(height: 10),
              Text(game.title,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600, color: TLColors.ink)),
              const SizedBox(height: 6),
              Row(
                children: [
                  _Chip(label: game.ageLabel, color: game.themeColor),
                  const Spacer(),
                  Container(
                    width: 36,
                    height: 36,
                    decoration: BoxDecoration(color: game.themeColor, shape: BoxShape.circle),
                    child: const Icon(Icons.play_arrow_rounded, color: Colors.white, size: 26),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Chip extends StatelessWidget {
  const _Chip({required this.label, required this.color});
  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(color: color.withValues(alpha: 0.18), borderRadius: BorderRadius.circular(12)),
        child: Text(label, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: TLColors.ink)),
      );
}

class _HoppingLoader extends StatefulWidget {
  const _HoppingLoader();

  @override
  State<_HoppingLoader> createState() => _HoppingLoaderState();
}

class _HoppingLoaderState extends State<_HoppingLoader> with SingleTickerProviderStateMixin {
  late final AnimationController _c =
      AnimationController(vsync: this, duration: const Duration(milliseconds: 700))..repeat(reverse: true);

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
        animation: _c,
        builder: (_, child) => Transform.translate(offset: Offset(0, -18 * Curves.easeOut.transform(_c.value)), child: child),
        child: SvgPicture.asset('assets/branding/mark.svg', width: 84),
      );
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.message, required this.onRetry});
  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            SvgPicture.asset('assets/branding/mark.svg', width: 96),
            const SizedBox(height: 18),
            Text(message, textAlign: TextAlign.center, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 22),
            SizedBox(width: 200, child: PillButton(label: 'Try again', onPressed: onRetry)),
          ],
        ),
      );
}

class _AccountButton extends StatelessWidget {
  const _AccountButton();

  @override
  Widget build(BuildContext context) {
    return Bouncy(
      onTap: () => _showAccount(context),
      child: Container(
        width: 48,
        height: 48,
        decoration: BoxDecoration(
          color: Colors.white,
          shape: BoxShape.circle,
          boxShadow: [BoxShadow(color: TLColors.ink.withValues(alpha: 0.12), offset: const Offset(0, 4))],
        ),
        child: const Icon(Icons.face_retouching_natural_rounded, color: TLColors.peach, size: 28),
      ),
    );
  }

  void _showAccount(BuildContext context) {
    final s = services.auth.current;
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(28))),
      builder: (sheet) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 20, 24, 16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(s == null || s.isGuest ? 'Playing as a guest' : (s.displayName ?? s.email ?? 'Parent account'),
                  style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 6),
              Text(
                s == null || s.isGuest
                    ? 'Sign in to keep progress safe and to order toys & games.'
                    : (s.email ?? 'Signed in'),
                style: Theme.of(context).textTheme.bodyMedium,
              ),
              const SizedBox(height: 20),
              if (s == null || s.isGuest)
                PillButton(
                  label: 'Sign in',
                  onPressed: () async {
                    Navigator.of(sheet).pop();
                    final ok = await Navigator.of(context)
                        .push<bool>(MaterialPageRoute(builder: (_) => const SignInScreen(fromHome: true)));
                    if (ok == true && context.mounted) showFriendlySnack(context, 'Your account is ready!');
                  },
                )
              else
                PillButton(
                  label: 'Sign out',
                  color: TLColors.blush,
                  textColor: TLColors.ink,
                  onPressed: () async {
                    Navigator.of(sheet).pop();
                    await services.auth.signOut();
                    if (context.mounted) {
                      Navigator.of(context).pushAndRemoveUntil(fadeRoute(const SplashScreen()), (_) => false);
                    }
                  },
                ),
            ],
          ),
        ),
      ),
    );
  }
}
