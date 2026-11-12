import '../../core/network/api_client.dart';
import '../../core/network/api_response.dart';
import 'game.dart';

class CataloguePage {
  CataloguePage(this.games, this.pagination);
  final List<Game> games;
  final Pagination? pagination;
}

class CatalogueRepository {
  CatalogueRepository(this._api);
  final ApiClient _api;

  Future<CataloguePage> fetchGames({int page = 0, int size = 20, String? category}) async {
    final res = await _api.get('/api/v1/catalogue/games', query: {
      'page': '$page',
      'size': '$size',
      'category': ?category,
    });
    final games = (res.data as List? ?? const [])
        .map((e) => Game.fromJson(e as Map<String, dynamic>))
        .toList();
    return CataloguePage(games, res.pagination);
  }
}
