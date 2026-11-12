import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';
import '../games/games_catalogue_screen.dart';
import '../marketplace/marketplace_screen.dart';

/// Two tabs: virtual games (default, for kids) and the toys & games marketplace (for parents).
class HomeShell extends StatefulWidget {
  const HomeShell({super.key});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _tab = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(index: _tab, children: const [GamesCatalogueScreen(), MarketplaceScreen()]),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(16, 0, 16, 12),
        child: Container(
          height: 72,
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(36),
            boxShadow: [BoxShadow(color: TLColors.ink.withValues(alpha: 0.12), blurRadius: 18, offset: const Offset(0, 6))],
          ),
          padding: const EdgeInsets.all(8),
          child: Row(
            children: [
              _NavItem(
                  icon: Icons.videogame_asset_rounded,
                  label: 'Games',
                  color: TLColors.peach,
                  selected: _tab == 0,
                  onTap: () => setState(() => _tab = 0)),
              _NavItem(
                  icon: Icons.storefront_rounded,
                  label: 'Toys & Games',
                  color: const Color(0xFF4FB37A),
                  selected: _tab == 1,
                  onTap: () => setState(() => _tab = 1)),
            ],
          ),
        ),
      ),
    );
  }
}

class _NavItem extends StatelessWidget {
  const _NavItem({required this.icon, required this.label, required this.color, required this.selected, required this.onTap});
  final IconData icon;
  final String label;
  final Color color;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Semantics(
        selected: selected,
        button: true,
        label: label,
        child: GestureDetector(
          onTap: onTap,
          behavior: HitTestBehavior.opaque,
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 250),
            curve: Curves.easeOutCubic,
            decoration: BoxDecoration(
              color: selected ? color.withValues(alpha: 0.16) : Colors.transparent,
              borderRadius: BorderRadius.circular(28),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(icon, size: 30, color: selected ? color : TLColors.inkSoft.withValues(alpha: 0.6)),
                const SizedBox(width: 8),
                Flexible(
                  child: Text(label,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: selected ? color : TLColors.inkSoft.withValues(alpha: 0.7))),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
