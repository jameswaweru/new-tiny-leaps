import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../../core/theme/app_theme.dart';

/// Parents' marketplace for physical toys & games. Placeholder until the marketplace service exists.
class MarketplaceScreen extends StatelessWidget {
  const MarketplaceScreen({super.key});

  static const _categories = [
    ('Puzzles', Icons.extension_rounded, Color(0xFFFFB74D)),
    ('Arts & Crafts', Icons.palette_rounded, Color(0xFF4FC3F7)),
    ('Building Blocks', Icons.view_in_ar_rounded, Color(0xFFBA68C8)),
    ('Board Games', Icons.casino_rounded, Color(0xFF7FD6A0)),
    ('Soft Toys', Icons.pets_rounded, Color(0xFFF48FB1)),
    ('Outdoor Play', Icons.sports_soccer_rounded, Color(0xFFFF8A65)),
  ];

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return SafeArea(
      bottom: false,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
        children: [
          Text('Toys & Games', style: text.headlineMedium),
          Text('For grown-ups', style: text.bodyMedium),
          const SizedBox(height: 18),
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(color: const Color(0xFFFFF1D6), borderRadius: BorderRadius.circular(28)),
            child: Column(
              children: [
                SvgPicture.asset('assets/illustrations/marketplace.svg', height: 190),
                const SizedBox(height: 12),
                Text('Our shop is opening soon!', style: text.titleLarge, textAlign: TextAlign.center),
                const SizedBox(height: 6),
                Text('Hand-picked toys and games, delivered to your door.',
                    style: text.bodyMedium, textAlign: TextAlign.center),
              ],
            ),
          ),
          const SizedBox(height: 22),
          Text("What's coming", style: text.titleLarge),
          const SizedBox(height: 12),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              for (final (label, icon, color) in _categories)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: [BoxShadow(color: TLColors.ink.withValues(alpha: 0.08), offset: const Offset(0, 4))],
                  ),
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    Icon(icon, color: color),
                    const SizedBox(width: 8),
                    Text(label, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: TLColors.ink)),
                  ]),
                ),
            ],
          ),
        ],
      ),
    );
  }
}
